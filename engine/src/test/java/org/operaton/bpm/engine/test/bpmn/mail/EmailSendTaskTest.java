/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.engine.test.bpmn.mail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.activation.DataHandler;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import org.junit.jupiter.api.Test;
import org.subethamail.wiser.WiserMessage;

import org.operaton.bpm.engine.test.Deployment;
import org.operaton.commons.utils.CollectionUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Joram Barrez
 * @author Falko Menge
 */
class EmailSendTaskTest extends EmailTestCase {

  @Deployment
  @Test
  void testSimpleTextMail() {
    runtimeService.startProcessInstanceByKey("simpleTextOnly");

    List<WiserMessage> messages = wiser.getMessages();
    assertThat(messages).hasSize(1);

    WiserMessage message = messages.get(0);
    assertEmailSend(message, false, "Hello Kermit!", "This a text only e-mail.", "operaton@localhost",
            Arrays.asList("kermit@operaton.org"), null);
  }

  @Deployment
  @Test
  void testSimpleTextMailMultipleRecipients() {
    runtimeService.startProcessInstanceByKey("simpleTextOnlyMultipleRecipients");

    // 3 recipients == 3 emails in wiser with different receivers
    List<WiserMessage> messages = wiser.getMessages();
    assertThat(messages).hasSize(3);

    // sort recipients for easy assertion
    List<String> recipients = new ArrayList<>();
    for (WiserMessage message : messages) {
      recipients.add(message.getEnvelopeReceiver());
    }
    Collections.sort(recipients);

    assertThat(recipients.get(0)).isEqualTo("fozzie@operaton.org");
    assertThat(recipients.get(1)).isEqualTo("kermit@operaton.org");
    assertThat(recipients.get(2)).isEqualTo("mispiggy@operaton.org");
  }

  @Deployment
  @Test
  void testTextMailExpressions() {

    String sender = "mispiggy@activiti.org";
    String recipient = "fozziebear@activiti.org";
    String recipientName = "Mr. Fozzie";
    String subject = "Fozzie, you should see this!";

    Map<String, Object> vars = new HashMap<>();
    vars.put("sender", sender);
    vars.put("recipient", recipient);
    vars.put("recipientName", recipientName);
    vars.put("subject", subject);

    runtimeService.startProcessInstanceByKey("textMailExpressions", vars);

    List<WiserMessage> messages = wiser.getMessages();
    assertThat(messages).hasSize(1);

    WiserMessage message = messages.get(0);
    assertEmailSend(message, false, subject, "Hello " + recipientName + ", this is an e-mail",
            sender, Arrays.asList(recipient), null);
  }

  @Deployment
  @Test
  void testCcAndBcc() {
    runtimeService.startProcessInstanceByKey("ccAndBcc");

    List<WiserMessage> messages = wiser.getMessages();
    assertEmailSend(messages.get(0), false, "Hello world", "This is the content", "operaton@localhost",
            Arrays.asList("kermit@operaton.org"), Arrays.asList("fozzie@operaton.org"));

    // Bcc is not stored in the header (obviously)
    // so the only way to verify the bcc, is that there are three messages send.
    assertThat(messages).hasSize(3);
  }

  @Deployment
  @Test
  void testHtmlMail() {
    runtimeService.startProcessInstanceByKey("htmlMail", CollectionUtil.singletonMap("gender", "male"));

    List<WiserMessage> messages = wiser.getMessages();
    assertThat(messages).hasSize(1);
    assertEmailSend(messages.get(0), true, "Test", "Mr. <b>Kermit</b>", "operaton@localhost", Arrays.asList("kermit@operaton.org"), null);
  }

  @Deployment
  @Test
  void testSendEmail() throws Exception {

    String from = "ordershipping@activiti.org";
    boolean male = true;
    String recipientName = "John Doe";
    String recipient = "johndoe@alfresco.com";
    Date now = new Date();
    String orderId = "123456";

    Map<String, Object> vars = new HashMap<>();
    vars.put("sender", from);
    vars.put("recipient", recipient);
    vars.put("recipientName", recipientName);
    vars.put("male", male);
    vars.put("now", now);
    vars.put("orderId", orderId);

    runtimeService.startProcessInstanceByKey("sendMailExample", vars);

    List<WiserMessage> messages = wiser.getMessages();
    assertThat(messages).hasSize(1);

    WiserMessage message = messages.get(0);
    MimeMessage mimeMessage = message.getMimeMessage();

    assertThat(mimeMessage.getHeader("Subject", null)).isEqualTo("Your order " + orderId + " has been shipped");
    assertThat(mimeMessage.getHeader("From", null)).isEqualTo(from);
    assertThat(mimeMessage.getHeader("To", null)).contains(recipient);
  }

  // Helper

  private void assertEmailSend(WiserMessage emailMessage, boolean htmlMail, String subject, String message,
          String from, List<String> to, List<String> cc) {
    try {
      MimeMessage mimeMessage = emailMessage.getMimeMessage();

      if (htmlMail) {
        assertThat(mimeMessage.getContentType()).contains("multipart/mixed");
      } else {
        assertThat(mimeMessage.getContentType()).contains("text/plain");
      }

      assertThat(mimeMessage.getHeader("Subject", null)).isEqualTo(subject);
      assertThat(mimeMessage.getHeader("From", null)).isEqualTo(from);
      assertThat(getMessage(mimeMessage)).contains(message);

      for (String t : to) {
        assertThat(mimeMessage.getHeader("To", null)).contains(t);
      }

      if (cc != null) {
        for (String c : cc) {
          assertThat(mimeMessage.getHeader("Cc", null)).contains(c);
        }
      }

    } catch (jakarta.mail.MessagingException | IOException e) {
      fail(e.getMessage());
    }

  }

  protected String getMessage(MimeMessage mimeMessage) throws MessagingException, IOException {
    DataHandler dataHandler = mimeMessage.getDataHandler();
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    dataHandler.writeTo(baos);
    baos.flush();
    return baos.toString();
  }

}

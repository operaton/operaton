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

import java.util.*;

import ch.martinelli.oss.testcontainers.mailpit.Address;
import ch.martinelli.oss.testcontainers.mailpit.Message;
import jakarta.mail.internet.MimeMessage;

import org.junit.jupiter.api.Test;

import org.operaton.bpm.engine.test.Deployment;
import org.operaton.commons.utils.CollectionUtil;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author Joram Barrez
 */
public class EmailServiceTaskTest extends EmailTestCase {

  @Deployment
  @Test
  void testSimpleTextMail() {
    String procId = runtimeService.startProcessInstanceByKey("simpleTextOnly").getId();

    List<Message> receivedEmails = getReceivedEmails();
    assertThat(receivedEmails).hasSize(1);

    String rawMessage = getRawMessage(receivedEmails.get(0));
    assertEmailSend(rawMessage, false, "Hello Kermit!", "This a text only e-mail.", "operaton@localhost",
            Arrays.asList("kermit@operaton.org"), null);
    testRule.assertProcessEnded(procId);
  }

  @Deployment
  @Test
  void testSimpleTextMailMultipleRecipients() {
    runtimeService.startProcessInstanceByKey("simpleTextOnlyMultipleRecipients");

    // in Mailpit we have 1 email (just 1 email was sent by BPMN workflow) with 3 recipients
    List<Message> messages = getReceivedEmails();
    assertThat(messages).hasSize(1);

    // sort recipients for easy assertion
    List<String> recipients = new ArrayList<>();
    for (Address receiver : messages.get(0).to()) {
      recipients.add(receiver.address());
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

    List<Message> messages = getReceivedEmails();
    assertThat(messages).hasSize(1);

    String rawMessage = getRawMessage(messages.get(0));
    assertEmailSend(rawMessage, false, subject, "Hello " + recipientName + ", this is an e-mail",
            sender, Arrays.asList(recipient), null);
  }

  @Deployment
  @Test
  void testCcAndBcc() {
    runtimeService.startProcessInstanceByKey("ccAndBcc");

    List<Message> messages = getReceivedEmails();
    Message emailMsg = messages.get(0);
    String rawMessage = getRawMessage(emailMsg);
    assertEmailSend(rawMessage, false, "Hello world", "This is the content", "operaton@localhost",
            Arrays.asList("kermit@operaton.org"), Arrays.asList("fozzie@operaton.org"));

    // Bcc is not stored in the header (obviously)
    // so the only way to verify the bcc, is that the messae has the bcc field in Mailpit message.
    assertThat(emailMsg.bcc()).hasSize(1);
    assertThat(emailMsg.bcc().get(0).address()).isEqualTo("mispiggy@operaton.org");
  }

  @Deployment
  @Test
  void testHtmlMail() {
    runtimeService.startProcessInstanceByKey("htmlMail", CollectionUtil.singletonMap("gender", "male"));

    List<Message> messages = getReceivedEmails();
    assertThat(messages).hasSize(1);

    String rawMessage = getRawMessage(messages.get(0));
    assertEmailSend(rawMessage, true, "Test", "Mr. <b>Kermit</b>", "operaton@localhost",
            Arrays.asList("kermit@operaton.org"), null);
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

    List<Message> messages = getReceivedEmails();
    assertThat(messages).hasSize(1);

    String rawMessage = getRawMessage(messages.get(0));
    MimeMessage mimeMessage = createMimeMessageFromRawData(rawMessage);

    assertThat(mimeMessage.getHeader("Subject", null)).isEqualTo("Your order " + orderId + " has been shipped");
    assertThat(mimeMessage.getHeader("From", null)).isEqualTo(from);
    assertThat(mimeMessage.getHeader("To", null)).contains(recipient);
  }
}

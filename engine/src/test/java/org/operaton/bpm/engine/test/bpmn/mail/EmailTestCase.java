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

import ch.martinelli.oss.testcontainers.mailpit.MailpitClient;
import ch.martinelli.oss.testcontainers.mailpit.Message;
import jakarta.activation.DataHandler;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;
import ch.martinelli.oss.testcontainers.mailpit.MailpitContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.test.TestLogger;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

import jakarta.mail.internet.MimeMessage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Joram Barrez
 */
@Testcontainers
public abstract class EmailTestCase {

  @RegisterExtension
  protected static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();

  @RegisterExtension
  protected ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  private static final Logger LOG = TestLogger.TEST_LOGGER.getLogger();

  protected ProcessEngineConfigurationImpl processEngineConfiguration;
  protected RuntimeService runtimeService;

  protected MailpitClient smtpClient;

  @Container
  protected MailpitContainer mailpit = new MailpitContainer();

  @BeforeEach
  public void setUp() {
    // Resolve container host + mapped ports
    String host = mailpit.getHost();
    int smtpPort = mailpit.getMappedPort(1025);  // internal SMTP port

    LOG.info("Starting Mailpit SMTP server on {}:{}", host, smtpPort);

    // Configure the process engine to use Mailpit's SMTP host/port
    processEngineConfiguration.setMailServerHost(host);
    processEngineConfiguration.setMailServerPort(smtpPort);

    smtpClient = mailpit.getClient();
  }

  @AfterEach
  public void tearDown() {
    smtpClient.deleteAllMessages();
  }

  /**
   * Helper to fetch received emails for assertions.
   */
  protected List<Message> getReceivedEmails() {
    return smtpClient.getAllMessages();
  }

  protected void assertEmailSend(String rawEmailMessage, boolean htmlMail, String subject, String message,
                                     String from, List<String> to, List<String> cc) {
    try {
      MimeMessage mimeMessage = createMimeMessageFromRawData(rawEmailMessage);

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

    } catch (MessagingException | IOException e) {
      fail(e.getMessage());
    }
  }

  protected MimeMessage createMimeMessageFromRawData(String rawEmailContent)
          throws MessagingException {

    Properties props = System.getProperties();
    Session session = Session.getInstance(props, null);

    InputStream inputStream = new ByteArrayInputStream(rawEmailContent.getBytes());
    return new MimeMessage(session, inputStream);
  }

  protected String getRawMessage(Message msg) {
    return smtpClient.getMessageSource(msg.id());
  }

  private String getMessage(MimeMessage mimeMessage) throws MessagingException, IOException {
    DataHandler dataHandler = mimeMessage.getDataHandler();
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    dataHandler.writeTo(baos);
    baos.flush();
    return baos.toString();
  }

}
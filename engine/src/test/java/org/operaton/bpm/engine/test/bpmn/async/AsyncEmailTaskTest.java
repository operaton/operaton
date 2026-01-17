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
package org.operaton.bpm.engine.test.bpmn.async;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.subethamail.wiser.WiserMessage;

import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.bpmn.mail.EmailServiceTaskTest;
import org.operaton.bpm.engine.test.bpmn.mail.EmailTestCase;

import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 *
 * @author Daniel Meyer
 */
class AsyncEmailTaskTest extends EmailTestCase {

  // copied from org.operaton.bpm.engine.test.bpmn.mail.EmailServiceTaskTest
  @Deployment
  @Test
  void testSimpleTextMail() {
    String procId = runtimeService.startProcessInstanceByKey("simpleTextOnly").getId();

    List<WiserMessage> messages = wiser.getMessages();
    assertThat(messages).isEmpty();

    testRule.waitForJobExecutorToProcessAllJobs(5000L);

    messages = wiser.getMessages();
    assertThat(messages).hasSize(1);

    WiserMessage message = messages.get(0);
    EmailServiceTaskTest.assertEmailSend(message, false, "Hello Kermit!", "This a text only e-mail.", "operaton@localhost",
            List.of("kermit@operaton.org"), null);
    testRule.assertProcessEnded(procId);
  }

  // copied from org.operaton.bpm.engine.test.bpmn.mail.EmailSendTaskTest
  @Deployment
  @Test
  void testSimpleTextMailSendTask() {
    runtimeService.startProcessInstanceByKey("simpleTextOnly");

    List<WiserMessage> messages = wiser.getMessages();
    assertThat(messages).isEmpty();

    testRule.waitForJobExecutorToProcessAllJobs(5000L);

    messages = wiser.getMessages();
    assertThat(messages).hasSize(1);

    WiserMessage message = messages.get(0);
    EmailServiceTaskTest.assertEmailSend(message, false, "Hello Kermit!", "This a text only e-mail.", "operaton@localhost",
            List.of("kermit@operaton.org"), null);
  }

}

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
package org.operaton.bpm.engine.test.jobexecutor;

import java.util.Date;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.persistence.entity.MessageEntity;
import org.operaton.bpm.engine.impl.persistence.entity.TimerEntity;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

/**
 * @author Tom Baeyens
 */
class JobExecutorTestCase {
  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  protected ProcessEngineConfigurationImpl processEngineConfiguration;
  
  protected TweetHandler tweetHandler = new TweetHandler();

  @BeforeEach
  void setUp() {
    processEngineConfiguration.getJobHandlers().put(tweetHandler.getType(), tweetHandler);
  }

  @AfterEach
  void tearDown() {
    processEngineConfiguration.getJobHandlers().remove(tweetHandler.getType());
  }

  protected MessageEntity createTweetMessage(String msg) {
    MessageEntity message = new MessageEntity();
    message.setJobHandlerType("tweet");
    message.setJobHandlerConfigurationRaw(msg);
    return message;
  }

  protected TimerEntity createTweetTimer(String msg, Date duedate) {
    TimerEntity timer = new TimerEntity();
    timer.setJobHandlerType("tweet");
    timer.setJobHandlerConfigurationRaw(msg);
    timer.setDuedate(duedate);
    return timer;
  }

}

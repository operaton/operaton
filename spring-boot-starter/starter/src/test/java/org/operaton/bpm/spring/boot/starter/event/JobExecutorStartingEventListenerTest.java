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
package org.operaton.bpm.spring.boot.starter.event;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import org.operaton.bpm.engine.impl.jobexecutor.JobExecutor;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobExecutorStartingEventListenerTest {

  @Mock
  private JobExecutor jobExecutor;

  @InjectMocks
  private JobExecutorStartingEventListener jobExecutorStartingEventListener;

  @Test
  void handleProcessApplicationStartedEventTest() {
    JobExecutorStartingEventListener spy = Mockito.spy(jobExecutorStartingEventListener);
    spy.handleProcessApplicationStartedEvent(null);
    verify(spy).activate();
  }

  @Test
  void activateIfNotStartedTest() {
    when(jobExecutor.isActive()).thenReturn(false);
    jobExecutorStartingEventListener.activate();
    verify(jobExecutor).start();
  }

  @Test
  void doNotActivateIfAlreadyStartedTest() {
    when(jobExecutor.isActive()).thenReturn(true);
    jobExecutorStartingEventListener.activate();
    verify(jobExecutor, times(0)).start();
  }
}

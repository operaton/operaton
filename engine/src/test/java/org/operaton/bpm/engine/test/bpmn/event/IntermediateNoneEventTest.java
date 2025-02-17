/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.engine.test.bpmn.event;

import static org.assertj.core.api.Assertions.assertThat;

import org.operaton.bpm.engine.delegate.DelegateExecution;
import org.operaton.bpm.engine.delegate.ExecutionListener;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.util.PluggableProcessEngineTest;
import org.junit.Test;

public class IntermediateNoneEventTest extends PluggableProcessEngineTest {
  
  private static boolean listenerExcecuted = false;
  
  public static class MyExecutionListener implements ExecutionListener {
    @Override
    public void notify(DelegateExecution execution) throws Exception {
      listenerExcecuted = true;
    }    
  }

  @Deployment
  @Test
  public void testIntermediateNoneTimerEvent() {
    assertThat(listenerExcecuted).isFalse();    
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("intermediateNoneEventExample");
    testRule.assertProcessEnded(pi.getProcessInstanceId());
    assertThat(listenerExcecuted).isTrue();    
  }


}
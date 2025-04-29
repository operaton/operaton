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
package org.operaton.bpm.engine.cdi.test.impl.context;

import static org.assertj.core.api.Assertions.assertThat;

import org.operaton.bpm.engine.cdi.test.CdiProcessEngineTestCase;
import org.operaton.bpm.engine.cdi.test.impl.beans.ProcessScopedMessageBean;
import org.operaton.bpm.engine.test.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.operaton.bpm.engine.test.util.JobExecutorHelper;

/**
 * 
 * @author Daniel Meyer
 */
@RunWith(Arquillian.class)
public class ThreadContextAssociationTest extends CdiProcessEngineTestCase {
  
  @Test
  @Deployment
  public void testBusinessProcessScopedWithJobExecutor() {
    String pid = runtimeService.startProcessInstanceByKey("processkey").getId();

    JobExecutorHelper.waitForJobExecutorToProcessAllJobs(processEngineConfiguration,5000L, 25L);
        
    assertThat(managementService.createJobQuery().singleResult()).isNull();
    
    ProcessScopedMessageBean messageBean = (ProcessScopedMessageBean) runtimeService.getVariable(pid, "processScopedMessageBean");
    assertThat(messageBean.getMessage()).isEqualTo("Greetings from Berlin");
    
    runtimeService.signal(pid);
    
  }

}

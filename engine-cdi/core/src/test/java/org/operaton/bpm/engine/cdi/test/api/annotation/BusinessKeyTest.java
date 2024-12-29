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
package org.operaton.bpm.engine.cdi.test.api.annotation;

import org.operaton.bpm.engine.cdi.BusinessProcess;
import org.operaton.bpm.engine.cdi.impl.util.ProgrammaticBeanLookup;
import org.operaton.bpm.engine.cdi.test.CdiProcessEngineTestCase;
import org.operaton.bpm.engine.test.Deployment;

import org.jboss.arquillian.junit.Arquillian;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;

/**
 * 
 * @author Daniel Meyer
 */
@RunWith(Arquillian.class)
class BusinessKeyTest extends CdiProcessEngineTestCase {

  @Test
  @Deployment
  void businessKeyInjectable() {
    String businessKey = "Activiti";
    String pid = runtimeService.startProcessInstanceByKey("keyOfTheProcess", businessKey).getId();
    getBeanInstance(BusinessProcess.class).associateExecutionById(pid);

    // assert that now the businessKey-Bean can be looked up:
    assertThat(ProgrammaticBeanLookup.lookup("businessKey")).isEqualTo(businessKey);
    
  } 
}

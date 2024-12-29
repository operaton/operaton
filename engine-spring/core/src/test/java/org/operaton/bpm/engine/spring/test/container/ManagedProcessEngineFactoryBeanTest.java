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
package org.operaton.bpm.engine.spring.test.container;
import static org.assertj.core.api.Assertions.assertThat;


import org.junit.jupiter.api.Test;
import org.operaton.bpm.BpmPlatform;
import org.operaton.bpm.engine.spring.container.ManagedProcessEngineFactoryBean;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * <p>Testcase for {@link ManagedProcessEngineFactoryBean}</p>
 * 
 * @author Daniel Meyer
 *
 */
class ManagedProcessEngineFactoryBeanTest {

  @Test
  void processApplicationDeployment() {

    // initially, no process engine is registered:
    assertThat(BpmPlatform.getDefaultProcessEngine()).isNull();
    assertThat(BpmPlatform.getProcessEngineService().getProcessEngines().size()).isEqualTo(0);
    
    // start spring application context
    AbstractApplicationContext applicationContext = new ClassPathXmlApplicationContext("org/operaton/bpm/engine/spring/test/container/ManagedProcessEngineFactoryBean-context.xml");
    applicationContext.start();

    // assert that now the process engine is registered:
    assertThat(BpmPlatform.getDefaultProcessEngine()).isNotNull();      
    
    // close the spring application context
    applicationContext.close();

    // after closing the application context, the process engine is gone
    assertThat(BpmPlatform.getDefaultProcessEngine()).isNull();
    assertThat(BpmPlatform.getProcessEngineService().getProcessEngines().size()).isEqualTo(0);
    
  }
    
}

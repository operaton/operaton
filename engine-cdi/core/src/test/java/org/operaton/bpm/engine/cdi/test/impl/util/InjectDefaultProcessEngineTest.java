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
package org.operaton.bpm.engine.cdi.test.impl.util;

import org.operaton.bpm.BpmPlatform;
import org.operaton.bpm.container.RuntimeContainerDelegate;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.cdi.impl.util.ProgrammaticBeanLookup;
import org.operaton.bpm.engine.cdi.test.CdiProcessEngineTestCase;
import org.operaton.bpm.engine.cdi.test.impl.beans.InjectedProcessEngineBean;
import org.operaton.bpm.engine.impl.test.TestHelper;

import org.jboss.arquillian.junit5.ArquillianExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Christopher Zell <christopher.zell@camunda.com>
 */
@ExtendWith(ArquillianExtension.class)
class InjectDefaultProcessEngineTest extends CdiProcessEngineTestCase {

  protected ProcessEngine defaultProcessEngine = null;
  protected ProcessEngine processEngine = null;

  @BeforeEach
  void init() {
    processEngine = TestHelper.getProcessEngine("activiti.cfg.xml");
    defaultProcessEngine = BpmPlatform.getProcessEngineService().getDefaultProcessEngine();

    if (defaultProcessEngine != null) {
      RuntimeContainerDelegate.INSTANCE.get().unregisterProcessEngine(defaultProcessEngine);
    }

    RuntimeContainerDelegate.INSTANCE.get().registerProcessEngine(processEngine);
  }

  @AfterEach
  @Override
  public void tearDownCdiProcessEngineTestCase() {
    RuntimeContainerDelegate.INSTANCE.get().unregisterProcessEngine(processEngine);

    if (defaultProcessEngine != null) {
      RuntimeContainerDelegate.INSTANCE.get().registerProcessEngine(defaultProcessEngine);
    }
  }

  @Test
  void processEngineInject() {
    //given only default engine exist

    //when TestClass is created
    InjectedProcessEngineBean testClass = ProgrammaticBeanLookup.lookup(InjectedProcessEngineBean.class);
    assertThat(testClass).isNotNull();

    //then default engine is injected
    assertThat(testClass.processEngine.getName()).isEqualTo("default");
    assertThat(testClass.processEngine.getProcessEngineConfiguration().getJdbcUrl()
        .contains("default-process-engine")).isTrue();
  }
}

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
package org.operaton.bpm.engine.cdi.test.impl.util;

import org.junit.jupiter.api.Test;

import org.operaton.bpm.BpmPlatform;
import org.operaton.bpm.container.RuntimeContainerDelegate;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.cdi.impl.util.ProgrammaticBeanLookup;
import org.operaton.bpm.engine.cdi.test.CdiProcessEngineTestCase;
import org.operaton.bpm.engine.cdi.test.impl.beans.InjectedProcessEngineBean;
import org.operaton.bpm.engine.impl.test.TestHelper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Christopher Zell <christopher.zell@camunda.com>
 * <p>
 * With the JUnit 5 extension we temporarily replace the default engine inside the test to mimic the old rule,
 * ensuring CDI injections still see the engine provided by the suite-level extension afterwards.
 */
class InjectDefaultProcessEngineTest extends CdiProcessEngineTestCase {

  @Test
  void testProcessEngineInject() {
    ProcessEngine previousDefault = BpmPlatform.getProcessEngineService().getDefaultProcessEngine();
    ProcessEngine defaultEngine = TestHelper.getProcessEngine("activiti.cfg.xml");

    try {
      if (previousDefault != null) {
        RuntimeContainerDelegate.INSTANCE.get().unregisterProcessEngine(previousDefault);
      }

      RuntimeContainerDelegate.INSTANCE.get().registerProcessEngine(defaultEngine);

      InjectedProcessEngineBean testClass = ProgrammaticBeanLookup.lookup(InjectedProcessEngineBean.class);
      assertThat(testClass).isNotNull();

      assertThat(testClass.processEngine.getName()).isEqualTo("default");
      assertThat(testClass.processEngine.getProcessEngineConfiguration().getJdbcUrl()).contains("default-process-engine");
    } finally {
      RuntimeContainerDelegate.INSTANCE.get().unregisterProcessEngine(defaultEngine);
      if (previousDefault != null) {
        RuntimeContainerDelegate.INSTANCE.get().registerProcessEngine(previousDefault);
      }
    }
  }
}

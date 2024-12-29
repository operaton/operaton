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
package org.operaton.bpm.engine.cdi.test.jsf;

import org.operaton.bpm.engine.cdi.compat.FoxTaskForm;
import org.operaton.bpm.engine.cdi.compat.OperatonTaskForm;
import org.operaton.bpm.engine.cdi.jsf.TaskForm;
import org.operaton.bpm.engine.cdi.test.CdiProcessEngineTestCase;

import javax.enterprise.inject.spi.Bean;
import java.util.Set;

import org.jboss.arquillian.junit5.ArquillianExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Daniel Meyer
 */
@ExtendWith(ArquillianExtension.class)
class TaskFormTest extends CdiProcessEngineTestCase {

  @Test
  void taskFormInjectable() {

    Set<Bean<?>> taskForm = beanManager.getBeans(TaskForm.class);
    Assertions.assertDoesNotThrow(() -> {
      Bean<? extends Object> bean = beanManager.resolve(taskForm);
      assertNotNull(bean);
    }, "Injection of TaskForm is ambiguous.");

    Set<Bean<?>> foxTaskForm = beanManager.getBeans(FoxTaskForm.class);
    Assertions.assertDoesNotThrow(() -> {
      Bean<? extends Object> bean = beanManager.resolve(foxTaskForm);
      assertNotNull(bean);
    }, "Injection of FoxTaskForm is ambiguous.");

    Set<Bean<?>> operatonTaskForm = beanManager.getBeans(OperatonTaskForm.class);
    Assertions.assertDoesNotThrow(() -> {
      Bean<? extends Object> bean = beanManager.resolve(operatonTaskForm);
      assertNotNull(bean);
    }, "Injection of OperatonTaskForm is ambiguous.");

  }

}

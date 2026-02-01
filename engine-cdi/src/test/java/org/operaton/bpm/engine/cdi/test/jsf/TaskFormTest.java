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
package org.operaton.bpm.engine.cdi.test.jsf;

import java.util.Set;
import jakarta.enterprise.inject.AmbiguousResolutionException;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;

import org.junit.jupiter.api.Test;

import org.operaton.bpm.engine.cdi.compat.FoxTaskForm;
import org.operaton.bpm.engine.cdi.compat.OperatonTaskForm;
import org.operaton.bpm.engine.cdi.jsf.TaskForm;
import org.operaton.bpm.engine.cdi.test.CdiProcessEngineTestCase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * @author Daniel Meyer
 */
class TaskFormTest extends CdiProcessEngineTestCase {

  @Test
  void testTaskFormInjectable() {

    BeanManager beanManager = getBeanManager();

    // given
    Set<Bean<?>> taskForm = beanManager.getBeans(TaskForm.class);

    // when/then
    assertThatCode(() -> {
      Bean<? extends Object> bean = beanManager.resolve(taskForm);
      assertThat(bean).isNotNull();
    }).doesNotThrowAnyException();

    // given
    Set<Bean<?>> foxTaskForm = beanManager.getBeans(FoxTaskForm.class);

    // when/then
    assertThatCode(() -> {
      Bean<? extends Object> bean = beanManager.resolve(foxTaskForm);
      assertThat(bean).isNotNull();
    }).doesNotThrowAnyException();

    // given
    Set<Bean<?>> operatonTaskForm = beanManager.getBeans(OperatonTaskForm.class);

    // when/then
    assertThatCode(() -> {
      Bean<? extends Object> bean = beanManager.resolve(operatonTaskForm);
      assertThat(bean).isNotNull();
    }).doesNotThrowAnyException();

  }

}

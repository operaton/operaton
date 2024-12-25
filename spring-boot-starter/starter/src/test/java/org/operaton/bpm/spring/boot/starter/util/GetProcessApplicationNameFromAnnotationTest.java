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
package org.operaton.bpm.spring.boot.starter.util;

import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.spring.boot.starter.annotation.EnableProcessApplication;
import org.operaton.bpm.spring.boot.starter.util.GetProcessApplicationNameFromAnnotation.AnnotatedBean;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GetProcessApplicationNameFromAnnotationTest {

  @EnableProcessApplication("withNameApplication")
  public static class WithName {
  }

  @EnableProcessApplication
  public static class NoName {
  }

  private final ApplicationContext applicationContext = mock(ApplicationContext.class);
  private final GetProcessApplicationNameFromAnnotation function = GetProcessApplicationNameFromAnnotation.processApplicationNameFromAnnotation(applicationContext);

  @Test
  public void getBean_WithName() throws Exception {
    WithName w = new WithName();
    when(applicationContext.getBeansWithAnnotation(EnableProcessApplication.class)).thenReturn(Variables.putValue("withName", w));
    assertThat(GetProcessApplicationNameFromAnnotation.getAnnotatedBean.apply(applicationContext).get()).isEqualTo(AnnotatedBean.of("withName", w));
  }

  @Test
  public void getBean_NoName() throws Exception {
    NoName n = new NoName();
    when(applicationContext.getBeansWithAnnotation(EnableProcessApplication.class)).thenReturn(Variables.putValue("noName", n));
    assertThat(GetProcessApplicationNameFromAnnotation.getAnnotatedBean.apply(applicationContext).get()).isEqualTo(AnnotatedBean.of("noName", n.getClass().getAnnotation(EnableProcessApplication.class)));
  }


  @Test
  public void getAnnotation() {

    assertThat(WithName.class.getAnnotation(EnableProcessApplication.class)).isNotNull();
    assertThat(NoName.class.getAnnotation(EnableProcessApplication.class)).isNotNull();
  }

  @Test
  public void findProcessEngineNameForValue() throws Exception {
    when(applicationContext.getBeansWithAnnotation(EnableProcessApplication.class)).thenReturn(Variables.putValue("withName", new WithName()));
    assertThat(GetProcessApplicationNameFromAnnotation.getProcessApplicationName.apply(applicationContext).get()).isEqualTo("withNameApplication");
  }

  @Test
  public void returnEmptyWhenNoNameIsGiven() throws Exception {
    when(applicationContext.getBeansWithAnnotation(EnableProcessApplication.class)).thenReturn(Variables.putValue("noName", new NoName()));
    assertThat(GetProcessApplicationNameFromAnnotation.getProcessApplicationName.apply(applicationContext).get()).isEqualTo("noName");
  }

  @Test
  public void getProcessApplicationNameFromContext_valuePresent() {
    assumeAnnotatedBeans(Variables.putValue("app", new WithName()));

    assertThat(function.apply(Optional.empty())).isEqualTo(Optional.of("withNameApplication"));
  }

  @Test
  public void getProcessApplicationNameFromContext_beanName() {
    assumeAnnotatedBeans(Variables.putValue("app2", new NoName()));

    assertThat(function.apply(Optional.empty())).isEqualTo(Optional.of("app2"));
  }

  @Test
  public void getProcessApplicationNameFromContext_annotationNotPresent() {
    assumeAnnotatedBeans(Variables.createVariables());

    assertThat(function.apply(Optional.empty())).isEqualTo(Optional.empty());
  }

  @Test
  public void getProcessApplicationNameFromContext_annotationNotPresent_fallbackProperty() {
    assumeAnnotatedBeans(Variables.createVariables());

    assertThat(function.apply(Optional.of("foo"))).isEqualTo(Optional.of("foo"));
  }

  private void assumeAnnotatedBeans(VariableMap beans) {
    when(applicationContext.getBeansWithAnnotation(EnableProcessApplication.class)).thenReturn(beans);
  }
}

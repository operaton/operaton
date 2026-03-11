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
package org.operaton.bpm.application.impl.context;

import java.util.concurrent.Callable;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.operaton.bpm.application.InvocationContext;
import org.operaton.bpm.application.ProcessApplicationContext;
import org.operaton.bpm.application.ProcessApplicationReference;
import org.operaton.bpm.application.impl.embedded.TestApplicationWithoutEngine;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.delegate.BaseDelegateExecution;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;

import static org.operaton.bpm.application.ProcessApplicationContext.withProcessApplicationContext;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Thorben Lindhauer
 *
 */
@ExtendWith(ProcessEngineExtension.class)
class ProcessApplicationContextTest {

  protected TestApplicationWithoutEngine pa;
  ProcessEngine processEngine;

  @BeforeEach
  void setUp() {
    pa = new TestApplicationWithoutEngine();
    pa.deploy();
  }

  @AfterEach
  void tearDown() {
    pa.undeploy();
  }

  @Test
  void testSetPAContextByName() throws Exception {
    assertThat(Context.getCurrentProcessApplication()).isNull();

    try {
      ProcessApplicationContext.setCurrentProcessApplication(pa.getName());

      assertThat(pa).isEqualTo(getCurrentContextApplication().getProcessApplication());
    } finally {
      ProcessApplicationContext.clear();
    }

    assertThat(Context.getCurrentProcessApplication()).isNull();
  }

  @Test
  void testExecutionInPAContextByName() throws Exception {
    assertThat(Context.getCurrentProcessApplication()).isNull();

    ProcessApplicationReference contextPA = withProcessApplicationContext(this::getCurrentContextApplication, pa.getName());

    assertThat(pa).isEqualTo(contextPA.getProcessApplication());

    assertThat(Context.getCurrentProcessApplication()).isNull();
  }

  @Test
  void testSetPAContextByReference() throws Exception {
    assertThat(Context.getCurrentProcessApplication()).isNull();

    try {
      ProcessApplicationContext.setCurrentProcessApplication(pa.getReference());

      assertThat(pa).isEqualTo(getCurrentContextApplication().getProcessApplication());
    } finally {
      ProcessApplicationContext.clear();
    }

    assertThat(Context.getCurrentProcessApplication()).isNull();
  }

  @Test
  void testExecutionInPAContextByReference() throws Exception {
    assertThat(Context.getCurrentProcessApplication()).isNull();

    ProcessApplicationReference contextPA = withProcessApplicationContext(this::getCurrentContextApplication, pa.getReference());

    assertThat(pa).isEqualTo(contextPA.getProcessApplication());

    assertThat(Context.getCurrentProcessApplication()).isNull();
  }

  @Test
  void testSetPAContextByRawPA() throws Exception {
    assertThat(Context.getCurrentProcessApplication()).isNull();

    try {
      ProcessApplicationContext.setCurrentProcessApplication(pa);

      assertThat(getCurrentContextApplication().getProcessApplication()).isEqualTo(pa);
    } finally {
      ProcessApplicationContext.clear();
    }

    assertThat(Context.getCurrentProcessApplication()).isNull();
  }

  @Test
  void testExecutionInPAContextByRawPA() throws Exception {
    assertThat(Context.getCurrentProcessApplication()).isNull();

    ProcessApplicationReference contextPA = withProcessApplicationContext(this::getCurrentContextApplication, pa);

    assertThat(pa).isEqualTo(contextPA.getProcessApplication());

    assertThat(Context.getCurrentProcessApplication()).isNull();
  }

  @Test
  void testCannotSetUnregisteredProcessApplicationName() {
    String nonExistingName = pa.getName() + pa.getName();

    try {
      ProcessApplicationContext.setCurrentProcessApplication(nonExistingName);

      assertThatThrownBy(this::getCurrentContextApplication)
          .isInstanceOf(ProcessEngineException.class)
          .hasMessageContaining("A process application with name '%s' is not registered".formatted(nonExistingName));

    } finally {
      ProcessApplicationContext.clear();
    }
  }

  @Test
  void testCannotExecuteInUnregisteredPaContext() {
    String nonExistingName = pa.getName() + pa.getName();
    Callable<Void> callable = () -> {
      getCurrentContextApplication();
      return null;
    };

    assertThatThrownBy(() -> withProcessApplicationContext(callable, nonExistingName))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("A process application with name '%s' is not registered".formatted(nonExistingName));
  }

  @SuppressWarnings("unchecked")
  @Test
  void testExecuteWithInvocationContext() throws Exception {
    // given a process application which extends the default one
    // - using a spy for verify the invocations
    TestApplicationWithoutEngine processApplication = spy(pa);
    ProcessApplicationReference processApplicationReference = mock(ProcessApplicationReference.class);
    when(processApplicationReference.getProcessApplication()).thenReturn(processApplication);

    // when execute with context
    InvocationContext invocationContext = new InvocationContext(mock(BaseDelegateExecution.class));
    Context.executeWithinProcessApplication(mock(Callable.class), processApplicationReference, invocationContext);

    // then the execute method should be invoked with context
    verify(processApplication).execute(any(Callable.class), eq(invocationContext));
    // and forward to call to the default execute method
    verify(processApplication).execute(any(Callable.class));
  }

  protected ProcessApplicationReference getCurrentContextApplication() {
    ProcessEngineConfigurationImpl engineConfiguration = (ProcessEngineConfigurationImpl) processEngine.getProcessEngineConfiguration();
    return engineConfiguration.getCommandExecutorTxRequired().execute(commandContext -> Context.getCurrentProcessApplication());
  }

}

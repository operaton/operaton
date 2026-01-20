/*
 * Copyright 2026 the Operaton contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.engine.cdi.impl.annotation;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import jakarta.interceptor.InvocationContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.cdi.BusinessProcess;
import org.operaton.bpm.engine.cdi.annotation.ProcessVariable;
import org.operaton.bpm.engine.cdi.annotation.StartProcess;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
class StartProcessInterceptorTest {

  @BeforeEach
  void resetPrivateFieldAccessibility() throws Exception{
    Field f = TestBean.class.getDeclaredField("privateField");
    // best-effort: set accessibility to false so tests start from a known state
    f.setAccessible(false);
  }

  @Test
  void extractVariables_shouldIncludeOnlyAnnotatedFields() throws Exception {
    StartProcessInterceptor interceptor = new StartProcessInterceptor();

    InvocationContext ctx = mock(InvocationContext.class);
    TestBean bean = new TestBean();

    Method m = TestBean.class.getMethod("dummyMethod");
    when(ctx.getMethod()).thenReturn(m);
    when(ctx.getTarget()).thenReturn(bean);

    Map<String, Object> vars = interceptor.extractVariables(ctx);

    assertThat(vars)
        .hasSize(4)
        .containsEntry("var1", "value1")
        .containsEntry("customName", 42)
        .containsEntry("privateName", "hidden")
        .containsEntry("nullVar", null)
        .doesNotContainKey("notAnnotated");
  }

  @Test
  void invoke_shouldStartProcessWithKeyAndVariables_andReturnProceedResult() throws Exception {
    StartProcessInterceptor interceptor = new StartProcessInterceptor();

    BusinessProcess bp = mock(BusinessProcess.class);
    // package-private field, test is in same package, so we can assign direkt
    interceptor.businessProcess = bp;

    InvocationContext ctx = mock(InvocationContext.class);
    TestBean bean = new TestBean();

    Method m = TestBean.class.getMethod("startProcessMethod");
    when(ctx.getMethod()).thenReturn(m);
    when(ctx.getTarget()).thenReturn(bean);
    when(ctx.proceed()).thenReturn("proceedResult");

    Object result = interceptor.invoke(ctx);

    var captor = ArgumentCaptor.forClass(Map.class);
    verify(bp).startProcessByKey(eq("myProcess"), captor.capture());

    Map<String, Object> passedVars = captor.getValue();
    assertThat(passedVars)
        .containsEntry("var1", "value1")
        .containsEntry("customName", 42)
        .containsEntry("nullVar", null);

    assertThat(result).isEqualTo("proceedResult");
  }

  @Test
  void invoke_shouldRethrowCause_whenProceedThrowsInvocationTargetExceptionWithExceptionCause() throws Exception {
    StartProcessInterceptor interceptor = new StartProcessInterceptor();

    interceptor.businessProcess = mock(BusinessProcess.class);

    InvocationContext ctx = mock(InvocationContext.class);
    TestBean bean = new TestBean();

    Method m = TestBean.class.getMethod("startProcessMethod");
    when(ctx.getMethod()).thenReturn(m);
    when(ctx.getTarget()).thenReturn(bean);
    when(ctx.proceed()).thenThrow(new java.lang.reflect.InvocationTargetException(new IllegalArgumentException("bad")));

    assertThatThrownBy(() -> interceptor.invoke(ctx))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("bad");
  }

  @Test
  void invoke_shouldRethrowInvocationTargetException_whenProceedThrowsInvocationTargetExceptionWithErrorCause() throws Exception {
    StartProcessInterceptor interceptor = new StartProcessInterceptor();

    interceptor.businessProcess = mock(BusinessProcess.class);

    InvocationContext ctx = mock(InvocationContext.class);
    TestBean bean = new TestBean();

    Method m = TestBean.class.getMethod("startProcessMethod");
    when(ctx.getMethod()).thenReturn(m);
    when(ctx.getTarget()).thenReturn(bean);
    when(ctx.proceed()).thenThrow(new java.lang.reflect.InvocationTargetException(new AssertionError("serious")));

    assertThatThrownBy(() -> interceptor.invoke(ctx))
      .isInstanceOf(java.lang.reflect.InvocationTargetException.class)
      .hasCauseInstanceOf(AssertionError.class);
  }

  @Test
  void invoke_shouldWrapExceptionInProcessEngineException_whenProceedThrowsCheckedException() throws Exception {
    StartProcessInterceptor interceptor = new StartProcessInterceptor();

    interceptor.businessProcess = mock(BusinessProcess.class);

    InvocationContext ctx = mock(InvocationContext.class);
    TestBean bean = new TestBean();

    Method m = TestBean.class.getMethod("startProcessMethod");
    when(ctx.getMethod()).thenReturn(m);
    when(ctx.getTarget()).thenReturn(bean);
    Exception cause = new Exception("boom");
    when(ctx.proceed()).thenThrow(cause);

    assertThatThrownBy(() -> interceptor.invoke(ctx))
      .isInstanceOf(ProcessEngineException.class)
      .hasCause(cause)
      .hasMessageContaining("Error while starting process using @StartProcess on method");
  }

  @SuppressWarnings("unused")
  static class TestBean {

    @ProcessVariable
    String var1 = "value1";

    @ProcessVariable("customName")
    int var2 = 42;

    @ProcessVariable("privateName")
    private String privateField = "hidden";

    String notAnnotated = "skip";

    @ProcessVariable("nullVar")
    String nullVar;

    @StartProcess("myProcess")
    public void startProcessMethod() {
      // ...existing code...
    }

    public void dummyMethod() {
      // ...existing code...
    }
  }
}

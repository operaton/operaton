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
package org.operaton.bpm.engine.impl.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.operaton.bpm.engine.ProcessEngineException;

@SuppressWarnings("unused")
class ReflectUtilTest {

  static class DummyNoArg {
  }

  static class DummyWithArgs {
    private final String text;
    private final Integer num;

    DummyWithArgs(String text, Integer num) {
      this.text = text;
      this.num = num;
    }

    String getText() {
      return text;
    }

    Integer getNum() {
      return num;
    }
  }

  public static class PrivateFieldClass {
    private final String secret = "init";
  }

  public static class SetterClass {
    private String name;

    public void setName(String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }
  }

  public static class AmbiguousSetter {
    public void setVal(String s) {
      // do nothing
    }
    public void setVal(Integer i) {
      // do nothing
    }
  }

  public static class PrivateMethodClass {
    @SuppressWarnings("unused")
    private String concat(String a, Integer b) {
      return a + b;
    }
  }

  @Test
  void shouldInstantiateByClass() {
    DummyNoArg instance = ReflectUtil.instantiate(DummyNoArg.class);
    assertThat(instance).isInstanceOf(DummyNoArg.class);
  }

  @Test
  void shouldInstantiateByClassNameWithArgs() {
    String fqcn = DummyWithArgs.class.getName();
    Object obj = ReflectUtil.instantiate(fqcn, new Object[] { "hello", 7 });
    assertThat(obj).isInstanceOf(DummyWithArgs.class);
    DummyWithArgs d = (DummyWithArgs) obj;
    assertThat(d.getText()).isEqualTo("hello");
    assertThat(d.getNum()).isEqualTo(7);
  }

  @Test
  void shouldGetAndSetPrivateFieldValue() {
    PrivateFieldClass target = new PrivateFieldClass();
    Field field = ReflectUtil.getField("secret", target);
    assertThat(field).isNotNull();

    Optional<Object> value = ReflectUtil.getFieldValue(field, target);
    assertThat(value).isPresent().contains("init");

    ReflectUtil.setField(field, target, "changed");
    Optional<Object> changed = ReflectUtil.getFieldValue(field, target);
    assertThat(changed).isPresent().contains("changed");
  }

  @Test
  void shouldFindSetterMethod() {
    Method setter = ReflectUtil.getSetter("name", SetterClass.class, String.class);
    assertThat(setter).isNotNull();
    SetterClass instance = new SetterClass();
    try {
      setter.invoke(instance, "max");
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    assertThat(instance.getName()).isEqualTo("max");
  }

  @Test
  void getSingleSetterShouldFailOnAmbiguity() {
    assertThatThrownBy(() -> ReflectUtil.getSingleSetter("val", AmbiguousSetter.class))
      .isInstanceOf(ProcessEngineException.class);
  }

  @Test
  void shouldInvokePrivateMethod() {
    PrivateMethodClass target = new PrivateMethodClass();
    Object result = ReflectUtil.invoke(target, "concat", new Object[] { "x", 5 });
    assertThat(result).isEqualTo("x5");
  }

  @Test
  void shouldFindMethodViaGetMethodHelper() {
    Method m = ReflectUtil.getMethod(SetterClass.class, "setName", String.class);
    assertThat(m).isNotNull();
    assertThat(m.getName()).isEqualTo("setName");
  }

}

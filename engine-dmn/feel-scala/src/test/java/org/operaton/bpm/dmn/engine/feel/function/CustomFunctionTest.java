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
package org.operaton.bpm.dmn.engine.feel.function;
import java.util.List;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.dmn.engine.feel.function.helper.FunctionProvider;
import org.operaton.bpm.dmn.engine.feel.function.helper.MyPojo;
import org.operaton.bpm.dmn.engine.feel.helper.FeelExtension;
import org.operaton.bpm.dmn.feel.impl.FeelException;
import org.operaton.bpm.dmn.feel.impl.scala.function.CustomFunction;
import org.operaton.bpm.dmn.feel.impl.scala.function.builder.CustomFunctionBuilder;

import static org.assertj.core.api.Assertions.*;

class CustomFunctionTest {

  @RegisterExtension
  FeelExtension feelExtension = FeelExtension.buildWithFunctionProvider();

  FunctionProvider functionProvider;

  @BeforeEach
  void assign() {
    functionProvider = feelExtension.getFunctionProvider();
  }

  @Test
  void shouldThrowExceptionBothFunctionAndReturnValueSet() {
    // given
    CustomFunctionBuilder myFunctionBuilder = CustomFunction.create()
      .setParams("x")
      .setFunction(args -> "")
      .setReturnValue("foo");

    // when, then
    assertThatThrownBy(myFunctionBuilder::build)
      .isInstanceOf(FeelException.class)
      .hasMessageContaining("Only set one return value or a function.");
  }

  @Test
  @SuppressWarnings("unchecked")
  void shouldSetMultipleArgs() {
    // given
    CustomFunction myFunction = CustomFunction.create()
      .setParams("x", "y", "z")
      .setFunction(args -> {
        String argX = (String) args.get(0);
        boolean argY = (boolean) args.get(1);
        List<String> argZ = (List<String>) args.get(2);

        return argX + "-" + argY + "-" + argZ;
      })
      .build();

    functionProvider.register("myFunction", myFunction);

    // when
    String result = feelExtension.evaluateExpression("myFunction(\"foo\", true, [\"elem\"])");

    // then
    assertThat(result).isEqualTo("foo-true-[elem]");
  }

  @Test
  void shouldRegisterMultipleFunctions() {
    // given
    CustomFunction myFunctionA = CustomFunction.create()
      .setReturnValue("A")
      .build();

    CustomFunction myFunctionB = CustomFunction.create()
      .setReturnValue("B")
      .build();

    CustomFunction myFunctionC = CustomFunction.create()
      .setReturnValue("C")
      .build();

    functionProvider.register("myFunctionA", myFunctionA);
    functionProvider.register("myFunctionB", myFunctionB);
    functionProvider.register("myFunctionC", myFunctionC);

    // when
    String result = feelExtension.evaluateExpression("myFunctionA()+myFunctionB()+myFunctionC()");

    // then
    assertThat(result).isEqualTo("ABC");
  }

  @Test
  void shouldCallBean() {
    // given
    MyPojo testBean = new MyPojo(3);
    CustomFunction myFunction = CustomFunction.create()
      .setFunction(args -> testBean.call(2))
      .build();

    functionProvider.register("myFunction", myFunction);

    // when
    long result = feelExtension.evaluateExpression("myFunction()");

    // then
    assertThat(result).isEqualTo(6);
  }

  @Test
  void shouldPassInteger() {
    // given
    CustomFunction myFunction = CustomFunction.create()
      .setParams("x")
      .setFunction(args -> {
        Object argX = args.get(0);

        // then
        assertThat(argX).isEqualTo((long) 12);

        return "";
      })
      .build();

    functionProvider.register("myFunction", myFunction);

    // when
    feelExtension.evaluateExpression("myFunction(variable)", 12);
  }

  @Test
  void shouldPassDouble() {
    // given
    CustomFunction myFunction = CustomFunction.create()
      .setParams("x")
      .setFunction(args -> {
        Object argX = args.get(0);

        // then
        assertThat(argX).isEqualTo(12.1);

        return "";
      })
      .build();

    functionProvider.register("myFunction", myFunction);

    // when
    feelExtension.evaluateExpression("myFunction(variable)", 12.1);
  }

  @Test
  void shouldPassString() {
    // given
    CustomFunction myFunction = CustomFunction.create()
      .setParams("x")
      .setFunction(args -> {
        Object argX = args.get(0);

        // then
        assertThat(argX).isEqualTo("foo");

        return "";
      })
      .build();

    functionProvider.register("myFunction", myFunction);

    // when
    feelExtension.evaluateExpression("myFunction(variable)", "foo");
  }

  @Test
  void shouldPassNull() {
    // given
    CustomFunction myFunction = CustomFunction.create()
      .setParams("x")
      .setFunction(args -> {
        Object argX = args.get(0);

        // then
        assertThat(argX).isNull();

        return "";
      })
      .build();

    functionProvider.register("myFunction", myFunction);

    // when
    feelExtension.evaluateExpression("myFunction(variable)", null);
  }

  @Test
  void shouldPassTrue() {
    // given
    CustomFunction myFunction = CustomFunction.create()
      .setParams("x")
      .setFunction(args -> {
        Object argX = args.get(0);

        // then
        assertThat(argX).isEqualTo(true);

        return "";
      })
      .build();

    functionProvider.register("myFunction", myFunction);

    // when
    feelExtension.evaluateExpression("myFunction(variable)", true);
  }

  @Test
  void shouldPassDate() {
    // given
    Date now = new Date();
    LocalDateTime localDateTime = LocalDateTime.ofInstant(now.toInstant(), ZoneId.systemDefault());

    CustomFunction myFunction = CustomFunction.create()
      .setParams("x")
      .setFunction(args -> {
        Object argX = args.get(0);

        // then
        assertThat(argX).isEqualTo(localDateTime);

        return "";
      })
      .build();

    functionProvider.register("myFunction", myFunction);

    // when
    feelExtension.evaluateExpression("myFunction(variable)", now);
  }

  @Test
  void shouldPassList() {
    // given
    List<String> list = List.of("foo", "bar", "bazz");

    CustomFunction myFunction = CustomFunction.create()
      .setParams("x")
      .setFunction(args -> {
        Object argX = args.get(0);

        // then
        assertThat(argX).isEqualTo(list);

        return "";
      })
      .build();

    functionProvider.register("myFunction", myFunction);

    // when
    feelExtension.evaluateExpression("myFunction(variable)", list);
  }

  @Test
  void shouldPassMap() {
    // given
    Map<String, String> map = Collections.singletonMap("foo", "bar");

    CustomFunction myFunction = CustomFunction.create()
      .setParams("x")
      .setFunction(args -> {
        Object argX = args.get(0);

        // then
        assertThat(argX).isEqualTo(map);

        return "";
      })
      .build();

    functionProvider.register("myFunction", myFunction);

    // when
    feelExtension.evaluateExpression("myFunction(variable)", map);
  }

  @Test
  void shouldReturnString() {
    // given
    CustomFunction myFunction = CustomFunction.create()
      .setReturnValue("foo")
      .build();

    functionProvider.register("myFunction", myFunction);

    // when
    String result = feelExtension.evaluateExpression("myFunction()");

    // then
    assertThat(result).isEqualTo("foo");
  }

  @Test
  void shouldReturnDouble() {
    // given
    CustomFunction myFunction = CustomFunction.create()
      .setReturnValue(1.7976931348623157)
      .build();

    functionProvider.register("myFunction", myFunction);

    // when
    double result = feelExtension.evaluateExpression("myFunction()");

    // then
    assertThat(result).isEqualTo(1.7976931348623157);
  }

  @Test
  void shouldReturnNull() {
    // given
    CustomFunction myFunction = CustomFunction.create()
      .setReturnValue(null)
      .build();

    functionProvider.register("myFunction", myFunction);

    // when
    Object result = feelExtension.evaluateExpression("myFunction()");

    // then
    assertThat(result).isNull();
  }

  @Test
  void shouldReturnTrue() {
    // given
    CustomFunction myFunction = CustomFunction.create()
      .setReturnValue(true)
      .build();

    functionProvider.register("myFunction", myFunction);

    // when
    boolean result = feelExtension.evaluateExpression("myFunction()");

    // then
    assertThat(result).isTrue();
  }

  @Test
  void shouldReturnDate() {
    // given
    Date now = new Date();

    CustomFunction myFunction = CustomFunction.create()
      .setReturnValue(now)
      .build();

    functionProvider.register("myFunction", myFunction);

    // when
    LocalDateTime result = feelExtension.evaluateExpression("myFunction()");

    LocalDateTime localDateTime = LocalDateTime.ofInstant(now.toInstant(), ZoneId.systemDefault());

    // then
    assertThat(result).isEqualTo(localDateTime);
  }

  @Test
  void shouldReturnList() {
    // given
    List<String> list = List.of("foo", "bar", "bazz");

    CustomFunction myFunction = CustomFunction.create()
      .setReturnValue(list)
      .build();

    functionProvider.register("myFunction", myFunction);

    // when
    List<String> result = feelExtension.evaluateExpression("myFunction()");

    // then
    assertThat(result).containsExactly("foo", "bar", "bazz");
  }

  @Test
  void shouldReturnList_Nested() {
    // given
    List<Object> list = List.of("foo", List.of("bar", "bazz"));

    CustomFunction myFunction = CustomFunction.create()
      .setReturnValue(list)
      .build();

    functionProvider.register("myFunction", myFunction);

    // when
    List<Object> result = feelExtension.evaluateExpression("myFunction()");

    // then
    assertThat(result).containsExactly("foo", List.of("bar", "bazz"));
  }

  @Test
  void shouldReturnMap() {
    // given
    Map<String, String> map = Collections.singletonMap("foo", "bar");

    CustomFunction myFunction = CustomFunction.create()
      .setReturnValue(map)
      .build();

    functionProvider.register("myFunction", myFunction);

    // when
    Map<String, String> result = feelExtension.evaluateExpression("myFunction()");

    // then
    assertThat(result).containsExactly(entry("foo", "bar"));
  }

  @Test
  void shouldReturnMap_Nested() {
    // given
    Map<String, Object> map = Collections.singletonMap("foo",
      Collections.singletonMap("bar", "bazz"));

    CustomFunction myFunction = CustomFunction.create()
      .setReturnValue(map)
      .build();

    functionProvider.register("myFunction", myFunction);

    // when
    Map<String, Object> result = feelExtension.evaluateExpression("myFunction()");

    // then
    assertThat(result).containsExactly(entry("foo", Collections.singletonMap("bar", "bazz")));
  }

  @Test
  void shouldPassVarargs() {
    // given
    CustomFunction myFunction = CustomFunction.create()
      .enableVarargs()
      .setFunction(args -> args)
      .build();

    functionProvider.register("myFunction", myFunction);

    // when
    List<String> result = feelExtension.evaluateExpression("myFunction(\"foo\", \"bar\", \"baz\")");

    // then
    assertThat(result).containsExactly("foo", "bar", "baz");
  }

  @Test
  void shouldThrowExceptionDueToDisabledVarargs() {
    // given
    CustomFunction myFunction = CustomFunction.create()
      .setFunction(args -> args)
      .build();

    functionProvider.register("myFunction", myFunction);

    // when
    var result = feelExtension.evaluateExpression("myFunction(\"foo\", \"bar\", \"baz\")");

    // then
    assertThat(result).isNull();
  }

  @Test
  void shouldPassExplicitParamsAndVarargs() {
    // given
    CustomFunction customFunction = CustomFunction.create()
      .enableVarargs()
      .setParams("x", "y")
      .setFunction(args -> args)
      .build();

    functionProvider.register("myFunction", customFunction);

    // when
    List<Object> result = feelExtension.evaluateExpression("myFunction(\"foo\", \"bar\", \"baz\")");

    // then
    assertThat(result).containsExactly("foo", List.of("bar", "baz"));
  }

  @Test
  void shouldPassExplicitParamsWithVarargsEnabled() {
    // given
    CustomFunction customFunction = CustomFunction.create()
      .enableVarargs()
      .setParams("x", "y")
      .setFunction(args -> args)
      .build();

    functionProvider.register("myFunction", customFunction);

    // when
    List<Object> result = feelExtension.evaluateExpression("myFunction(y: \"bar\", x: \"foo\")");

    // then
    assertThat(result).containsExactly("foo", "bar");
  }

}

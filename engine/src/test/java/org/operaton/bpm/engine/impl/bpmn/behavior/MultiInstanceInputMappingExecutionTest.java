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
package org.operaton.bpm.engine.impl.bpmn.behavior;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.operaton.bpm.engine.delegate.DelegateExecution;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.engine.variable.value.ObjectValue;
import org.operaton.bpm.engine.variable.value.TypedValue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MultiInstanceInputMappingExecutionTest {

  @Test
  void shouldSnapshotLoopVariablesAndKeepParentReadsLive() {
    DelegateExecution parent = mock(DelegateExecution.class);
    DelegateExecution iteration = mock(DelegateExecution.class);
    when(iteration.hasVariableLocal("item")).thenReturn(true);
    when(iteration.hasVariableLocal("loopCounter")).thenReturn(true);
    when(iteration.getVariableLocalTyped("item", false)).thenReturn(Variables.stringValue("original"));
    when(iteration.getVariableLocalTyped("loopCounter", false)).thenReturn(Variables.integerValue(2));
    when(parent.getVariable("ordinary")).thenReturn("before", "after");

    MultiInstanceInputMappingExecution scope = new MultiInstanceInputMappingExecution(parent, iteration, "item");
    when(iteration.getVariableLocalTyped("item", false)).thenReturn(Variables.stringValue("mapped"));

    assertThat(scope.getVariable("item")).isEqualTo("original");
    assertThat(scope.getVariableLocal("item")).isEqualTo("original");
    assertThat(scope.getVariableTyped("item", false).getValue()).isEqualTo("original");
    assertThat(scope.getVariableLocalTyped("loopCounter", false).getValue()).isEqualTo(2);
    assertThat(scope.getVariable("ordinary")).isEqualTo("before");
    assertThat(scope.getVariable("ordinary")).isEqualTo("after");
  }

  @Test
  void shouldShadowParentWithNullAndReturnIndependentVariableMaps() {
    DelegateExecution parent = mock(DelegateExecution.class);
    DelegateExecution iteration = mock(DelegateExecution.class);
    when(iteration.hasVariableLocal("item")).thenReturn(true);
    when(iteration.getVariableLocalTyped("item", false)).thenReturn(Variables.stringValue(null));
    VariableMap parentVariables = Variables.createVariables().putValue("item", "parent").putValue("other", 1);
    when(parent.getVariableTyped("other", true)).thenReturn(Variables.integerValue(1));
    when(parent.getVariableLocalTyped("other", true)).thenReturn(Variables.integerValue(1));
    when(parent.getVariableNames()).thenReturn(parentVariables.keySet());
    when(parent.getVariableNamesLocal()).thenReturn(parentVariables.keySet());

    MultiInstanceInputMappingExecution scope = new MultiInstanceInputMappingExecution(parent, iteration, "item");

    assertThat(scope.hasVariable("item")).isTrue();
    assertThat(scope.hasVariableLocal("item")).isTrue();
    assertThat(scope.hasVariables()).isTrue();
    assertThat(scope.hasVariablesLocal()).isTrue();
    assertThat(scope.getVariable("item")).isNull();
    assertThat(scope.getVariableLocal("item")).isNull();
    assertThat(scope.getVariables()).containsEntry("item", null).containsEntry("other", 1);
    assertThat(scope.getVariablesLocal()).containsEntry("item", null).containsEntry("other", 1);
    assertThat(scope.getVariableNames()).containsExactlyInAnyOrder("item", "other");
    assertThat(scope.getVariableNamesLocal()).containsExactlyInAnyOrder("item", "other");
    scope.getVariables().put("item", "modified");
    scope.getVariablesLocal().clear();
    scope.getVariableNames().clear();
    assertThat(scope.getVariable("item")).isNull();
    assertThat(parentVariables).containsEntry("item", "parent").containsEntry("other", 1);
    verify(parent, never()).getVariableTyped("item", true);
    verify(parent, never()).getVariableLocalTyped("item", true);
  }

  @Test
  void shouldNotDeserializeCapturedValuesForPresenceOrSerializedReads() {
    DelegateExecution parent = mock(DelegateExecution.class);
    DelegateExecution iteration = mock(DelegateExecution.class);
    ObjectValue serialized = Variables.serializedObjectValue("unreadable")
        .serializationDataFormat(Variables.SerializationDataFormats.JAVA)
        .objectTypeName("unavailable.Class").create();
    when(iteration.hasVariableLocal("item")).thenReturn(true);
    when(iteration.getVariableLocalTyped("item", false)).thenReturn(serialized);

    MultiInstanceInputMappingExecution scope = new MultiInstanceInputMappingExecution(parent, iteration, "item");

    assertThat(scope.hasVariable("item")).isTrue();
    assertThat(scope.<TypedValue>getVariableTyped("item", false)).isSameAs(serialized);
    assertThat(scope.<TypedValue>getVariableLocalTyped("item", false)).isSameAs(serialized);
    assertThat(scope.getVariablesTyped(false).<TypedValue>getValueTyped("item")).isSameAs(serialized);
    assertThat(scope.getVariablesLocalTyped(false).<TypedValue>getValueTyped("item")).isSameAs(serialized);
  }

  @Test
  void shouldForwardExplicitMutationsAndMetadataToOriginalParent() {
    DelegateExecution parent = mock(DelegateExecution.class);
    DelegateExecution iteration = mock(DelegateExecution.class);
    when(parent.getId()).thenReturn("outer-id");
    when(parent.getCurrentActivityId()).thenReturn("multi-instance-body");
    when(parent.getVariableScopeKey()).thenReturn("execution");
    MultiInstanceInputMappingExecution scope = new MultiInstanceInputMappingExecution(parent, iteration, null);
    Map<String, Object> variables = Map.of("x", 1);
    List<String> names = List.of("x");

    scope.setVariable("x", 1);
    scope.setVariableLocal("x", 2);
    scope.setVariable("x", 3, "activity");
    scope.setVariables(variables);
    scope.setVariablesLocal(variables);
    scope.removeVariable("x");
    scope.removeVariableLocal("x");
    scope.removeVariables(names);
    scope.removeVariablesLocal(names);
    scope.removeVariables();
    scope.removeVariablesLocal();

    verify(parent).setVariable("x", 1);
    verify(parent).setVariableLocal("x", 2);
    verify(parent).setVariable("x", 3, "activity");
    verify(parent).setVariables(variables);
    verify(parent).setVariablesLocal(variables);
    verify(parent).removeVariable("x");
    verify(parent).removeVariableLocal("x");
    verify(parent).removeVariables(names);
    verify(parent).removeVariablesLocal(names);
    verify(parent).removeVariables();
    verify(parent).removeVariablesLocal();
    assertThat(scope.getId()).isEqualTo("outer-id");
    assertThat(scope.getCurrentActivityId()).isEqualTo("multi-instance-body");
    assertThat(scope.getVariableScopeKey()).isEqualTo("execution");
    assertThat(scope.hasIterationVariables()).isFalse();
  }
}

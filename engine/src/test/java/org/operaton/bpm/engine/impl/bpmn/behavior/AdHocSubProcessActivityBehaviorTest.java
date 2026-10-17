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

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.operaton.bpm.engine.impl.bpmn.parser.BpmnParse;
import org.operaton.bpm.engine.impl.el.Expression;
import org.operaton.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.operaton.bpm.engine.impl.pvm.process.ActivityImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdHocSubProcessActivityBehaviorTest {

  @Test
  void configuredCollectionPreservesOrderAndDuplicatesWhileIgnoringNullAndBlankValues() {
    ExecutionEntity execution = mock(ExecutionEntity.class);
    ActivityImpl activity = mock(ActivityImpl.class);
    Expression expression = mock(Expression.class);
    when(execution.getActivity()).thenReturn(activity);
    when(activity.getProperty(BpmnParse.PROPERTYNAME_AD_HOC_ACTIVE_TASKS_COLLECTION)).thenReturn(expression);
    when(expression.getValue(execution)).thenReturn(Arrays.asList(null, "", "  ", " taskA ", 42, "taskA"));

    List<String> ids = new AdHocSubProcessActivityBehavior().getConfiguredActiveTaskIds(execution);

    assertThat(ids).containsExactly("taskA", "42", "taskA");
    ids.add("taskB");
    assertThat(ids).containsExactly("taskA", "42", "taskA", "taskB");
  }

  @Test
  void completedIdsKeepWhitespaceAndDuplicatesAndRemainMutable() {
    ExecutionEntity execution = mock(ExecutionEntity.class);
    when(execution.getVariableLocal("history")).thenReturn(Arrays.asList(null, "", "  ", "a", 42, "a"));

    List<String> ids = new AdHocSubProcessActivityBehavior().getAdHocActivityIds(execution, "history");

    assertThat(ids).containsExactly("", "  ", "a", "42", "a");
    ids.add("b");
    assertThat(ids).containsExactly("", "  ", "a", "42", "a", "b");
  }

  @Test
  void missingAndEmptyCollectionsProduceEmptyMutableLists() {
    ExecutionEntity execution = mock(ExecutionEntity.class);
    ActivityImpl activity = mock(ActivityImpl.class);
    Expression expression = mock(Expression.class);
    when(execution.getActivity()).thenReturn(activity);
    when(activity.getProperty(BpmnParse.PROPERTYNAME_AD_HOC_ACTIVE_TASKS_COLLECTION)).thenReturn(expression);
    when(expression.getValue(execution)).thenReturn(null, List.of());
    AdHocSubProcessActivityBehavior behavior = new AdHocSubProcessActivityBehavior();

    List<String> missing = behavior.getConfiguredActiveTaskIds(execution);
    List<String> empty = behavior.getConfiguredActiveTaskIds(execution);

    assertThat(missing).isEmpty();
    assertThat(empty).isEmpty();
    missing.add("a");
    empty.add("b");
    assertThat(missing).containsExactly("a");
    assertThat(empty).containsExactly("b");
  }
}

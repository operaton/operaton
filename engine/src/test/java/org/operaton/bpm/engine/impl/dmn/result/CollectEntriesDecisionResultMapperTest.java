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
package org.operaton.bpm.engine.impl.dmn.result;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.operaton.bpm.dmn.engine.DmnDecisionResult;
import org.operaton.bpm.dmn.engine.DmnDecisionResultEntries;
import org.operaton.bpm.dmn.engine.impl.DmnDecisionResultEntriesImpl;
import org.operaton.bpm.dmn.engine.impl.DmnDecisionResultImpl;
import org.operaton.bpm.engine.ProcessEngineException;

class CollectEntriesDecisionResultMapperTest {

  protected final CollectEntriesDecisionResultMapper mapper = new CollectEntriesDecisionResultMapper();

  protected DmnDecisionResult decisionResult(DmnDecisionResultEntries... entries) {
    List<DmnDecisionResultEntries> ruleResults = new ArrayList<>(List.of(entries));
    return new DmnDecisionResultImpl(ruleResults);
  }

  protected DmnDecisionResultEntriesImpl entry(String outputName, Object value) {
    DmnDecisionResultEntriesImpl entries = new DmnDecisionResultEntriesImpl();
    entries.putValue(outputName, org.operaton.bpm.engine.variable.Variables.untypedValue(value));
    return entries;
  }

  @Test
  void shouldReturnEmptyListForEmptyDecisionResult() {
    DmnDecisionResult decisionResult = decisionResult();

    Object result = mapper.mapDecisionResult(decisionResult);

    assertThat(result).isInstanceOf(List.class);
    assertThat((List<?>) result).isEmpty();
  }

  @Test
  @SuppressWarnings("unchecked")
  void shouldCollectEntriesForSingleOutputName() {
    DmnDecisionResult decisionResult = decisionResult(
        entry("output1", "a"),
        entry("output1", "b"));

    Object result = mapper.mapDecisionResult(decisionResult);

    assertThat(result).isInstanceOf(List.class);
    assertThat((List<Object>) result).containsExactly("a", "b");
  }

  @Test
  void shouldThrowExceptionForMultipleOutputNames() {
    DmnDecisionResult decisionResult = decisionResult(
        entry("output1", "a"),
        entry("output2", "b"));

    assertThatExceptionOfType(ProcessEngineException.class)
        .isThrownBy(() -> mapper.mapDecisionResult(decisionResult));
  }

  @Test
  void shouldReturnImmutableCollection() {
    DmnDecisionResult decisionResult = decisionResult(
        entry("output1", "a"),
        entry("output1", "b"));

    @SuppressWarnings("unchecked")
    List<Object> result = (List<Object>) mapper.mapDecisionResult(decisionResult);

    assertThatThrownBy(() -> result.add("c"))
        .isInstanceOf(UnsupportedOperationException.class);
  }

}

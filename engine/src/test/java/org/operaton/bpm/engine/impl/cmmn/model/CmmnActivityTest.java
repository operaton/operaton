/*
 * Copyright 2025 the Operaton contributors.
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
package org.operaton.bpm.engine.impl.cmmn.model;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.operaton.bpm.engine.delegate.VariableListener;
import org.operaton.bpm.engine.impl.variable.listener.DelegateCaseVariableInstanceImpl;

import static org.assertj.core.api.Assertions.assertThat;

class CmmnActivityTest {

  static class TestCmmnActivity extends CmmnActivity {
    private final Map<String, List<VariableListener<?>>> customLocal = new HashMap<>();
    private final Map<String, List<VariableListener<?>>> builtInLocal = new HashMap<>();

    TestCmmnActivity(String id) {
      super(id, null);
    }

    void addCustomLocal(String event, VariableListener<?>... listeners) {
      customLocal.put(event, Arrays.asList(listeners));
    }

    void addBuiltInLocal(String event, VariableListener<?>... listeners) {
      builtInLocal.put(event, Arrays.asList(listeners));
    }

    @Override
    public List<VariableListener<?>> getVariableListenersLocal(String eventName) {
      return customLocal.get(eventName);
    }

    @Override
    public List<VariableListener<?>> getBuiltInVariableListenersLocal(String eventName) {
      return builtInLocal.get(eventName);
    }
  }

  @Test
  void shouldReturnEmptyWhenNoListeners() {
    TestCmmnActivity root = new TestCmmnActivity("root");
    TestCmmnActivity child = new TestCmmnActivity("child");

    // link hierarchy
    child.setParent(root);
    root.activities.add(child);

    Map<String, List<VariableListener<?>>> result = child.getVariableListeners("evt", true);
    assertThat(result).isEmpty();
  }

  @Test
  void shouldAggregateCustomAndBuiltinListenersFromParents() {
    TestCmmnActivity root = new TestCmmnActivity("root");
    TestCmmnActivity parent = new TestCmmnActivity("parent");
    TestCmmnActivity child = new TestCmmnActivity("child");

    // link hierarchy: root <- parent <- child
    parent.setParent(root);
    root.activities.add(parent);

    child.setParent(parent);
    parent.activities.add(child);

    VariableListener<DelegateCaseVariableInstanceImpl> rootListener = ctx -> {};
    VariableListener<DelegateCaseVariableInstanceImpl> parentCustomListener = ctx -> {};
    VariableListener<DelegateCaseVariableInstanceImpl> childCustomListener = ctx -> {};
    VariableListener<DelegateCaseVariableInstanceImpl> parentBuiltIn = ctx -> {};

    // custom listeners on different levels
    root.addCustomLocal("evt", rootListener);
    parent.addCustomLocal("evt", parentCustomListener);
    child.addCustomLocal("evt", childCustomListener);

    // built-in listener on parent
    parent.addBuiltInLocal("evt", parentBuiltIn);

    // custom listeners aggregation
    Map<String, List<VariableListener<?>>> customMap = child.getVariableListeners("evt", true);
    assertThat(customMap).hasSize(3);
    assertThat(customMap).containsKey("child");
    assertThat(customMap).containsKey("parent");
    assertThat(customMap).containsKey("root");
    assertThat(customMap.get("child")).containsExactly(childCustomListener);
    assertThat(customMap.get("parent")).containsExactly(parentCustomListener);
    assertThat(customMap.get("root")).containsExactly(rootListener);

    // built-in listeners aggregation
    Map<String, List<VariableListener<?>>> builtInMap = child.getVariableListeners("evt", false);
    assertThat(builtInMap).containsOnlyKeys("parent");
    assertThat(builtInMap.get("parent")).containsExactly(parentBuiltIn);
  }

  @Test
  void shouldCacheResolvedListenersPerEvent() {
    TestCmmnActivity root = new TestCmmnActivity("root");
    TestCmmnActivity child = new TestCmmnActivity("child");

    child.setParent(root);
    root.activities.add(child);

    VariableListener<DelegateCaseVariableInstanceImpl> l = ctx -> {};
    root.addCustomLocal("evt", l);

    Map<String, List<VariableListener<?>>> first = child.getVariableListeners("evt", true);
    Map<String, List<VariableListener<?>>> second = child.getVariableListeners("evt", true);

    // same instance due to caching
    assertThat(second).isSameAs(first);
  }
}

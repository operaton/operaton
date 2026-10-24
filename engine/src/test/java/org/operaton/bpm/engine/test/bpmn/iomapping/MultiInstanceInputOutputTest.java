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
package org.operaton.bpm.engine.test.bpmn.iomapping;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.delegate.DelegateExecution;
import org.operaton.bpm.engine.delegate.JavaDelegate;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.engine.variable.value.ObjectValue;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.operaton.bpm.model.bpmn.instance.Activity;
import org.operaton.bpm.model.bpmn.instance.ExtensionElements;
import org.operaton.bpm.model.bpmn.instance.LoopCardinality;
import org.operaton.bpm.model.bpmn.instance.MultiInstanceLoopCharacteristics;
import org.operaton.bpm.model.bpmn.instance.operaton.OperatonEntry;
import org.operaton.bpm.model.bpmn.instance.operaton.OperatonInputOutput;
import org.operaton.bpm.model.bpmn.instance.operaton.OperatonInputParameter;
import org.operaton.bpm.model.bpmn.instance.operaton.OperatonList;
import org.operaton.bpm.model.bpmn.instance.operaton.OperatonMap;
import org.operaton.bpm.model.bpmn.instance.operaton.OperatonScript;
import org.operaton.bpm.model.bpmn.instance.operaton.OperatonValue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.OPERATON_NS;

class MultiInstanceInputOutputTest {

  private static final String PROCESS_KEY = "multiInstanceInputMapping";
  private static final List<Map<String, Object>> RECORDED_VARIABLES = new ArrayList<>();
  private static boolean failDelegate;

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  RuntimeService runtimeService;
  RepositoryService repositoryService;
  TaskService taskService;
  ManagementService managementService;

  @BeforeEach
  @AfterEach
  void resetDelegate() {
    RECORDED_VARIABLES.clear();
    failDelegate = false;
  }

  static Stream<Arguments> parentConfigurations() {
    return Stream.of(false, true).flatMap(sequential -> Stream.of(false, true)
        .flatMap(serviceTask -> Stream.of("missing", "collision", "null")
            .map(parent -> Arguments.of(sequential, serviceTask, parent))));
  }

  @ParameterizedTest
  @MethodSource("parentConfigurations")
  void shouldResolveCurrentIterationInExpressionsAndScripts(boolean sequential, boolean serviceTask, String parent) {
    BpmnModelInstance model = taskModel(sequential, serviceTask);
    addIterationInputs(model, "miTask");
    addScript(model, "miTask", "scriptItem", "item");
    addScript(model, "miTask", "scriptCounter", "loopCounter");
    addScript(model, "miTask", "scriptExecutionItem", "execution.getVariable('item')");
    addScript(model, "miTask", "scriptExecutionCounter", "execution.getVariable('loopCounter')");
    addInput(model, "miTask", "itemPresent", "${execution.hasVariable('item')}");
    testRule.deploy(model);

    List<Object> items = Arrays.asList("first", "second", null);
    Map<String, Object> variables = variables(items);
    if (!"missing".equals(parent)) {
      variables.put("item", "collision".equals(parent) ? "parentItem" : null);
      variables.put("loopCounter", "collision".equals(parent) ? 42 : null);
    }
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(PROCESS_KEY, variables);

    if (!serviceTask) {
      assertThat(taskService.createTaskQuery().taskDefinitionKey("miTask").count())
          .isEqualTo(sequential ? 1 : items.size());
      completeIterationTasks();
    }

    List<Map<String, Object>> snapshots = orderedSnapshots();
    assertThat(snapshots).hasSize(items.size());
    for (int index = 0; index < items.size(); index++) {
      assertThat(snapshots.get(index))
          .containsEntry("mappedItem", items.get(index))
          .containsEntry("mappedCounter", index)
          .containsEntry("executionItem", items.get(index))
          .containsEntry("executionCounter", index)
          .containsEntry("scriptItem", items.get(index))
          .containsEntry("scriptCounter", index)
          .containsEntry("scriptExecutionItem", items.get(index))
          .containsEntry("scriptExecutionCounter", index)
          .containsEntry("itemPresent", true);
    }
    assertParentVariablesUnchanged(processInstance, variables);
  }

  static Stream<Arguments> isolationConfigurations() {
    return Stream.of(false, true).flatMap(sequential -> Stream.of(false, true)
        .flatMap(serviceTask -> Stream.of(false, true)
            .map(nullParentSource -> Arguments.of(sequential, serviceTask, nullParentSource))));
  }

  @ParameterizedTest
  @MethodSource("isolationConfigurations")
  void shouldKeepOriginalIterationAndParentInputsIsolated(boolean sequential, boolean serviceTask,
      boolean nullParentSource) {
    BpmnModelInstance model = taskModel(sequential, serviceTask);
    addInput(model, "miTask", "item", "overwrittenItem");
    addInput(model, "miTask", "source", "mappedSource");
    addInput(model, "miTask", "createdByMapping", "localValue");
    addIterationInputs(model, "miTask");
    addInput(model, "miTask", "parentSource", "${source}");
    addInput(model, "miTask", "executionParentSource", "${execution.getVariable('source')}");
    addInput(model, "miTask", "earlierInput", "${execution.getVariable('createdByMapping')}");
    addInput(model, "miTask", "earlierInputPresent", "${execution.hasVariable('createdByMapping')}");
    addScript(model, "miTask", "scriptItem", "item");
    addScript(model, "miTask", "scriptExecutionItem", "execution.getVariable('item')");
    addScript(model, "miTask", "scriptParentSource", "source");
    testRule.deploy(model);

    List<Object> items = Arrays.asList("first", null, "third");
    Map<String, Object> variables = variables(items);
    variables.put("item", "parentItem");
    Object parentSource = nullParentSource ? null : "parentSource";
    variables.put("source", parentSource);
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(PROCESS_KEY, variables);
    if (!serviceTask) {
      completeIterationTasks();
    }

    List<Map<String, Object>> snapshots = orderedSnapshots();
    assertThat(snapshots).hasSize(items.size());
    for (int index = 0; index < items.size(); index++) {
      assertThat(snapshots.get(index))
          .containsEntry("item", "overwrittenItem")
          .containsEntry("mappedItem", items.get(index))
          .containsEntry("executionItem", items.get(index))
          .containsEntry("scriptItem", items.get(index))
          .containsEntry("scriptExecutionItem", items.get(index))
          .containsEntry("mappedCounter", index)
          .containsEntry("source", "mappedSource")
          .containsEntry("parentSource", parentSource)
          .containsEntry("executionParentSource", parentSource)
          .containsEntry("scriptParentSource", parentSource)
          .containsEntry("earlierInput", null)
          .containsEntry("earlierInputPresent", false);
    }
    assertParentVariablesUnchanged(processInstance, variables);
  }

  @ParameterizedTest
  @CsvSource({"false, false", "false, true", "true, false", "true, true"})
  void shouldPreserveElementTypesAndSupportBothNamespaces(boolean sequential, boolean legacyNamespace) {
    BpmnModelInstance model = taskModel(sequential, false);
    addIterationInputs(model, "miTask");
    addInput(model, "miTask", "typedItem", "${execution.getVariableTyped('item').getValue()}");
    addListInput(model);
    addMapInput(model);
    String xml = Bpmn.convertToString(model);
    if (legacyNamespace) {
      xml = xml.replace("http://operaton.org/schema/1.0/bpmn", "http://camunda.org/schema/1.0/bpmn");
    }
    testRule.deploy(repositoryService.createDeployment().addString("multiInstance.bpmn", xml));

    List<Object> items = Arrays.asList(7, 12L, 2.5, true, "text", null, new HashMap<>(Map.of("key", "value")));
    Map<String, Object> variables = variables(items);
    variables.put("item", "parentItem");
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(PROCESS_KEY, variables);
    completeIterationTasks();

    List<Map<String, Object>> snapshots = orderedSnapshots();
    assertThat(snapshots).hasSize(items.size());
    for (int index = 0; index < items.size(); index++) {
      Object item = items.get(index);
      Map<String, Object> expectedMap = new HashMap<>();
      expectedMap.put("current", item);
      expectedMap.put("index", index);
      assertThat(snapshots.get(index))
          .containsEntry("mappedItem", item)
          .containsEntry("typedItem", item)
          .containsEntry("mappedList", Arrays.asList(item, index))
          .containsEntry("mappedMap", expectedMap);
      if (item != null) {
        assertThat(snapshots.get(index).get("mappedItem")).isInstanceOf(item.getClass());
      }
    }
    assertParentVariablesUnchanged(processInstance, variables);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void shouldResolveLoopCounterWithoutCollection(boolean sequential) {
    BpmnModelInstance model = taskModel(sequential, false);
    MultiInstanceLoopCharacteristics characteristics = model.getModelElementById("miTask")
        .getChildElementsByType(MultiInstanceLoopCharacteristics.class).iterator().next();
    characteristics.removeAttributeNs(OPERATON_NS, "collection");
    characteristics.removeAttributeNs(OPERATON_NS, "elementVariable");
    LoopCardinality cardinality = model.newInstance(LoopCardinality.class);
    cardinality.setTextContent("3");
    characteristics.setLoopCardinality(cardinality);
    addInput(model, "miTask", "mappedCounter", "${loopCounter}");
    addInput(model, "miTask", "executionCounter", "${execution.getVariable('loopCounter')}");
    testRule.deploy(model);

    runtimeService.startProcessInstanceByKey(PROCESS_KEY, Map.of("loopCounter", 42));
    completeIterationTasks();

    assertThat(orderedSnapshots()).extracting(snapshot -> snapshot.get("mappedCounter")).containsExactly(0, 1, 2);
    assertThat(orderedSnapshots()).extracting(snapshot -> snapshot.get("executionCounter")).containsExactly(0, 1, 2);
  }

  @ParameterizedTest
  @CsvSource({"false, false", "false, true", "true, false", "true, true"})
  void shouldResolveInnermostIterationInNestedMultiInstance(boolean outerSequential, boolean innerSequential) {
    BpmnModelInstance model = Bpmn.createExecutableProcess(PROCESS_KEY)
        .startEvent()
        .subProcess("outer").embeddedSubProcess()
          .startEvent().userTask("miTask").endEvent()
        .subProcessDone()
        .userTask("after").endEvent().done();
    addMultiInstance(model, "outer", outerSequential, "groups", "outerItem");
    addMultiInstance(model, "miTask", innerSequential, "${outerItem}", "item");
    addIterationInputs(model, "miTask");
    addInput(model, "miTask", "outerFirst", "${outerItem[0]}");
    testRule.deploy(model);

    Map<String, Object> variables = new HashMap<>();
    variables.put("groups", List.of(List.of("a1", "a2"), List.of("b1", "b2")));
    variables.put("item", "parentItem");
    variables.put("loopCounter", 42);
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(PROCESS_KEY, variables);
    completeIterationTasks();

    assertThat(RECORDED_VARIABLES).hasSize(4);
    assertThat(RECORDED_VARIABLES).extracting(snapshot -> snapshot.get("mappedItem"))
        .containsExactlyInAnyOrder("a1", "a2", "b1", "b2");
    for (Map<String, Object> snapshot : RECORDED_VARIABLES) {
      String item = (String) snapshot.get("mappedItem");
      assertThat(snapshot)
          .containsEntry("executionItem", item)
          .containsEntry("mappedCounter", item.endsWith("1") ? 0 : 1)
          .containsEntry("executionCounter", item.endsWith("1") ? 0 : 1)
          .containsEntry("outerFirst", item.startsWith("a") ? "a1" : "b1");
    }
    assertParentVariablesUnchanged(processInstance, variables);
  }

  @ParameterizedTest
  @CsvSource({"false, false", "false, true", "true, false", "true, true"})
  void shouldShadowEnclosingIterationWithSameElementVariable(boolean outerSequential, boolean innerSequential) {
    BpmnModelInstance model = Bpmn.createExecutableProcess(PROCESS_KEY)
        .startEvent()
        .subProcess("outer").embeddedSubProcess()
          .startEvent().userTask("miTask").endEvent()
        .subProcessDone()
        .userTask("after").endEvent().done();
    addMultiInstance(model, "outer", outerSequential, "groups", "item");
    addMultiInstance(model, "miTask", innerSequential, "${innerRows}", "item");
    addInput(model, "outer", "innerRows", "${item}");
    addInput(model, "outer", "outerFirst", "${item[0]}");
    addIterationInputs(model, "miTask");
    addInput(model, "miTask", "outerFirst", "${outerFirst}");
    testRule.deploy(model);

    Map<String, Object> variables = new HashMap<>();
    variables.put("groups", List.of(List.of("a1", "a2"), List.of("b1", "b2")));
    variables.put("item", "parentItem");
    variables.put("loopCounter", 42);
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(PROCESS_KEY, variables);
    completeIterationTasks();

    assertThat(RECORDED_VARIABLES).hasSize(4);
    assertThat(RECORDED_VARIABLES).extracting(snapshot -> snapshot.get("mappedItem"))
        .containsExactlyInAnyOrder("a1", "a2", "b1", "b2");
    for (Map<String, Object> snapshot : RECORDED_VARIABLES) {
      String item = (String) snapshot.get("mappedItem");
      assertThat(snapshot)
          .containsEntry("executionItem", item)
          .containsEntry("mappedCounter", item.endsWith("1") ? 0 : 1)
          .containsEntry("executionCounter", item.endsWith("1") ? 0 : 1)
          .containsEntry("outerFirst", item.startsWith("a") ? "a1" : "b1");
    }
    assertParentVariablesUnchanged(processInstance, variables);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void shouldKeepSubprocessWorkaroundWorking(boolean sequential) {
    BpmnModelInstance model = Bpmn.createExecutableProcess(PROCESS_KEY)
        .startEvent()
        .subProcess("outer").embeddedSubProcess()
          .startEvent().userTask("miTask").endEvent()
        .subProcessDone()
        .userTask("after").endEvent().done();
    addMultiInstance(model, "outer", sequential, "items", "item");
    addIterationInputs(model, "miTask");
    testRule.deploy(model);

    runtimeService.startProcessInstanceByKey(PROCESS_KEY, variables(List.of("first", "second")));
    completeIterationTasks();

    assertThat(orderedSnapshots()).extracting(snapshot -> snapshot.get("mappedItem"))
        .containsExactly("first", "second");
    assertThat(orderedSnapshots()).extracting(snapshot -> snapshot.get("executionCounter")).containsExactly(0, 1);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void shouldResolveIterationInMultiInstanceSubprocessInputs(boolean sequential) {
    BpmnModelInstance model = Bpmn.createExecutableProcess(PROCESS_KEY)
        .startEvent()
        .subProcess("outer").embeddedSubProcess()
          .startEvent().userTask("miTask").endEvent()
        .subProcessDone()
        .userTask("after").endEvent().done();
    addMultiInstance(model, "outer", sequential, "items", "item");
    addInput(model, "outer", "source", "mappedSource");
    addIterationInputs(model, "outer");
    addInput(model, "outer", "parentSource", "${source}");
    testRule.deploy(model);

    Map<String, Object> variables = variables(List.of("first", "second"));
    variables.put("item", "parentItem");
    variables.put("loopCounter", 42);
    variables.put("source", "parentSource");
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(PROCESS_KEY, variables);
    completeIterationTasks();

    assertThat(orderedSnapshots()).extracting(snapshot -> snapshot.get("mappedItem"))
        .containsExactly("first", "second");
    assertThat(orderedSnapshots()).extracting(snapshot -> snapshot.get("executionItem"))
        .containsExactly("first", "second");
    assertThat(orderedSnapshots()).extracting(snapshot -> snapshot.get("executionCounter")).containsExactly(0, 1);
    assertThat(RECORDED_VARIABLES).allSatisfy(snapshot -> assertThat(snapshot)
        .containsEntry("source", "mappedSource").containsEntry("parentSource", "parentSource"));
    assertParentVariablesUnchanged(processInstance, variables);
  }

  @ParameterizedTest
  @CsvSource({"false, false", "false, true", "true, false", "true, true"})
  void shouldResolveIterationAfterAsyncResumeAndRetry(boolean sequential, boolean innerAsync) {
    BpmnModelInstance model = taskModel(sequential, true);
    Activity task = model.getModelElementById("miTask");
    if (innerAsync) {
      ((MultiInstanceLoopCharacteristics) task.getLoopCharacteristics()).setOperatonAsyncBefore(true);
    } else {
      task.setOperatonAsyncBefore(true);
    }
    addIterationInputs(model, "miTask");
    addInput(model, "miTask", "source", "mappedSource");
    addInput(model, "miTask", "parentSource", "${source}");
    testRule.deploy(model);

    Map<String, Object> variables = variables(List.of("first", "second"));
    variables.put("item", "parentItem");
    variables.put("loopCounter", 42);
    variables.put("source", "parentSource");
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(PROCESS_KEY, variables);
    assertThat(RECORDED_VARIABLES).isEmpty();
    Job job = managementService.createJobQuery().processInstanceId(processInstance.getId()).list().get(0);
    failDelegate = true;

    assertThatThrownBy(() -> managementService.executeJob(job.getId()))
        .isInstanceOf(ProcessEngineException.class).hasMessageContaining("retry input mapping");
    assertThat(RECORDED_VARIABLES).isEmpty();
    assertThat(managementService.createJobQuery().jobId(job.getId()).singleResult()).isNotNull();
    failDelegate = false;
    managementService.executeJob(job.getId());
    List<Job> jobs = managementService.createJobQuery().processInstanceId(processInstance.getId()).list();
    while (!jobs.isEmpty()) {
      for (Job remainingJob : jobs) {
        managementService.executeJob(remainingJob.getId());
      }
      jobs = managementService.createJobQuery().processInstanceId(processInstance.getId()).list();
    }

    assertThat(orderedSnapshots()).extracting(snapshot -> snapshot.get("mappedItem"))
        .containsExactly("first", "second");
    assertThat(orderedSnapshots()).extracting(snapshot -> snapshot.get("executionCounter")).containsExactly(0, 1);
    assertThat(RECORDED_VARIABLES).allSatisfy(snapshot -> assertThat(snapshot)
        .containsEntry("source", "mappedSource").containsEntry("parentSource", "parentSource"));
    assertParentVariablesUnchanged(processInstance, variables);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void shouldDeserializeOriginalIterationSnapshotAfterAsyncResume(boolean sequential) {
    BpmnModelInstance model = taskModel(sequential, false);
    Activity task = model.getModelElementById("miTask");
    ((MultiInstanceLoopCharacteristics) task.getLoopCharacteristics()).setOperatonAsyncBefore(true);
    addInput(model, "miTask", "item", "overwrittenItem");
    addIterationInputs(model, "miTask");
    testRule.deploy(model);

    List<Map<String, String>> items = List.of(new HashMap<>(Map.of("key", "first")),
        new HashMap<>(Map.of("key", "second")));
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(PROCESS_KEY, variables(items));
    List<Job> jobs = managementService.createJobQuery().processInstanceId(processInstance.getId()).list();
    ObjectValue persistedItem = runtimeService.getVariableLocalTyped(jobs.get(0).getExecutionId(), "item", false);
    assertThat(persistedItem.isDeserialized()).isFalse();
    assertThat(persistedItem.getValueSerialized()).isNotEmpty();

    while (!jobs.isEmpty()) {
      for (Job job : jobs) {
        managementService.executeJob(job.getId());
      }
      completeIterationTasks();
      jobs = managementService.createJobQuery().processInstanceId(processInstance.getId()).list();
    }

    List<Map<String, Object>> snapshots = orderedSnapshots();
    assertThat(snapshots).hasSize(items.size());
    for (int index = 0; index < items.size(); index++) {
      assertThat(snapshots.get(index))
          .containsEntry("item", "overwrittenItem")
          .containsEntry("mappedItem", items.get(index))
          .containsEntry("executionItem", items.get(index))
          .containsEntry("mappedCounter", index);
    }
    assertParentVariablesUnchanged(processInstance, variables(items));
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void shouldNotDeserializeUnusedIterationElement(boolean sequential) {
    BpmnModelInstance model = taskModel(sequential, false);
    Activity task = model.getModelElementById("miTask");
    ((MultiInstanceLoopCharacteristics) task.getLoopCharacteristics()).setOperatonAsyncBefore(true);
    addInput(model, "miTask", "mappedSource", "${source}");
    testRule.deploy(model);

    Map<String, Object> variables = variables(List.of("first"));
    variables.put("source", "parentSource");
    runtimeService.startProcessInstanceByKey(PROCESS_KEY, variables);
    Job job = managementService.createJobQuery().singleResult();
    assertThat(runtimeService.getVariableLocal(job.getExecutionId(), "item")).isEqualTo("first");
    ObjectValue serializedItem = Variables.serializedObjectValue("bm90LWphdmE=")
        .serializationDataFormat(Variables.SerializationDataFormats.JAVA)
        .objectTypeName("java.lang.Object").create();
    boolean javaSerializationEnabled = engineRule.getProcessEngineConfiguration().isJavaSerializationFormatEnabled();
    try {
      engineRule.getProcessEngineConfiguration().setJavaSerializationFormatEnabled(true);
      runtimeService.setVariableLocal(job.getExecutionId(), "item", serializedItem);
    } finally {
      engineRule.getProcessEngineConfiguration().setJavaSerializationFormatEnabled(javaSerializationEnabled);
    }

    managementService.executeJob(job.getId());

    Task userTask = taskService.createTaskQuery().taskDefinitionKey("miTask").singleResult();
    assertThat(userTask).isNotNull();
    assertThat(runtimeService.getVariableLocal(userTask.getExecutionId(), "mappedSource")).isEqualTo("parentSource");
    ObjectValue persistedItem = runtimeService.getVariableLocalTyped(userTask.getExecutionId(), "item", false);
    assertThat(persistedItem.isDeserialized()).isFalse();
    assertThat(persistedItem.getValueSerialized()).isEqualTo("bm90LWphdmE=");
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void shouldObserveExplicitParentWritesInLaterInputs(boolean sequential) {
    BpmnModelInstance model = taskModel(sequential, false);
    addInput(model, "miTask", "source", "mappedSource");
    addScript(model, "miTask", "writeResult", "execution.setVariable('source', 'updated'); source");
    addInput(model, "miTask", "parentSource", "${source}");
    addInput(model, "miTask", "executionParentSource", "${execution.getVariable('source')}");
    testRule.deploy(model);

    Map<String, Object> variables = variables(List.of("first", "second"));
    variables.put("source", "original");
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(PROCESS_KEY, variables);
    completeIterationTasks();

    assertThat(RECORDED_VARIABLES).hasSize(2).allSatisfy(snapshot -> assertThat(snapshot)
        .containsEntry("source", "mappedSource")
        .containsEntry("writeResult", "updated")
        .containsEntry("parentSource", "updated")
        .containsEntry("executionParentSource", "updated"));
    variables.put("source", "updated");
    assertParentVariablesUnchanged(processInstance, variables);
  }

  @Test
  void shouldPreserveOrdinaryInputMappingIsolation() {
    BpmnModelInstance model = Bpmn.createExecutableProcess(PROCESS_KEY)
        .startEvent().userTask("miTask").userTask("after").endEvent().done();
    addInput(model, "miTask", "source", "mappedSource");
    addInput(model, "miTask", "createdByMapping", "localValue");
    addInput(model, "miTask", "parentSource", "${source}");
    addInput(model, "miTask", "executionParentSource", "${execution.getVariable('source')}");
    addInput(model, "miTask", "earlierInput", "${execution.getVariable('createdByMapping')}");
    addScript(model, "miTask", "scriptParentSource", "source");
    testRule.deploy(model);

    Map<String, Object> variables = Map.of("source", "parentSource");
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(PROCESS_KEY, variables);
    completeIterationTasks();

    assertThat(RECORDED_VARIABLES).singleElement().satisfies(snapshot -> assertThat(snapshot)
        .containsEntry("source", "mappedSource")
        .containsEntry("parentSource", "parentSource")
        .containsEntry("executionParentSource", "parentSource")
        .containsEntry("scriptParentSource", "parentSource")
        .containsEntry("earlierInput", null));
    assertParentVariablesUnchanged(processInstance, variables);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void shouldNotResolveEarlierInputWhenParentVariableIsMissing(boolean sequential) {
    BpmnModelInstance model = taskModel(sequential, false);
    addInput(model, "miTask", "createdByMapping", "localValue");
    addInput(model, "miTask", "laterInput", "${createdByMapping}");
    testRule.deploy(model);

    assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey(PROCESS_KEY, variables(List.of("item"))))
        .isInstanceOf(ProcessEngineException.class).hasMessageContaining("createdByMapping");
  }

  private BpmnModelInstance taskModel(boolean sequential, boolean serviceTask) {
    BpmnModelInstance model;
    if (serviceTask) {
      model = Bpmn.createExecutableProcess(PROCESS_KEY).startEvent()
          .serviceTask("miTask").operatonClass(RecordingDelegate.class.getName())
          .userTask("after").endEvent().done();
    } else {
      model = Bpmn.createExecutableProcess(PROCESS_KEY).startEvent()
          .userTask("miTask").userTask("after").endEvent().done();
    }
    addMultiInstance(model, "miTask", sequential, "items", "item");
    return model;
  }

  private void addMultiInstance(BpmnModelInstance model, String activityId, boolean sequential,
      String collection, String elementVariable) {
    Activity activity = model.getModelElementById(activityId);
    MultiInstanceLoopCharacteristics characteristics = model.newInstance(MultiInstanceLoopCharacteristics.class);
    characteristics.setSequential(sequential);
    characteristics.setOperatonCollection(collection);
    characteristics.setOperatonElementVariable(elementVariable);
    activity.setLoopCharacteristics(characteristics);
  }

  private void addIterationInputs(BpmnModelInstance model, String activityId) {
    addInput(model, activityId, "mappedItem", "${item}");
    addInput(model, activityId, "mappedCounter", "${loopCounter}");
    addInput(model, activityId, "executionItem", "${execution.getVariable('item')}");
    addInput(model, activityId, "executionCounter", "${execution.getVariable('loopCounter')}");
  }

  private OperatonInputParameter addInput(BpmnModelInstance model, String activityId, String name, String value) {
    Activity activity = model.getModelElementById(activityId);
    ExtensionElements extensions = activity.getExtensionElements();
    if (extensions == null) {
      extensions = model.newInstance(ExtensionElements.class);
      activity.setExtensionElements(extensions);
    }
    OperatonInputOutput inputOutput = extensions.getChildElementsByType(OperatonInputOutput.class)
        .stream().findFirst().orElse(null);
    if (inputOutput == null) {
      inputOutput = model.newInstance(OperatonInputOutput.class);
      extensions.addChildElement(inputOutput);
    }
    OperatonInputParameter input = model.newInstance(OperatonInputParameter.class);
    input.setOperatonName(name);
    if (value != null) {
      input.setTextContent(value);
    }
    inputOutput.addChildElement(input);
    return input;
  }

  private void addScript(BpmnModelInstance model, String activityId, String name, String source) {
    OperatonInputParameter input = addInput(model, activityId, name, null);
    OperatonScript script = model.newInstance(OperatonScript.class);
    script.setOperatonScriptFormat("groovy");
    script.setTextContent(source);
    input.setValue(script);
  }

  private void addListInput(BpmnModelInstance model) {
    OperatonList list = model.newInstance(OperatonList.class);
    for (String expression : List.of("${item}", "${loopCounter}")) {
      OperatonValue value = model.newInstance(OperatonValue.class);
      value.setTextContent(expression);
      list.getValues().add(value);
    }
    addInput(model, "miTask", "mappedList", null).setValue(list);
  }

  private void addMapInput(BpmnModelInstance model) {
    OperatonMap map = model.newInstance(OperatonMap.class);
    for (Map.Entry<String, String> entry : Map.of("current", "${item}", "index", "${loopCounter}").entrySet()) {
      OperatonEntry value = model.newInstance(OperatonEntry.class);
      value.setOperatonKey(entry.getKey());
      value.setTextContent(entry.getValue());
      map.getOperatonEntries().add(value);
    }
    addInput(model, "miTask", "mappedMap", null).setValue(map);
  }

  private Map<String, Object> variables(List<?> items) {
    Map<String, Object> variables = new HashMap<>();
    variables.put("items", items);
    return variables;
  }

  private void completeIterationTasks() {
    List<Task> tasks = taskService.createTaskQuery().taskDefinitionKey("miTask").list();
    while (!tasks.isEmpty()) {
      for (Task task : tasks) {
        RECORDED_VARIABLES.add(new HashMap<>(runtimeService.getVariablesLocal(task.getExecutionId())));
        taskService.complete(task.getId());
      }
      tasks = taskService.createTaskQuery().taskDefinitionKey("miTask").list();
    }
  }

  private List<Map<String, Object>> orderedSnapshots() {
    return RECORDED_VARIABLES.stream()
        .sorted(Comparator.comparingInt(snapshot -> (Integer) snapshot.get("mappedCounter")))
        .toList();
  }

  private void assertParentVariablesUnchanged(ProcessInstance processInstance, Map<String, Object> variables) {
    assertThat(runtimeService.getVariablesLocal(processInstance.getId())).containsExactlyInAnyOrderEntriesOf(variables);
    assertThat(taskService.createTaskQuery().taskDefinitionKey("after").count()).isOne();
  }

  public static class RecordingDelegate implements JavaDelegate {
    @Override
    public void execute(DelegateExecution execution) {
      if (failDelegate) {
        throw new ProcessEngineException("retry input mapping");
      }
      RECORDED_VARIABLES.add(new HashMap<>(execution.getVariablesLocal()));
    }
  }
}

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
package org.operaton.bpm.engine.test.bpmn.parse;

import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.ActivityTypes;
import org.operaton.bpm.engine.ParseException;
import org.operaton.bpm.engine.Problem;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.impl.bpmn.behavior.BoundaryConditionalEventActivityBehavior;
import org.operaton.bpm.engine.impl.bpmn.behavior.BoundaryEventActivityBehavior;
import org.operaton.bpm.engine.impl.bpmn.behavior.CompensationEventActivityBehavior;
import org.operaton.bpm.engine.impl.bpmn.behavior.EventSubProcessStartConditionalEventActivityBehavior;
import org.operaton.bpm.engine.impl.bpmn.behavior.EventSubProcessStartEventActivityBehavior;
import org.operaton.bpm.engine.impl.bpmn.behavior.IntermediateConditionalEventBehavior;
import org.operaton.bpm.engine.impl.bpmn.behavior.NoneStartEventActivityBehavior;
import org.operaton.bpm.engine.impl.bpmn.behavior.ThrowEscalationEventActivityBehavior;
import org.operaton.bpm.engine.impl.bpmn.helper.BpmnProperties;
import org.operaton.bpm.engine.impl.bpmn.parser.BpmnParse;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.impl.interceptor.CommandExecutor;
import org.operaton.bpm.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.operaton.bpm.engine.impl.pvm.PvmActivity;
import org.operaton.bpm.engine.impl.pvm.PvmTransition;
import org.operaton.bpm.engine.impl.pvm.process.ActivityImpl;
import org.operaton.bpm.engine.impl.pvm.process.ScopeImpl;
import org.operaton.bpm.engine.impl.pvm.process.TransitionImpl;
import org.operaton.bpm.engine.impl.test.TestHelper;
import org.operaton.bpm.engine.repository.DeploymentBuilder;
import org.operaton.bpm.engine.repository.DeploymentWithDefinitions;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineLoggingExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.test.junit5.SystemPropertiesExtension;
import org.operaton.bpm.engine.test.junit5.WatchLogger;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.Assumptions.assumeThat;

/**
 *
 * @author Joram Barrez
 */
class BpmnParseTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);
  @RegisterExtension
  SystemPropertiesExtension systemProperties = SystemPropertiesExtension.resetPropsAfterTest();
  @RegisterExtension
  ProcessEngineLoggingExtension loggingRule = new ProcessEngineLoggingExtension();

  public RepositoryService repositoryService;
  public RuntimeService runtimeService;
  public ProcessEngineConfigurationImpl processEngineConfiguration;

  private Locale defaultLocale;

  @BeforeEach
  void setup() {
    processEngineConfiguration.setEnableXxeProcessing(false);
    defaultLocale = Locale.getDefault();
    Locale.setDefault(Locale.US);
  }

  @AfterEach
  void tearDown() {
    Locale.setDefault(defaultLocale);
    for (org.operaton.bpm.engine.repository.Deployment deployment : repositoryService.createDeploymentQuery().list()) {
      repositoryService.deleteDeployment(deployment.getId(), true);
    }
  }

  @Test
  void testInvalidSubProcessWithTimerStartEvent() {
    String resource = TestHelper.getBpmnProcessDefinitionResource(getClass(), "testInvalidSubProcessWithTimerStartEvent");
    var deploymentBuilder = repositoryService.createDeployment().name(resource).addClasspathResource(resource);
    try {
      deploymentBuilder.deploy();
      fail("Exception expected: Process definition could be parsed, although the sub process contains a timer start event.");
    } catch (ParseException e) {
      testRule.assertTextPresent("timerEventDefinition is not allowed on start event within a subprocess", e.getMessage());
      List<Problem> errors = e.getResourceReports().get(0).getErrors();
      assertThat(errors).hasSize(1);
      assertThat(errors.get(0).getMainElementId()).isEqualTo("StartEvent_3");
    }
  }

  @Test
  void testInvalidSubProcessWithMessageStartEvent() {
    String resource = TestHelper.getBpmnProcessDefinitionResource(getClass(), "testInvalidSubProcessWithMessageStartEvent");
    var deploymentBuilder = repositoryService.createDeployment().name(resource).addClasspathResource(resource);
    try {
      deploymentBuilder.deploy();
      fail("Process definition could be parsed, although the sub process contains not a blanco start event.");
    } catch (ParseException e) {
      testRule.assertTextPresent("messageEventDefinition only allowed on start event if subprocess is an event subprocess", e.getMessage());
      List<Problem> errors = e.getResourceReports().get(0).getErrors();
      assertThat(errors).hasSize(1);
      assertThat(errors.get(0).getMainElementId()).isEqualTo("StartEvent_3");
    }
  }

  @Test
  void testInvalidSubProcessWithoutStartEvent() {
    String resource = TestHelper.getBpmnProcessDefinitionResource(getClass(), "testInvalidSubProcessWithoutStartEvent");
    var deploymentBuilder = repositoryService.createDeployment().name(resource).addClasspathResource(resource);
    try {
      deploymentBuilder.deploy();
      fail("Process definition could be parsed, although the sub process did not contain a start event.");
    } catch (ParseException e) {
      testRule.assertTextPresent("subProcess must define a startEvent element", e.getMessage());
      assertErrors(e.getResourceReports().get(0).getErrors(), "SubProcess_1");
    }
  }

  @Test
  void testInvalidSubProcessWithConditionalStartEvent() {
    String resource = TestHelper.getBpmnProcessDefinitionResource(getClass(), "testInvalidSubProcessWithConditionalStartEvent");
    var deploymentBuilder = repositoryService.createDeployment().name(resource).addClasspathResource(resource);
    try {
      deploymentBuilder.deploy();
      fail("Exception expected: Process definition could be parsed, although the sub process contains a conditional start event.");
    } catch (ParseException e) {
      testRule.assertTextPresent("The content of element 'bpmn2:conditionalEventDefinition' is not complete.", e.getMessage());
      testRule.assertTextPresent("conditionalEventDefinition is not allowed on start event within a subprocess", e.getMessage());
      List<Problem> errors = e.getResourceReports().get(0).getErrors();
      assertThat(errors).hasSize(2);
      assertThat(errors.get(0).getElementIds()).isEmpty();
      assertThat(errors.get(1).getMainElementId()).isEqualTo("StartEvent_2");
    }
  }

  @Test
  void testInvalidSubProcessWithSignalStartEvent() {
    String resource = TestHelper.getBpmnProcessDefinitionResource(getClass(), "testInvalidSubProcessWithSignalStartEvent");
    var deploymentBuilder = repositoryService.createDeployment().name(resource).addClasspathResource(resource);
    try {
      deploymentBuilder.deploy();
      fail("Exception expected: Process definition could be parsed, although the sub process contains a signal start event.");
    } catch (ParseException e) {
      testRule.assertTextPresent("signalEventDefinition only allowed on start event if subprocess is an event subprocess", e.getMessage());
      List<Problem> errors = e.getResourceReports().get(0).getErrors();
      assertThat(errors).hasSize(1);
      assertThat(errors.get(0).getMainElementId()).isEqualTo("StartEvent_3");
    }
  }

  @Test
  void testInvalidSubProcessWithErrorStartEvent() {
    String resource = TestHelper.getBpmnProcessDefinitionResource(getClass(), "testInvalidSubProcessWithErrorStartEvent");
    var deploymentBuilder = repositoryService.createDeployment().name(resource).addClasspathResource(resource);
    try {
      deploymentBuilder.deploy();
      fail("Exception expected: Process definition could be parsed, although the sub process contains a error start event.");
    } catch (ParseException e) {
      testRule.assertTextPresent("errorEventDefinition only allowed on start event if subprocess is an event subprocess", e.getMessage());
      List<Problem> errors = e.getResourceReports().get(0).getErrors();
      assertThat(errors).hasSize(1);
      assertThat(errors.get(0).getMainElementId()).isEqualTo("StartEvent_3");
    }
  }

  @Test
  void testInvalidSubProcessWithEscalationStartEvent() {
    String resource = TestHelper.getBpmnProcessDefinitionResource(getClass(), "testInvalidSubProcessWithEscalationStartEvent");
    var deploymentBuilder = repositoryService.createDeployment().name(resource).addClasspathResource(resource);
    try {
      deploymentBuilder.deploy();
      fail("Exception expected: Process definition could be parsed, although the sub process contains a escalation start event.");
    } catch (ParseException e) {
      testRule.assertTextPresent("escalationEventDefinition is not allowed on start event within a subprocess", e.getMessage());
      List<Problem> errors = e.getResourceReports().get(0).getErrors();
      assertThat(errors).hasSize(1);
      assertThat(errors.get(0).getMainElementId()).isEqualTo("StartEvent_3");
    }
  }

  @Test
  void testInvalidSubProcessWithCompensationStartEvent() {
    String resource = TestHelper.getBpmnProcessDefinitionResource(getClass(), "testInvalidSubProcessWithCompensationStartEvent");
    var deploymentBuilder = repositoryService.createDeployment().name(resource).addClasspathResource(resource);
    try {
      deploymentBuilder.deploy();
      fail("Exception expected: Process definition could be parsed, although the sub process contains a compensation start event.");
    } catch (ParseException e) {
      testRule.assertTextPresent("compensateEventDefinition is not allowed on start event within a subprocess", e.getMessage());
      List<Problem> errors = e.getResourceReports().get(0).getErrors();
      assertThat(errors).hasSize(1);
      assertThat(errors.get(0).getMainElementId()).isEqualTo("StartEvent_3");
    }
  }

  @Test
  void testInvalidTransactionWithMessageStartEvent() {
    String resource = TestHelper.getBpmnProcessDefinitionResource(getClass(), "testInvalidTransactionWithMessageStartEvent");
    var deploymentBuilder = repositoryService.createDeployment().name(resource).addClasspathResource(resource);
    try {
      deploymentBuilder.deploy();
      fail("Process definition could be parsed, although the sub process contains not a blanco start event.");
    } catch (ParseException e) {
      testRule.assertTextPresent("messageEventDefinition only allowed on start event if subprocess is an event subprocess", e.getMessage());
      List<Problem> errors = e.getResourceReports().get(0).getErrors();
      assertThat(errors).hasSize(1);
      assertThat(errors.get(0).getMainElementId()).isEqualTo("StartEvent_2");
    }
  }

  @Test
  void testInvalidTransactionWithTimerStartEvent() {
    String resource = TestHelper.getBpmnProcessDefinitionResource(getClass(), "testInvalidTransactionWithTimerStartEvent");
    var deploymentBuilder = repositoryService.createDeployment().name(resource).addClasspathResource(resource);
    try {
      deploymentBuilder.deploy();
      fail("Exception expected: Process definition could be parsed, although the sub process contains a timer start event.");
    } catch (ParseException e) {
      testRule.assertTextPresent("timerEventDefinition is not allowed on start event within a subprocess", e.getMessage());
      List<Problem> errors = e.getResourceReports().get(0).getErrors();
      assertThat(errors).hasSize(1);
      assertThat(errors.get(0).getMainElementId()).isEqualTo("StartEvent_2");
    }
  }

  @Test
  void testInvalidTransactionWithConditionalStartEvent() {
    String resource = TestHelper.getBpmnProcessDefinitionResource(getClass(), "testInvalidTransactionWithConditionalStartEvent");
    var deploymentBuilder = repositoryService.createDeployment().name(resource).addClasspathResource(resource);
    try {
      deploymentBuilder.deploy();
      fail("Exception expected: Process definition could be parsed, although the sub process contains a conditional start event.");
    } catch (ParseException e) {
      testRule.assertTextPresent("The content of element 'bpmn2:conditionalEventDefinition' is not complete.", e.getMessage());
      testRule.assertTextPresent("conditionalEventDefinition is not allowed on start event within a subprocess", e.getMessage());
      List<Problem> errors = e.getResourceReports().get(0).getErrors();
      assertThat(errors).hasSize(2);
      assertThat(errors.get(0).getElementIds()).isEmpty();
      assertThat(errors.get(1).getMainElementId()).isEqualTo("StartEvent_3");
    }
  }

  @Test
  void testInvalidTransactionWithSignalStartEvent() {
    String resource = TestHelper.getBpmnProcessDefinitionResource(getClass(), "testInvalidTransactionWithSignalStartEvent");
    var deploymentBuilder = repositoryService.createDeployment().name(resource).addClasspathResource(resource);
    try {
      deploymentBuilder.deploy();
      fail("Exception expected: Process definition could be parsed, although the sub process contains a signal start event.");
    } catch (ParseException e) {
      testRule.assertTextPresent("signalEventDefinition only allowed on start event if subprocess is an event subprocess", e.getMessage());
      List<Problem> errors = e.getResourceReports().get(0).getErrors();
      assertThat(errors).hasSize(1);
      assertThat(errors.get(0).getMainElementId()).isEqualTo("StartEvent_2");
    }
  }

  @Test
  void testInvalidTransactionWithErrorStartEvent() {
    String resource = TestHelper.getBpmnProcessDefinitionResource(getClass(), "testInvalidTransactionWithErrorStartEvent");
    var deploymentBuilder = repositoryService.createDeployment().name(resource).addClasspathResource(resource);
    try {
      deploymentBuilder.deploy();
      fail("Exception expected: Process definition could be parsed, although the sub process contains a error start event.");
    } catch (ParseException e) {
      testRule.assertTextPresent("errorEventDefinition only allowed on start event if subprocess is an event subprocess", e.getMessage());
      List<Problem> errors = e.getResourceReports().get(0).getErrors();
      assertThat(errors).hasSize(1);
      assertThat(errors.get(0).getMainElementId()).isEqualTo("StartEvent_2");
    }
  }

  @Test
  void testInvalidTransactionWithEscalationStartEvent() {
    String resource = TestHelper.getBpmnProcessDefinitionResource(getClass(), "testInvalidTransactionWithEscalationStartEvent");
    var deploymentBuilder = repositoryService.createDeployment().name(resource).addClasspathResource(resource);
    try {
      deploymentBuilder.deploy();
      fail("Exception expected: Process definition could be parsed, although the sub process contains a escalation start event.");
    } catch (ParseException e) {
      testRule.assertTextPresent("escalationEventDefinition is not allowed on start event within a subprocess", e.getMessage());
      List<Problem> errors = e.getResourceReports().get(0).getErrors();
      assertThat(errors).hasSize(1);
      assertThat(errors.get(0).getMainElementId()).isEqualTo("StartEvent_2");
    }
  }

  @Test
  void testInvalidTransactionWithCompensationStartEvent() {
    String resource = TestHelper.getBpmnProcessDefinitionResource(getClass(), "testInvalidTransactionWithCompensationStartEvent");
    var deploymentBuilder = repositoryService.createDeployment().name(resource).addClasspathResource(resource);
    try {
      deploymentBuilder.deploy();
      fail("Exception expected: Process definition could be parsed, although the sub process contains a compensation start event.");
    } catch (ParseException e) {
      testRule.assertTextPresent("compensateEventDefinition is not allowed on start event within a subprocess", e.getMessage());
      List<Problem> errors = e.getResourceReports().get(0).getErrors();
      assertThat(errors).hasSize(1);
      assertThat(errors.get(0).getMainElementId()).isEqualTo("StartEvent_2");
    }
  }

  @Test
  void testInvalidProcessDefinition() {
    String resource = TestHelper.getBpmnProcessDefinitionResource(getClass(), "testInvalidProcessDefinition");
    var deploymentBuilder = repositoryService.createDeployment().name(resource).addClasspathResource(resource);
    try {
      deploymentBuilder.deploy();
      fail("");
    } catch (ParseException e) {
      testRule.assertTextPresent("cvc-complex-type.3.2.2:", e.getMessage());
      testRule.assertTextPresent("invalidAttribute", e.getMessage());
      testRule.assertTextPresent("process", e.getMessage());
      assertThat(e.getResourceReports().get(0).getErrors().get(0).getElementIds()).isEmpty();
    }
  }

  @Test
  void testExpressionParsingErrors() {
    String resource = TestHelper.getBpmnProcessDefinitionResource(getClass(), "testExpressionParsingErrors");
    var deploymentBuilder = repositoryService.createDeployment().name(resource).addClasspathResource(resource);
    try {
      deploymentBuilder.deploy();
      fail("Exception expected: Process definition could not be parsed, the expression contains an escalation start event.");
    } catch (ProcessEngineException e) {
      testRule.assertTextPresent("Error parsing '${currentUser()': syntax error at position 15, encountered 'null', expected '}'", e.getMessage());
    }
  }

  @Test
  void testXmlParsingErrors() {
    String resource = TestHelper.getBpmnProcessDefinitionResource(getClass(), "testXMLParsingErrors");
    var deploymentBuilder = repositoryService.createDeployment().name(resource).addClasspathResource(resource);
    try {
      deploymentBuilder.deploy();
      fail("Exception expected: Process definition could not be parsed, the XML contains an escalation start event.");
    } catch (ProcessEngineException e) {
      testRule.assertTextPresent("The end-tag for element type \"bpmndi:BPMNLabel\" must end with a '>' delimiter", e.getMessage());
    }
  }

  @Test
  void testInvalidSequenceFlowInAndOutEventSubProcess() {
    String resource = TestHelper.getBpmnProcessDefinitionResource(getClass(), "testInvalidSequenceFlowInAndOutEventSubProcess");
    var deploymentBuilder = repositoryService.createDeployment().name(resource).addClasspathResource(resource);
    try {
      deploymentBuilder.deploy();
      fail("Exception expected: Process definition could be parsed, although the sub process has incoming and outgoing sequence flows");
    } catch (ParseException e) {
      testRule.assertTextPresent("start event of event subprocess must be of type 'error', 'message', 'timer', 'signal', 'compensation' or 'escalation'", e.getMessage());
      testRule.assertTextPresent("Invalid incoming sequence flow of event subprocess", e.getMessage());
      testRule.assertTextPresent("Invalid outgoing sequence flow of event subprocess", e.getMessage());
      List<Problem> errors = e.getResourceReports().get(0).getErrors();
      assertThat(errors).hasSize(3);
      assertThat(errors.get(0).getMainElementId()).isEqualTo("StartEvent_2");
      assertThat(errors.get(1).getMainElementId()).isEqualTo("SequenceFlow_2");
      assertThat(errors.get(2).getMainElementId()).isEqualTo("SequenceFlow_1");
    }
  }

  @Test
  void testInvalidProcessWithoutStartEvent() {
    String resource = TestHelper.getBpmnProcessDefinitionResource(getClass(), "testInvalidProcessWithoutStartEvent");
    var deploymentBuilder = repositoryService.createDeployment().name(resource).addClasspathResource(resource);
    try {
      deploymentBuilder.deploy();
      fail("Process definition could be parsed, although the process did not contain a start event.");
    } catch (ParseException e) {
      testRule.assertTextPresent("process must define a startEvent element", e.getMessage());
      assertErrors(e.getResourceReports().get(0).getErrors(), "Process_1");
    }
  }

  /**
   * this test case check if the multiple start event is supported the test case
   * doesn't fail in this behavior because the {@link BpmnParse} parse the event
   * definitions with if-else, this means only the first event definition is
   * taken
   **/
  @Test
  void testParseMultipleStartEvent() {
    String resource = TestHelper.getBpmnProcessDefinitionResource(getClass(), "testParseMultipleStartEvent");
    var deploymentBuilder = repositoryService.createDeployment().name(resource).addClasspathResource(resource);
    try {
      deploymentBuilder.deploy();
      fail("");
    } catch (ParseException e) {
      // fail in "regular" subprocess
      testRule.assertTextPresent("timerEventDefinition is not allowed on start event within a subprocess", e.getMessage());
      testRule.assertTextPresent("messageEventDefinition only allowed on start event if subprocess is an event subprocess", e.getMessage());
      // doesn't fail in event subprocess/process because the bpmn parser parse
      // only this first event definition
      List<Problem> errors = e.getResourceReports().get(0).getErrors();
      assertThat(errors).hasSize(2);
      assertThat(errors.get(0).getMainElementId()).isEqualTo("startSubProcess");
      assertThat(errors.get(1).getMainElementId()).isEqualTo("startSubProcess");
    }
  }

  @Test
  void testParseWithBpmnNamespacePrefix() {
    repositoryService.createDeployment()
        .addClasspathResource("org/operaton/bpm/engine/test/bpmn/parse/BpmnParseTest.testParseWithBpmnNamespacePrefix.bpmn20.xml").deploy();
    assertThat(repositoryService.createProcessDefinitionQuery().count()).isOne();
  }

  @Test
  void testParseWithMultipleDocumentation() {
    repositoryService.createDeployment()
        .addClasspathResource("org/operaton/bpm/engine/test/bpmn/parse/BpmnParseTest.testParseWithMultipleDocumentation.bpmn20.xml").deploy();
    assertThat(repositoryService.createProcessDefinitionQuery().count()).isOne();
  }

  @Test
  void testParseCollaborationPlane() {
    repositoryService.createDeployment().addClasspathResource("org/operaton/bpm/engine/test/bpmn/parse/BpmnParseTest.testParseCollaborationPlane.bpmn").deploy();
    assertThat(repositoryService.createProcessDefinitionQuery().count()).isOne();
  }

  @Test
  void testInvalidAsyncAfterEventBasedGateway() {
    String resource = TestHelper.getBpmnProcessDefinitionResource(getClass(), "testInvalidAsyncAfterEventBasedGateway");
    var deploymentBuilder = repositoryService.createDeployment().name(resource).addClasspathResource(resource);
    try {
      deploymentBuilder.deploy();
      fail("");
    } catch (ParseException e) {
      // fail on asyncAfter
      testRule.assertTextPresent("'asyncAfter' not supported for", e.getMessage());
      assertErrors(e.getResourceReports().get(0).getErrors(), "eventBasedGateway");
    }
  }

  @Deployment
  @Test
  void testParseDiagramInterchangeElements() {

    // Graphical information is not yet exposed publicly, so we need to do some
    // plumbing
    CommandExecutor commandExecutor = processEngineConfiguration.getCommandExecutorTxRequired();
    ProcessDefinitionEntity processDefinitionEntity = commandExecutor.execute(commandContext -> Context.getProcessEngineConfiguration().getDeploymentCache().findDeployedLatestProcessDefinitionByKey("myProcess"));

    assertThat(processDefinitionEntity).isNotNull();
    assertThat(processDefinitionEntity.getActivities()).hasSize(7);

    // Check if diagram has been created based on Diagram Interchange when it's
    // not a headless instance
    List<String> resourceNames = repositoryService.getDeploymentResourceNames(processDefinitionEntity.getDeploymentId());
    if (processEngineConfiguration.isCreateDiagramOnDeploy()) {
      assertThat(resourceNames).hasSize(2);
    } else {
      assertThat(resourceNames).hasSize(1);
    }

    for (ActivityImpl activity : processDefinitionEntity.getActivities()) {

      if ("theStart".equals(activity.getId())) {
        assertActivityBounds(activity, 70, 255, 30, 30);
      } else if ("task1".equals(activity.getId())) {
        assertActivityBounds(activity, 176, 230, 100, 80);
      } else if ("gateway1".equals(activity.getId())) {
        assertActivityBounds(activity, 340, 250, 40, 40);
      } else if ("task2".equals(activity.getId())) {
        assertActivityBounds(activity, 445, 138, 100, 80);
      } else if ("gateway2".equals(activity.getId())) {
        assertActivityBounds(activity, 620, 250, 40, 40);
      } else if ("task3".equals(activity.getId())) {
        assertActivityBounds(activity, 453, 304, 100, 80);
      } else if ("theEnd".equals(activity.getId())) {
        assertActivityBounds(activity, 713, 256, 28, 28);
      }

      for (PvmTransition sequenceFlow : activity.getOutgoingTransitions()) {
        assertThat(((TransitionImpl) sequenceFlow).getWaypoints()).hasSizeGreaterThanOrEqualTo(4);

        TransitionImpl transitionImpl = (TransitionImpl) sequenceFlow;
        if ("flowStartToTask1".equals(transitionImpl.getId())) {
          assertSequenceFlowWayPoints(transitionImpl, 100, 270, 176, 270);
        } else if ("flowTask1ToGateway1".equals(transitionImpl.getId())) {
          assertSequenceFlowWayPoints(transitionImpl, 276, 270, 340, 270);
        } else if ("flowGateway1ToTask2".equals(transitionImpl.getId())) {
          assertSequenceFlowWayPoints(transitionImpl, 360, 250, 360, 178, 445, 178);
        } else if ("flowGateway1ToTask3".equals(transitionImpl.getId())) {
          assertSequenceFlowWayPoints(transitionImpl, 360, 290, 360, 344, 453, 344);
        } else if ("flowTask2ToGateway2".equals(transitionImpl.getId())) {
          assertSequenceFlowWayPoints(transitionImpl, 545, 178, 640, 178, 640, 250);
        } else if ("flowTask3ToGateway2".equals(transitionImpl.getId())) {
          assertSequenceFlowWayPoints(transitionImpl, 553, 344, 640, 344, 640, 290);
        } else if ("flowGateway2ToEnd".equals(transitionImpl.getId())) {
          assertSequenceFlowWayPoints(transitionImpl, 660, 270, 713, 270);
        }

      }
    }
  }

  @Deployment
  @Test
  void testParseNamespaceInConditionExpressionType() {
    CommandExecutor commandExecutor = processEngineConfiguration.getCommandExecutorTxRequired();
    ProcessDefinitionEntity processDefinitionEntity = commandExecutor.execute(commandContext -> Context.getProcessEngineConfiguration().getDeploymentCache().findDeployedLatestProcessDefinitionByKey("resolvableNamespacesProcess"));

    // Test that the process definition has been deployed
    assertThat(processDefinitionEntity).isNotNull();
    PvmActivity activity = processDefinitionEntity.findActivity("ExclusiveGateway_1");
    assertThat(activity).isNotNull();

    // Test that the conditions has been resolved
    for (PvmTransition transition : activity.getOutgoingTransitions()) {
      if ("Task_2".equals(transition.getDestination().getId())) {
        assertThat(transition.getProperty("conditionText")).isEqualTo("#{approved}");
      } else if ("Task_3".equals(transition.getDestination().getId())) {
        assertThat(transition.getProperty("conditionText")).isEqualTo("#{!approved}");
      } else {
        fail("Something went wrong");
      }

    }
  }

  @Deployment
  @Test
  void testParseDiagramInterchangeElementsForUnknownModelElements() {
    // test behavior defined by @Deployment annotation
  }

  /**
   * We want to make sure that BPMNs created with the namespace http://activiti.org/bpmn still work.
   */
  @Test
  @Deployment
  void testParseDefinitionWithDeprecatedActivitiNamespace(){
    // test behavior defined by @Deployment annotation
  }

  @Test
  @Deployment
  void testParseDefinitionWithOperatonNamespace(){
    // test behavior defined by @Deployment annotation
  }

  @Deployment
  @Test
  void testParseCompensationEndEvent() {
    ActivityImpl endEvent = findActivityInDeployedProcessDefinition("end");

    assertThat(endEvent.getProperty("type")).isEqualTo("compensationEndEvent");
    assertThat(endEvent.getProperty(BpmnParse.PROPERTYNAME_THROWS_COMPENSATION)).isEqualTo(Boolean.TRUE);
    assertThat(endEvent.getActivityBehavior().getClass()).isEqualTo(CompensationEventActivityBehavior.class);
  }

  @Deployment
  @Test
  void testParseCompensationStartEvent() {
    ActivityImpl compensationStartEvent = findActivityInDeployedProcessDefinition("compensationStartEvent");

    assertThat(compensationStartEvent.getProperty("type")).isEqualTo("compensationStartEvent");
    assertThat(compensationStartEvent.getActivityBehavior().getClass()).isEqualTo(EventSubProcessStartEventActivityBehavior.class);

    ActivityImpl compensationEventSubProcess = (ActivityImpl) compensationStartEvent.getFlowScope();
    assertThat(compensationEventSubProcess.getProperty(BpmnParse.PROPERTYNAME_IS_FOR_COMPENSATION)).isEqualTo(Boolean.TRUE);

    ScopeImpl subprocess = compensationEventSubProcess.getFlowScope();
    assertThat(subprocess.getProperty(BpmnParse.PROPERTYNAME_COMPENSATION_HANDLER_ID)).isEqualTo(compensationEventSubProcess.getActivityId());
  }

  @Deployment
  @Test
  void testParseAsyncMultiInstanceBody(){
    ActivityImpl innerTask = findActivityInDeployedProcessDefinition("miTask");
    ActivityImpl miBody = innerTask.getParentFlowScopeActivity();

    assertThat(miBody.isAsyncBefore()).isTrue();
    assertThat(miBody.isAsyncAfter()).isTrue();

    assertThat(innerTask.isAsyncBefore()).isFalse();
    assertThat(innerTask.isAsyncAfter()).isFalse();
  }

  @Deployment
  @Test
  void testParseAsyncActivityWrappedInMultiInstanceBody(){
    ActivityImpl innerTask = findActivityInDeployedProcessDefinition("miTask");
    assertThat(innerTask.isAsyncBefore()).isTrue();
    assertThat(innerTask.isAsyncAfter()).isTrue();

    ActivityImpl miBody = innerTask.getParentFlowScopeActivity();
    assertThat(miBody.isAsyncBefore()).isFalse();
    assertThat(miBody.isAsyncAfter()).isFalse();
  }

  @Deployment
  @Test
  void testParseAsyncActivityWrappedInMultiInstanceBodyWithAsyncMultiInstance(){
    ActivityImpl innerTask = findActivityInDeployedProcessDefinition("miTask");
    assertThat(innerTask.isAsyncBefore()).isTrue();
    assertThat(innerTask.isAsyncAfter()).isFalse();

    ActivityImpl miBody = innerTask.getParentFlowScopeActivity();
    assertThat(miBody.isAsyncBefore()).isFalse();
    assertThat(miBody.isAsyncAfter()).isTrue();
  }

  @Test
  void testParseSwitchedSourceAndTargetRefsForAssociations() {
    repositoryService.createDeployment()
        .addClasspathResource("org/operaton/bpm/engine/test/bpmn/parse/BpmnParseTest.testParseSwitchedSourceAndTargetRefsForAssociations.bpmn20.xml").deploy();

    assertThat(repositoryService.createProcessDefinitionQuery().count()).isOne();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/event/compensate/CompensateEventTest.compensationMiActivity.bpmn20.xml")
  @Test
  @SuppressWarnings("deprecation")
  void testParseCompensationHandlerOfMiActivity() {
    ActivityImpl miActivity = findActivityInDeployedProcessDefinition("undoBookHotel");
    ScopeImpl flowScope = miActivity.getFlowScope();

    assertThat(flowScope.getProperty(BpmnParse.PROPERTYNAME_TYPE)).isEqualTo(ActivityTypes.MULTI_INSTANCE_BODY);
    assertThat(((ActivityImpl) flowScope).getActivityId()).isEqualTo("bookHotel" + BpmnParse.MULTI_INSTANCE_BODY_ID_SUFFIX);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/event/compensate/CompensateEventTest.compensationMiSubprocess.bpmn20.xml")
  @Test
  @SuppressWarnings("deprecation")
  void testParseCompensationHandlerOfMiSubprocess() {
    ActivityImpl miActivity = findActivityInDeployedProcessDefinition("undoBookHotel");
    ScopeImpl flowScope = miActivity.getFlowScope();

    assertThat(flowScope.getProperty(BpmnParse.PROPERTYNAME_TYPE)).isEqualTo(ActivityTypes.MULTI_INSTANCE_BODY);
    assertThat(((ActivityImpl) flowScope).getActivityId()).isEqualTo("scope" + BpmnParse.MULTI_INSTANCE_BODY_ID_SUFFIX);
  }

  @Deployment
  @Test
  void testParseSignalStartEvent(){
    ActivityImpl signalStartActivity = findActivityInDeployedProcessDefinition("start");

    assertThat(signalStartActivity.getProperty("type")).isEqualTo(ActivityTypes.START_EVENT_SIGNAL);
    assertThat(signalStartActivity.getActivityBehavior().getClass()).isEqualTo(NoneStartEventActivityBehavior.class);
  }

  @Deployment
  @Test
  void testParseEscalationBoundaryEvent() {
    ActivityImpl escalationBoundaryEvent = findActivityInDeployedProcessDefinition("escalationBoundaryEvent");

    assertThat(escalationBoundaryEvent.getProperties().get(BpmnProperties.TYPE)).isEqualTo(ActivityTypes.BOUNDARY_ESCALATION);
    assertThat(escalationBoundaryEvent.getActivityBehavior().getClass()).isEqualTo(BoundaryEventActivityBehavior.class);
  }

  @Deployment
  @Test
  void testParseEscalationIntermediateThrowingEvent() {
    ActivityImpl escalationThrowingEvent = findActivityInDeployedProcessDefinition("escalationThrowingEvent");

    assertThat(escalationThrowingEvent.getProperties().get(BpmnProperties.TYPE)).isEqualTo(ActivityTypes.INTERMEDIATE_EVENT_ESCALATION_THROW);
    assertThat(escalationThrowingEvent.getActivityBehavior().getClass()).isEqualTo(ThrowEscalationEventActivityBehavior.class);
  }

  @Deployment
  @Test
  void testParseEscalationEndEvent() {
    ActivityImpl escalationEndEvent = findActivityInDeployedProcessDefinition("escalationEndEvent");

    assertThat(escalationEndEvent.getProperties().get(BpmnProperties.TYPE)).isEqualTo(ActivityTypes.END_EVENT_ESCALATION);
    assertThat(escalationEndEvent.getActivityBehavior().getClass()).isEqualTo(ThrowEscalationEventActivityBehavior.class);
  }

  @Deployment
  @Test
  void testParseEscalationStartEvent() {
    ActivityImpl escalationStartEvent = findActivityInDeployedProcessDefinition("escalationStartEvent");

    assertThat(escalationStartEvent.getProperties().get(BpmnProperties.TYPE)).isEqualTo(ActivityTypes.START_EVENT_ESCALATION);
    assertThat(escalationStartEvent.getActivityBehavior().getClass()).isEqualTo(EventSubProcessStartEventActivityBehavior.class);
  }


  public void parseInvalidConditionalEvent(String processDefinitionResource, String elementId) {
    String resource = TestHelper.getBpmnProcessDefinitionResource(getClass(), processDefinitionResource);
    var deploymentBuilder = repositoryService.createDeployment().name(resource).addClasspathResource(resource);
    try {
      deploymentBuilder.deploy();
      fail("Exception expected: Process definition could be parsed, conditional event definition contains no condition.");
    } catch (ParseException e) {
      testRule.assertTextPresent("The content of element 'bpmn:conditionalEventDefinition' is not complete.", e.getMessage());
      testRule.assertTextPresent("Conditional event must contain an expression for evaluation.", e.getMessage());
      List<Problem> errors = e.getResourceReports().get(0).getErrors();
      assertThat(errors).hasSize(2);
      assertThat(errors.get(0).getElementIds()).isEmpty();
      assertThat(errors.get(1).getMainElementId()).isEqualTo(elementId);
    }
  }

  @Test
  void testParseInvalidConditionalBoundaryEvent() {
    parseInvalidConditionalEvent("testParseInvalidConditionalBoundaryEvent", "conditionalBoundaryEvent");
  }

  @Deployment
  @Test
  void testParseConditionalBoundaryEvent() {
    ActivityImpl conditionalBoundaryEvent = findActivityInDeployedProcessDefinition("conditionalBoundaryEvent");

    assertThat(conditionalBoundaryEvent.getProperties().get(BpmnProperties.TYPE)).isEqualTo(ActivityTypes.BOUNDARY_CONDITIONAL);
    assertThat(conditionalBoundaryEvent.getActivityBehavior().getClass()).isEqualTo(BoundaryConditionalEventActivityBehavior.class);
  }

  @Deployment
  @Test
  void testParseAsyncBoundaryEvent() {
    ActivityImpl conditionalBoundaryEvent1 = findActivityInDeployedProcessDefinition("conditionalBoundaryEvent1");
    ActivityImpl conditionalBoundaryEvent2 = findActivityInDeployedProcessDefinition("conditionalBoundaryEvent2");

    assertThat(conditionalBoundaryEvent1.isAsyncAfter()).isTrue();
    assertThat(conditionalBoundaryEvent1.isAsyncBefore()).isTrue();

    assertThat(conditionalBoundaryEvent2.isAsyncAfter()).isFalse();
    assertThat(conditionalBoundaryEvent2.isAsyncBefore()).isFalse();
  }

  @Test
  void testParseInvalidIntermediateConditionalEvent() {
    parseInvalidConditionalEvent("testParseInvalidIntermediateConditionalEvent", "intermediateConditionalEvent");
  }

  @Deployment
  @Test
  void testParseIntermediateConditionalEvent() {
    ActivityImpl intermediateConditionalEvent = findActivityInDeployedProcessDefinition("intermediateConditionalEvent");

    assertThat(intermediateConditionalEvent.getProperties().get(BpmnProperties.TYPE)).isEqualTo(ActivityTypes.INTERMEDIATE_EVENT_CONDITIONAL);
    assertThat(intermediateConditionalEvent.getActivityBehavior().getClass()).isEqualTo(IntermediateConditionalEventBehavior.class);
  }

  @Test
  void testParseInvalidEventSubprocessConditionalStartEvent() {
    parseInvalidConditionalEvent("testParseInvalidEventSubprocessConditionalStartEvent", "conditionalStartEventSubProcess");
  }

  @Deployment
  @Test
  void testParseEventSubprocessConditionalStartEvent() {
    ActivityImpl conditionalStartEventSubProcess = findActivityInDeployedProcessDefinition("conditionalStartEventSubProcess");

    assertThat(conditionalStartEventSubProcess.getProperties().get(BpmnProperties.TYPE)).isEqualTo(ActivityTypes.START_EVENT_CONDITIONAL);
    assertThat(conditionalStartEventSubProcess.getActivityBehavior().getClass()).isEqualTo(EventSubProcessStartConditionalEventActivityBehavior.class);

  }

  protected void assertActivityBounds(ActivityImpl activity, int x, int y, int width, int height) {
    assertThat(activity.getX()).isEqualTo(x);
    assertThat(activity.getY()).isEqualTo(y);
    assertThat(activity.getWidth()).isEqualTo(width);
    assertThat(activity.getHeight()).isEqualTo(height);
  }

  protected void assertSequenceFlowWayPoints(TransitionImpl sequenceFlow, Integer... waypoints) {
    assertThat(sequenceFlow.getWaypoints()).hasSize(waypoints.length);
    for (int i = 0; i < waypoints.length; i++) {
      assertThat(sequenceFlow.getWaypoints().get(i)).isEqualTo(waypoints[i]);
    }
  }

  protected ActivityImpl findActivityInDeployedProcessDefinition(String activityId) {
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    assertThat(processDefinition).isNotNull();

    ProcessDefinitionEntity cachedProcessDefinition = processEngineConfiguration.getDeploymentCache()
                                                        .getProcessDefinitionCache()
                                                        .get(processDefinition.getId());
    return cachedProcessDefinition.findActivity(activityId);
  }

  @Test
  void testNoOperatonInSourceThrowsError() {
    String resource = TestHelper.getBpmnProcessDefinitionResource(getClass(), "testNoOperatonInSourceThrowsError");
    var deploymentBuilder = repositoryService.createDeployment().name(resource).addClasspathResource(resource);
    try {
      deploymentBuilder.deploy();
      fail("Process operaton:in extension element should contain source!");
    } catch (ParseException e) {
      testRule.assertTextPresent("Missing parameter 'source' or 'sourceExpression' when passing variables", e.getMessage());
      testRule.assertTextPresent("Missing attribute 'target' when attribute 'source' or 'sourceExpression' is set", e.getMessage());
      List<Problem> errors = e.getResourceReports().get(0).getErrors();
      assertThat(errors).hasSize(2);
      assertThat(errors.get(0).getMainElementId()).isEqualTo("callActivity");
      assertThat(errors.get(1).getMainElementId()).isEqualTo("callActivity");
    }
  }

  @Test
  void testNoOperatonInSourceShouldWithoutValidation() {
    try {
      processEngineConfiguration.setDisableStrictCallActivityValidation(true);

      String resource = TestHelper.getBpmnProcessDefinitionResource(getClass(), "testNoOperatonInSourceThrowsError");
      assertThatCode(() -> repositoryService.createDeployment().name(resource).addClasspathResource(resource).deploy()).doesNotThrowAnyException();
    } finally {
      processEngineConfiguration.setDisableStrictCallActivityValidation(false);
    }
  }

  @Test
  void testEmptyOperatonInSourceThrowsError() {
    String resource = TestHelper.getBpmnProcessDefinitionResource(getClass(), "testEmptyOperatonInSourceThrowsError");
    var deploymentBuilder = repositoryService.createDeployment().name(resource).addClasspathResource(resource);
    try {
      deploymentBuilder.deploy();
      fail("Process operaton:in extension element should contain source!");
    } catch (ParseException e) {
      testRule.assertTextPresent("Empty attribute 'source' when passing variables", e.getMessage());
      testRule.assertTextPresent("Missing attribute 'target' when attribute 'source' or 'sourceExpression' is set", e.getMessage());
      List<Problem> errors = e.getResourceReports().get(0).getErrors();
      assertThat(errors).hasSize(2);
      assertThat(errors.get(0).getMainElementId()).isEqualTo("callActivity");
      assertThat(errors.get(1).getMainElementId()).isEqualTo("callActivity");
    }
  }

  @Test
  void testEmptyOperatonInSourceWithoutValidation() {
    try {
      processEngineConfiguration.setDisableStrictCallActivityValidation(true);

      String resource = TestHelper.getBpmnProcessDefinitionResource(getClass(), "testEmptyOperatonInSourceThrowsError");
      assertThatCode(() -> repositoryService.createDeployment().name(resource).addClasspathResource(resource).deploy()).doesNotThrowAnyException();
    } finally {
      processEngineConfiguration.setDisableStrictCallActivityValidation(false);
    }
  }

  @Test
  void testNoOperatonInTargetThrowsError() {
    String resource = TestHelper.getBpmnProcessDefinitionResource(getClass(), "testNoOperatonInTargetThrowsError");
    var deploymentBuilder = repositoryService.createDeployment().name(resource).addClasspathResource(resource);
    try {
      deploymentBuilder.deploy();
      fail("Process operaton:in extension element should contain target!");
    } catch (ParseException e) {
      testRule.assertTextPresent("Missing attribute 'target' when attribute 'source' or 'sourceExpression' is set", e.getMessage());
      assertErrors(e.getResourceReports().get(0).getErrors(), "callActivity");
    }
  }

  @Test
  void testNoOperatonInTargetWithoutValidation() {
    processEngineConfiguration.setDisableStrictCallActivityValidation(true);
    String resource = TestHelper.getBpmnProcessDefinitionResource(getClass(), "testNoOperatonInTargetThrowsError");
    var deploymentBuilder = repositoryService.createDeployment().name(resource).addClasspathResource(resource);
    try {
      deploymentBuilder.deploy();
      fail("Process operaton:in extension element should contain target!");
    } catch (ParseException e) {
      testRule.assertTextPresent("Missing attribute 'target' when attribute 'source' or 'sourceExpression' is set", e.getMessage());
      assertErrors(e.getResourceReports().get(0).getErrors(), "callActivity");
    } finally {
      processEngineConfiguration.setDisableStrictCallActivityValidation(false);
    }
  }

  @Test
  void testEmptyOperatonInTargetThrowsError() {
    String resource = TestHelper.getBpmnProcessDefinitionResource(getClass(), "testEmptyOperatonInTargetThrowsError");
    var deploymentBuilder = repositoryService.createDeployment().name(resource).addClasspathResource(resource);
    try {
      deploymentBuilder.deploy();
      fail("Process operaton:in extension element should contain target!");
    } catch (ParseException e) {
      testRule.assertTextPresent("Empty attribute 'target' when attribute 'source' or 'sourceExpression' is set", e.getMessage());
      assertErrors(e.getResourceReports().get(0).getErrors(), "callActivity");
    }
  }

  @Test
  void testEmptyOperatonInTargetWithoutValidation() {
    try {
      processEngineConfiguration.setDisableStrictCallActivityValidation(true);

      String resource = TestHelper.getBpmnProcessDefinitionResource(getClass(), "testEmptyOperatonInTargetThrowsError");
      assertThatCode(() -> repositoryService.createDeployment().name(resource).addClasspathResource(resource).deploy()).doesNotThrowAnyException();
    } finally {
      processEngineConfiguration.setDisableStrictCallActivityValidation(false);
    }
  }

  @Test
  void testNoOperatonOutSourceThrowsError() {
    String resource = TestHelper.getBpmnProcessDefinitionResource(getClass(), "testNoOperatonOutSourceThrowsError");
    var deploymentBuilder = repositoryService.createDeployment().name(resource).addClasspathResource(resource);
    try {
      deploymentBuilder.deploy();
      fail("Process operaton:out extension element should contain source!");
    } catch (ParseException e) {
      testRule.assertTextPresent("Missing parameter 'source' or 'sourceExpression' when passing variables", e.getMessage());
      testRule.assertTextPresent("Missing attribute 'target' when attribute 'source' or 'sourceExpression' is set", e.getMessage());
      List<Problem> errors = e.getResourceReports().get(0).getErrors();
      assertThat(errors).hasSize(2);
      assertThat(errors.get(0).getMainElementId()).isEqualTo("callActivity");
      assertThat(errors.get(1).getMainElementId()).isEqualTo("callActivity");
    }
  }

  @Test
  void testNoOperatonOutSourceWithoutValidation() {
    try {
      processEngineConfiguration.setDisableStrictCallActivityValidation(true);

      String resource = TestHelper.getBpmnProcessDefinitionResource(getClass(), "testNoOperatonOutSourceThrowsError");
      assertThatCode(() -> repositoryService.createDeployment().name(resource).addClasspathResource(resource).deploy()).doesNotThrowAnyException();
    } finally {
      processEngineConfiguration.setDisableStrictCallActivityValidation(false);
    }
  }

  @Test
  void testEmptyOperatonOutSourceThrowsError() {
    String resource = TestHelper.getBpmnProcessDefinitionResource(getClass(), "testEmptyOperatonOutSourceThrowsError");
    var deploymentBuilder = repositoryService.createDeployment().name(resource).addClasspathResource(resource);
    try {
      deploymentBuilder.deploy();
      fail("Process operaton:out extension element should contain source!");
    } catch (ParseException e) {
      testRule.assertTextPresent("Empty attribute 'source' when passing variables", e.getMessage());
      testRule.assertTextPresent("Missing attribute 'target' when attribute 'source' or 'sourceExpression' is set", e.getMessage());
      List<Problem> errors = e.getResourceReports().get(0).getErrors();
      assertThat(errors).hasSize(2);
      assertThat(errors.get(0).getMainElementId()).isEqualTo("callActivity");
      assertThat(errors.get(1).getMainElementId()).isEqualTo("callActivity");
    }
  }

  @Test
  void testEmptyOperatonOutSourceWithoutValidation() {
    try {
      processEngineConfiguration.setDisableStrictCallActivityValidation(true);

      String resource = TestHelper.getBpmnProcessDefinitionResource(getClass(), "testEmptyOperatonOutSourceThrowsError");
      assertThatCode(() -> repositoryService.createDeployment().name(resource).addClasspathResource(resource).deploy()).doesNotThrowAnyException();
    } finally {
      processEngineConfiguration.setDisableStrictCallActivityValidation(false);
    }
  }

  @Test
  void testNoOperatonOutTargetThrowsError() {
    String resource = TestHelper.getBpmnProcessDefinitionResource(getClass(), "testNoOperatonOutTargetThrowsError");
    var deploymentBuilder = repositoryService.createDeployment().name(resource).addClasspathResource(resource);
    try {
      deploymentBuilder.deploy();
      fail("Process operaton:out extension element should contain target!");
    } catch (ParseException e) {
      testRule.assertTextPresent("Missing attribute 'target' when attribute 'source' or 'sourceExpression' is set", e.getMessage());
      assertErrors(e.getResourceReports().get(0).getErrors(), "callActivity");
    }
  }

  @Test
  void testNoOperatonOutTargetWithoutValidation() {
    processEngineConfiguration.setDisableStrictCallActivityValidation(true);
    String resource = TestHelper.getBpmnProcessDefinitionResource(getClass(), "testNoOperatonOutTargetThrowsError");
    var deploymentBuilder = repositoryService.createDeployment().name(resource).addClasspathResource(resource);
    try {
      deploymentBuilder.deploy();
      fail("Process operaton:out extension element should contain target!");
    } catch (ParseException e) {
      testRule.assertTextPresent("Missing attribute 'target' when attribute 'source' or 'sourceExpression' is set", e.getMessage());
      assertErrors(e.getResourceReports().get(0).getErrors(), "callActivity");
    } finally {
      processEngineConfiguration.setDisableStrictCallActivityValidation(false);
    }
  }

  @Test
  void testEmptyOperatonOutTargetThrowsError() {
    String resource = TestHelper.getBpmnProcessDefinitionResource(getClass(), "testEmptyOperatonOutTargetThrowsError");
    var deploymentBuilder = repositoryService.createDeployment().name(resource).addClasspathResource(resource);
    try {
      deploymentBuilder.deploy();
      fail("Process operaton:out extension element should contain target!");
    } catch (ParseException e) {
      testRule.assertTextPresent("Empty attribute 'target' when attribute 'source' or 'sourceExpression' is set", e.getMessage());
      assertErrors(e.getResourceReports().get(0).getErrors(), "callActivity");
    }
  }

  @Test
  void testEmptyOperatonOutTargetWithoutValidation() {
    try {
      processEngineConfiguration.setDisableStrictCallActivityValidation(true);

      String resource = TestHelper.getBpmnProcessDefinitionResource(getClass(), "testEmptyOperatonOutTargetThrowsError");
      assertThatCode(() -> repositoryService.createDeployment().name(resource).addClasspathResource(resource).deploy()).doesNotThrowAnyException();
    } finally {
      processEngineConfiguration.setDisableStrictCallActivityValidation(false);
    }
  }

  @Deployment
  @Test
  void testParseProcessDefinitionTtl() {
    List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery().list();
    assertThat(processDefinitions)
            .isNotNull()
            .hasSize(1);

    Integer timeToLive = processDefinitions.get(0).getHistoryTimeToLive();
    assertThat(timeToLive).isNotNull();
    assertThat(timeToLive.intValue()).isEqualTo(5);

    assertThat(processDefinitions.get(0).isStartableInTasklist()).isTrue();
  }

  @Deployment
  @Test
  void testParseProcessDefinitionStringTtl() {
    List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery().list();
    assertThat(processDefinitions)
            .isNotNull()
            .hasSize(1);

    Integer timeToLive = processDefinitions.get(0).getHistoryTimeToLive();
    assertThat(timeToLive).isNotNull();
    assertThat(timeToLive.intValue()).isEqualTo(5);
  }

  @Test
  void testParseProcessDefinitionMalformedStringTtl() {
    String resource = TestHelper.getBpmnProcessDefinitionResource(getClass(), "testParseProcessDefinitionMalformedStringTtl");
    var deploymentBuilder = repositoryService.createDeployment().name(resource).addClasspathResource(resource);
    try {
      deploymentBuilder.deploy();
      fail("Exception expected: Process definition historyTimeToLive value can not be parsed.");
    } catch (ParseException e) {
      testRule.assertTextPresent("Cannot parse historyTimeToLive", e.getMessage());
      assertErrors(e.getResourceReports().get(0).getErrors(), "oneTaskProcess");
    }
  }

  @Deployment
  @Test
  void testParseProcessDefinitionEmptyTtl() {
    List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery().list();
    assertThat(processDefinitions)
            .isNotNull()
            .hasSize(1);

    Integer timeToLive = processDefinitions.get(0).getHistoryTimeToLive();
    assertThat(timeToLive).isNull();
  }

  @Deployment
  @Test
  void testParseProcessDefinitionWithoutTtl() {
    List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery().list();
    assertThat(processDefinitions)
            .isNotNull()
            .hasSize(1);

    Integer timeToLive = processDefinitions.get(0).getHistoryTimeToLive();
    assertThat(timeToLive).isNull();
  }

  @Test
  void testParseProcessDefinitionWithoutTtlWithConfigDefault() {
    processEngineConfiguration.setHistoryTimeToLive("6");
    try {
      String resource = TestHelper.getBpmnProcessDefinitionResource(getClass(), "testParseProcessDefinitionWithoutTtl");
      repositoryService.createDeployment().name(resource).addClasspathResource(resource).deploy();
      List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery().list();
      assertThat(processDefinitions)
              .isNotNull()
              .hasSize(1);

      Integer timeToLive = processDefinitions.get(0).getHistoryTimeToLive();
      assertThat(timeToLive).isNotNull();
      assertThat(timeToLive.intValue()).isEqualTo(6);
    } finally {
      processEngineConfiguration.setHistoryTimeToLive(null);
    }
  }

  @Test
  void testParseProcessDefinitionWithoutTtlWithMalformedConfigDefault() {
    processEngineConfiguration.setHistoryTimeToLive("PP555DDD");
    String resource = TestHelper.getBpmnProcessDefinitionResource(getClass(), "testParseProcessDefinitionWithoutTtl");
    var deploymentBuilder = repositoryService.createDeployment().name(resource).addClasspathResource(resource);
    try {
      deploymentBuilder.deploy();
      fail("Exception expected: Process definition historyTimeToLive value can not be parsed.");
    } catch (ParseException e) {
      testRule.assertTextPresent("Cannot parse historyTimeToLive", e.getMessage());
    } finally {
      processEngineConfiguration.setHistoryTimeToLive(null);
    }
  }

  @Test
  void testParseProcessDefinitionWithoutTtlWithInvalidConfigDefault() {
    processEngineConfiguration.setHistoryTimeToLive("invalidValue");
    String resource = TestHelper.getBpmnProcessDefinitionResource(getClass(), "testParseProcessDefinitionWithoutTtl");
    var deploymentBuilder = repositoryService.createDeployment().name(resource).addClasspathResource(resource);
    try {
      deploymentBuilder.deploy();
      fail("Exception expected: Process definition historyTimeToLive value can not be parsed.");
    } catch (ParseException e) {
      testRule.assertTextPresent("Cannot parse historyTimeToLive", e.getMessage());
    } finally {
      processEngineConfiguration.setHistoryTimeToLive(null);
    }
  }

  @Test
  void testParseProcessDefinitionWithoutTtlWithNegativeConfigDefault() {
    processEngineConfiguration.setHistoryTimeToLive("-6");
    String resource = TestHelper.getBpmnProcessDefinitionResource(getClass(), "testParseProcessDefinitionWithoutTtl");
    var deploymentBuilder = repositoryService.createDeployment().name(resource).addClasspathResource(resource);
    try {
      deploymentBuilder.deploy();
      fail("Exception expected: Process definition historyTimeToLive value can not be parsed.");
    } catch (ParseException e) {
      testRule.assertTextPresent("Cannot parse historyTimeToLive", e.getMessage());
    } finally {
      processEngineConfiguration.setHistoryTimeToLive(null);
    }
  }

  @Test
  void testParseProcessDefinitionInvalidTtl() {
    String resource = TestHelper.getBpmnProcessDefinitionResource(getClass(), "testParseProcessDefinitionInvalidTtl");
    var deploymentBuilder = repositoryService.createDeployment().name(resource).addClasspathResource(resource);
    try {
      deploymentBuilder.deploy();
      fail("Exception expected: Process definition historyTimeToLive value can not be parsed.");
    } catch (ParseException e) {
      testRule.assertTextPresent("Cannot parse historyTimeToLive", e.getMessage());
    }
  }

  @Test
  void testParseProcessDefinitionNegativTtl() {
    String resource = TestHelper.getBpmnProcessDefinitionResource(getClass(), "testParseProcessDefinitionNegativeTtl");
    var deploymentBuilder = repositoryService.createDeployment().name(resource).addClasspathResource(resource);
    try {
      deploymentBuilder.deploy();
      fail("Exception expected: Process definition historyTimeToLive value can not be parsed.");
    } catch (ParseException e) {
      testRule.assertTextPresent("Cannot parse historyTimeToLive", e.getMessage());
    }
  }

  @Deployment
  @Test
  void testParseProcessDefinitionStartable() {
    List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery().list();
    assertThat(processDefinitions)
            .isNotNull()
            .hasSize(1);

    assertThat(processDefinitions.get(0).isStartableInTasklist()).isFalse();
  }

  @Test
  void testInvalidExecutionListenerClassDefinition() {
    // given
    String resource = TestHelper.getBpmnProcessDefinitionResource(getClass(), "testInvalidExecutionListenerClassDefinition");
    DeploymentBuilder deploymentBuilder = repositoryService.createDeployment().name(resource).addClasspathResource(resource);

    try {
      // when
      deploymentBuilder.deploy();
    } catch (ParseException e) {
      // then
      testRule.assertTextPresent("Attribute 'class' cannot be empty", e.getMessage());
      assertThat(e.getResourceReports().get(0).getErrors().get(0).getMainElementId()).isEqualTo("Task_1v32izq");
    }
  }

  @Test
  void testInvalidExecutionListenerDelegateDefinition() {
    // given
    String resource = TestHelper.getBpmnProcessDefinitionResource(getClass(), "testInvalidExecutionListenerDelegateDefinition");
    DeploymentBuilder deploymentBuilder = repositoryService.createDeployment().name(resource).addClasspathResource(resource);

    try {
      // when
      deploymentBuilder.deploy();
    } catch (ParseException e) {
      // then
      testRule.assertTextPresent("Attribute 'delegateExpression' cannot be empty", e.getMessage());
      assertThat(e.getResourceReports().get(0).getErrors().get(0).getMainElementId()).isEqualTo("Task_1v32izq");
    }
  }

  @Test
  void shouldPreventXxeProcessing() {
    // given
    String resource =
        TestHelper.getBpmnProcessDefinitionResource(getClass(), "testParseProcessDefinitionXXE");
    var deploymentBuilder = repositoryService.createDeployment()
      .name(resource)
      .addClasspathResource(resource);

    // when/then
    assertThatThrownBy(deploymentBuilder::deploy)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("DOCTYPE is disallowed when the feature " +
          "\"http://apache.org/xml/features/disallow-doctype-decl\" set to true.");
  }

  @Test
  void shouldAllowXxeProcessing() {
    // given
    processEngineConfiguration.setEnableXxeProcessing(true);
    String resource =
        TestHelper.getBpmnProcessDefinitionResource(getClass(), "testParseProcessDefinitionXXE");
    var deploymentBuilder = repositoryService.createDeployment()
      .name(resource)
      .addClasspathResource(resource);

    // when/then
    assertThatThrownBy(deploymentBuilder::deploy)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Could not parse")
      .hasMessageContaining("file.txt");
  }

  @Test
  void testFeatureSecureProcessingRejectsDefinitionDueToAttributeLimit() {
    // IBM JDKs do not check on attribute number limits, skip the test there
    assumeThat(System.getProperty("java.vm.vendor")).doesNotContain("IBM");
    String resource = TestHelper.getBpmnProcessDefinitionResource(getClass(), "testParseProcessDefinitionFSP");
    var deploymentBuilder = repositoryService.createDeployment().name(resource).addClasspathResource(resource);
    try {
      deploymentBuilder.deploy();
      fail("Exception expected: Attribute Number Limit should have been exceeded while parsing the model!");
    } catch (ProcessEngineException e) {
      testRule.assertTextPresent("JAXP00010002", e.getMessage());
    }
  }

  @Test
  void testFeatureSecureProcessingAcceptsDefinitionWhenAttributeLimitOverridden() {
    // given
    System.setProperty("jdk.xml.elementAttributeLimit", "0");

    String resource = TestHelper.getBpmnProcessDefinitionResource(getClass(), "testParseProcessDefinitionFSP");
    DeploymentBuilder deploymentBuilder = repositoryService.createDeployment().name(resource).addClasspathResource(resource);

    // when
    testRule.deploy(deploymentBuilder);

    // then
    assertThat(repositoryService.createProcessDefinitionQuery().count()).isOne();
  }

  @Test
  void testFeatureSecureProcessingRestrictExternalSchemaAccess() {
    // given
    // the external schema access property is not supported on certain
    // IBM JDK versions, in which case schema access cannot be restricted
    Assumptions.assumeTrue(doesJdkSupportExternalSchemaAccessProperty());

    BpmnModelInstance process = Bpmn.createExecutableProcess("process")
        .startEvent()
        .userTask()
        .endEvent()
        .done();

    DeploymentBuilder builder = repositoryService.createDeployment()
        .addModelInstance("process.bpmn", process);

    System.setProperty("javax.xml.accessExternalSchema", ""); // empty string prohibits all external schema access

    // when/then
    // fails, because the BPMN XSD references other external XSDs, e.g. BPMNDI
    assertThatThrownBy(() -> testRule.deploy(builder))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Could not parse 'process.bpmn'");

  }

  @Test
  void testFeatureSecureProcessingAllowExternalSchemaAccess() {
    // given
    BpmnModelInstance process = Bpmn.createExecutableProcess("process")
        .startEvent()
        .userTask()
        .endEvent()
        .done();

    System.setProperty("javax.xml.accessExternalSchema", "all"); // empty string prohibits all external schema access

    // when
    DeploymentWithDefinitions deployment = testRule.deploy(process);

    // then
    assertThat(deployment).isNotNull();
  }

  @Test
  void testTimerWithoutFullDefinition() {
    String timerWithoutDetails = "<?xml version='1.0' encoding='UTF-8'?>" +
          "<definitions xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'" +
          "  xmlns='http://www.omg.org/spec/BPMN/20100524/MODEL'" +
          "  xmlns:operaton='http://operaton.org/schema/1.0/bpmn'" +
          "  targetNamespace='Examples'>" +
          "  <process id='process' isExecutable='true'>" +
          "    <startEvent id='start'>" +
          "      <timerEventDefinition id='TimerEventDefinition_1'/>" +
          "    </startEvent>" +
          "  </process>" +
          "</definitions>";
    var deploymentBuilder = repositoryService.createDeployment().addString("process.bpmn20.xml", timerWithoutDetails);
    try {
      deploymentBuilder.deploy();
      fail("Exception expected: Process definition could be parsed, it contains uncomplete timer event definition.");
    } catch (ParseException e) {
      testRule.assertTextPresent("Timer needs configuration (either timeDate, timeCycle or timeDuration is needed).", e.getMessage());
      List<Problem> errors = e.getResourceReports().get(0).getErrors();
      assertThat(errors).hasSize(1);
      assertThat(errors.get(0).getMainElementId()).isEqualTo("start");
    }
  }

  @Test
  void testSequenceFlowNoIdAndUnexistentDestination() {
    String incorrectSequenceFlow = "<?xml version='1.0' encoding='UTF-8'?>" +
          "<definitions xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'" +
          "  xmlns='http://www.omg.org/spec/BPMN/20100524/MODEL'" +
          "  xmlns:operaton='http://operaton.org/schema/1.0/bpmn'" +
          "  targetNamespace='Examples'>" +
          "  <process id='process' isExecutable='true'>" +
          "    <startEvent id='start'/>" +
          "    <sequenceFlow sourceRef='start' targetRef='eventBasedGateway' />" +
          "  </process>" +
          "</definitions>";
    var deploymentBuilder = repositoryService.createDeployment().addString("process.bpmn20.xml", incorrectSequenceFlow);
    try {
      deploymentBuilder.deploy();
      fail("Exception expected: Unexisting target.");
    } catch (ParseException e) {
      testRule.assertTextPresent("Invalid destination 'eventBasedGateway' of sequence flow 'null'", e.getMessage());
      List<Problem> errors = e.getResourceReports().get(0).getErrors();
      assertThat(errors).hasSize(2);
      assertThat(errors.get(1).getMainElementId()).isNull();
      assertThat(errors.get(1).getElementIds()).isEmpty();
    }
  }

  @Test
  void testMultipleTimerStartEvents() {
    String resource = TestHelper.getBpmnProcessDefinitionResource(getClass(), "testMultipleTimerStartEvents");
    var deploymentBuilder = repositoryService.createDeployment().name(resource).addClasspathResource(resource);
    try {
      deploymentBuilder.deploy();
      fail("Exception expected: Process definition could be parsed, it contains multiple multiple none start events or timer start events.");
    } catch (ParseException e) {
      testRule.assertTextPresent("multiple none start events or timer start events not supported on process definition", e.getMessage());
      testRule.assertTextPresent("multiple start events not supported for subprocess", e.getMessage());
      List<Problem> errors = e.getResourceReports().get(0).getErrors();
      assertThat(errors).hasSize(4);
      assertThat(errors.get(0).getMainElementId()).isEqualTo("timerStart2");
      assertThat(errors.get(1).getMainElementId()).isEqualTo("plainStart1");
      assertThat(errors.get(2).getMainElementId()).isEqualTo("plainStart2");
      assertThat(errors.get(3).getMainElementId()).isEqualTo("plainStartInSub1");
    }
  }

  @Test
  @WatchLogger(loggerNames = {"org.operaton.bpm.engine.bpmn.parser"}, level = "INFO")
  void testIntermediateCatchTimerEventWithTimeCycleNotRecommendedInfoMessage() {
    BpmnModelInstance process = Bpmn.createExecutableProcess("process")
        .startEvent()
        .intermediateCatchEvent("timerintermediatecatchevent1")
        .timerWithCycle("0 0/5 * * * ?")
        .endEvent()
        .done();
    testRule.deploy(process);

    String logMessage = "definitionKey: process; It is not recommended to use an intermediate catch timer event with a time cycle, "
        + "element with id 'timerintermediatecatchevent1'.";
    assertThat(loggingRule.getFilteredLog(logMessage)).hasSize(1);
  }

  @Test
  void testParseEmptyExtensionProperty() {
    // given process definition with empty property (key and value = null) is deployed
    // when
    repositoryService.createDeployment()
    .addClasspathResource("org/operaton/bpm/engine/test/bpmn/parse/BpmnParseTest.testParseEmptyExtensionProperty.bpmn").deploy();

    // then
    assertThat(repositoryService.createProcessDefinitionQuery().count()).isOne();
  }

  protected boolean doesJdkSupportExternalSchemaAccessProperty() {
    String jvmVendor = System.getProperty("java.vm.vendor");
    String javaVersion = System.getProperty("java.version");

    boolean isIbmJDK = jvmVendor != null && jvmVendor.contains("IBM");
    boolean isJava6 = javaVersion != null && javaVersion.startsWith("1.6");
    boolean isJava7 = javaVersion != null && javaVersion.startsWith("1.7");

    return !isJava6 && !(isIbmJDK && isJava7);

  }


  protected void assertErrors(List<Problem> errors, String elementId) {
    assertThat(errors).hasSize(1);
    assertThat(errors.get(0).getMainElementId()).isEqualTo(elementId);
  }

}

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
package org.operaton.bpm.engine.impl.bpmn.parser;

import java.io.InputStream;
import java.net.URL;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.operaton.bpm.engine.ActivityTypes;
import org.operaton.bpm.engine.BpmnParseException;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.delegate.ExecutionListener;
import org.operaton.bpm.engine.delegate.TaskListener;
import org.operaton.bpm.engine.delegate.VariableListener;
import org.operaton.bpm.engine.impl.Condition;
import org.operaton.bpm.engine.impl.HistoryTimeToLiveParser;
import org.operaton.bpm.engine.impl.ProcessEngineLogger;
import org.operaton.bpm.engine.impl.bpmn.behavior.BoundaryConditionalEventActivityBehavior;
import org.operaton.bpm.engine.impl.bpmn.behavior.BoundaryEventActivityBehavior;
import org.operaton.bpm.engine.impl.bpmn.behavior.CallActivityBehavior;
import org.operaton.bpm.engine.impl.bpmn.behavior.CallableElementActivityBehavior;
import org.operaton.bpm.engine.impl.bpmn.behavior.CancelBoundaryEventActivityBehavior;
import org.operaton.bpm.engine.impl.bpmn.behavior.CancelEndEventActivityBehavior;
import org.operaton.bpm.engine.impl.bpmn.behavior.CaseCallActivityBehavior;
import org.operaton.bpm.engine.impl.bpmn.behavior.ClassDelegateActivityBehavior;
import org.operaton.bpm.engine.impl.bpmn.behavior.CompensationEventActivityBehavior;
import org.operaton.bpm.engine.impl.bpmn.behavior.DmnBusinessRuleTaskActivityBehavior;
import org.operaton.bpm.engine.impl.bpmn.behavior.ErrorEndEventActivityBehavior;
import org.operaton.bpm.engine.impl.bpmn.behavior.EventBasedGatewayActivityBehavior;
import org.operaton.bpm.engine.impl.bpmn.behavior.EventSubProcessActivityBehavior;
import org.operaton.bpm.engine.impl.bpmn.behavior.EventSubProcessStartConditionalEventActivityBehavior;
import org.operaton.bpm.engine.impl.bpmn.behavior.EventSubProcessStartEventActivityBehavior;
import org.operaton.bpm.engine.impl.bpmn.behavior.ExclusiveGatewayActivityBehavior;
import org.operaton.bpm.engine.impl.bpmn.behavior.ExternalTaskActivityBehavior;
import org.operaton.bpm.engine.impl.bpmn.behavior.InclusiveGatewayActivityBehavior;
import org.operaton.bpm.engine.impl.bpmn.behavior.IntermediateCatchEventActivityBehavior;
import org.operaton.bpm.engine.impl.bpmn.behavior.IntermediateCatchLinkEventActivityBehavior;
import org.operaton.bpm.engine.impl.bpmn.behavior.IntermediateConditionalEventBehavior;
import org.operaton.bpm.engine.impl.bpmn.behavior.IntermediateThrowNoneEventActivityBehavior;
import org.operaton.bpm.engine.impl.bpmn.behavior.MailActivityBehavior;
import org.operaton.bpm.engine.impl.bpmn.behavior.ManualTaskActivityBehavior;
import org.operaton.bpm.engine.impl.bpmn.behavior.MultiInstanceActivityBehavior;
import org.operaton.bpm.engine.impl.bpmn.behavior.NoneEndEventActivityBehavior;
import org.operaton.bpm.engine.impl.bpmn.behavior.NoneStartEventActivityBehavior;
import org.operaton.bpm.engine.impl.bpmn.behavior.ParallelGatewayActivityBehavior;
import org.operaton.bpm.engine.impl.bpmn.behavior.ParallelMultiInstanceActivityBehavior;
import org.operaton.bpm.engine.impl.bpmn.behavior.ReceiveTaskActivityBehavior;
import org.operaton.bpm.engine.impl.bpmn.behavior.ScriptTaskActivityBehavior;
import org.operaton.bpm.engine.impl.bpmn.behavior.SequentialMultiInstanceActivityBehavior;
import org.operaton.bpm.engine.impl.bpmn.behavior.ServiceTaskDelegateExpressionActivityBehavior;
import org.operaton.bpm.engine.impl.bpmn.behavior.ServiceTaskExpressionActivityBehavior;
import org.operaton.bpm.engine.impl.bpmn.behavior.ShellActivityBehavior;
import org.operaton.bpm.engine.impl.bpmn.behavior.SubProcessActivityBehavior;
import org.operaton.bpm.engine.impl.bpmn.behavior.TaskActivityBehavior;
import org.operaton.bpm.engine.impl.bpmn.behavior.TerminateEndEventActivityBehavior;
import org.operaton.bpm.engine.impl.bpmn.behavior.ThrowEscalationEventActivityBehavior;
import org.operaton.bpm.engine.impl.bpmn.behavior.ThrowSignalEventActivityBehavior;
import org.operaton.bpm.engine.impl.bpmn.behavior.UserTaskActivityBehavior;
import org.operaton.bpm.engine.impl.bpmn.helper.BpmnProperties;
import org.operaton.bpm.engine.impl.bpmn.listener.ClassDelegateExecutionListener;
import org.operaton.bpm.engine.impl.bpmn.listener.DelegateExpressionExecutionListener;
import org.operaton.bpm.engine.impl.bpmn.listener.ExpressionExecutionListener;
import org.operaton.bpm.engine.impl.bpmn.listener.ScriptExecutionListener;
import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.impl.core.model.BaseCallableElement;
import org.operaton.bpm.engine.impl.core.model.BaseCallableElement.CallableElementBinding;
import org.operaton.bpm.engine.impl.core.model.CallableElement;
import org.operaton.bpm.engine.impl.core.model.CallableElementParameter;
import org.operaton.bpm.engine.impl.core.model.Properties;
import org.operaton.bpm.engine.impl.core.variable.mapping.IoMapping;
import org.operaton.bpm.engine.impl.core.variable.mapping.value.ConstantValueProvider;
import org.operaton.bpm.engine.impl.core.variable.mapping.value.NullValueProvider;
import org.operaton.bpm.engine.impl.core.variable.mapping.value.ParameterValueProvider;
import org.operaton.bpm.engine.impl.dmn.result.DecisionResultMapper;
import org.operaton.bpm.engine.impl.el.ElValueProvider;
import org.operaton.bpm.engine.impl.el.Expression;
import org.operaton.bpm.engine.impl.el.ExpressionManager;
import org.operaton.bpm.engine.impl.el.FixedValue;
import org.operaton.bpm.engine.impl.el.UelExpressionCondition;
import org.operaton.bpm.engine.impl.event.EventType;
import org.operaton.bpm.engine.impl.form.FormDefinition;
import org.operaton.bpm.engine.impl.form.handler.DefaultStartFormHandler;
import org.operaton.bpm.engine.impl.form.handler.DefaultTaskFormHandler;
import org.operaton.bpm.engine.impl.form.handler.DelegateStartFormHandler;
import org.operaton.bpm.engine.impl.form.handler.DelegateTaskFormHandler;
import org.operaton.bpm.engine.impl.form.handler.StartFormHandler;
import org.operaton.bpm.engine.impl.form.handler.TaskFormHandler;
import org.operaton.bpm.engine.impl.jobexecutor.AsyncAfterMessageJobDeclaration;
import org.operaton.bpm.engine.impl.jobexecutor.AsyncBeforeMessageJobDeclaration;
import org.operaton.bpm.engine.impl.jobexecutor.EventSubscriptionJobDeclaration;
import org.operaton.bpm.engine.impl.jobexecutor.JobDeclaration;
import org.operaton.bpm.engine.impl.jobexecutor.MessageJobDeclaration;
import org.operaton.bpm.engine.impl.jobexecutor.TimerCatchIntermediateEventJobHandler;
import org.operaton.bpm.engine.impl.jobexecutor.TimerDeclarationImpl;
import org.operaton.bpm.engine.impl.jobexecutor.TimerDeclarationType;
import org.operaton.bpm.engine.impl.jobexecutor.TimerEventJobHandler;
import org.operaton.bpm.engine.impl.jobexecutor.TimerExecuteNestedActivityJobHandler;
import org.operaton.bpm.engine.impl.jobexecutor.TimerStartEventJobHandler;
import org.operaton.bpm.engine.impl.jobexecutor.TimerStartEventSubprocessJobHandler;
import org.operaton.bpm.engine.impl.jobexecutor.TimerTaskListenerJobHandler;
import org.operaton.bpm.engine.impl.persistence.entity.DeploymentEntity;
import org.operaton.bpm.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.operaton.bpm.engine.impl.pvm.PvmTransition;
import org.operaton.bpm.engine.impl.pvm.delegate.ActivityBehavior;
import org.operaton.bpm.engine.impl.pvm.process.ActivityImpl;
import org.operaton.bpm.engine.impl.pvm.process.ActivityStartBehavior;
import org.operaton.bpm.engine.impl.pvm.process.HasDIBounds;
import org.operaton.bpm.engine.impl.pvm.process.Lane;
import org.operaton.bpm.engine.impl.pvm.process.LaneSet;
import org.operaton.bpm.engine.impl.pvm.process.ParticipantProcess;
import org.operaton.bpm.engine.impl.pvm.process.ProcessDefinitionImpl;
import org.operaton.bpm.engine.impl.pvm.process.ScopeImpl;
import org.operaton.bpm.engine.impl.pvm.process.TransitionImpl;
import org.operaton.bpm.engine.impl.pvm.runtime.LegacyBehavior;
import org.operaton.bpm.engine.impl.scripting.ExecutableScript;
import org.operaton.bpm.engine.impl.scripting.ScriptCondition;
import org.operaton.bpm.engine.impl.scripting.engine.ScriptingEngines;
import org.operaton.bpm.engine.impl.task.TaskDecorator;
import org.operaton.bpm.engine.impl.task.TaskDefinition;
import org.operaton.bpm.engine.impl.task.listener.ClassDelegateTaskListener;
import org.operaton.bpm.engine.impl.task.listener.DelegateExpressionTaskListener;
import org.operaton.bpm.engine.impl.task.listener.ExpressionTaskListener;
import org.operaton.bpm.engine.impl.task.listener.ScriptTaskListener;
import org.operaton.bpm.engine.impl.util.DecisionEvaluationUtil;
import org.operaton.bpm.engine.impl.util.ReflectUtil;
import org.operaton.bpm.engine.impl.util.ScriptUtil;
import org.operaton.bpm.engine.impl.util.StringUtil;
import org.operaton.bpm.engine.impl.util.xml.Element;
import org.operaton.bpm.engine.impl.util.xml.Namespace;
import org.operaton.bpm.engine.impl.util.xml.Parse;
import org.operaton.bpm.engine.impl.variable.VariableDeclaration;
import org.operaton.bpm.engine.repository.ProcessDefinition;

import static org.operaton.bpm.engine.impl.bpmn.parser.BpmnParseUtil.findOperatonExtensionElement;
import static org.operaton.bpm.engine.impl.bpmn.parser.BpmnParseUtil.parseInputOutput;
import static org.operaton.bpm.engine.impl.bpmn.parser.BpmnParseUtil.parseOperatonExtensionProperties;
import static org.operaton.bpm.engine.impl.bpmn.parser.BpmnParseUtil.parseOperatonScript;
import static org.operaton.bpm.engine.impl.form.handler.DefaultFormHandler.ALLOWED_FORM_REF_BINDINGS;
import static org.operaton.bpm.engine.impl.form.handler.DefaultFormHandler.FORM_REF_BINDING_VERSION;
import static org.operaton.bpm.engine.impl.persistence.entity.AcquirableJobEntity.DEFAULT_EXCLUSIVE;
import static org.operaton.bpm.engine.impl.util.ClassDelegateUtil.instantiateDelegate;

/**
 * Specific parsing of one BPMN 2.0 XML file, created by the {@link BpmnParser}.
 *
 * <p>
 * Instances of this class should not be reused and are also not threadsafe.
 * </p>
 *
 * @author Tom Baeyens
 * @author Bernd Ruecker
 * @author Joram Barrez
 * @author Christian Stettler
 * @author Frederik Heremans
 * @author Falko Menge
 * @author Esteban Robles
 * @author Daniel Meyer
 * @author Saeid Mirzaei
 * @author Nico Rehwaldt
 * @author Ronny Bräunlich
 * @author Christopher Zell
 * @author Deivarayan Azhagappan
 * @author Ingo Richtsmeier
 */
@SuppressWarnings({"java:S3776", "java:S6541"}) // this class is complex by its nature
public class BpmnParse extends Parse {

  public static final String MULTI_INSTANCE_BODY_ID_SUFFIX = "#multiInstanceBody";

  protected static final BpmnParseLogger LOG = ProcessEngineLogger.BPMN_PARSE_LOGGER;

  public static final String PROPERTYNAME_DOCUMENTATION = "documentation";
  public static final String PROPERTYNAME_INITIATOR_VARIABLE_NAME = "initiatorVariableName";
  public static final String PROPERTYNAME_HAS_CONDITIONAL_EVENTS = "hasConditionalEvents";
  public static final String PROPERTYNAME_CONDITION = "condition";
  public static final String PROPERTYNAME_CONDITION_TEXT = "conditionText";
  public static final String PROPERTYNAME_VARIABLE_DECLARATIONS = "variableDeclarations";
  public static final String PROPERTYNAME_TIMER_DECLARATION = "timerDeclarations";
  public static final String PROPERTYNAME_MESSAGE_JOB_DECLARATION = "messageJobDeclaration";
  public static final String PROPERTYNAME_ISEXPANDED = "isExpanded";
  public static final String PROPERTYNAME_START_TIMER = "timerStart";
  public static final String PROPERTYNAME_COMPENSATION_HANDLER_ID = "compensationHandler";
  public static final String PROPERTYNAME_IS_FOR_COMPENSATION = "isForCompensation";
  public static final String PROPERTYNAME_EVENT_SUBSCRIPTION_JOB_DECLARATION = "eventJobDeclarations";
  public static final String PROPERTYNAME_THROWS_COMPENSATION = "throwsCompensation";
  public static final String PROPERTYNAME_CONSUMES_COMPENSATION = "consumesCompensation";
  public static final String PROPERTYNAME_JOB_PRIORITY = "jobPriority";
  public static final String PROPERTYNAME_TASK_PRIORITY = "taskPriority";
  public static final String PROPERTYNAME_EXTERNAL_TASK_TOPIC = "topic";
  public static final String PROPERTYNAME_CLASS = "class";
  public static final String PROPERTYNAME_EXPRESSION = "expression";
  public static final String PROPERTYNAME_DELEGATE_EXPRESSION = "delegateExpression";
  public static final String PROPERTYNAME_VARIABLE_MAPPING_CLASS = "variableMappingClass";
  public static final String PROPERTYNAME_VARIABLE_MAPPING_DELEGATE_EXPRESSION = "variableMappingDelegateExpression";
  public static final String PROPERTYNAME_RESOURCE = "resource";
  public static final String PROPERTYNAME_LANGUAGE = "language";
  public static final String PROPERTYNAME_DEFAULT = "default";
  public static final String TYPE = "type";

  public static final String TRUE = "true";
  public static final String FALSE = "false";
  public static final String INTERRUPTING = "isInterrupting";

  public static final String CONDITIONAL_EVENT_DEFINITION = "conditionalEventDefinition";
  public static final String ESCALATION_EVENT_DEFINITION = "escalationEventDefinition";
  public static final String COMPENSATE_EVENT_DEFINITION = "compensateEventDefinition";
  public static final String TIMER_EVENT_DEFINITION = "timerEventDefinition";
  public static final String SIGNAL_EVENT_DEFINITION = "signalEventDefinition";
  public static final String MESSAGE_EVENT_DEFINITION = "messageEventDefinition";
  public static final String ERROR_EVENT_DEFINITION = "errorEventDefinition";
  public static final String CANCEL_EVENT_DEFINITION = "cancelEventDefinition";
  public static final String TERMINATE_EVENT_DEFINITION = "terminateEventDefinition";
  public static final String LINK_EVENT_DEFINITION = "linkEventDefinition";
  public static final String CONDITION_EXPRESSION = "conditionExpression";
  public static final String CONDITION = "condition";

  private static final List<String> VARIABLE_EVENTS = Arrays.asList(
      VariableListener.CREATE,
      VariableListener.DELETE,
      VariableListener.UPDATE
  );

  /**
   * @deprecated Use {@link BpmnProperties#TYPE} instead.
   */
  @Deprecated(forRemoval = true, since = "1.0")
  public static final String PROPERTYNAME_TYPE = BpmnProperties.TYPE.name();

  /**
   * @deprecated Use {@link BpmnProperties#ERROR_EVENT_DEFINITIONS} instead.
   */
  @Deprecated(forRemoval = true, since = "1.0")
  public static final String PROPERTYNAME_ERROR_EVENT_DEFINITIONS = BpmnProperties.ERROR_EVENT_DEFINITIONS.name();

  /* process start authorization specific finals */
  protected static final String POTENTIAL_STARTER = "potentialStarter";
  protected static final String CANDIDATE_STARTER_USERS_EXTENSION = "candidateStarterUsers";
  protected static final String CANDIDATE_STARTER_GROUPS_EXTENSION = "candidateStarterGroups";

  protected static final String ATTRIBUTEVALUE_T_FORMAL_EXPRESSION = BpmnParser.BPMN20_NS + ":tFormalExpression";

  public static final String PROPERTYNAME_IS_MULTI_INSTANCE = "isMultiInstance";

  public static final Namespace OPERATON_BPMN_EXTENSIONS_NS = new Namespace(BpmnParser.OPERATON_BPMN_EXTENSIONS_NS, BpmnParser.CAMUNDA_BPMN_EXTENSIONS_NS);
  public static final Namespace XSI_NS = new Namespace(BpmnParser.XSI_NS);
  public static final Namespace BPMN_DI_NS = new Namespace(BpmnParser.BPMN_DI_NS);
  public static final Namespace OMG_DI_NS = new Namespace(BpmnParser.OMG_DI_NS);
  public static final Namespace BPMN_DC_NS = new Namespace(BpmnParser.BPMN_DC_NS);
  public static final String ALL = "all";

  private static final String PROCESS_TAG = "process";
  private static final String SUB_PROCESS_TAG = "subProcess";
  private static final String EXTENSION_ELEMENTS_TAG = "extensionElements";
  private static final String TRANSACTION_TAG = "transaction";
  private static final String SCRIPT_TAG = "script";
  private static final String TIME_CYCLE = "timeCycle";

  private static final String SOURCE_REF_ATTRIBUTE = "sourceRef";
  private static final String TARGET_REF_ATTRIBUTE = "targetRef";
  private static final String ERROR_REF_ATTRIBUTE = "errorRef";
  private static final String DEFAULT_ATTRIBUTE = "default";
  private static final String BPMN_ELEMENT_ATTRIBUTE = "bpmnElement";
  private static final String ATTR_DECISION_REF_BINDING = "decisionRefBinding";
  private static final String ATTR_DECISION_REF_TENANT_ID = "decisionRefTenantId";

  /** The deployment to which the parsed process definitions will be added. */
  protected DeploymentEntity deployment;

  /** The end result of the parsing: a list of process definition. */
  protected List<ProcessDefinitionEntity> processDefinitions = new ArrayList<>();

  /** Mapping of found errors in BPMN 2.0 file */
  protected Map<String, Error> bpmnParseErrors = new HashMap<>();

  /** Mapping of found escalation elements */
  protected Map<String, Escalation> escalations = new HashMap<>();

  /**
   * Mapping from a process definition key to his containing list of job
   * declarations
   **/
  protected Map<String, List<JobDeclaration<?, ?>>> jobDeclarations = new HashMap<>();

  /** A map for storing sequence flow based on their id during parsing. */
  protected Map<String, TransitionImpl> sequenceFlows;

  /**
   * A list of all element IDs. This allows us to parse only what we actually
   * support but still validate the references among elements we do not support.
   */
  protected List<String> elementIds = new ArrayList<>();

  /** A map for storing the process references of participants */
  protected Map<String, String> participantProcesses = new HashMap<>();

  /**
   * Mapping containing values stored during the first phase of parsing since
   * other elements can reference these messages.
   *
   * <p>
   * All the map's elements are defined outside the process definition(s), which
   * means that this map doesn't need to be re-initialized for each new process
   * definition.
   * </p>
   */
  protected Map<String, MessageDefinition> messages = new HashMap<>();
  protected Map<String, SignalDefinition> signals = new HashMap<>();

  // Members
  protected ExpressionManager expressionManager;
  protected List<BpmnParseListener> parseListeners;
  protected Map<String, XMLImporter> importers = new HashMap<>();
  protected Map<String, String> prefixs = new HashMap<>();
  protected String targetNamespace;

  private final Map<String, String> eventLinkTargets = new HashMap<>();
  private final Map<String, String> eventLinkSources = new HashMap<>();

  /**
   * Constructor to be called by the {@link BpmnParser}.
   */
  public BpmnParse(BpmnParser parser) {
    super(parser);
    expressionManager = parser.getExpressionManager();
    parseListeners = parser.getParseListeners();

    setSchemaResource(ReflectUtil.getResourceUrlAsString(BpmnParser.BPMN_20_SCHEMA_LOCATION));
  }

  public BpmnParse deployment(DeploymentEntity deployment) {
    this.deployment = deployment;
    return this;
  }

  @Override
  public BpmnParse execute() {
    super.execute(); // schema validation

    try {
      parseRootElement();
    } catch (BpmnParseException e) {
      addError(e);

    } catch (Exception e) {
      LOG.parsingFailure(e);

      // ALL unexpected exceptions should bubble up since they are not handled
      // accordingly by underlying parse-methods and the process can't be
      // deployed
      throw LOG.parsingProcessException(e);

    } finally {
      if (hasWarnings()) {
        logWarnings();
      }
      if (hasErrors()) {
        throwExceptionForErrors();
      }
    }

    return this;
  }

  /**
   * Parses the 'definitions' root element
   */
  protected void parseRootElement() {
    collectElementIds();
    parseDefinitionsAttributes();
    parseImports();
    parseMessages();
    parseSignals();
    parseErrors();
    parseEscalations();
    parseProcessDefinitions();
    parseCollaboration();

    // Diagram interchange parsing must be after parseProcessDefinitions,
    // since it depends and sets values on existing process definition objects
    parseDiagramInterchangeElements();

    for (BpmnParseListener parseListener : parseListeners) {
      parseListener.parseRootElement(rootElement, getProcessDefinitions());
    }
  }

  protected void collectElementIds() {
    rootElement.collectIds(elementIds);
  }

  protected void parseDefinitionsAttributes() {
    this.targetNamespace = rootElement.attribute("targetNamespace");

    for (String attribute : rootElement.attributes()) {
      if (attribute.startsWith("xmlns:")) {
        String prefixValue = rootElement.attribute(attribute);
        String prefixName = attribute.substring(6);
        this.prefixs.put(prefixName, prefixValue);
      }
    }
  }

  protected String resolveName(String name) {
    if (name == null) {
      return null;
    }
    int indexOfP = name.indexOf(':');
    if (indexOfP != -1) {
      String prefix = name.substring(0, indexOfP);
      String resolvedPrefix = this.prefixs.get(prefix);
      return resolvedPrefix + ":" + name.substring(indexOfP + 1);
    } else {
      return this.targetNamespace + ":" + name;
    }
  }

  /**
   * Parses the rootElement importing structures
   */
  protected void parseImports() {
    List<Element> imports = rootElement.elements("import");
    for (Element theImport : imports) {
      String importType = theImport.attribute("importType");
      XMLImporter importer = this.getImporter(importType, theImport);
      if (importer == null) {
        addError("Could not import item of type " + importType, theImport);
      } else {
        importer.importFrom(theImport, this);
      }
    }
  }

  protected XMLImporter getImporter(String importType, Element theImport) {
    if (this.importers.containsKey(importType)) {
      return this.importers.get(importType);
    } else {
      if ("http://schemas.xmlsoap.org/wsdl/".equals(importType)) {
        Class<?> wsdlImporterClass;
        try {
          wsdlImporterClass = Class.forName("org.operaton.bpm.engine.impl.webservice.CxfWSDLImporter", true, Thread.currentThread().getContextClassLoader());
          XMLImporter newInstance = (XMLImporter) wsdlImporterClass.getDeclaredConstructor().newInstance();
          this.importers.put(importType, newInstance);
          return newInstance;
        } catch (Exception e) {
          addError("Could not find importer for type " + importType, theImport);
        }
      }
      return null;
    }
  }

  /**
   * Parses the messages of the given definitions file. Messages are not
   * contained within a process element, but they can be referenced from inner
   * process elements.
   */
  public void parseMessages() {
    for (Element messageElement : rootElement.elements("message")) {
      String id = messageElement.attribute("id");
      String messageName = messageElement.attribute("name");

      Expression messageExpression = null;
      if (messageName != null) {
        messageExpression = expressionManager.createExpression(messageName);
      }

      MessageDefinition messageDefinition = new MessageDefinition(this.targetNamespace + ":" + id, messageExpression);
      this.messages.put(messageDefinition.getId(), messageDefinition);
    }
  }

  /**
   * Parses the signals of the given definitions file. Signals are not contained
   * within a process element, but they can be referenced from inner process
   * elements.
   */
  protected void parseSignals() {
    for (Element signalElement : rootElement.elements("signal")) {
      String id = signalElement.attribute("id");
      String signalName = signalElement.attribute("name");

      for (SignalDefinition signalDefinition : signals.values()) {
        if (signalDefinition.getName().equals(signalName)) {
          addError("duplicate signal name '%s'.".formatted(signalName), signalElement);
        }
      }

      if (id == null) {
        addError("signal must have an id", signalElement);
      } else if (signalName == null) {
        addError("signal with id '%s' has no name".formatted(id), signalElement);
      } else {
        Expression signalExpression = expressionManager.createExpression(signalName);
        SignalDefinition signal = new SignalDefinition();
        signal.setId(this.targetNamespace + ":" + id);
        signal.setExpression(signalExpression);

        this.signals.put(signal.getId(), signal);
      }
    }
  }

  public void parseErrors() {
    for (Element errorElement : rootElement.elements("error")) {
      Error error = new Error();

      String id = errorElement.attribute("id");
      if (id == null) {
        addError("'id' is mandatory on error definition", errorElement);
      }
      error.setId(id);

      String errorCode = errorElement.attribute("errorCode");
      if (errorCode != null) {
        error.setErrorCode(errorCode);
      }

      String errorMessage = errorElement.attributeNS(OPERATON_BPMN_EXTENSIONS_NS, "errorMessage");
      if(errorMessage != null) {
        error.setErrorMessageExpression(createParameterValueProvider(errorMessage, expressionManager));
      }

      bpmnParseErrors.put(id, error);
    }
  }

  protected void parseEscalations() {
    for (Element element : rootElement.elements("escalation")) {

      String id = element.attribute("id");
      if (id == null) {
        addError("escalation must have an id", element);
      } else {

        Escalation escalation = createEscalation(id, element);
        escalations.put(id, escalation);
      }
    }
  }

  protected Escalation createEscalation(String id, Element element) {

    Escalation escalation = new Escalation(id);

    String name = element.attribute("name");
    if (name != null) {
      escalation.setName(name);
    }

    String escalationCode = element.attribute("escalationCode");
    if (escalationCode != null && !escalationCode.isEmpty()) {
      escalation.setEscalationCode(escalationCode);
    }
    return escalation;
  }

  /**
   * Parses all the process definitions defined within the 'definitions' root
   * element.
   */
  public void parseProcessDefinitions() {
    for (Element processElement : rootElement.elements(PROCESS_TAG)) {
      boolean isExecutable = !deployment.isNew();
      String isExecutableStr = processElement.attribute("isExecutable");
      if (isExecutableStr != null) {
        isExecutable = Boolean.parseBoolean(isExecutableStr);
        if (!isExecutable) {
          LOG.ignoringNonExecutableProcess(processElement.attribute("id"));
        }
      } else {
        LOG.missingIsExecutableAttribute(processElement.attribute("id"));
      }

      // Only process executable processes
      if (isExecutable) {
        processDefinitions.add(parseProcess(processElement));
      }
    }
  }

  /**
   * Parses the collaboration definition defined within the 'definitions' root
   * element and get all participants to lookup their process references during
   * DI parsing.
   */
  public void parseCollaboration() {
    Element collaboration = rootElement.element("collaboration");
    if (collaboration != null) {
      for (Element participant : collaboration.elements("participant")) {
        String processRef = participant.attribute("processRef");
        if (processRef != null) {
          ProcessDefinitionImpl procDef = getProcessDefinition(processRef);
          if (procDef != null) {
            // Set participant process on the procDef, so it can get rendered
            // later on if needed
            ParticipantProcess participantProcess = new ParticipantProcess();
            participantProcess.setId(participant.attribute("id"));
            participantProcess.setName(participant.attribute("name"));
            procDef.setParticipantProcess(participantProcess);

            participantProcesses.put(participantProcess.getId(), processRef);
          }
        }
      }
    }
  }

  /**
   * Parses one process (ie anything inside a &lt;process&gt; element).
   *
   * @param processElement
   *          The 'process' element.
   * @return The parsed version of the XML: a {@link ProcessDefinitionImpl}
   *         object.
   */
  public ProcessDefinitionEntity parseProcess(Element processElement) {
    // reset all mappings that are related to one process definition
    sequenceFlows = new HashMap<>();

    ProcessDefinitionEntity processDefinition = new ProcessDefinitionEntity();

    /*
     * Mapping object model - bpmn xml: processDefinition.id -> generated by
     * processDefinition.key -> bpmn id (required) processDefinition.name ->
     * bpmn name (optional)
     */
    processDefinition.setKey(processElement.attribute("id"));
    processDefinition.setName(processElement.attribute("name"));
    processDefinition.setCategory(rootElement.attribute("targetNamespace"));
    processDefinition.setProperty(PROPERTYNAME_DOCUMENTATION, parseDocumentation(processElement));
    processDefinition.setTaskDefinitions(new HashMap<>());
    processDefinition.setDeploymentId(deployment.getId());
    processDefinition.setTenantId(deployment.getTenantId());
    processDefinition.setProperty(PROPERTYNAME_JOB_PRIORITY, parsePriority(processElement, PROPERTYNAME_JOB_PRIORITY));
    processDefinition.setProperty(PROPERTYNAME_TASK_PRIORITY, parsePriority(processElement, PROPERTYNAME_TASK_PRIORITY));
    processDefinition.setVersionTag(processElement.attributeNS(OPERATON_BPMN_EXTENSIONS_NS, "versionTag"));

    boolean skipEnforceTtl = !deployment.isNew();
    validateAndSetHTTL(processElement, processDefinition, skipEnforceTtl);

    boolean isStartableInTasklist = isStartable(processElement);
    processDefinition.setStartableInTasklist(isStartableInTasklist);

    LOG.parsingElement(PROCESS_TAG, processDefinition.getKey());

    parseScope(processElement, processDefinition);

    // Parse any laneSets defined for this process
    parseLaneSets(processElement, processDefinition);

    for (BpmnParseListener parseListener : parseListeners) {
      parseListener.parseProcess(processElement, processDefinition);
    }

    // now we have parsed anything we can validate some stuff
    validateActivities(processDefinition.getActivities());

    //unregister delegates
    for (ActivityImpl activity : processDefinition.getActivities()) {
      activity.setDelegateAsyncAfterUpdate(null);
      activity.setDelegateAsyncBeforeUpdate(null);
    }
    return processDefinition;
  }

  protected void validateAndSetHTTL(Element processElement, ProcessDefinitionEntity processDefinition, boolean skipEnforceTtl) {
    try {
      String processDefinitionKey = processDefinition.getKey();
      Integer historyTimeToLive = HistoryTimeToLiveParser.create().parse(processElement, processDefinitionKey, skipEnforceTtl);
      processDefinition.setHistoryTimeToLive(historyTimeToLive);
    }
    catch (Exception e) {
      addError(new BpmnParseException(e.getMessage(), processElement, e));
    }
  }

  protected void parseLaneSets(Element parentElement, ProcessDefinitionEntity processDefinition) {
    List<Element> laneSets = parentElement.elements("laneSet");

    if (laneSets != null && !laneSets.isEmpty()) {
      for (Element laneSetElement : laneSets) {
        LaneSet newLaneSet = new LaneSet();

        newLaneSet.setId(laneSetElement.attribute("id"));
        newLaneSet.setName(laneSetElement.attribute("name"));
        parseLanes(laneSetElement, newLaneSet);

        // Finally, add the set
        processDefinition.addLaneSet(newLaneSet);
      }
    }
  }

  protected void parseLanes(Element laneSetElement, LaneSet laneSet) {
    List<Element> lanes = laneSetElement.elements("lane");
    if (lanes != null && !lanes.isEmpty()) {
      for (Element laneElement : lanes) {
        // Parse basic attributes
        Lane lane = new Lane();
        lane.setId(laneElement.attribute("id"));
        lane.setName(laneElement.attribute("name"));

        // Parse ID's of flow-nodes that live inside this lane
        List<Element> flowNodeElements = laneElement.elements("flowNodeRef");
        if (flowNodeElements != null && !flowNodeElements.isEmpty()) {
          for (Element flowNodeElement : flowNodeElements) {
            lane.getFlowNodeIds().add(flowNodeElement.getText());
          }
        }

        laneSet.addLane(lane);
      }
    }
  }

  /**
   * Parses a scope: a process, subprocess, etc.
   *
   * <p>
   * Note that a process definition is a scope on itself.
   * </p>
   *
   * @param scopeElement
   *          The XML element defining the scope
   * @param parentScope
   *          The scope that contains the nested scope.
   */
  public void parseScope(Element scopeElement, ScopeImpl parentScope) {

    // Not yet supported on process level (PVM additions needed):
    // parseProperties(processElement);

    // filter activities that must be parsed separately
    List<Element> activityElements = new ArrayList<>(scopeElement.elements());
    Map<String, Element> intermediateCatchEvents = filterIntermediateCatchEvents(activityElements);
    activityElements.removeAll(intermediateCatchEvents.values());
    Map<String, Element> compensationHandlers = filterCompensationHandlers(activityElements);
    activityElements.removeAll(compensationHandlers.values());

    parseStartEvents(scopeElement, parentScope);
    parseActivities(activityElements, scopeElement, parentScope);
    parseIntermediateCatchEvents(scopeElement, parentScope, intermediateCatchEvents);
    parseEndEvents(scopeElement, parentScope);
    parseBoundaryEvents(scopeElement, parentScope);
    parseSequenceFlow(scopeElement, parentScope, compensationHandlers);
    parseExecutionListenersOnScope(scopeElement, parentScope);
    parseAssociations(scopeElement, parentScope, compensationHandlers);
    parseCompensationHandlers(parentScope, compensationHandlers);

    for (ScopeImpl.BacklogErrorCallback callback : parentScope.getBacklogErrorCallbacks()) {
      callback.callback();
    }

    if (parentScope instanceof ProcessDefinition processDefinition) {
      parseProcessDefinitionCustomExtensions(scopeElement, processDefinition);
    }
  }

  protected HashMap<String, Element> filterIntermediateCatchEvents(List<Element> activityElements) {
    HashMap<String, Element> intermediateCatchEvents = new HashMap<>();
    for(Element activityElement : activityElements) {
      if (ActivityTypes.INTERMEDIATE_EVENT_CATCH.equals(activityElement.getTagName())) {
        intermediateCatchEvents.put(activityElement.attribute("id"), activityElement);
      }
    }
    return intermediateCatchEvents;
  }

  protected HashMap<String, Element> filterCompensationHandlers(List<Element> activityElements) {
    HashMap<String, Element> compensationHandlers = new HashMap<>();
    for(Element activityElement : activityElements) {
      if (isCompensationHandler(activityElement)) {
        compensationHandlers.put(activityElement.attribute("id"), activityElement);
      }
    }
    return compensationHandlers;
  }

  @SuppressWarnings("unused")
  protected void parseIntermediateCatchEvents(Element scopeElement, ScopeImpl parentScope, Map<String, Element> intermediateCatchEventElements) {
    for (Element intermediateCatchEventElement : intermediateCatchEventElements.values()) {

      if (parentScope.findActivity(intermediateCatchEventElement.attribute("id")) == null) {
        // check whether activity is already parsed
        ActivityImpl activity = parseIntermediateCatchEvent(intermediateCatchEventElement, parentScope, null);

        if (activity != null) {
          parseActivityInputOutput(intermediateCatchEventElement, activity);
        }
      }
    }
    intermediateCatchEventElements.clear();
  }

  protected void parseProcessDefinitionCustomExtensions(Element scopeElement, ProcessDefinition definition) {
    parseStartAuthorization(scopeElement, definition);
  }

  protected void parseStartAuthorization(Element scopeElement, ProcessDefinition definition) {
    ProcessDefinitionEntity processDefinition = (ProcessDefinitionEntity) definition;

    // parse activiti:potentialStarters
    Element extentionsElement = scopeElement.element(EXTENSION_ELEMENTS_TAG);
    if (extentionsElement != null) {
      List<Element> potentialStarterElements = extentionsElement.elementsNS(OPERATON_BPMN_EXTENSIONS_NS, POTENTIAL_STARTER);

      for (Element potentialStarterElement : potentialStarterElements) {
        parsePotentialStarterResourceAssignment(potentialStarterElement, processDefinition);
      }
    }

    // parse activiti:candidateStarterUsers
    String candidateUsersString = scopeElement.attributeNS(OPERATON_BPMN_EXTENSIONS_NS, CANDIDATE_STARTER_USERS_EXTENSION);
    if (candidateUsersString != null) {
      List<String> candidateUsers = parseCommaSeparatedList(candidateUsersString);
      for (String candidateUser : candidateUsers) {
        processDefinition.addCandidateStarterUserIdExpression(expressionManager.createExpression(candidateUser.trim()));
      }
    }

    // Candidate activiti:candidateStarterGroups
    String candidateGroupsString = scopeElement.attributeNS(OPERATON_BPMN_EXTENSIONS_NS, CANDIDATE_STARTER_GROUPS_EXTENSION);
    if (candidateGroupsString != null) {
      List<String> candidateGroups = parseCommaSeparatedList(candidateGroupsString);
      for (String candidateGroup : candidateGroups) {
        processDefinition.addCandidateStarterGroupIdExpression(expressionManager.createExpression(candidateGroup.trim()));
      }
    }

  }

  protected void parsePotentialStarterResourceAssignment(Element performerElement, ProcessDefinitionEntity processDefinition) {
    Element raeElement = performerElement.element(RESOURCE_ASSIGNMENT_EXPR);
    if (raeElement != null) {
      Element feElement = raeElement.element(FORMAL_EXPRESSION);
      if (feElement != null) {
        List<String> assignmentExpressions = parseCommaSeparatedList(feElement.getText());
        for (String assignmentExpression : assignmentExpressions) {
          assignmentExpression = assignmentExpression.trim();
          if (assignmentExpression.startsWith(USER_PREFIX)) {
            String userAssignementId = getAssignmentId(assignmentExpression, USER_PREFIX);
            processDefinition.addCandidateStarterUserIdExpression(expressionManager.createExpression(userAssignementId));
          } else if (assignmentExpression.startsWith(GROUP_PREFIX)) {
            String groupAssignementId = getAssignmentId(assignmentExpression, GROUP_PREFIX);
            processDefinition.addCandidateStarterGroupIdExpression(expressionManager.createExpression(groupAssignementId));
          } else { // default: given string is a goupId, as-is.
            processDefinition.addCandidateStarterGroupIdExpression(expressionManager.createExpression(assignmentExpression));
          }
        }
      }
    }
  }

  protected void parseAssociations(Element scopeElement, ScopeImpl parentScope, Map<String, Element> compensationHandlers) {
    for (Element associationElement : scopeElement.elements("association")) {
      String sourceRef = associationElement.attribute(SOURCE_REF_ATTRIBUTE);
      if (sourceRef == null) {
        addError("association element missing attribute 'sourceRef'", associationElement);
      }
      String targetRef = associationElement.attribute(TARGET_REF_ATTRIBUTE);
      if (targetRef == null) {
        addError("association element missing attribute 'targetRef'", associationElement);
      }
      ActivityImpl sourceActivity = parentScope.findActivity(sourceRef);
      ActivityImpl targetActivity = parentScope.findActivity(targetRef);

      // an association may reference elements that are not parsed as activities
      // (like for instance text annotations so do not throw an exception if sourceActivity or targetActivity are null)
      // However, we make sure they reference 'something':
      if (sourceActivity == null && !elementIds.contains(sourceRef)) {
        addError("Invalid reference sourceRef '%s' of association element ".formatted(sourceRef), associationElement);
      } else if (targetActivity == null && !elementIds.contains(targetRef)) {
        addError("Invalid reference targetRef '%s' of association element ".formatted(targetRef), associationElement);
      } else {

        if (sourceActivity != null && ActivityTypes.BOUNDARY_COMPENSATION.equals(sourceActivity.getProperty(BpmnProperties.TYPE.name()))) {

          if (targetActivity == null && compensationHandlers.containsKey(targetRef)) {
            targetActivity = parseCompensationHandlerForCompensationBoundaryEvent(parentScope, sourceActivity, targetRef, compensationHandlers);

            compensationHandlers.remove(targetActivity.getId());
          }

          if (targetActivity != null) {
            parseAssociationOfCompensationBoundaryEvent(associationElement, sourceActivity, targetActivity);
          }
        }
      }
    }
  }

  protected ActivityImpl parseCompensationHandlerForCompensationBoundaryEvent(ScopeImpl parentScope, ActivityImpl sourceActivity, String targetRef,
      Map<String, Element> compensationHandlers) {

    Element compensationHandler = compensationHandlers.get(targetRef);

    ActivityImpl eventScope = (ActivityImpl) sourceActivity.getEventScope();
    ActivityImpl compensationHandlerActivity = null;
    if (eventScope.isMultiInstance()) {
      ScopeImpl miBody = eventScope.getFlowScope();
      compensationHandlerActivity = parseActivity(compensationHandler, null, miBody);
    } else {
      compensationHandlerActivity = parseActivity(compensationHandler, null, parentScope);
    }

    compensationHandlerActivity.getProperties().set(BpmnProperties.COMPENSATION_BOUNDARY_EVENT, sourceActivity);
    return compensationHandlerActivity;
  }

  protected void parseAssociationOfCompensationBoundaryEvent(Element associationElement, ActivityImpl sourceActivity, ActivityImpl targetActivity) {
    if (!targetActivity.isCompensationHandler()) {
      addError("compensation boundary catch must be connected to element with isForCompensation=true",
          associationElement,
          sourceActivity.getId(),
          targetActivity.getId());

    } else {
      ActivityImpl compensatedActivity = (ActivityImpl) sourceActivity.getEventScope();

      ActivityImpl compensationHandler = compensatedActivity.findCompensationHandler();
      if (compensationHandler != null && compensationHandler.isSubProcessScope()) {
        addError("compensation boundary event and event subprocess with compensation start event are not supported on the same scope",
            associationElement,
            compensatedActivity.getId(),
            sourceActivity.getId()
            );
      } else {

        compensatedActivity.setProperty(PROPERTYNAME_COMPENSATION_HANDLER_ID, targetActivity.getId());
      }
    }
  }

  protected void parseCompensationHandlers(ScopeImpl parentScope, Map<String, Element> compensationHandlers) {
    // compensation handlers attached to compensation boundary events should be already parsed
    for (Element compensationHandler : new HashSet<>(compensationHandlers.values())) {
      parseActivity(compensationHandler, null, parentScope);
    }
    compensationHandlers.clear();
  }

  /**
   * Parses the start events of a certain level in the process (process,
   * subprocess or another scope).
   *
   * @param parentElement
   *          The 'parent' element that contains the start events (process,
   *          subprocess).
   * @param scope
   *          The {@link ScopeImpl} to which the start events must be added.
   */
  public void parseStartEvents(Element parentElement, ScopeImpl scope) {
    List<Element> startEventElements = parentElement.elements("startEvent");
    List<ActivityImpl> startEventActivities = new ArrayList<>();
    if (!startEventElements.isEmpty()) {
      for (Element startEventElement : startEventElements) {

        ActivityImpl startEventActivity = createActivityOnScope(startEventElement, scope);
        parseAsynchronousContinuationForActivity(startEventElement, startEventActivity);

        if (scope instanceof ProcessDefinitionEntity) {
          parseProcessDefinitionStartEvent(startEventActivity, startEventElement, parentElement, scope);
          startEventActivities.add(startEventActivity);
        } else {
          parseScopeStartEvent(startEventActivity, startEventElement, parentElement, (ActivityImpl) scope);
        }

        ensureNoIoMappingDefined(startEventElement);

        parseExecutionListenersOnScope(startEventElement, startEventActivity);
      }
    } else {
      if (Arrays.asList(PROCESS_TAG, SUB_PROCESS_TAG).contains(parentElement.getTagName())) {
        addError(parentElement.getTagName() + " must define a startEvent element", parentElement);
      }
    }
    if (scope instanceof ProcessDefinitionEntity processDefinitionEntity) {
      selectInitial(startEventActivities, processDefinitionEntity, parentElement);
      parseStartFormHandlers(startEventElements, processDefinitionEntity);
    }

    // invoke parse listeners
    for (Element startEventElement : startEventElements) {
      ActivityImpl startEventActivity = scope.getChildActivity(startEventElement.attribute("id"));
      for (BpmnParseListener parseListener : parseListeners) {
        parseListener.parseStartEvent(startEventElement, scope, startEventActivity);
      }
    }
  }

  protected void selectInitial(List<ActivityImpl> startEventActivities, ProcessDefinitionEntity processDefinition, Element parentElement) {
    ActivityImpl initial = null;
    // validate that there is s single none start event / timer start event:
    List<String> exclusiveStartEventTypes = Arrays.asList("startEvent", "startTimerEvent");

    for (ActivityImpl activityImpl : startEventActivities) {
      if (exclusiveStartEventTypes.contains(activityImpl.getProperty(BpmnProperties.TYPE.name()))) {
        if (initial == null) {
          initial = activityImpl;
        } else {
          addError("multiple none start events or timer start events not supported on process definition", parentElement, activityImpl.getId());
        }
      }
    }
    // if there is a single start event, select it as initial, regardless of its type:
    if (initial == null && startEventActivities.size() == 1) {
      initial = startEventActivities.get(0);
    }
    processDefinition.setInitial(initial);
  }

  @SuppressWarnings("unused")
  protected void parseProcessDefinitionStartEvent(ActivityImpl startEventActivity, Element startEventElement, Element parentElement, ScopeImpl scope) {
    ProcessDefinitionEntity processDefinition = (ProcessDefinitionEntity) scope;

    String initiatorVariableName = startEventElement.attributeNS(OPERATON_BPMN_EXTENSIONS_NS, "initiator");
    if (initiatorVariableName != null) {
      processDefinition.setProperty(PROPERTYNAME_INITIATOR_VARIABLE_NAME, initiatorVariableName);
    }

    // all start events share the same behavior:
    startEventActivity.setActivityBehavior(new NoneStartEventActivityBehavior());

    Element timerEventDefinition = startEventElement.element(TIMER_EVENT_DEFINITION);
    Element messageEventDefinition = startEventElement.element(MESSAGE_EVENT_DEFINITION);
    Element signalEventDefinition = startEventElement.element(SIGNAL_EVENT_DEFINITION);
    Element conditionEventDefinition = startEventElement.element(CONDITIONAL_EVENT_DEFINITION);
    if (timerEventDefinition != null) {
      parseTimerStartEventDefinition(timerEventDefinition, startEventActivity, processDefinition);
    } else if (messageEventDefinition != null) {
      startEventActivity.getProperties().set(BpmnProperties.TYPE, ActivityTypes.START_EVENT_MESSAGE);

      EventSubscriptionDeclaration messageStartEventSubscriptionDeclaration =
          parseMessageEventDefinition(messageEventDefinition, startEventElement.attribute("id"));
      messageStartEventSubscriptionDeclaration.setActivityId(startEventActivity.getId());
      messageStartEventSubscriptionDeclaration.setStartEvent(true);

      ensureNoExpressionInMessageStartEvent(messageEventDefinition, messageStartEventSubscriptionDeclaration, startEventElement.attribute("id"));
      addEventSubscriptionDeclaration(messageStartEventSubscriptionDeclaration, processDefinition, startEventElement);
    } else if (signalEventDefinition != null) {
      startEventActivity.getProperties().set(BpmnProperties.TYPE, ActivityTypes.START_EVENT_SIGNAL);
      startEventActivity.setEventScope(scope);

      parseSignalCatchEventDefinition(signalEventDefinition, startEventActivity, true);
    } else if (conditionEventDefinition != null) {
      startEventActivity.getProperties().set(BpmnProperties.TYPE, ActivityTypes.START_EVENT_CONDITIONAL);

      ConditionalEventDefinition conditionalEventDefinition = parseConditionalEventDefinition(conditionEventDefinition, startEventActivity);
      conditionalEventDefinition.setStartEvent(true);
      conditionalEventDefinition.setActivityId(startEventActivity.getId());
      startEventActivity.getProperties().set(BpmnProperties.CONDITIONAL_EVENT_DEFINITION, conditionalEventDefinition);

      addEventSubscriptionDeclaration(conditionalEventDefinition, processDefinition, startEventElement);
    }
  }

  protected void parseStartFormHandlers(List<Element> startEventElements, ProcessDefinitionEntity processDefinition) {
    if (processDefinition.getInitial() != null) {
      for (Element startEventElement : startEventElements) {

        if (startEventElement.attribute("id").equals(processDefinition.getInitial().getId())) {

          StartFormHandler startFormHandler;
          String startFormHandlerClassName = startEventElement.attributeNS(OPERATON_BPMN_EXTENSIONS_NS, "formHandlerClass");
          if (startFormHandlerClassName != null) {
            startFormHandler = (StartFormHandler) ReflectUtil.instantiate(startFormHandlerClassName);
          } else {
            startFormHandler = new DefaultStartFormHandler();
          }

          startFormHandler.parseConfiguration(startEventElement, deployment, processDefinition, this);

          processDefinition.setStartFormHandler(new DelegateStartFormHandler(startFormHandler, deployment));

          FormDefinition formDefinition = parseFormDefinition(startEventElement);
          processDefinition.setStartFormDefinition(formDefinition);

          processDefinition.setHasStartFormKey(formDefinition.getFormKey() != null);
        }

      }
    }
  }

  protected void parseScopeStartEvent(ActivityImpl startEventActivity, Element startEventElement, Element parentElement, ActivityImpl scopeActivity) {

    Properties scopeProperties = scopeActivity.getProperties();

    // set this as the scope's initial
    if (!scopeProperties.contains(BpmnProperties.INITIAL_ACTIVITY)) {
      scopeProperties.set(BpmnProperties.INITIAL_ACTIVITY, startEventActivity);
    } else {
      addError("multiple start events not supported for subprocess", parentElement, startEventActivity.getId());
    }

    Element errorEventDefinition = startEventElement.element(ERROR_EVENT_DEFINITION);
    Element messageEventDefinition = startEventElement.element(MESSAGE_EVENT_DEFINITION);
    Element signalEventDefinition = startEventElement.element(SIGNAL_EVENT_DEFINITION);
    Element timerEventDefinition = startEventElement.element(TIMER_EVENT_DEFINITION);
    Element compensateEventDefinition = startEventElement.element(COMPENSATE_EVENT_DEFINITION);
    Element escalationEventDefinitionElement = startEventElement.element(ESCALATION_EVENT_DEFINITION);
    Element conditionalEventDefinitionElement = startEventElement.element(CONDITIONAL_EVENT_DEFINITION);

    if (scopeActivity.isTriggeredByEvent()) {
      // event subprocess
      EventSubProcessStartEventActivityBehavior behavior = new EventSubProcessStartEventActivityBehavior();

      // parse isInterrupting
      String isInterruptingAttr = startEventElement.attribute(INTERRUPTING);
      boolean isInterrupting = TRUE.equalsIgnoreCase(isInterruptingAttr);

      if (isInterrupting) {
        scopeActivity.setActivityStartBehavior(ActivityStartBehavior.INTERRUPT_EVENT_SCOPE);
      } else {
        scopeActivity.setActivityStartBehavior(ActivityStartBehavior.CONCURRENT_IN_FLOW_SCOPE);
      }

      // the event scope of the start event is the flow scope of the event subprocess
      startEventActivity.setEventScope(scopeActivity.getFlowScope());

      if (errorEventDefinition != null) {
        if (!isInterrupting) {
          addError("error start event of event subprocess must be interrupting", startEventElement);
        }
        parseErrorStartEventDefinition(errorEventDefinition, startEventActivity);

      } else if (messageEventDefinition != null) {
        startEventActivity.getProperties().set(BpmnProperties.TYPE, ActivityTypes.START_EVENT_MESSAGE);

        EventSubscriptionDeclaration messageStartEventSubscriptionDeclaration =
            parseMessageEventDefinition(messageEventDefinition, startEventActivity.getId());
        parseEventDefinitionForSubprocess(messageStartEventSubscriptionDeclaration, startEventActivity, messageEventDefinition);

      } else if (signalEventDefinition != null) {
        startEventActivity.getProperties().set(BpmnProperties.TYPE, ActivityTypes.START_EVENT_SIGNAL);

        EventSubscriptionDeclaration eventSubscriptionDeclaration = parseSignalEventDefinition(signalEventDefinition, false, startEventActivity.getId());
        parseEventDefinitionForSubprocess(eventSubscriptionDeclaration, startEventActivity, signalEventDefinition);

      } else if (timerEventDefinition != null) {
        parseTimerStartEventDefinitionForEventSubprocess(timerEventDefinition, startEventActivity, isInterrupting);

      } else if (compensateEventDefinition != null) {
        parseCompensationEventSubprocess(startEventActivity, startEventElement, scopeActivity, compensateEventDefinition);

      } else if (escalationEventDefinitionElement != null) {
        startEventActivity.getProperties().set(BpmnProperties.TYPE, ActivityTypes.START_EVENT_ESCALATION);

        EscalationEventDefinition escalationEventDefinition = createEscalationEventDefinitionForEscalationHandler(escalationEventDefinitionElement, scopeActivity, isInterrupting, startEventActivity.getId());
        addEscalationEventDefinition(startEventActivity.getEventScope(), escalationEventDefinition, escalationEventDefinitionElement, startEventActivity.getId());
      } else if (conditionalEventDefinitionElement != null) {

        final ConditionalEventDefinition conditionalEventDef = parseConditionalStartEventForEventSubprocess(conditionalEventDefinitionElement, startEventActivity, isInterrupting);
        behavior = new EventSubProcessStartConditionalEventActivityBehavior(conditionalEventDef);
      } else {
        addError("start event of event subprocess must be of type 'error', 'message', 'timer', 'signal', 'compensation' or 'escalation'", startEventElement);
      }

     startEventActivity.setActivityBehavior(behavior);
    } else { // "regular" subprocess
      Element conditionalEventDefinition = startEventElement.element(CONDITIONAL_EVENT_DEFINITION);

      if (conditionalEventDefinition != null) {
        addError("conditionalEventDefinition is not allowed on start event within a subprocess", conditionalEventDefinition, startEventActivity.getId());
      }
      if (timerEventDefinition != null) {
        addError("timerEventDefinition is not allowed on start event within a subprocess", timerEventDefinition, startEventActivity.getId());
      }
      if (escalationEventDefinitionElement != null) {
        addError("escalationEventDefinition is not allowed on start event within a subprocess", escalationEventDefinitionElement, startEventActivity.getId());
      }
      if (compensateEventDefinition != null) {
        addError("compensateEventDefinition is not allowed on start event within a subprocess", compensateEventDefinition, startEventActivity.getId());
      }
      if (errorEventDefinition != null) {
        addError("errorEventDefinition only allowed on start event if subprocess is an event subprocess", errorEventDefinition, startEventActivity.getId());
      }
      if (messageEventDefinition != null) {
        addError("messageEventDefinition only allowed on start event if subprocess is an event subprocess", messageEventDefinition, startEventActivity.getId());
      }
      if (signalEventDefinition != null) {
        addError("signalEventDefinition only allowed on start event if subprocess is an event subprocess", signalEventDefinition, startEventActivity.getId());
      }

      startEventActivity.setActivityBehavior(new NoneStartEventActivityBehavior());
    }
  }

  protected void parseCompensationEventSubprocess(ActivityImpl startEventActivity, Element startEventElement, ActivityImpl scopeActivity, Element compensateEventDefinition) {
    startEventActivity.getProperties().set(BpmnProperties.TYPE, ActivityTypes.START_EVENT_COMPENSATION);
    scopeActivity.setProperty(PROPERTYNAME_IS_FOR_COMPENSATION, Boolean.TRUE);

    if (scopeActivity.getFlowScope() instanceof ProcessDefinitionEntity) {
      addError("event subprocess with compensation start event is only supported for embedded subprocess "
          + "(since throwing compensation through a call activity-induced process hierarchy is not supported)", startEventElement);
    }

    ScopeImpl subprocess = scopeActivity.getFlowScope();
    ActivityImpl compensationHandler = ((ActivityImpl) subprocess).findCompensationHandler();
    if (compensationHandler == null) {
      // add property to subprocess
      subprocess.setProperty(PROPERTYNAME_COMPENSATION_HANDLER_ID, scopeActivity.getActivityId());
    } else {

      if (compensationHandler.isSubProcessScope()) {
        addError("multiple event subprocesses with compensation start event are not supported on the same scope", startEventElement);
      } else {
        addError("compensation boundary event and event subprocess with compensation start event are not supported on the same scope", startEventElement);
      }
    }

    validateCatchCompensateEventDefinition(compensateEventDefinition, startEventActivity.getId());
  }

  protected void parseErrorStartEventDefinition(Element errorEventDefinition, ActivityImpl startEventActivity) {
    startEventActivity.getProperties().set(BpmnProperties.TYPE, ActivityTypes.START_EVENT_ERROR);
    String errorRef = errorEventDefinition.attribute(ERROR_REF_ATTRIBUTE);
    Error error = null;
    // the error event definition executes the event subprocess activity which
    // hosts the start event
    String eventSubProcessActivity = startEventActivity.getFlowScope().getId();
    ErrorEventDefinition definition = new ErrorEventDefinition(eventSubProcessActivity);
    if (errorRef != null) {
      error = bpmnParseErrors.get(errorRef);
      String errorCode = error == null ? errorRef : error.getErrorCode();
      definition.setErrorCode(errorCode);
    }
    definition.setPrecedence(10);
    setErrorCodeVariableOnErrorEventDefinition(errorEventDefinition, definition);
    setErrorMessageVariableOnErrorEventDefinition(errorEventDefinition, definition);
    addErrorEventDefinition(definition, startEventActivity.getEventScope());
  }

  /**
   * Sets the value for "operaton:errorCodeVariable" on the passed definition if
   * it's present.
   *
   * @param errorEventDefinition
   *          the XML errorEventDefinition tag
   * @param definition
   *          the errorEventDefinition that can get the errorCodeVariable value
   */
  protected void setErrorCodeVariableOnErrorEventDefinition(Element errorEventDefinition, ErrorEventDefinition definition) {
    String errorCodeVar = errorEventDefinition.attributeNS(OPERATON_BPMN_EXTENSIONS_NS, "errorCodeVariable");
    if (errorCodeVar != null) {
      definition.setErrorCodeVariable(errorCodeVar);
    }
  }

  /**
   * Sets the value for "operaton:errorMessageVariable" on the passed definition if
   * it's present.
   *
   * @param errorEventDefinition
   *          the XML errorEventDefinition tag
   * @param definition
   *          the errorEventDefinition that can get the errorMessageVariable value
   */
  protected void setErrorMessageVariableOnErrorEventDefinition(Element errorEventDefinition, ErrorEventDefinition definition) {
    String errorMessageVariable = errorEventDefinition.attributeNS(OPERATON_BPMN_EXTENSIONS_NS, "errorMessageVariable");
    if (errorMessageVariable != null) {
      definition.setErrorMessageVariable(errorMessageVariable);
    }
  }

  protected EventSubscriptionDeclaration parseMessageEventDefinition(Element messageEventDefinition, String messageElementId) {
    String messageRef = messageEventDefinition.attribute("messageRef");
    if (messageRef == null) {
      addError("attribute 'messageRef' is required", messageEventDefinition, messageElementId);
    }
    MessageDefinition messageDefinition = messages.get(resolveName(messageRef));
    if (messageDefinition == null) {
      addError("Invalid 'messageRef': no message with id '%s' found.".formatted(messageRef), messageEventDefinition, messageElementId);
    }
    return new EventSubscriptionDeclaration(messageDefinition.getExpression(), EventType.MESSAGE);
  }

  protected void addEventSubscriptionDeclaration(EventSubscriptionDeclaration subscription, ScopeImpl scope, Element element) {
    if (subscription.getEventType().equals(EventType.MESSAGE.name()) && (!subscription.hasEventName())) {
      addError("Cannot have a message event subscription with an empty or missing name", element, subscription.getActivityId());
    }

    Map<String, EventSubscriptionDeclaration> eventDefinitions = scope.getProperties().get(BpmnProperties.EVENT_SUBSCRIPTION_DECLARATIONS);

    // if this is a message event, validate that it is the only one with the provided name for this scope
    if (hasMultipleMessageEventDefinitionsWithSameName(subscription, eventDefinitions.values())){
      addError("Cannot have more than one message event subscription with name '%s' for scope '%s'".formatted(subscription.getUnresolvedEventName(), scope.getId()),
          element, subscription.getActivityId());
    }

    // if this is a signal event, validate that it is the only one with the provided name for this scope
    if (hasMultipleSignalEventDefinitionsWithSameName(subscription, eventDefinitions.values())){
      addError("Cannot have more than one signal event subscription with name '%s' for scope '%s'".formatted(subscription.getUnresolvedEventName(), scope.getId()),
          element, subscription.getActivityId());
    }
    // if this is a conditional event, validate that it is the only one with the provided condition
    if (subscription.isStartEvent() && hasMultipleConditionalEventDefinitionsWithSameCondition(subscription, eventDefinitions.values())) {
      addError("Cannot have more than one conditional event subscription with the same condition '%s'".formatted(((ConditionalEventDefinition) subscription).getConditionAsString()), element, subscription.getActivityId());
    }

    scope.getProperties().putMapEntry(BpmnProperties.EVENT_SUBSCRIPTION_DECLARATIONS, subscription.getActivityId(), subscription);
  }

  protected boolean hasMultipleMessageEventDefinitionsWithSameName(EventSubscriptionDeclaration subscription, Collection<EventSubscriptionDeclaration> eventDefinitions) {
    return hasMultipleEventDefinitionsWithSameName(subscription, eventDefinitions, EventType.MESSAGE.name());
  }

  protected boolean hasMultipleSignalEventDefinitionsWithSameName(EventSubscriptionDeclaration subscription, Collection<EventSubscriptionDeclaration> eventDefinitions) {
    return hasMultipleEventDefinitionsWithSameName(subscription, eventDefinitions, EventType.SIGNAL.name());
  }

  protected boolean hasMultipleConditionalEventDefinitionsWithSameCondition(EventSubscriptionDeclaration subscription, Collection<EventSubscriptionDeclaration> eventDefinitions) {
    if (subscription.getEventType().equals(EventType.CONDITONAL.name())) {
      for (EventSubscriptionDeclaration eventDefinition : eventDefinitions) {
        if (eventDefinition.getEventType().equals(EventType.CONDITONAL.name()) && eventDefinition.isStartEvent() == subscription.isStartEvent()
            && ((ConditionalEventDefinition) eventDefinition).getConditionAsString().equals(((ConditionalEventDefinition) subscription).getConditionAsString())) {
          return true;
        }
      }
    }
    return false;
  }

  protected boolean hasMultipleEventDefinitionsWithSameName(EventSubscriptionDeclaration subscription, Collection<EventSubscriptionDeclaration> eventDefinitions, String eventType) {
    if (subscription.getEventType().equals(eventType)) {
      for (EventSubscriptionDeclaration eventDefinition : eventDefinitions) {
        if (eventDefinition.getEventType().equals(eventType) && eventDefinition.getUnresolvedEventName().equals(subscription.getUnresolvedEventName())
            && eventDefinition.isStartEvent() == subscription.isStartEvent()) {
         return true;
        }
      }
    }
    return false;
  }

  protected void addEventSubscriptionJobDeclaration(EventSubscriptionJobDeclaration jobDeclaration, ActivityImpl activity, Element element) {
    List<EventSubscriptionJobDeclaration> jobDeclarationsForActivity = (List<EventSubscriptionJobDeclaration>) activity.getProperty(PROPERTYNAME_EVENT_SUBSCRIPTION_JOB_DECLARATION);

    if (jobDeclarationsForActivity == null) {
      jobDeclarationsForActivity = new ArrayList<>();
      activity.setProperty(PROPERTYNAME_EVENT_SUBSCRIPTION_JOB_DECLARATION, jobDeclarationsForActivity);
    }

    if(activityAlreadyContainsJobDeclarationEventType(jobDeclarationsForActivity, jobDeclaration)){
      addError("Activity contains already job declaration with type " + jobDeclaration.getEventType(), element, activity.getId());
    }

    jobDeclarationsForActivity.add(jobDeclaration);
  }

  /**
   * Assumes that an activity has at most one declaration of a certain eventType.
   */
  protected boolean activityAlreadyContainsJobDeclarationEventType(List<EventSubscriptionJobDeclaration> jobDeclarationsForActivity,
                                                                   EventSubscriptionJobDeclaration jobDeclaration){
    for(EventSubscriptionJobDeclaration declaration: jobDeclarationsForActivity){
      if(declaration.getEventType().equals(jobDeclaration.getEventType())){
        return true;
      }
    }
    return false;
  }

  /**
   * Parses the activities of a certain level in the process (process,
   * subprocess or another scope).
   *
   * @param activityElements
   *          The list of activities to be parsed. This list may be filtered before.
   * @param parentElement
   *          The 'parent' element that contains the activities (process, subprocess).
   * @param scopeElement
   *          The {@link ScopeImpl} to which the activities must be added.
   */
  public void parseActivities(List<Element> activityElements, Element parentElement, ScopeImpl scopeElement) {
    for (Element activityElement : activityElements) {
      parseActivity(activityElement, parentElement, scopeElement);
    }
  }

  protected ActivityImpl parseActivity(Element activityElement, Element parentElement, ScopeImpl scopeElement) {
    ActivityImpl activity = null;

    boolean isMultiInstance = false;
    ScopeImpl miBody = parseMultiInstanceLoopCharacteristics(activityElement, scopeElement);
    if (miBody != null) {
      scopeElement = miBody;
      isMultiInstance = true;
    }

    if (ActivityTypes.GATEWAY_EXCLUSIVE.equals(activityElement.getTagName())) {
      activity = parseExclusiveGateway(activityElement, scopeElement);
    } else if (ActivityTypes.GATEWAY_INCLUSIVE.equals(activityElement.getTagName())) {
      activity = parseInclusiveGateway(activityElement, scopeElement);
    } else if (ActivityTypes.GATEWAY_PARALLEL.equals(activityElement.getTagName())) {
      activity = parseParallelGateway(activityElement, scopeElement);
    } else if (ActivityTypes.TASK_SCRIPT.equals(activityElement.getTagName())) {
      activity = parseScriptTask(activityElement, scopeElement);
    } else if (ActivityTypes.TASK_SERVICE.equals(activityElement.getTagName())) {
      activity = parseServiceTask(activityElement, scopeElement);
    } else if (ActivityTypes.TASK_BUSINESS_RULE.equals(activityElement.getTagName())) {
      activity = parseBusinessRuleTask(activityElement, scopeElement);
    } else if (ActivityTypes.TASK.equals(activityElement.getTagName())) {
      activity = parseTask(activityElement, scopeElement);
    } else if (ActivityTypes.TASK_MANUAL_TASK.equals(activityElement.getTagName())) {
      activity = parseManualTask(activityElement, scopeElement);
    } else if (ActivityTypes.TASK_USER_TASK.equals(activityElement.getTagName())) {
      activity = parseUserTask(activityElement, scopeElement);
    } else if (ActivityTypes.TASK_SEND_TASK.equals(activityElement.getTagName())) {
      activity = parseSendTask(activityElement, scopeElement);
    } else if (ActivityTypes.TASK_RECEIVE_TASK.equals(activityElement.getTagName())) {
      activity = parseReceiveTask(activityElement, scopeElement);
    } else if (ActivityTypes.SUB_PROCESS.equals(activityElement.getTagName())) {
      activity = parseSubProcess(activityElement, scopeElement);
    } else if (ActivityTypes.CALL_ACTIVITY.equals(activityElement.getTagName())) {
      activity = parseCallActivity(activityElement, scopeElement, isMultiInstance);
    } else if (ActivityTypes.INTERMEDIATE_EVENT_THROW.equals(activityElement.getTagName())) {
      activity = parseIntermediateThrowEvent(activityElement, scopeElement);
    } else if (ActivityTypes.GATEWAY_EVENT_BASED.equals(activityElement.getTagName())) {
      activity = parseEventBasedGateway(activityElement, parentElement, scopeElement);
    } else if (ActivityTypes.TRANSACTION.equals(activityElement.getTagName())) {
      activity = parseTransaction(activityElement, scopeElement);
    } else if (ActivityTypes.SUB_PROCESS_AD_HOC.equals(activityElement.getTagName()) || ActivityTypes.GATEWAY_COMPLEX.equals(activityElement.getTagName())) {
      addWarning("Ignoring unsupported activity type", activityElement);
    }

    if (isMultiInstance && activity != null) {
      activity.setProperty(PROPERTYNAME_IS_MULTI_INSTANCE, true);
    }

    if (activity != null) {
      activity.setName(activityElement.attribute("name"));
      parseActivityInputOutput(activityElement, activity);
    }

    return activity;
  }

  public void validateActivities(List<ActivityImpl> activities) {
    for (ActivityImpl activity : activities) {
      validateActivity(activity);
      // check children if it is an own scope / subprocess / ...
      if (!activity.getActivities().isEmpty()) {
        validateActivities(activity.getActivities());
      }
    }
  }

  protected void validateActivity(ActivityImpl activity) {
    if (activity.getActivityBehavior() instanceof ExclusiveGatewayActivityBehavior) {
      validateExclusiveGateway(activity);
    }
    validateOutgoingFlows(activity);
  }

  protected void validateOutgoingFlows(ActivityImpl activity) {
    if (activity.isAsyncAfter()) {
      for (PvmTransition transition : activity.getOutgoingTransitions()) {
        if (transition.getId() == null) {
          addError("Sequence flow with sourceRef='%s' must have an id, activity with id '%s' uses 'asyncAfter'.".formatted(activity.getId(), activity.getId()),
              null, activity.getId());
        }
      }
    }
  }

  public void validateExclusiveGateway(ActivityImpl activity) {
    if (activity.getOutgoingTransitions().isEmpty()) {
      addError("Exclusive Gateway '%s' has no outgoing sequence flows.".formatted(activity.getId()), null, activity.getId());
    } else if (activity.getOutgoingTransitions().size() == 1) {
      PvmTransition flow = activity.getOutgoingTransitions().get(0);
      Condition condition = (Condition) flow.getProperty(BpmnParse.PROPERTYNAME_CONDITION);
      if (condition != null) {
        addError("Exclusive Gateway '%s' has only one outgoing sequence flow ('%s'). This is not allowed to have a condition.".formatted(activity.getId(), flow.getId()), null, activity.getId(), flow.getId());
      }
    } else {
      String defaultSequenceFlow = (String) activity.getProperty(PROPERTYNAME_DEFAULT);
      boolean hasDefaultFlow = defaultSequenceFlow != null && !defaultSequenceFlow.isEmpty();

      ArrayList<PvmTransition> flowsWithoutCondition = new ArrayList<>();
      for (PvmTransition flow : activity.getOutgoingTransitions()) {
        Condition condition = (Condition) flow.getProperty(BpmnParse.PROPERTYNAME_CONDITION);
        boolean isDefaultFlow = isDefaultFlow(flow, activity);
        boolean hasConditon = condition != null;

        if (!hasConditon && !isDefaultFlow) {
          flowsWithoutCondition.add(flow);
        }
        if (hasConditon && isDefaultFlow) {
          addError("Exclusive Gateway '%s' has outgoing sequence flow '%s' which is the default flow but has a condition too.".formatted(activity.getId(), flow.getId()),
              null, activity.getId(), flow.getId());
        }
      }
      if (hasDefaultFlow || flowsWithoutCondition.size() > 1) {
        // if we either have a default flow (then no flows without conditions
        // are valid at all) or if we have more than one flow without condition
        // this is an error
        for (PvmTransition flow : flowsWithoutCondition) {
          addError(
              "Exclusive Gateway '%s' has outgoing sequence flow '%s' without condition which is not the default flow.".formatted(activity.getId(), flow.getId()),
              null, activity.getId(), flow.getId());
        }
      } else if (flowsWithoutCondition.size() == 1) {
        // Havinf no default and exactly one flow without condition this is
        // considered the default one now (to not break backward compatibility)
        PvmTransition flow = flowsWithoutCondition.get(0);
        addWarning(
            "Exclusive Gateway '%s' has outgoing sequence flow '%s' without condition which is not the default flow. We assume it to be the default flow, but it is bad modeling practice, better set the default flow in your gateway.".formatted(activity.getId(), flow.getId()),
             null, activity.getId(), flow.getId());
      }
    }
  }

  private boolean isDefaultFlow (PvmTransition flow, ActivityImpl activity) {
    if (flow.getId() == null) {
      return false;
    }
    return Objects.equals(flow.getId(), activity.getProperty(PROPERTYNAME_DEFAULT));
  }

  public ActivityImpl parseIntermediateCatchEvent(Element intermediateEventElement, ScopeImpl scopeElement, ActivityImpl eventBasedGateway) {
    ActivityImpl nestedActivity = createActivityOnScope(intermediateEventElement, scopeElement);

    Element timerEventDefinition = intermediateEventElement.element(TIMER_EVENT_DEFINITION);
    Element signalEventDefinition = intermediateEventElement.element(SIGNAL_EVENT_DEFINITION);
    Element messageEventDefinition = intermediateEventElement.element(MESSAGE_EVENT_DEFINITION);
    Element linkEventDefinitionElement = intermediateEventElement.element(LINK_EVENT_DEFINITION);
    Element conditionalEventDefinitionElement = intermediateEventElement.element(CONDITIONAL_EVENT_DEFINITION);

    // shared by all events except for link event
    IntermediateCatchEventActivityBehavior defaultCatchBehaviour = new IntermediateCatchEventActivityBehavior(eventBasedGateway != null);

    parseAsynchronousContinuationForActivity(intermediateEventElement, nestedActivity);
    boolean isEventBaseGatewayPresent = eventBasedGateway != null;

    if (isEventBaseGatewayPresent) {
      nestedActivity.setEventScope(eventBasedGateway);
      nestedActivity.setActivityStartBehavior(ActivityStartBehavior.CANCEL_EVENT_SCOPE);
    } else {
      nestedActivity.setEventScope(nestedActivity);
      nestedActivity.setScope(true);
    }

    nestedActivity.setActivityBehavior(defaultCatchBehaviour);
    if (timerEventDefinition != null) {
      parseIntermediateTimerEventDefinition(timerEventDefinition, nestedActivity);

    } else if (signalEventDefinition != null) {
      parseIntermediateSignalEventDefinition(signalEventDefinition, nestedActivity);

    } else if (messageEventDefinition != null) {
      parseIntermediateMessageEventDefinition(messageEventDefinition, nestedActivity);

    } else if (linkEventDefinitionElement != null) {
      if (isEventBaseGatewayPresent) {
        addError("IntermediateCatchLinkEvent is not allowed after an EventBasedGateway.", intermediateEventElement);
      }
      nestedActivity.setActivityBehavior(new IntermediateCatchLinkEventActivityBehavior());
      parseIntermediateLinkEventCatchBehavior(intermediateEventElement, nestedActivity, linkEventDefinitionElement);

    } else if (conditionalEventDefinitionElement != null) {
      ConditionalEventDefinition conditionalEvent = parseIntermediateConditionalEventDefinition(conditionalEventDefinitionElement, nestedActivity);
      nestedActivity.setActivityBehavior(new IntermediateConditionalEventBehavior(conditionalEvent, isEventBaseGatewayPresent));
    } else {
      addError("Unsupported intermediate catch event type", intermediateEventElement);
    }

    parseExecutionListenersOnScope(intermediateEventElement, nestedActivity);

    for (BpmnParseListener parseListener : parseListeners) {
      parseListener.parseIntermediateCatchEvent(intermediateEventElement, scopeElement, nestedActivity);
    }

    return nestedActivity;
  }

  protected void parseIntermediateLinkEventCatchBehavior(Element intermediateEventElement, ActivityImpl activity, Element linkEventDefinitionElement) {

    activity.getProperties().set(BpmnProperties.TYPE, ActivityTypes.INTERMEDIATE_EVENT_LINK);

    String linkName = linkEventDefinitionElement.attribute("name");
    String elementName = intermediateEventElement.attribute("name");
    String elementId = intermediateEventElement.attribute("id");

    if (eventLinkTargets.containsKey(linkName)) {
      addError("Multiple Intermediate Catch Events with the same link event name ('%s') are not allowed.".formatted(linkName), intermediateEventElement);
    } else {
      if (!linkName.equals(elementName)) {
        // this is valid - but not a good practice (as it is really confusing
        // for the reader of the process model) - hence we log a warning
        addWarning("Link Event named '%s' contains link event definition with name '%s' - it is recommended to use the same name for both.".formatted(elementName, linkName), intermediateEventElement);
      }

      // now we remember the link in order to replace the sequence flow later on
      eventLinkTargets.put(linkName, elementId);
    }
  }

  protected void parseIntermediateMessageEventDefinition(Element messageEventDefinition, ActivityImpl nestedActivity) {

    nestedActivity.getProperties().set(BpmnProperties.TYPE, ActivityTypes.INTERMEDIATE_EVENT_MESSAGE);

    EventSubscriptionDeclaration messageDefinition = parseMessageEventDefinition(messageEventDefinition, nestedActivity.getId());
    messageDefinition.setActivityId(nestedActivity.getId());
    addEventSubscriptionDeclaration(messageDefinition, nestedActivity.getEventScope(), messageEventDefinition);

    for (BpmnParseListener parseListener : parseListeners) {
      parseListener.parseIntermediateMessageCatchEventDefinition(messageEventDefinition, nestedActivity);
    }
  }

  public ActivityImpl parseIntermediateThrowEvent(Element intermediateEventElement, ScopeImpl scopeElement) {
    Element signalEventDefinitionElement = intermediateEventElement.element(SIGNAL_EVENT_DEFINITION);
    Element compensateEventDefinitionElement = intermediateEventElement.element(COMPENSATE_EVENT_DEFINITION);
    Element linkEventDefinitionElement = intermediateEventElement.element(LINK_EVENT_DEFINITION);
    Element messageEventDefinitionElement = intermediateEventElement.element(MESSAGE_EVENT_DEFINITION);
    Element escalationEventDefinition = intermediateEventElement.element(ESCALATION_EVENT_DEFINITION);
    String elementId = intermediateEventElement.attribute("id");

    // the link event gets a special treatment as a throwing link event (event
    // source)
    // will not create any activity instance but serves as a "redirection" to
    // the catching link
    // event (event target)
    if (linkEventDefinitionElement != null) {
      String linkName = linkEventDefinitionElement.attribute("name");

      // now we remember the link in order to replace the sequence flow later on
      eventLinkSources.put(elementId, linkName);
      // and done - no activity created
      return null;
    }

    ActivityImpl nestedActivityImpl = createActivityOnScope(intermediateEventElement, scopeElement);
    ActivityBehavior activityBehavior = null;

    parseAsynchronousContinuationForActivity(intermediateEventElement, nestedActivityImpl);

    boolean isServiceTaskLike = isServiceTaskLike(messageEventDefinitionElement);

    if (signalEventDefinitionElement != null) {
      nestedActivityImpl.getProperties().set(BpmnProperties.TYPE, ActivityTypes.INTERMEDIATE_EVENT_SIGNAL_THROW);

      EventSubscriptionDeclaration signalDefinition = parseSignalEventDefinition(signalEventDefinitionElement, true, nestedActivityImpl.getId());
      activityBehavior = new ThrowSignalEventActivityBehavior(signalDefinition);
    } else if (compensateEventDefinitionElement != null) {
      nestedActivityImpl.getProperties().set(BpmnProperties.TYPE, ActivityTypes.INTERMEDIATE_EVENT_COMPENSATION_THROW);
      CompensateEventDefinition compensateEventDefinition = parseThrowCompensateEventDefinition(compensateEventDefinitionElement, scopeElement, elementId);
      activityBehavior = new CompensationEventActivityBehavior(compensateEventDefinition);
      nestedActivityImpl.setProperty(PROPERTYNAME_THROWS_COMPENSATION, true);
      nestedActivityImpl.setScope(true);
    } else if (messageEventDefinitionElement != null) {
      if (isServiceTaskLike) {

        // CAM-436 same behavior as service task
        nestedActivityImpl.getProperties().set(BpmnProperties.TYPE, ActivityTypes.INTERMEDIATE_EVENT_MESSAGE_THROW);
        parseServiceTaskLike(
            nestedActivityImpl,
            ActivityTypes.INTERMEDIATE_EVENT_MESSAGE_THROW,
            messageEventDefinitionElement,
            intermediateEventElement);
      } else {
        // default to non behavior if no service task
        // properties have been specified
        nestedActivityImpl.getProperties().set(BpmnProperties.TYPE, ActivityTypes.INTERMEDIATE_EVENT_NONE_THROW);
        activityBehavior = new IntermediateThrowNoneEventActivityBehavior();
      }
    } else if (escalationEventDefinition != null) {
      nestedActivityImpl.getProperties().set(BpmnProperties.TYPE, ActivityTypes.INTERMEDIATE_EVENT_ESCALATION_THROW);

      Escalation escalation = findEscalationForEscalationEventDefinition(escalationEventDefinition, nestedActivityImpl.getId());
      if (escalation != null && escalation.getEscalationCode() == null) {
        addError("throwing escalation event must have an 'escalationCode'", escalationEventDefinition, nestedActivityImpl.getId());
      }

      activityBehavior = new ThrowEscalationEventActivityBehavior(escalation);

    } else { // None intermediate event
      nestedActivityImpl.getProperties().set(BpmnProperties.TYPE, ActivityTypes.INTERMEDIATE_EVENT_NONE_THROW);
      activityBehavior = new IntermediateThrowNoneEventActivityBehavior();
    }

    if (activityBehavior != null) {
      nestedActivityImpl.setActivityBehavior(activityBehavior);
    }

    parseExecutionListenersOnScope(intermediateEventElement, nestedActivityImpl);

    for (BpmnParseListener parseListener : parseListeners) {
      parseListener.parseIntermediateThrowEvent(intermediateEventElement, scopeElement, nestedActivityImpl);
    }

    if (isServiceTaskLike) {
      // activity behavior could be set by a listener (e.g. connector); thus,
      // check is after listener invocation
      validateServiceTaskLike(nestedActivityImpl,
          ActivityTypes.INTERMEDIATE_EVENT_MESSAGE_THROW,
          messageEventDefinitionElement);
    }

    return nestedActivityImpl;
  }

  protected CompensateEventDefinition parseThrowCompensateEventDefinition(final Element compensateEventDefinitionElement, ScopeImpl scopeElement, final String parentElementId) {
    final String activityRef = compensateEventDefinitionElement.attribute("activityRef");
    boolean waitForCompletion = TRUE.equals(compensateEventDefinitionElement.attribute("waitForCompletion", TRUE));

    if (activityRef != null && scopeElement.findActivityAtLevelOfSubprocess(activityRef) == null) {
      Boolean isTriggeredByEvent = scopeElement.getProperties().get(BpmnProperties.TRIGGERED_BY_EVENT);
      String type = (String) scopeElement.getProperty(BpmnProperties.TYPE.name());
      if (Boolean.TRUE == isTriggeredByEvent && SUB_PROCESS_TAG.equals(type)) {
        scopeElement = scopeElement.getFlowScope();
      }
      if (scopeElement.findActivityAtLevelOfSubprocess(activityRef) == null) {
        final String scopeId = scopeElement.getId();
        scopeElement.addToBacklog(activityRef, () ->
            addError("Invalid attribute value for 'activityRef': no activity with id '%s' in scope '%s'".formatted(activityRef, scopeId),
                compensateEventDefinitionElement,
                parentElementId));
      }
    }

    CompensateEventDefinition compensateEventDefinition = new CompensateEventDefinition();
    compensateEventDefinition.setActivityRef(activityRef);

    compensateEventDefinition.setWaitForCompletion(waitForCompletion);
    if (!waitForCompletion) {
      addWarning(
          "Unsupported attribute value for 'waitForCompletion': 'waitForCompletion=false' is not supported. Compensation event will wait for compensation to join.",
          compensateEventDefinitionElement, parentElementId);
    }

    return compensateEventDefinition;
  }

  protected void validateCatchCompensateEventDefinition(Element compensateEventDefinitionElement, String parentElementId) {
    String activityRef = compensateEventDefinitionElement.attribute("activityRef");
    if (activityRef != null) {
      addWarning("attribute 'activityRef' is not supported on catching compensation event. attribute will be ignored",
          compensateEventDefinitionElement, parentElementId);
    }

    String waitForCompletion = compensateEventDefinitionElement.attribute("waitForCompletion");
    if (waitForCompletion != null) {
      addWarning("attribute 'waitForCompletion' is not supported on catching compensation event. attribute will be ignored", compensateEventDefinitionElement, parentElementId);
    }
  }

  protected void parseBoundaryCompensateEventDefinition(Element compensateEventDefinition, ActivityImpl activity) {
    activity.getProperties().set(BpmnProperties.TYPE, ActivityTypes.BOUNDARY_COMPENSATION);

    ScopeImpl hostActivity = activity.getEventScope();
    for (ActivityImpl sibling : activity.getFlowScope().getActivities()) {
      if ("compensationBoundaryCatch".equals(sibling.getProperty(BpmnProperties.TYPE.name())) && sibling.getEventScope().equals(hostActivity) && sibling != activity) {
        addError("multiple boundary events with compensateEventDefinition not supported on same activity", compensateEventDefinition, activity.getId());
      }
    }

    validateCatchCompensateEventDefinition(compensateEventDefinition, activity.getId());
  }

  protected ActivityBehavior parseBoundaryCancelEventDefinition(Element cancelEventDefinition, ActivityImpl activity) {
    activity.getProperties().set(BpmnProperties.TYPE, ActivityTypes.BOUNDARY_CANCEL);

    LegacyBehavior.parseCancelBoundaryEvent(activity);

    ActivityImpl transaction = (ActivityImpl) activity.getEventScope();
    if (transaction.getActivityBehavior() instanceof MultiInstanceActivityBehavior) {
      transaction = transaction.getActivities().get(0);
    }

    if (!TRANSACTION_TAG.equals(transaction.getProperty(BpmnProperties.TYPE.name()))) {
      addError("boundary event with cancelEventDefinition only supported on transaction subprocesses", cancelEventDefinition, activity.getId());
    }

    // ensure there is only one cancel boundary event
    for (ActivityImpl sibling : activity.getFlowScope().getActivities()) {
      if ("cancelBoundaryCatch".equals(sibling.getProperty(BpmnProperties.TYPE.name())) && sibling != activity && sibling.getEventScope() == transaction) {
        addError("multiple boundary events with cancelEventDefinition not supported on same transaction subprocess", cancelEventDefinition, activity.getId());
      }
    }

    // find all cancel end events
    for (ActivityImpl childActivity : transaction.getActivities()) {
      ActivityBehavior activityBehavior = childActivity.getActivityBehavior();
      if (activityBehavior instanceof CancelEndEventActivityBehavior cancelEndEventBehavior) {
        cancelEndEventBehavior.setCancelBoundaryEvent(activity);
      }
    }

    return new CancelBoundaryEventActivityBehavior();
  }

  /**
   * Parses loopCharacteristics (standardLoop/Multi-instance) of an activity, if
   * any is defined.
   */
  public ScopeImpl parseMultiInstanceLoopCharacteristics(Element activityElement, ScopeImpl scope) {

    Element miLoopCharacteristics = activityElement.element("multiInstanceLoopCharacteristics");
    if (miLoopCharacteristics == null) {
      return null;
    }
    String id = activityElement.attribute("id");

    LOG.parsingElement("mi body for activity", id);

    id = getIdForMiBody(id);
    ActivityImpl miBodyScope = scope.createActivity(id);
    setActivityAsyncDelegates(miBodyScope);
    miBodyScope.setProperty(BpmnProperties.TYPE.name(), ActivityTypes.MULTI_INSTANCE_BODY);
    miBodyScope.setScope(true);

    boolean isSequential = parseBooleanAttribute(miLoopCharacteristics.attribute("isSequential"), false);

    MultiInstanceActivityBehavior behavior = null;
    if (isSequential) {
      behavior = new SequentialMultiInstanceActivityBehavior();
    } else {
      behavior = new ParallelMultiInstanceActivityBehavior();
    }
    miBodyScope.setActivityBehavior(behavior);

    // loopCardinality
    Element loopCardinality = miLoopCharacteristics.element("loopCardinality");
    if (loopCardinality != null) {
      String loopCardinalityText = loopCardinality.getText();
      if (loopCardinalityText == null || "".equals(loopCardinalityText)) {
        addError("loopCardinality must be defined for a multiInstanceLoopCharacteristics definition ", miLoopCharacteristics, id);
      }
      behavior.setLoopCardinalityExpression(expressionManager.createExpression(loopCardinalityText));
    }

    // completionCondition
    Element completionCondition = miLoopCharacteristics.element("completionCondition");
    if (completionCondition != null) {
      String completionConditionText = completionCondition.getText();
      behavior.setCompletionConditionExpression(expressionManager.createExpression(completionConditionText));
    }

    // activiti:collection
    String collection = miLoopCharacteristics.attributeNS(OPERATON_BPMN_EXTENSIONS_NS, "collection");
    if (collection != null) {
      if (collection.contains("{")) {
        behavior.setCollectionExpression(expressionManager.createExpression(collection));
      } else {
        behavior.setCollectionVariable(collection);
      }
    }

    // loopDataInputRef
    Element loopDataInputRef = miLoopCharacteristics.element("loopDataInputRef");
    if (loopDataInputRef != null) {
      String loopDataInputRefText = loopDataInputRef.getText();
      if (loopDataInputRefText != null) {
        if (loopDataInputRefText.contains("{")) {
          behavior.setCollectionExpression(expressionManager.createExpression(loopDataInputRefText));
        } else {
          behavior.setCollectionVariable(loopDataInputRefText);
        }
      }
    }

    // activiti:elementVariable
    String elementVariable = miLoopCharacteristics.attributeNS(OPERATON_BPMN_EXTENSIONS_NS, "elementVariable");
    if (elementVariable != null) {
      behavior.setCollectionElementVariable(elementVariable);
    }

    // dataInputItem
    Element inputDataItem = miLoopCharacteristics.element("inputDataItem");
    if (inputDataItem != null) {
      String inputDataItemName = inputDataItem.attribute("name");
      behavior.setCollectionElementVariable(inputDataItemName);
    }

    // Validation
    if (behavior.getLoopCardinalityExpression() == null && behavior.getCollectionExpression() == null && behavior.getCollectionVariable() == null) {
      addError("Either loopCardinality or loopDataInputRef/activiti:collection must be set", miLoopCharacteristics, id);
    }

    // Validation
    if (behavior.getCollectionExpression() == null && behavior.getCollectionVariable() == null && behavior.getCollectionElementVariable() != null) {
      addError("LoopDataInputRef/activiti:collection must be set when using inputDataItem or activiti:elementVariable", miLoopCharacteristics, id);
    }

    for (BpmnParseListener parseListener : parseListeners) {
      parseListener.parseMultiInstanceLoopCharacteristics(activityElement, miLoopCharacteristics, miBodyScope);
    }

    return miBodyScope;

  }

  public static String getIdForMiBody(String id) {
    return id + MULTI_INSTANCE_BODY_ID_SUFFIX;
  }

  /**
   * Parses the generic information of an activity element (id, name,
   * documentation, etc.), and creates a new {@link ActivityImpl} on the given
   * scope element.
   */
  public ActivityImpl createActivityOnScope(Element activityElement, ScopeImpl scopeElement) {
    String id = activityElement.attribute("id");

    LOG.parsingElement("activity", id);
    ActivityImpl activity = scopeElement.createActivity(id);

    activity.setProperty("name", activityElement.attribute("name"));
    activity.setProperty(PROPERTYNAME_DOCUMENTATION, parseDocumentation(activityElement));
    activity.setProperty(PROPERTYNAME_DEFAULT, activityElement.attribute(DEFAULT_ATTRIBUTE));
    activity.getProperties().set(BpmnProperties.TYPE, activityElement.getTagName());
    activity.setProperty("line", activityElement.getLine());
    setActivityAsyncDelegates(activity);
    activity.setProperty(PROPERTYNAME_JOB_PRIORITY, parsePriority(activityElement, PROPERTYNAME_JOB_PRIORITY));

    if (isCompensationHandler(activityElement)) {
      activity.setProperty(PROPERTYNAME_IS_FOR_COMPENSATION, true);
    }

    return activity;
  }

  /**
   * Sets the delegates for the activity, which will be called
   * if the attribute asyncAfter or asyncBefore was changed.
   *
   * @param activity the activity which gets the delegates
   */
  protected void setActivityAsyncDelegates(final ActivityImpl activity) {
    activity.setDelegateAsyncAfterUpdate((asyncAfter, exclusive) -> {
      if (asyncAfter) {
        addMessageJobDeclaration(new AsyncAfterMessageJobDeclaration(), activity, exclusive);
      } else {
        removeMessageJobDeclarationWithJobConfiguration(activity, MessageJobDeclaration.ASYNC_AFTER);
      }
    });

    activity.setDelegateAsyncBeforeUpdate((asyncBefore, exclusive) -> {
      if (asyncBefore) {
        addMessageJobDeclaration(new AsyncBeforeMessageJobDeclaration(), activity, exclusive);
      } else {
        removeMessageJobDeclarationWithJobConfiguration(activity, MessageJobDeclaration.ASYNC_BEFORE);
      }
    });
  }

  /**
   * Adds the new message job declaration to existing declarations.
   * There will be executed an existing check before the adding is executed.
   *
   * @param messageJobDeclaration the new message job declaration
   * @param activity the corresponding activity
   * @param exclusive the flag which indicates if the async should be exclusive
   */
  protected void addMessageJobDeclaration(MessageJobDeclaration messageJobDeclaration, ActivityImpl activity, boolean exclusive) {
    ProcessDefinition procDef = (ProcessDefinition) activity.getProcessDefinition();
    if (!exists(messageJobDeclaration, procDef.getKey(), activity.getActivityId())) {
      messageJobDeclaration.setExclusive(exclusive);
      messageJobDeclaration.setActivity(activity);
      messageJobDeclaration.setJobPriorityProvider((ParameterValueProvider) activity.getProperty(PROPERTYNAME_JOB_PRIORITY));

      addMessageJobDeclarationToActivity(messageJobDeclaration, activity);
      addJobDeclarationToProcessDefinition(messageJobDeclaration, procDef);
    }
  }

  /**
   * Checks whether the message declaration already exists.
   *
   * @param msgJobdecl the message job declaration which is searched
   * @param procDefKey the corresponding process definition key
   * @param activityId the corresponding activity id
   * @return true if the message job declaration exists, false otherwise
   */
  protected boolean exists(MessageJobDeclaration msgJobdecl, String procDefKey, String activityId) {
    boolean exist = false;
    List<JobDeclaration<?, ?>> declarations = jobDeclarations.get(procDefKey);
    if (declarations != null) {
      for (int i = 0; i < declarations.size() && !exist; i++) {
        JobDeclaration<?, ?> decl = declarations.get(i);
        if (decl.getActivityId().equals(activityId) &&
            decl.getJobConfiguration().equalsIgnoreCase(msgJobdecl.getJobConfiguration())) {
          exist = true;
        }
      }
    }
    return exist;
  }

  /**
   * Removes a job declaration which belongs to the given activity and has the given job configuration.
   *
   * @param activity the activity of the job declaration
   * @param jobConfiguration  the job configuration of the declaration
   */
  protected void removeMessageJobDeclarationWithJobConfiguration(ActivityImpl activity, String jobConfiguration) {
    List<MessageJobDeclaration> messageJobDeclarations = (List<MessageJobDeclaration>) activity.getProperty(PROPERTYNAME_MESSAGE_JOB_DECLARATION);
    if (messageJobDeclarations != null) {
      Iterator<MessageJobDeclaration> iter = messageJobDeclarations.iterator();
      while (iter.hasNext()) {
        MessageJobDeclaration msgDecl = iter.next();
        if (msgDecl.getJobConfiguration().equalsIgnoreCase(jobConfiguration)
          && msgDecl.getActivityId().equalsIgnoreCase(activity.getActivityId())) {
          iter.remove();
        }
      }
    }

    ProcessDefinition procDef = (ProcessDefinition) activity.getProcessDefinition();
    List<JobDeclaration<?, ?>> declarations = jobDeclarations.get(procDef.getKey());
    if (declarations != null) {
      Iterator<JobDeclaration<?, ?>> iter = declarations.iterator();
      while (iter.hasNext()) {
        JobDeclaration<?, ?> jobDcl = iter.next();
        if (jobDcl.getJobConfiguration().equalsIgnoreCase(jobConfiguration)
            && jobDcl.getActivityId().equalsIgnoreCase(activity.getActivityId())) {
          iter.remove();
        }
      }
    }
  }

  public String parseDocumentation(Element element) {
    List<Element> docElements = element.elements(PROPERTYNAME_DOCUMENTATION);
    List<String> docStrings = new ArrayList<>();
    for (Element e : docElements) {
      docStrings.add(e.getText());
    }

    return parseDocumentation(docStrings);
  }

  public static String parseDocumentation(List<String> docStrings) {
    if (docStrings.isEmpty()) {
      return null;
    }

    StringBuilder builder = new StringBuilder();
    for (String e : docStrings) {
      if (!builder.isEmpty()) {
        builder.append("\n\n");
      }

      builder.append(e.trim());
    }

    return builder.toString();
  }

  protected boolean isCompensationHandler(Element activityElement) {
    String isForCompensation = activityElement.attribute(PROPERTYNAME_IS_FOR_COMPENSATION);
    return TRUE.equalsIgnoreCase(isForCompensation);
  }

  /**
   * Parses an exclusive gateway declaration.
   */
  public ActivityImpl parseExclusiveGateway(Element exclusiveGwElement, ScopeImpl scope) {
    ActivityImpl activity = createActivityOnScope(exclusiveGwElement, scope);
    activity.setActivityBehavior(new ExclusiveGatewayActivityBehavior());

    parseAsynchronousContinuationForActivity(exclusiveGwElement, activity);

    parseExecutionListenersOnScope(exclusiveGwElement, activity);

    for (BpmnParseListener parseListener : parseListeners) {
      parseListener.parseExclusiveGateway(exclusiveGwElement, scope, activity);
    }
    return activity;
  }

  /**
   * Parses an inclusive gateway declaration.
   */
  public ActivityImpl parseInclusiveGateway(Element inclusiveGwElement, ScopeImpl scope) {
    ActivityImpl activity = createActivityOnScope(inclusiveGwElement, scope);
    activity.setActivityBehavior(new InclusiveGatewayActivityBehavior());

    parseAsynchronousContinuationForActivity(inclusiveGwElement, activity);

    parseExecutionListenersOnScope(inclusiveGwElement, activity);

    for (BpmnParseListener parseListener : parseListeners) {
      parseListener.parseInclusiveGateway(inclusiveGwElement, scope, activity);
    }
    return activity;
  }

  public ActivityImpl parseEventBasedGateway(Element eventBasedGwElement, Element parentElement, ScopeImpl scope) {
    ActivityImpl activity = createActivityOnScope(eventBasedGwElement, scope);
    activity.setActivityBehavior(new EventBasedGatewayActivityBehavior());
    activity.setScope(true);

    parseAsynchronousContinuationForActivity(eventBasedGwElement, activity);

    if (activity.isAsyncAfter()) {
      addError("'asyncAfter' not supported for %s elements.".formatted(eventBasedGwElement.getTagName()), eventBasedGwElement);
    }

    parseExecutionListenersOnScope(eventBasedGwElement, activity);

    for (BpmnParseListener parseListener : parseListeners) {
      parseListener.parseEventBasedGateway(eventBasedGwElement, scope, activity);
    }

    // find all outgoing sequence flows:
    List<Element> seqFlows = parentElement.elements("sequenceFlow");

    // collect all siblings in a map
    Map<String, Element> siblingsMap = new HashMap<>();
    List<Element> siblings = parentElement.elements();
    for (Element sibling : siblings) {
      siblingsMap.put(sibling.attribute("id"), sibling);
    }

    for (Element sequenceFlow : seqFlows) {

      String sourceRef = sequenceFlow.attribute(SOURCE_REF_ATTRIBUTE);
      String targetRef = sequenceFlow.attribute(TARGET_REF_ATTRIBUTE);

      if (activity.getId().equals(sourceRef)) {
        Element sibling = siblingsMap.get(targetRef);
        if (sibling != null) {
          if (ActivityTypes.INTERMEDIATE_EVENT_CATCH.equals(sibling.getTagName())) {
            ActivityImpl catchEventActivity = parseIntermediateCatchEvent(sibling, scope, activity);

            if (catchEventActivity != null) {
              parseActivityInputOutput(sibling, catchEventActivity);
            }

          } else {
            addError("Event based gateway can only be connected to elements of type intermediateCatchEvent", eventBasedGwElement);
          }
        }
      }
    }

    return activity;
  }

  /**
   * Parses a parallel gateway declaration.
   */
  public ActivityImpl parseParallelGateway(Element parallelGwElement, ScopeImpl scope) {
    ActivityImpl activity = createActivityOnScope(parallelGwElement, scope);
    activity.setActivityBehavior(new ParallelGatewayActivityBehavior());

    parseAsynchronousContinuationForActivity(parallelGwElement, activity);

    parseExecutionListenersOnScope(parallelGwElement, activity);

    for (BpmnParseListener parseListener : parseListeners) {
      parseListener.parseParallelGateway(parallelGwElement, scope, activity);
    }
    return activity;
  }

  /**
   * Parses a scriptTask declaration.
   */
  public ActivityImpl parseScriptTask(Element scriptTaskElement, ScopeImpl scope) {
    ActivityImpl activity = createActivityOnScope(scriptTaskElement, scope);

    ScriptTaskActivityBehavior activityBehavior = parseScriptTaskElement(scriptTaskElement);

    if (activityBehavior != null) {
      parseAsynchronousContinuationForActivity(scriptTaskElement, activity);

      activity.setActivityBehavior(activityBehavior);

      parseExecutionListenersOnScope(scriptTaskElement, activity);

      for (BpmnParseListener parseListener : parseListeners) {
        parseListener.parseScriptTask(scriptTaskElement, scope, activity);
      }
    }

    return activity;
  }

  /**
   * Returns a {@link ScriptTaskActivityBehavior} for the script task element
   * corresponding to the script source or resource specified.
   *
   * @param scriptTaskElement
   *          the script task element
   * @return the corresponding {@link ScriptTaskActivityBehavior}
   */
  protected ScriptTaskActivityBehavior parseScriptTaskElement(Element scriptTaskElement) {
    // determine script language
    String language = scriptTaskElement.attribute("scriptFormat");
    if (language == null) {
      language = ScriptingEngines.DEFAULT_SCRIPTING_LANGUAGE;
    }
    String resultVariableName = parseResultVariable(scriptTaskElement);

    // determine script source
    String scriptSource = null;
    Element scriptElement = scriptTaskElement.element(SCRIPT_TAG);
    if (scriptElement != null) {
      scriptSource = scriptElement.getText();
    }
    String scriptResource = scriptTaskElement.attributeNS(OPERATON_BPMN_EXTENSIONS_NS, PROPERTYNAME_RESOURCE);

    try {
      ExecutableScript script = ScriptUtil.getScript(language, scriptSource, scriptResource, expressionManager);
      return new ScriptTaskActivityBehavior(script, resultVariableName);
    } catch (ProcessEngineException e) {
      addError("Unable to process ScriptTask: " + e.getMessage(), scriptElement);
      return null;
    }
  }

  protected String parseResultVariable(Element element) {
    // determine if result variable exists
    String resultVariableName = element.attributeNS(OPERATON_BPMN_EXTENSIONS_NS, "resultVariable");
    if (resultVariableName == null) {
      // for backwards compatible reasons
      resultVariableName = element.attributeNS(OPERATON_BPMN_EXTENSIONS_NS, "resultVariableName");
    }
    return resultVariableName;
  }

  /**
   * Parses a serviceTask declaration.
   */
  public ActivityImpl parseServiceTask(Element serviceTaskElement, ScopeImpl scope) {
    ActivityImpl activity = createActivityOnScope(serviceTaskElement, scope);

    parseAsynchronousContinuationForActivity(serviceTaskElement, activity);

    String elementName = "serviceTask";
    parseServiceTaskLike(activity, elementName, serviceTaskElement, serviceTaskElement);

    parseExecutionListenersOnScope(serviceTaskElement, activity);

    for (BpmnParseListener parseListener : parseListeners) {
      parseListener.parseServiceTask(serviceTaskElement, scope, activity);
    }

    // activity behavior could be set by a listener (e.g. connector); thus,
    // check is after listener invocation
    validateServiceTaskLike(activity, elementName, serviceTaskElement);

    return activity;
  }

  /**
   * @param elementName
   * @param serviceTaskElement the element that contains the operaton service task definition
   *   (e.g. operaton:class attributes)
   * @param operatonPropertiesElement the element that contains the operaton:properties extension elements
   *   that apply to this service task. Usually, but not always, this is the same as serviceTaskElement
   * @return
   */
  public void parseServiceTaskLike(
      ActivityImpl activity,
      String elementName,
      Element serviceTaskElement,
      Element operatonPropertiesElement) {

    String type = serviceTaskElement.attributeNS(OPERATON_BPMN_EXTENSIONS_NS, TYPE);
    String className = serviceTaskElement.attributeNS(OPERATON_BPMN_EXTENSIONS_NS, PROPERTYNAME_CLASS);
    String expression = serviceTaskElement.attributeNS(OPERATON_BPMN_EXTENSIONS_NS, PROPERTYNAME_EXPRESSION);
    String delegateExpression = serviceTaskElement.attributeNS(OPERATON_BPMN_EXTENSIONS_NS, PROPERTYNAME_DELEGATE_EXPRESSION);
    String resultVariableName = parseResultVariable(serviceTaskElement);

    if (type != null) {
      if ("mail".equalsIgnoreCase(type)) {
        parseEmailServiceTask(activity, serviceTaskElement, parseFieldDeclarations(serviceTaskElement));
      } else if ("shell".equalsIgnoreCase(type)) {
        parseShellServiceTask(activity, serviceTaskElement, parseFieldDeclarations(serviceTaskElement));
      } else if ("external".equalsIgnoreCase(type)) {
        parseExternalServiceTask(activity, serviceTaskElement, operatonPropertiesElement);
      } else {
        addError("Invalid usage of type attribute on %s: '%s'".formatted(elementName, type), serviceTaskElement);
      }
    } else if (className != null && !className.trim().isEmpty()) {
      if (resultVariableName != null) {
        addError("'resultVariableName' not supported for %s elements using 'class'".formatted(elementName), serviceTaskElement);
      }
      activity.setActivityBehavior(new ClassDelegateActivityBehavior(className, parseFieldDeclarations(serviceTaskElement)));

    } else if (delegateExpression != null) {
      if (resultVariableName != null) {
        addError("'resultVariableName' not supported for %s elements using 'delegateExpression'".formatted(elementName), serviceTaskElement);
      }
      activity.setActivityBehavior(new ServiceTaskDelegateExpressionActivityBehavior(expressionManager.createExpression(delegateExpression),
          parseFieldDeclarations(serviceTaskElement)));

    } else if (expression != null && !expression.trim().isEmpty()) {
      activity.setActivityBehavior(new ServiceTaskExpressionActivityBehavior(expressionManager.createExpression(expression), resultVariableName));

    }
  }

  protected void validateServiceTaskLike(
      ActivityImpl activity,
      String elementName,
      Element serviceTaskElement
      ) {
    if (activity.getActivityBehavior() == null) {
      addError("One of the attributes 'class', 'delegateExpression', 'type', "
          + "or 'expression' is mandatory on %s. If you are using a connector, make sure the connect process engine plugin is registered with the process engine.".formatted(elementName), serviceTaskElement);
    }
  }

  /**
   * Parses a businessRuleTask declaration.
   */
  public ActivityImpl parseBusinessRuleTask(Element businessRuleTaskElement, ScopeImpl scope) {
    String decisionRef = businessRuleTaskElement.attributeNS(OPERATON_BPMN_EXTENSIONS_NS, "decisionRef");
    if (decisionRef != null) {
      return parseDmnBusinessRuleTask(businessRuleTaskElement, scope);
    }
    else {
      ActivityImpl activity = createActivityOnScope(businessRuleTaskElement, scope);
      parseAsynchronousContinuationForActivity(businessRuleTaskElement, activity);

      String elementName = "businessRuleTask";
      parseServiceTaskLike(
          activity,
          elementName,
          businessRuleTaskElement,
          businessRuleTaskElement);

      parseExecutionListenersOnScope(businessRuleTaskElement, activity);

      for (BpmnParseListener parseListener : parseListeners) {
        parseListener.parseBusinessRuleTask(businessRuleTaskElement, scope, activity);
      }

      // activity behavior could be set by a listener (e.g. connector); thus,
      // check is after listener invocation
      validateServiceTaskLike(activity,
          elementName,
          businessRuleTaskElement);

      return activity;
    }
  }

  /**
   * Parse a Business Rule Task which references a decision.
   */
  protected ActivityImpl parseDmnBusinessRuleTask(Element businessRuleTaskElement, ScopeImpl scope) {
    ActivityImpl activity = createActivityOnScope(businessRuleTaskElement, scope);
    // the activity is a scope since the result variable is stored as local variable
    activity.setScope(true);

    parseAsynchronousContinuationForActivity(businessRuleTaskElement, activity);

    String decisionRef = businessRuleTaskElement.attributeNS(OPERATON_BPMN_EXTENSIONS_NS, "decisionRef");

    BaseCallableElement callableElement = new BaseCallableElement();
    callableElement.setDeploymentId(deployment.getId());

    ParameterValueProvider definitionKeyProvider = createParameterValueProvider(decisionRef, expressionManager);
    callableElement.setDefinitionKeyValueProvider(definitionKeyProvider);

    parseBinding(businessRuleTaskElement, activity, callableElement, ATTR_DECISION_REF_BINDING);
    parseVersion(businessRuleTaskElement, activity, callableElement, ATTR_DECISION_REF_BINDING, "decisionRefVersion");
    parseVersionTag(businessRuleTaskElement, activity, callableElement, ATTR_DECISION_REF_BINDING, "decisionRefVersionTag");
    parseTenantId(businessRuleTaskElement, activity, callableElement, ATTR_DECISION_REF_TENANT_ID);

    String resultVariable = parseResultVariable(businessRuleTaskElement);
    DecisionResultMapper decisionResultMapper = parseDecisionResultMapper(businessRuleTaskElement);

    DmnBusinessRuleTaskActivityBehavior behavior = new DmnBusinessRuleTaskActivityBehavior(callableElement, resultVariable, decisionResultMapper);
    activity.setActivityBehavior(behavior);

    parseExecutionListenersOnScope(businessRuleTaskElement, activity);

    for (BpmnParseListener parseListener : parseListeners) {
      parseListener.parseBusinessRuleTask(businessRuleTaskElement, scope, activity);
    }

    return activity;
  }

  protected DecisionResultMapper parseDecisionResultMapper(Element businessRuleTaskElement) {
    // default mapper is 'resultList'
    String decisionResultMapper = businessRuleTaskElement.attributeNS(OPERATON_BPMN_EXTENSIONS_NS, "mapDecisionResult");
    DecisionResultMapper mapper = DecisionEvaluationUtil.getDecisionResultMapperForName(decisionResultMapper);

    if (mapper == null) {
      addError("No decision result mapper found for name '%s'. Supported mappers are 'singleEntry', 'singleResult', 'collectEntries' and 'resultList'.".formatted(decisionResultMapper), businessRuleTaskElement);
    }

    return mapper;
  }

  /**
   * Parse async continuation of an activity and create async jobs for the activity.
   * <br/> <br/>
   * When the activity is marked as multi instance, then async jobs create instead for the multi instance body.
   * When the wrapped activity has async characteristics in 'multiInstanceLoopCharacteristics' element,
   * then async jobs create additionally for the wrapped activity.
   */
  protected void parseAsynchronousContinuationForActivity(Element activityElement, ActivityImpl activity) {
    // can't use #getMultiInstanceScope here to determine whether the task is multi-instance,
    // since the property hasn't been set yet (cf parseActivity)
    ActivityImpl parentFlowScopeActivity = activity.getParentFlowScopeActivity();
    if (parentFlowScopeActivity != null && parentFlowScopeActivity.getActivityBehavior() instanceof MultiInstanceActivityBehavior
        && !activity.isCompensationHandler()) {

      parseAsynchronousContinuation(activityElement, parentFlowScopeActivity);

      Element miLoopCharacteristics = activityElement.element("multiInstanceLoopCharacteristics");
      parseAsynchronousContinuation(miLoopCharacteristics, activity);

    } else {
      parseAsynchronousContinuation(activityElement, activity);
    }
  }

  /**
   * Parse async continuation of the given element and create async jobs for the activity.
   *
   * @param element with async characteristics
   * @param activity
   */
  protected void parseAsynchronousContinuation(Element element, ActivityImpl activity) {

    boolean isAsyncBefore = isAsyncBefore(element);
    boolean isAsyncAfter = isAsyncAfter(element);
    boolean exclusive = isExclusive(element);

    // set properties on activity
    activity.setAsyncBefore(isAsyncBefore, exclusive);
    activity.setAsyncAfter(isAsyncAfter, exclusive);
  }

  protected ParameterValueProvider parsePriority(Element element, String priorityAttribute) {
    String priorityAttributeValue = element.attributeNS(OPERATON_BPMN_EXTENSIONS_NS, priorityAttribute);

    if (priorityAttributeValue == null) {
      return null;

    } else {
      Object value = priorityAttributeValue;
      if (!StringUtil.isExpression(priorityAttributeValue)) {
        // constant values must be valid integers
        try {
          value = Integer.parseInt(priorityAttributeValue);

        } catch (NumberFormatException e) {
          addError("Value '%s' for attribute '%s' is not a valid number".formatted(priorityAttributeValue, priorityAttribute), element);
        }
      }

      return createParameterValueProvider(value, expressionManager);
    }
  }

  protected ParameterValueProvider parseTopic(Element element, String topicAttribute) {
    String topicAttributeValue = element.attributeNS(OPERATON_BPMN_EXTENSIONS_NS, topicAttribute);

    if (topicAttributeValue == null) {
      addError("External tasks must specify a 'topic' attribute in the operaton namespace", element);
      return null;

    } else {
      return createParameterValueProvider(topicAttributeValue, expressionManager);
    }
  }

  @SuppressWarnings("unchecked")
  protected void addMessageJobDeclarationToActivity(MessageJobDeclaration messageJobDeclaration, ActivityImpl activity) {
    List<MessageJobDeclaration> messageJobDeclarations = (List<MessageJobDeclaration>) activity.getProperty(PROPERTYNAME_MESSAGE_JOB_DECLARATION);
    if (messageJobDeclarations == null) {
      messageJobDeclarations = new ArrayList<>();
      activity.setProperty(PROPERTYNAME_MESSAGE_JOB_DECLARATION, messageJobDeclarations);
    }
    messageJobDeclarations.add(messageJobDeclaration);
  }

  protected void addJobDeclarationToProcessDefinition(JobDeclaration<?, ?> jobDeclaration, ProcessDefinition processDefinition) {
    String key = processDefinition.getKey();

    jobDeclarations
      .computeIfAbsent(key, k -> new ArrayList<>())
      .add(jobDeclaration);
  }

  /**
   * Parses a sendTask declaration.
   */
  public ActivityImpl parseSendTask(Element sendTaskElement, ScopeImpl scope) {
    ActivityImpl activity = createActivityOnScope(sendTaskElement, scope);

    if (isServiceTaskLike(sendTaskElement)) {
      // CAM-942: If expression or class is set on a SendTask it behaves like a service task
      // to allow implementing the send handling yourself
      String elementName = "sendTask";
      parseAsynchronousContinuationForActivity(sendTaskElement, activity);

      parseServiceTaskLike(activity, elementName, sendTaskElement, sendTaskElement);

      parseExecutionListenersOnScope(sendTaskElement, activity);

      for (BpmnParseListener parseListener : parseListeners) {
        parseListener.parseSendTask(sendTaskElement, scope, activity);
      }

      // activity behavior could be set by a listener (e.g. connector); thus,
      // check is after listener invocation
      validateServiceTaskLike(activity, elementName, sendTaskElement);

    } else {
      parseAsynchronousContinuationForActivity(sendTaskElement, activity);
      parseExecutionListenersOnScope(sendTaskElement, activity);

      for (BpmnParseListener parseListener : parseListeners) {
        parseListener.parseSendTask(sendTaskElement, scope, activity);
      }

      // activity behavior could be set by a listener; thus, check is after listener invocation
      if (activity.getActivityBehavior() == null) {
        addError("One of the attributes 'class', 'delegateExpression', 'type', or 'expression' is mandatory on sendTask.", sendTaskElement);
      }
    }

    return activity;
  }

  protected void parseEmailServiceTask(ActivityImpl activity, Element serviceTaskElement, List<FieldDeclaration> fieldDeclarations) {
    validateFieldDeclarationsForEmail(serviceTaskElement, fieldDeclarations);
    activity.setActivityBehavior((MailActivityBehavior) instantiateDelegate(MailActivityBehavior.class, fieldDeclarations));
  }

  protected void parseShellServiceTask(ActivityImpl activity, Element serviceTaskElement, List<FieldDeclaration> fieldDeclarations) {
    validateFieldDeclarationsForShell(serviceTaskElement, fieldDeclarations);
    activity.setActivityBehavior((ActivityBehavior) instantiateDelegate(ShellActivityBehavior.class, fieldDeclarations));
  }

  protected void parseExternalServiceTask(ActivityImpl activity,
      Element serviceTaskElement,
      Element operatonPropertiesElement) {
    activity.setScope(true);

    ParameterValueProvider topicNameProvider = parseTopic(serviceTaskElement, PROPERTYNAME_EXTERNAL_TASK_TOPIC);
    ParameterValueProvider priorityProvider = parsePriority(serviceTaskElement, PROPERTYNAME_TASK_PRIORITY);
    Map<String, String> properties = parseOperatonExtensionProperties(operatonPropertiesElement);
    activity.getProperties().set(BpmnProperties.EXTENSION_PROPERTIES, properties);
    List<OperatonErrorEventDefinition> operatonErrorEventDefinitions = parseOperatonErrorEventDefinitions(activity, serviceTaskElement);
    activity.getProperties().set(BpmnProperties.OPERATON_ERROR_EVENT_DEFINITION, operatonErrorEventDefinitions);
    activity.setActivityBehavior(new ExternalTaskActivityBehavior(topicNameProvider, priorityProvider));
  }

  protected void validateFieldDeclarationsForEmail(Element serviceTaskElement, List<FieldDeclaration> fieldDeclarations) {
    boolean toDefined = false;
    boolean textOrHtmlDefined = false;
    for (FieldDeclaration fieldDeclaration : fieldDeclarations) {
      if ("to".equals(fieldDeclaration.getName())) {
        toDefined = true;
      }
      if ("html".equals(fieldDeclaration.getName())) {
        textOrHtmlDefined = true;
      }
      if ("text".equals(fieldDeclaration.getName())) {
        textOrHtmlDefined = true;
      }
    }

    if (!toDefined) {
      addError("No recipient is defined on the mail activity", serviceTaskElement);
    }
    if (!textOrHtmlDefined) {
      addError("Text or html field should be provided", serviceTaskElement);
    }
  }

  protected void validateFieldDeclarationsForShell(Element serviceTaskElement, List<FieldDeclaration> fieldDeclarations) {
    boolean shellCommandDefined = false;

    for (FieldDeclaration fieldDeclaration : fieldDeclarations) {
      String fieldName = fieldDeclaration.getName();
      FixedValue fieldFixedValue = (FixedValue) fieldDeclaration.getValue();
      String fieldValue = fieldFixedValue.getExpressionText();

      shellCommandDefined |= "command".equals(fieldName);

      if (("wait".equals(fieldName) || "redirectError".equals(fieldName) || "cleanEnv".equals(fieldName)) && !TRUE.equalsIgnoreCase(fieldValue)
          && !FALSE.equalsIgnoreCase(fieldValue)) {
        addError("undefined value for shell %s parameter :%s".formatted(fieldName, fieldValue), serviceTaskElement);
      }

    }

    if (!shellCommandDefined) {
      addError("No shell command is defined on the shell activity", serviceTaskElement);
    }
  }

  public List<FieldDeclaration> parseFieldDeclarations(Element element) {
    List<FieldDeclaration> fieldDeclarations = new ArrayList<>();

    Element elementWithFieldInjections = element.element(EXTENSION_ELEMENTS_TAG);
    if (elementWithFieldInjections == null) { // Custom extensions will just
                                              // have the <field.. as a
                                              // subelement
      elementWithFieldInjections = element;
    }
    List<Element> fieldDeclarationElements = elementWithFieldInjections.elementsNS(OPERATON_BPMN_EXTENSIONS_NS, "field");
    if (fieldDeclarationElements != null && !fieldDeclarationElements.isEmpty()) {

      for (Element fieldDeclarationElement : fieldDeclarationElements) {
        FieldDeclaration fieldDeclaration = parseFieldDeclaration(element, fieldDeclarationElement);
        if (fieldDeclaration != null) {
          fieldDeclarations.add(fieldDeclaration);
        }
      }
    }

    return fieldDeclarations;
  }

  protected FieldDeclaration parseFieldDeclaration(Element serviceTaskElement, Element fieldDeclarationElement) {
    String fieldName = fieldDeclarationElement.attribute("name");

    FieldDeclaration fieldDeclaration = parseStringFieldDeclaration(fieldDeclarationElement, serviceTaskElement, fieldName);
    if (fieldDeclaration == null) {
      fieldDeclaration = parseExpressionFieldDeclaration(fieldDeclarationElement, serviceTaskElement, fieldName);
    }

    if (fieldDeclaration == null) {
      addError("One of the following is mandatory on a field declaration: one of attributes stringValue|expression or one of child elements string|expression",
          serviceTaskElement);
    }
    return fieldDeclaration;
  }

  protected FieldDeclaration parseStringFieldDeclaration(Element fieldDeclarationElement, Element serviceTaskElement, String fieldName) {
    try {
      String fieldValue = getStringValueFromAttributeOrElement("stringValue", "string", fieldDeclarationElement, serviceTaskElement.attribute("id"));
      if (fieldValue != null) {
        return new FieldDeclaration(fieldName, Expression.class.getName(), new FixedValue(fieldValue));
      }
    } catch (ProcessEngineException ae) {
      if (ae.getMessage().contains("multiple elements with tag name")) {
        addError("Multiple string field declarations found", serviceTaskElement);
      } else {
        addError("Error when paring field declarations: " + ae.getMessage(), serviceTaskElement);
      }
    }
    return null;
  }

  protected FieldDeclaration parseExpressionFieldDeclaration(Element fieldDeclarationElement, Element serviceTaskElement, String fieldName) {
    try {
      String expression = getStringValueFromAttributeOrElement(PROPERTYNAME_EXPRESSION, PROPERTYNAME_EXPRESSION, fieldDeclarationElement, serviceTaskElement.attribute("id"));
      if (expression != null && !expression.trim().isEmpty()) {
        return new FieldDeclaration(fieldName, Expression.class.getName(), expressionManager.createExpression(expression));
      }
    } catch (ProcessEngineException ae) {
      if (ae.getMessage().contains("multiple elements with tag name")) {
        addError("Multiple expression field declarations found", serviceTaskElement);
      } else {
        addError("Error when paring field declarations: " + ae.getMessage(), serviceTaskElement);
      }
    }
    return null;
  }

  protected String getStringValueFromAttributeOrElement(String attributeName, String elementName, Element element, String ancestorElementId) {
    String value = null;

    String attributeValue = element.attribute(attributeName);
    Element childElement = element.elementNS(OPERATON_BPMN_EXTENSIONS_NS, elementName);
    String stringElementText = null;

    if (attributeValue != null && childElement != null) {
      addError("Can't use attribute '%s' and element '%s' together, only use one".formatted(attributeName, elementName), element, ancestorElementId);
    } else if (childElement != null) {
      stringElementText = childElement.getText();
      if (stringElementText == null || stringElementText.isEmpty()) {
        addError("No valid value found in attribute '%s' nor element '%s'".formatted(attributeName, elementName), element, ancestorElementId);
      } else {
        // Use text of element
        value = stringElementText;
      }
    } else if (attributeValue != null && !attributeValue.isEmpty()) {
      // Using attribute
      value = attributeValue;
    }

    return value;
  }

  /**
   * Parses a task with no specific type (behaves as passthrough).
   */
  public ActivityImpl parseTask(Element taskElement, ScopeImpl scope) {
    ActivityImpl activity = createActivityOnScope(taskElement, scope);
    activity.setActivityBehavior(new TaskActivityBehavior());

    parseAsynchronousContinuationForActivity(taskElement, activity);

    parseExecutionListenersOnScope(taskElement, activity);

    for (BpmnParseListener parseListener : parseListeners) {
      parseListener.parseTask(taskElement, scope, activity);
    }
    return activity;
  }

  /**
   * Parses a manual task.
   */
  public ActivityImpl parseManualTask(Element manualTaskElement, ScopeImpl scope) {
    ActivityImpl activity = createActivityOnScope(manualTaskElement, scope);
    activity.setActivityBehavior(new ManualTaskActivityBehavior());

    parseAsynchronousContinuationForActivity(manualTaskElement, activity);

    parseExecutionListenersOnScope(manualTaskElement, activity);

    for (BpmnParseListener parseListener : parseListeners) {
      parseListener.parseManualTask(manualTaskElement, scope, activity);
    }
    return activity;
  }

  /**
   * Parses a receive task.
   */
  public ActivityImpl parseReceiveTask(Element receiveTaskElement, ScopeImpl scope) {
    ActivityImpl activity = createActivityOnScope(receiveTaskElement, scope);
    activity.setActivityBehavior(new ReceiveTaskActivityBehavior());

    parseAsynchronousContinuationForActivity(receiveTaskElement, activity);

    parseExecutionListenersOnScope(receiveTaskElement, activity);

    // please check https://app.camunda.com/jira/browse/CAM-10989
    if (receiveTaskElement.attribute("messageRef") != null) {
      activity.setScope(true);
      activity.setEventScope(activity);
      EventSubscriptionDeclaration declaration = parseMessageEventDefinition(receiveTaskElement, activity.getId());
      declaration.setActivityId(activity.getActivityId());
      declaration.setEventScopeActivityId(activity.getActivityId());
      addEventSubscriptionDeclaration(declaration, activity, receiveTaskElement);
    }

    for (BpmnParseListener parseListener : parseListeners) {
      parseListener.parseReceiveTask(receiveTaskElement, scope, activity);
    }
    return activity;
  }

  /* userTask specific finals */

  protected static final String HUMAN_PERFORMER = "humanPerformer";
  protected static final String POTENTIAL_OWNER = "potentialOwner";

  protected static final String RESOURCE_ASSIGNMENT_EXPR = "resourceAssignmentExpression";
  protected static final String FORMAL_EXPRESSION = "formalExpression";

  protected static final String USER_PREFIX = "user(";
  protected static final String GROUP_PREFIX = "group(";

  protected static final String ASSIGNEE_EXTENSION = "assignee";
  protected static final String CANDIDATE_USERS_EXTENSION = "candidateUsers";
  protected static final String CANDIDATE_GROUPS_EXTENSION = "candidateGroups";
  protected static final String DUE_DATE_EXTENSION = "dueDate";
  protected static final String FOLLOW_UP_DATE_EXTENSION = "followUpDate";
  protected static final String PRIORITY_EXTENSION = "priority";

  /**
   * Parses a userTask declaration.
   */
  public ActivityImpl parseUserTask(Element userTaskElement, ScopeImpl scope) {
    ActivityImpl activity = createActivityOnScope(userTaskElement, scope);

    parseAsynchronousContinuationForActivity(userTaskElement, activity);

    TaskDefinition taskDefinition = parseTaskDefinition(userTaskElement, activity.getId(), activity, (ProcessDefinitionEntity) scope.getProcessDefinition());
    TaskDecorator taskDecorator = new TaskDecorator(taskDefinition, expressionManager);

    UserTaskActivityBehavior userTaskActivity = new UserTaskActivityBehavior(taskDecorator);
    activity.setActivityBehavior(userTaskActivity);

    parseProperties(userTaskElement, activity);
    parseExecutionListenersOnScope(userTaskElement, activity);

    for (BpmnParseListener parseListener : parseListeners) {
      parseListener.parseUserTask(userTaskElement, scope, activity);
    }
    return activity;
  }

  public TaskDefinition parseTaskDefinition(Element taskElement, String taskDefinitionKey, ActivityImpl activity, ProcessDefinitionEntity processDefinition) {
    TaskFormHandler taskFormHandler;
    String taskFormHandlerClassName = taskElement.attributeNS(OPERATON_BPMN_EXTENSIONS_NS, "formHandlerClass");
    if (taskFormHandlerClassName != null) {
      taskFormHandler = (TaskFormHandler) ReflectUtil.instantiate(taskFormHandlerClassName);
    } else {
      taskFormHandler = new DefaultTaskFormHandler();
    }
    taskFormHandler.parseConfiguration(taskElement, deployment, processDefinition, this);

    TaskDefinition taskDefinition = new TaskDefinition(new DelegateTaskFormHandler(taskFormHandler, deployment));

    taskDefinition.setKey(taskDefinitionKey);
    processDefinition.getTaskDefinitions().put(taskDefinitionKey, taskDefinition);

    FormDefinition formDefinition = parseFormDefinition(taskElement);
    taskDefinition.setFormDefinition(formDefinition);

    String name = taskElement.attribute("name");
    if (name != null) {
      taskDefinition.setNameExpression(expressionManager.createExpression(name));
    }

    String descriptionStr = parseDocumentation(taskElement);
    if (descriptionStr != null) {
      taskDefinition.setDescriptionExpression(expressionManager.createExpression(descriptionStr));
    }

    parseHumanPerformer(taskElement, taskDefinition);
    parsePotentialOwner(taskElement, taskDefinition);

    // Activiti custom extension
    parseUserTaskCustomExtensions(taskElement, activity, taskDefinition);

    return taskDefinition;
  }

  protected FormDefinition parseFormDefinition(Element flowNodeElement) {
    FormDefinition formDefinition = new FormDefinition();

    String formKeyAttribute = flowNodeElement.attributeNS(OPERATON_BPMN_EXTENSIONS_NS, "formKey");
    String formRefAttribute = flowNodeElement.attributeNS(BpmnParse.OPERATON_BPMN_EXTENSIONS_NS, "formRef");

    if(formKeyAttribute != null && formRefAttribute != null) {
      addError("Invalid element definition: only one of the attributes formKey and formRef is allowed.", flowNodeElement);
    }

    if (formKeyAttribute != null) {
      formDefinition.setFormKey(expressionManager.createExpression(formKeyAttribute));
    }

    if(formRefAttribute != null) {
      formDefinition.setOperatonFormDefinitionKey(expressionManager.createExpression(formRefAttribute));

      String formRefBindingAttribute = flowNodeElement.attributeNS(BpmnParse.OPERATON_BPMN_EXTENSIONS_NS, "formRefBinding");

      if (formRefBindingAttribute == null || !ALLOWED_FORM_REF_BINDINGS.contains(formRefBindingAttribute)) {
        addError("Invalid element definition: value for formRefBinding attribute has to be one of %s but was %s".formatted(ALLOWED_FORM_REF_BINDINGS, formRefBindingAttribute), flowNodeElement);
      }


      if(formRefBindingAttribute != null) {
        formDefinition.setOperatonFormDefinitionBinding(formRefBindingAttribute);
      }

      if(FORM_REF_BINDING_VERSION.equals(formRefBindingAttribute)) {
        String formRefVersionAttribute = flowNodeElement.attributeNS(BpmnParse.OPERATON_BPMN_EXTENSIONS_NS, "formRefVersion");

        Expression operatonFormDefinitionVersion = expressionManager.createExpression(formRefVersionAttribute);

        if(formRefVersionAttribute != null) {
          formDefinition.setOperatonFormDefinitionVersion(operatonFormDefinitionVersion);
        }
      }
    }

    return formDefinition;
  }

  protected void parseHumanPerformer(Element taskElement, TaskDefinition taskDefinition) {
    List<Element> humanPerformerElements = taskElement.elements(HUMAN_PERFORMER);

    if (humanPerformerElements.size() > 1) {
      addError("Invalid task definition: multiple %s sub elements defined for %s".formatted(HUMAN_PERFORMER, taskDefinition.getNameExpression()), taskElement);
    } else if (humanPerformerElements.size() == 1) {
      Element humanPerformerElement = humanPerformerElements.get(0);
      if (humanPerformerElement != null) {
        parseHumanPerformerResourceAssignment(humanPerformerElement, taskDefinition);
      }
    }
  }

  protected void parsePotentialOwner(Element taskElement, TaskDefinition taskDefinition) {
    List<Element> potentialOwnerElements = taskElement.elements(POTENTIAL_OWNER);
    for (Element potentialOwnerElement : potentialOwnerElements) {
      parsePotentialOwnerResourceAssignment(potentialOwnerElement, taskDefinition);
    }
  }

  protected void parseHumanPerformerResourceAssignment(Element performerElement, TaskDefinition taskDefinition) {
    Element raeElement = performerElement.element(RESOURCE_ASSIGNMENT_EXPR);
    if (raeElement != null) {
      Element feElement = raeElement.element(FORMAL_EXPRESSION);
      if (feElement != null) {
        taskDefinition.setAssigneeExpression(expressionManager.createExpression(feElement.getText()));
      }
    }
  }

  protected void parsePotentialOwnerResourceAssignment(Element performerElement, TaskDefinition taskDefinition) {
    Element raeElement = performerElement.element(RESOURCE_ASSIGNMENT_EXPR);
    if (raeElement != null) {
      Element feElement = raeElement.element(FORMAL_EXPRESSION);
      if (feElement != null) {
        List<String> assignmentExpressions = parseCommaSeparatedList(feElement.getText());
        for (String assignmentExpression : assignmentExpressions) {
          assignmentExpression = assignmentExpression.trim();
          if (assignmentExpression.startsWith(USER_PREFIX)) {
            String userAssignementId = getAssignmentId(assignmentExpression, USER_PREFIX);
            taskDefinition.addCandidateUserIdExpression(expressionManager.createExpression(userAssignementId));
          } else if (assignmentExpression.startsWith(GROUP_PREFIX)) {
            String groupAssignementId = getAssignmentId(assignmentExpression, GROUP_PREFIX);
            taskDefinition.addCandidateGroupIdExpression(expressionManager.createExpression(groupAssignementId));
          } else { // default: given string is a goupId, as-is.
            taskDefinition.addCandidateGroupIdExpression(expressionManager.createExpression(assignmentExpression));
          }
        }
      }
    }
  }

  protected String getAssignmentId(String expression, String prefix) {
    return expression.substring(prefix.length(), expression.length() - 1).trim();
  }

  protected void parseUserTaskCustomExtensions(Element taskElement, ActivityImpl activity, TaskDefinition taskDefinition) {

    // assignee
    String assignee = taskElement.attributeNS(OPERATON_BPMN_EXTENSIONS_NS, ASSIGNEE_EXTENSION);
    if (assignee != null) {
      if (taskDefinition.getAssigneeExpression() == null) {
        taskDefinition.setAssigneeExpression(expressionManager.createExpression(assignee));
      } else {
        addError("Invalid usage: duplicate assignee declaration for task " + taskDefinition.getNameExpression(), taskElement);
      }
    }

    // Candidate users
    String candidateUsersString = taskElement.attributeNS(OPERATON_BPMN_EXTENSIONS_NS, CANDIDATE_USERS_EXTENSION);
    if (candidateUsersString != null) {
      List<String> candidateUsers = parseCommaSeparatedList(candidateUsersString);
      for (String candidateUser : candidateUsers) {
        taskDefinition.addCandidateUserIdExpression(expressionManager.createExpression(candidateUser.trim()));
      }
    }

    // Candidate groups
    String candidateGroupsString = taskElement.attributeNS(OPERATON_BPMN_EXTENSIONS_NS, CANDIDATE_GROUPS_EXTENSION);
    if (candidateGroupsString != null) {
      List<String> candidateGroups = parseCommaSeparatedList(candidateGroupsString);
      for (String candidateGroup : candidateGroups) {
        taskDefinition.addCandidateGroupIdExpression(expressionManager.createExpression(candidateGroup.trim()));
      }
    }

    // Task listeners
    parseTaskListeners(taskElement, activity, taskDefinition);

    // Due date
    String dueDateExpression = taskElement.attributeNS(OPERATON_BPMN_EXTENSIONS_NS, DUE_DATE_EXTENSION);
    if (dueDateExpression != null) {
      taskDefinition.setDueDateExpression(expressionManager.createExpression(dueDateExpression));
    }

    // follow up date
    String followUpDateExpression = taskElement.attributeNS(OPERATON_BPMN_EXTENSIONS_NS, FOLLOW_UP_DATE_EXTENSION);
    if (followUpDateExpression != null) {
      taskDefinition.setFollowUpDateExpression(expressionManager.createExpression(followUpDateExpression));
    }

    // Priority
    final String priorityExpression = taskElement.attributeNS(OPERATON_BPMN_EXTENSIONS_NS, PRIORITY_EXTENSION);
    if (priorityExpression != null) {
      taskDefinition.setPriorityExpression(expressionManager.createExpression(priorityExpression));
    }
  }

  /**
   * Parses the given String as a list of comma separated entries, where an
   * entry can possibly be an expression that has comma's.
   *
   * <p>
   * If somebody is smart enough to write a regex for this, please let us know.
   * </p>
   *
   * @return the entries of the comma separated list, trimmed.
   */
  protected List<String> parseCommaSeparatedList(String s) {
    List<String> result = new ArrayList<>();
    if (s != null && !"".equals(s)) {

      StringCharacterIterator iterator = new StringCharacterIterator(s);
      char character = iterator.first();

      StringBuilder stringBuilder = new StringBuilder();
      boolean insideExpression = false;

      while (character != CharacterIterator.DONE) {
        if (character == '{' || character == '$') {
          insideExpression = true;
        } else if (character == '}') {
          insideExpression = false;
        } else if (character == ',' && !insideExpression) {
          result.add(stringBuilder.toString().trim());
          stringBuilder.delete(0, stringBuilder.length());
        }

        if (character != ',' || insideExpression) {
          stringBuilder.append(character);
        }

        character = iterator.next();
      }

      if (!stringBuilder.isEmpty()) {
        result.add(stringBuilder.toString().trim());
      }

    }
    return result;
  }

  protected void parseTaskListeners(Element userTaskElement, ActivityImpl activity, TaskDefinition taskDefinition) {
    Element extentionsElement = userTaskElement.element(EXTENSION_ELEMENTS_TAG);
    if (extentionsElement != null) {
      List<Element> taskListenerElements = extentionsElement.elementsNS(OPERATON_BPMN_EXTENSIONS_NS, "taskListener");
      for (Element taskListenerElement : taskListenerElements) {
        String eventName = taskListenerElement.attribute("event");
        if (eventName != null) {
          if (TaskListener.EVENTNAME_CREATE.equals(eventName) || TaskListener.EVENTNAME_ASSIGNMENT.equals(eventName)
              || TaskListener.EVENTNAME_COMPLETE.equals(eventName) || TaskListener.EVENTNAME_UPDATE.equals(eventName)
              || TaskListener.EVENTNAME_DELETE.equals(eventName)) {
            TaskListener taskListener = parseTaskListener(taskListenerElement, activity.getId());
            taskDefinition.addTaskListener(eventName, taskListener);
          } else if (TaskListener.EVENTNAME_TIMEOUT.equals(eventName)) {
            TaskListener taskListener = parseTimeoutTaskListener(taskListenerElement, activity, taskDefinition);
            taskDefinition.addTimeoutTaskListener(taskListenerElement.attribute("id"), taskListener);
          } else {
            addError("Attribute 'event' must be one of {create|assignment|complete|update|delete|timeout}", userTaskElement);
          }
        } else {
          addError("Attribute 'event' is mandatory on taskListener", userTaskElement);
        }
      }
    }
  }

  protected TaskListener parseTaskListener(Element taskListenerElement, String taskElementId) {
    TaskListener taskListener = null;

    String className = taskListenerElement.attribute(PROPERTYNAME_CLASS);
    String expression = taskListenerElement.attribute(PROPERTYNAME_EXPRESSION);
    String delegateExpression = taskListenerElement.attribute(PROPERTYNAME_DELEGATE_EXPRESSION);
    Element scriptElement = taskListenerElement.elementNS(OPERATON_BPMN_EXTENSIONS_NS, SCRIPT_TAG);

    if (className != null) {
      taskListener = new ClassDelegateTaskListener(className, parseFieldDeclarations(taskListenerElement));
    } else if (expression != null) {
      taskListener = new ExpressionTaskListener(expressionManager.createExpression(expression));
    } else if (delegateExpression != null) {
      taskListener = new DelegateExpressionTaskListener(expressionManager.createExpression(delegateExpression), parseFieldDeclarations(taskListenerElement));
    } else if (scriptElement != null) {
      try {
        ExecutableScript executableScript = parseOperatonScript(scriptElement);
        if (executableScript != null) {
          taskListener = new ScriptTaskListener(executableScript);
        }
      } catch (BpmnParseException e) {
        addError(e, taskElementId);
      }
    } else {
      addError("Element 'class', 'expression', 'delegateExpression' or 'script' is mandatory on taskListener", taskListenerElement, taskElementId);
    }
    return taskListener;
  }

  @SuppressWarnings("unused")
  protected TaskListener parseTimeoutTaskListener(Element taskListenerElement, ActivityImpl timerActivity, TaskDefinition taskDefinition) {
    String listenerId = taskListenerElement.attribute("id");
    String timerActivityId = timerActivity.getId();
    if (listenerId == null) {
      addError("Element 'id' is mandatory on taskListener of type 'timeout'", taskListenerElement, timerActivityId);
    }
    Element timerEventDefinition = taskListenerElement.element(TIMER_EVENT_DEFINITION);
    if (timerEventDefinition == null) {
      addError("Element 'timerEventDefinition' is mandatory on taskListener of type 'timeout'", taskListenerElement, timerActivityId);
    }
    timerActivity.setScope(true);
    timerActivity.setEventScope(timerActivity);
    TimerDeclarationImpl timerDeclaration = parseTimer(timerEventDefinition, timerActivity, TimerTaskListenerJobHandler.TYPE);
    timerDeclaration.setRawJobHandlerConfiguration(timerActivityId + TimerEventJobHandler.JOB_HANDLER_CONFIG_PROPERTY_DELIMITER +
        TimerEventJobHandler.JOB_HANDLER_CONFIG_TASK_LISTENER_PREFIX + listenerId);
    addTimerListenerDeclaration(listenerId, timerActivity, timerDeclaration);

    return parseTaskListener(taskListenerElement, timerActivityId);
  }

  /**
   * Parses the end events of a certain level in the process (process,
   * subprocess or another scope).
   *
   * @param parentElement
   *          The 'parent' element that contains the end events (process,
   *          subprocess).
   * @param scope
   *          The {@link ScopeImpl} to which the end events must be added.
   */
  public void parseEndEvents(Element parentElement, ScopeImpl scope) {
    for (Element endEventElement : parentElement.elements("endEvent")) {
      ActivityImpl activity = createActivityOnScope(endEventElement, scope);

      Element errorEventDefinition = endEventElement.element(ERROR_EVENT_DEFINITION);
      Element cancelEventDefinition = endEventElement.element(CANCEL_EVENT_DEFINITION);
      Element terminateEventDefinition = endEventElement.element(TERMINATE_EVENT_DEFINITION);
      Element messageEventDefinitionElement = endEventElement.element(MESSAGE_EVENT_DEFINITION);
      Element signalEventDefinition = endEventElement.element(SIGNAL_EVENT_DEFINITION);
      Element compensateEventDefinitionElement = endEventElement.element(COMPENSATE_EVENT_DEFINITION);
      Element escalationEventDefinition = endEventElement.element(ESCALATION_EVENT_DEFINITION);

      boolean isServiceTaskLike = isServiceTaskLike(messageEventDefinitionElement);

      String activityId = activity.getId();
      if (errorEventDefinition != null) { // error end event
        String errorRef = errorEventDefinition.attribute(ERROR_REF_ATTRIBUTE);

        if (errorRef == null || "".equals(errorRef)) {
          addError("'errorRef' attribute is mandatory on error end event", errorEventDefinition, activityId);
        } else {
          Error error = bpmnParseErrors.get(errorRef);
          if (error != null && (error.getErrorCode() == null || "".equals(error.getErrorCode()))) {
            addError(
                "'errorCode' is mandatory on errors referenced by throwing error event definitions, but the error '%s' does not define one.".formatted(error.getId()),
                errorEventDefinition,
                activityId);
          }
          activity.getProperties().set(BpmnProperties.TYPE, ActivityTypes.END_EVENT_ERROR);
          if(error != null) {
            activity.setActivityBehavior(new ErrorEndEventActivityBehavior(error.getErrorCode(), error.getErrorMessageExpression()));
          } else {
            activity.setActivityBehavior(new ErrorEndEventActivityBehavior(errorRef, null));
          }
        }
      } else if (cancelEventDefinition != null) {
        if (scope.getProperty(BpmnProperties.TYPE.name()) == null || !TRANSACTION_TAG.equals(scope.getProperty(BpmnProperties.TYPE.name()))) {
          addError("end event with cancelEventDefinition only supported inside transaction subprocess", cancelEventDefinition, activityId);
        } else {
          activity.getProperties().set(BpmnProperties.TYPE, ActivityTypes.END_EVENT_CANCEL);
          activity.setActivityBehavior(new CancelEndEventActivityBehavior());
          activity.setActivityStartBehavior(ActivityStartBehavior.INTERRUPT_FLOW_SCOPE);
          activity.setProperty(PROPERTYNAME_THROWS_COMPENSATION, true);
          activity.setScope(true);
        }
      } else if (terminateEventDefinition != null) {
        activity.getProperties().set(BpmnProperties.TYPE, ActivityTypes.END_EVENT_TERMINATE);
        activity.setActivityBehavior(new TerminateEndEventActivityBehavior());
        activity.setActivityStartBehavior(ActivityStartBehavior.INTERRUPT_FLOW_SCOPE);
      } else if (messageEventDefinitionElement != null) {
        if (isServiceTaskLike) {

          // CAM-436 same behaviour as service task
          parseServiceTaskLike(
              activity,
              ActivityTypes.END_EVENT_MESSAGE,
              messageEventDefinitionElement,
              endEventElement);
          activity.getProperties().set(BpmnProperties.TYPE, ActivityTypes.END_EVENT_MESSAGE);
        } else {
          // default to non behavior if no service task
          // properties have been specified
          activity.setActivityBehavior(new IntermediateThrowNoneEventActivityBehavior());
        }
      } else if (signalEventDefinition != null) {
        activity.getProperties().set(BpmnProperties.TYPE, ActivityTypes.END_EVENT_SIGNAL);
        EventSubscriptionDeclaration signalDefinition = parseSignalEventDefinition(signalEventDefinition, true, activityId);
        activity.setActivityBehavior(new ThrowSignalEventActivityBehavior(signalDefinition));

      } else if (compensateEventDefinitionElement != null) {
        activity.getProperties().set(BpmnProperties.TYPE, ActivityTypes.END_EVENT_COMPENSATION);
        CompensateEventDefinition compensateEventDefinition = parseThrowCompensateEventDefinition(compensateEventDefinitionElement, scope, endEventElement.attribute("id"));
        activity.setActivityBehavior(new CompensationEventActivityBehavior(compensateEventDefinition));
        activity.setProperty(PROPERTYNAME_THROWS_COMPENSATION, true);
        activity.setScope(true);

      } else if(escalationEventDefinition != null) {
        activity.getProperties().set(BpmnProperties.TYPE, ActivityTypes.END_EVENT_ESCALATION);

        Escalation escalation = findEscalationForEscalationEventDefinition(escalationEventDefinition, activityId);
        if (escalation != null && escalation.getEscalationCode() == null) {
          addError("escalation end event must have an 'escalationCode'", escalationEventDefinition, activityId);
        }
        activity.setActivityBehavior(new ThrowEscalationEventActivityBehavior(escalation));

      } else { // default: none end event
        activity.getProperties().set(BpmnProperties.TYPE, ActivityTypes.END_EVENT_NONE);
        activity.setActivityBehavior(new NoneEndEventActivityBehavior());
      }

      parseActivityInputOutput(endEventElement, activity);

      parseAsynchronousContinuationForActivity(endEventElement, activity);

      parseExecutionListenersOnScope(endEventElement, activity);

      for (BpmnParseListener parseListener : parseListeners) {
        parseListener.parseEndEvent(endEventElement, scope, activity);
      }

      if (isServiceTaskLike) {
        // activity behavior could be set by a listener (e.g. connector); thus,
        // check is after listener invocation
        validateServiceTaskLike(activity,
            ActivityTypes.END_EVENT_MESSAGE,
            messageEventDefinitionElement);
      }
    }
  }

  /**
   * Parses the boundary events of a certain 'level' (process, subprocess or
   * other scope).
   *
   * <p>
   * Note that the boundary events are not parsed during the parsing of the bpmn
   * activities, since the semantics are different (boundaryEvent needs to be
   * added as nested activity to the reference activity on PVM level).
   * </p>
   *
   * @param parentElement
   *          The 'parent' element that contains the activities (process,
   *          subprocess).
   * @param flowScope
   *          The {@link ScopeImpl} to which the activities must be added.
   */
  public void parseBoundaryEvents(Element parentElement, ScopeImpl flowScope) {
    for (Element boundaryEventElement : parentElement.elements("boundaryEvent")) {

      // The boundary event is attached to an activity, reference by the
      // 'attachedToRef' attribute
      String attachedToRef = boundaryEventElement.attribute("attachedToRef");
      if (attachedToRef == null || "".equals(attachedToRef)) {
        addError("AttachedToRef is required when using a timerEventDefinition", boundaryEventElement);
      }

      // Representation structure-wise is a nested activity in the activity to
      // which its attached
      String id = boundaryEventElement.attribute("id");

      LOG.parsingElement("boundary event", id);

      // Depending on the sub-element definition, the correct activityBehavior
      // parsing is selected
      Element timerEventDefinition = boundaryEventElement.element(TIMER_EVENT_DEFINITION);
      Element errorEventDefinition = boundaryEventElement.element(ERROR_EVENT_DEFINITION);
      Element signalEventDefinition = boundaryEventElement.element(SIGNAL_EVENT_DEFINITION);
      Element cancelEventDefinition = boundaryEventElement.element(CANCEL_EVENT_DEFINITION);
      Element compensateEventDefinition = boundaryEventElement.element(COMPENSATE_EVENT_DEFINITION);
      Element messageEventDefinition = boundaryEventElement.element(MESSAGE_EVENT_DEFINITION);
      Element escalationEventDefinition = boundaryEventElement.element(ESCALATION_EVENT_DEFINITION);
      Element conditionalEventDefinition = boundaryEventElement.element(CONDITIONAL_EVENT_DEFINITION);

      // create the boundary event activity
      ActivityImpl boundaryEventActivity = createActivityOnScope(boundaryEventElement, flowScope);
      parseAsynchronousContinuation(boundaryEventElement, boundaryEventActivity);

      ActivityImpl attachedActivity = flowScope.findActivityAtLevelOfSubprocess(attachedToRef);
      if (attachedActivity == null) {
        addError("Invalid reference in boundary event. Make sure that the referenced activity is defined in the same scope as the boundary event",
            boundaryEventElement);
      }

      // determine the correct event scope (the scope in which the boundary event catches events)
      if (compensateEventDefinition == null) {
        ActivityImpl multiInstanceScope = getMultiInstanceScope(attachedActivity);
        if (multiInstanceScope != null) {
          // if the boundary event is attached to a multi instance activity,
          // then the scope of the boundary event is the multi instance body.
          boundaryEventActivity.setEventScope(multiInstanceScope);
        } else {
          attachedActivity.setScope(true);
          boundaryEventActivity.setEventScope(attachedActivity);
        }
      } else {
        boundaryEventActivity.setEventScope(attachedActivity);
      }

      // except escalation, by default is assumed to abort the activity
      String cancelActivityAttr = boundaryEventElement.attribute("cancelActivity", TRUE);
      boolean isCancelActivity = Boolean.parseBoolean(cancelActivityAttr);

      // determine start behavior
      if (isCancelActivity) {
        boundaryEventActivity.setActivityStartBehavior(ActivityStartBehavior.CANCEL_EVENT_SCOPE);
      } else {
        boundaryEventActivity.setActivityStartBehavior(ActivityStartBehavior.CONCURRENT_IN_FLOW_SCOPE);
      }

      // Catch event behavior is the same for most types
      ActivityBehavior behavior = new BoundaryEventActivityBehavior();
      if (timerEventDefinition != null) {
        parseBoundaryTimerEventDefinition(timerEventDefinition, isCancelActivity, boundaryEventActivity);

      } else if (errorEventDefinition != null) {
        parseBoundaryErrorEventDefinition(errorEventDefinition, boundaryEventActivity);

      } else if (signalEventDefinition != null) {
        parseBoundarySignalEventDefinition(signalEventDefinition, isCancelActivity, boundaryEventActivity);

      } else if (cancelEventDefinition != null) {
        behavior = parseBoundaryCancelEventDefinition(cancelEventDefinition, boundaryEventActivity);

      } else if (compensateEventDefinition != null) {
        parseBoundaryCompensateEventDefinition(compensateEventDefinition, boundaryEventActivity);

      } else if (messageEventDefinition != null) {
        parseBoundaryMessageEventDefinition(messageEventDefinition, isCancelActivity, boundaryEventActivity);

      } else if (escalationEventDefinition != null) {

        if (attachedActivity.isSubProcessScope() || attachedActivity.getActivityBehavior() instanceof CallActivityBehavior ||
            attachedActivity.getActivityBehavior() instanceof UserTaskActivityBehavior) {
          parseBoundaryEscalationEventDefinition(escalationEventDefinition, isCancelActivity, boundaryEventActivity);
        } else {
          addError("An escalation boundary event should only be attached to a subprocess, a call activity or an user task", boundaryEventElement);
        }

      } else if (conditionalEventDefinition != null) {
        behavior = parseBoundaryConditionalEventDefinition(conditionalEventDefinition, isCancelActivity, boundaryEventActivity);
      } else {
        addError("Unsupported boundary event type", boundaryEventElement);

      }

      ensureNoIoMappingDefined(boundaryEventElement);

      boundaryEventActivity.setActivityBehavior(behavior);

      parseExecutionListenersOnScope(boundaryEventElement, boundaryEventActivity);

      for (BpmnParseListener parseListener : parseListeners) {
        parseListener.parseBoundaryEvent(boundaryEventElement, flowScope, boundaryEventActivity);
      }

    }

  }

  public List<OperatonErrorEventDefinition> parseOperatonErrorEventDefinitions(ActivityImpl activity, Element scopeElement) {
    List<OperatonErrorEventDefinition> errorEventDefinitions = new ArrayList<>();
    Element extensionElements = scopeElement.element(EXTENSION_ELEMENTS_TAG);
    if (extensionElements != null) {
      List<Element> errorEventDefinitionElements = extensionElements.elements(ERROR_EVENT_DEFINITION);
      for (Element errorEventDefinitionElement : errorEventDefinitionElements) {
        String errorRef = errorEventDefinitionElement.attribute(ERROR_REF_ATTRIBUTE);
        Error error = null;
        if (errorRef != null) {
          String operatonExpression = errorEventDefinitionElement.attribute(PROPERTYNAME_EXPRESSION);
          error = bpmnParseErrors.get(errorRef);
          OperatonErrorEventDefinition definition = new OperatonErrorEventDefinition(activity.getId(), expressionManager.createExpression(operatonExpression));
          definition.setErrorCode(error == null ? errorRef : error.getErrorCode());
          setErrorCodeVariableOnErrorEventDefinition(errorEventDefinitionElement, definition);
          setErrorMessageVariableOnErrorEventDefinition(errorEventDefinitionElement, definition);

          errorEventDefinitions.add(definition);
        }
      }
    }
    return errorEventDefinitions;
  }

  protected ActivityImpl getMultiInstanceScope(ActivityImpl activity) {
    if (activity.isMultiInstance()) {
      return activity.getParentFlowScopeActivity();
    } else {
      return null;
    }
  }

  /**
   * Parses a boundary timer event. The end-result will be that the given nested
   * activity will get the appropriate {@link ActivityBehavior}.
   *
   * @param timerEventDefinition
   *          The XML element corresponding with the timer event details
   * @param interrupting
   *          Indicates whether this timer is interrupting.
   * @param boundaryActivity
   *          The activity which maps to the structure of the timer event on the
   *          boundary of another activity. Note that this is NOT the activity
   *          onto which the boundary event is attached, but a nested activity
   *          inside this activity, specifically created for this event.
   */
  public void parseBoundaryTimerEventDefinition(Element timerEventDefinition, boolean interrupting, ActivityImpl boundaryActivity) {
    boundaryActivity.getProperties().set(BpmnProperties.TYPE, ActivityTypes.BOUNDARY_TIMER);
    TimerDeclarationImpl timerDeclaration = parseTimer(timerEventDefinition, boundaryActivity, TimerExecuteNestedActivityJobHandler.TYPE);

    // ACT-1427
    if (interrupting) {
      timerDeclaration.setInterruptingTimer(true);

      Element timeCycleElement = timerEventDefinition.element(TIME_CYCLE);
      if (timeCycleElement != null) {
        addTimeCycleWarning(timeCycleElement, "cancelling boundary", boundaryActivity.getId());
      }
    }

    addTimerDeclaration(boundaryActivity.getEventScope(), timerDeclaration);

    for (BpmnParseListener parseListener : parseListeners) {
      parseListener.parseBoundaryTimerEventDefinition(timerEventDefinition, interrupting, boundaryActivity);
    }
  }

  public void parseBoundarySignalEventDefinition(Element element, boolean interrupting, ActivityImpl signalActivity) {
    signalActivity.getProperties().set(BpmnProperties.TYPE, ActivityTypes.BOUNDARY_SIGNAL);

    EventSubscriptionDeclaration signalDefinition = parseSignalEventDefinition(element, false, signalActivity.getId());
    if (signalActivity.getId() == null) {
      addError("boundary event has no id", element);
    }
    signalDefinition.setActivityId(signalActivity.getId());
    addEventSubscriptionDeclaration(signalDefinition, signalActivity.getEventScope(), element);

    for (BpmnParseListener parseListener : parseListeners) {
      parseListener.parseBoundarySignalEventDefinition(element, interrupting, signalActivity);
    }

  }

  public void parseBoundaryMessageEventDefinition(Element element, boolean interrupting, ActivityImpl messageActivity) {
    messageActivity.getProperties().set(BpmnProperties.TYPE, ActivityTypes.BOUNDARY_MESSAGE);

    EventSubscriptionDeclaration messageEventDefinition = parseMessageEventDefinition(element, messageActivity.getId());
    if (messageActivity.getId() == null) {
      addError("boundary event has no id", element);
    }
    messageEventDefinition.setActivityId(messageActivity.getId());
    addEventSubscriptionDeclaration(messageEventDefinition, messageActivity.getEventScope(), element);

    for (BpmnParseListener parseListener : parseListeners) {
      parseListener.parseBoundaryMessageEventDefinition(element, interrupting, messageActivity);
    }

  }

  @SuppressWarnings("unchecked")
  protected void parseTimerStartEventDefinition(Element timerEventDefinition, ActivityImpl timerActivity, ProcessDefinitionEntity processDefinition) {
    timerActivity.getProperties().set(BpmnProperties.TYPE, ActivityTypes.START_EVENT_TIMER);
    TimerDeclarationImpl timerDeclaration = parseTimer(timerEventDefinition, timerActivity, TimerStartEventJobHandler.TYPE);
    timerDeclaration.setRawJobHandlerConfiguration(processDefinition.getKey());

    List<TimerDeclarationImpl> timerDeclarations = (List<TimerDeclarationImpl>) processDefinition.getProperty(PROPERTYNAME_START_TIMER);
    if (timerDeclarations == null) {
      timerDeclarations = new ArrayList<>();
      processDefinition.setProperty(PROPERTYNAME_START_TIMER, timerDeclarations);
    }
    timerDeclarations.add(timerDeclaration);

  }

  protected void parseTimerStartEventDefinitionForEventSubprocess(Element timerEventDefinition, ActivityImpl timerActivity, boolean interrupting) {
    timerActivity.getProperties().set(BpmnProperties.TYPE, ActivityTypes.START_EVENT_TIMER);

    TimerDeclarationImpl timerDeclaration = parseTimer(timerEventDefinition, timerActivity, TimerStartEventSubprocessJobHandler.TYPE);

    timerDeclaration.setActivity(timerActivity);
    timerDeclaration.setEventScopeActivityId(timerActivity.getEventScope().getId());
    timerDeclaration.setRawJobHandlerConfiguration(timerActivity.getFlowScope().getId());
    timerDeclaration.setInterruptingTimer(interrupting);

    if (interrupting) {
      Element timeCycleElement = timerEventDefinition.element(TIME_CYCLE);
      if (timeCycleElement != null) {
        addTimeCycleWarning(timeCycleElement, "interrupting start", timerActivity.getId());
      }

    }

    addTimerDeclaration(timerActivity.getEventScope(), timerDeclaration);
  }

  protected void parseEventDefinitionForSubprocess(EventSubscriptionDeclaration subscriptionDeclaration, ActivityImpl activity, Element element) {
    subscriptionDeclaration.setActivityId(activity.getId());
    subscriptionDeclaration.setEventScopeActivityId(activity.getEventScope().getId());
    subscriptionDeclaration.setStartEvent(false);
    addEventSubscriptionDeclaration(subscriptionDeclaration, activity.getEventScope(), element);
  }

  protected void parseIntermediateSignalEventDefinition(Element element, ActivityImpl signalActivity) {
    signalActivity.getProperties().set(BpmnProperties.TYPE, ActivityTypes.INTERMEDIATE_EVENT_SIGNAL);

    parseSignalCatchEventDefinition(element, signalActivity, false);

    for (BpmnParseListener parseListener : parseListeners) {
      parseListener.parseIntermediateSignalCatchEventDefinition(element, signalActivity);
    }
  }

  protected void parseSignalCatchEventDefinition(Element element, ActivityImpl signalActivity, boolean isStartEvent) {
    EventSubscriptionDeclaration signalDefinition = parseSignalEventDefinition(element, false, signalActivity.getId());
    signalDefinition.setActivityId(signalActivity.getId());
    signalDefinition.setStartEvent(isStartEvent);
    addEventSubscriptionDeclaration(signalDefinition, signalActivity.getEventScope(), element);

    EventSubscriptionJobDeclaration catchingAsyncDeclaration = new EventSubscriptionJobDeclaration(signalDefinition);
    catchingAsyncDeclaration.setJobPriorityProvider((ParameterValueProvider) signalActivity.getProperty(PROPERTYNAME_JOB_PRIORITY));
    catchingAsyncDeclaration.setActivity(signalActivity);
    signalDefinition.setJobDeclaration(catchingAsyncDeclaration);
    addEventSubscriptionJobDeclaration(catchingAsyncDeclaration, signalActivity, element);
  }

  /**
   * Parses the Signal Event Definition XML including payload definition.
   *
   * @param signalEventDefinitionElement the Signal Event Definition element
   * @param isThrowing true if a Throwing signal event is being parsed
   * @return
   */
  protected EventSubscriptionDeclaration parseSignalEventDefinition(Element signalEventDefinitionElement, boolean isThrowing, String signalElementId) {
    String signalRef = signalEventDefinitionElement.attribute("signalRef");
    if (signalRef == null) {
      addError("signalEventDefinition does not have required property 'signalRef'", signalEventDefinitionElement, signalElementId);
      return null;
    }

    SignalDefinition signalDefinition = signals.get(resolveName(signalRef));
    if (signalDefinition == null) {
      addError("Could not find signal with id '%s'".formatted(signalRef), signalEventDefinitionElement, signalElementId);
      return null;
    }

    EventSubscriptionDeclaration signalEventDefinition;
    if (isThrowing) {
      CallableElement payload = new CallableElement();
      parseInputParameter(signalEventDefinitionElement, payload);
      signalEventDefinition = new EventSubscriptionDeclaration(signalDefinition.getExpression(), EventType.SIGNAL, payload);
    } else {
      signalEventDefinition = new EventSubscriptionDeclaration(signalDefinition.getExpression(), EventType.SIGNAL);
    }

    boolean throwingAsync = TRUE.equals(signalEventDefinitionElement.attributeNS(OPERATON_BPMN_EXTENSIONS_NS, "async", FALSE));
    signalEventDefinition.setAsync(throwingAsync);

    return signalEventDefinition;

  }

  protected void parseIntermediateTimerEventDefinition(Element timerEventDefinition, ActivityImpl timerActivity) {
    timerActivity.getProperties().set(BpmnProperties.TYPE, ActivityTypes.INTERMEDIATE_EVENT_TIMER);
    TimerDeclarationImpl timerDeclaration = parseTimer(timerEventDefinition, timerActivity, TimerCatchIntermediateEventJobHandler.TYPE);

    Element timeCycleElement = timerEventDefinition.element(TIME_CYCLE);
    if (timeCycleElement != null) {
      ProcessDefinition processDefinition = (ProcessDefinition) timerActivity.getProcessDefinition();
      LOG.intermediateCatchTimerEventWithTimeCycleNotRecommended(processDefinition.getKey(), timerActivity.getId());
    }

    addTimerDeclaration(timerActivity.getEventScope(), timerDeclaration);

    for (BpmnParseListener parseListener : parseListeners) {
      parseListener.parseIntermediateTimerEventDefinition(timerEventDefinition, timerActivity);
    }
  }

  protected TimerDeclarationImpl parseTimer(Element timerEventDefinition, ActivityImpl timerActivity, String jobHandlerType) {
    // TimeDate
    TimerDeclarationType type = TimerDeclarationType.DATE;
    Expression expression = parseExpression(timerEventDefinition, "timeDate");
    // TimeCycle
    if (expression == null) {
      type = TimerDeclarationType.CYCLE;
      expression = parseExpression(timerEventDefinition, TIME_CYCLE);
    }
    // TimeDuration
    if (expression == null) {
      type = TimerDeclarationType.DURATION;
      expression = parseExpression(timerEventDefinition, "timeDuration");
    }
    // neither date, cycle or duration configured!
    if (expression == null) {
      addError("Timer needs configuration (either timeDate, timeCycle or timeDuration is needed).", timerEventDefinition, timerActivity.getId());
    }

    // Parse the timer declaration
    TimerDeclarationImpl timerDeclaration = new TimerDeclarationImpl(expression, type, jobHandlerType);
    timerDeclaration.setRawJobHandlerConfiguration(timerActivity.getId());
    timerDeclaration.setExclusive(TRUE.equals(timerEventDefinition.attributeNS(OPERATON_BPMN_EXTENSIONS_NS, "exclusive", String.valueOf(DEFAULT_EXCLUSIVE))));
    if (timerActivity.getId() == null) {
      addError("Attribute \"id\" is required!", timerEventDefinition);
    }
    timerDeclaration.setActivity(timerActivity);
    timerDeclaration.setJobConfiguration(type.toString() + ": " + expression.getExpressionText());
    addJobDeclarationToProcessDefinition(timerDeclaration, (ProcessDefinition) timerActivity.getProcessDefinition());

    timerDeclaration.setJobPriorityProvider((ParameterValueProvider) timerActivity.getProperty(PROPERTYNAME_JOB_PRIORITY));

    return timerDeclaration;
  }

  protected Expression parseExpression(Element parent, String name) {
    Element value = parent.element(name);
    if (value != null) {
      String expressionText = value.getText().trim();
      return expressionManager.createExpression(expressionText);
    }
    return null;
  }

  public void parseBoundaryErrorEventDefinition(Element errorEventDefinition, ActivityImpl boundaryEventActivity) {

    boundaryEventActivity.getProperties().set(BpmnProperties.TYPE, ActivityTypes.BOUNDARY_ERROR);

    String errorRef = errorEventDefinition.attribute(ERROR_REF_ATTRIBUTE);
    Error error = null;
    ErrorEventDefinition definition = new ErrorEventDefinition(boundaryEventActivity.getId());
    if (errorRef != null) {
      error = bpmnParseErrors.get(errorRef);
      definition.setErrorCode(error == null ? errorRef : error.getErrorCode());
    }
    setErrorCodeVariableOnErrorEventDefinition(errorEventDefinition, definition);
    setErrorMessageVariableOnErrorEventDefinition(errorEventDefinition, definition);

    addErrorEventDefinition(definition, boundaryEventActivity.getEventScope());

    for (BpmnParseListener parseListener : parseListeners) {
      parseListener.parseBoundaryErrorEventDefinition(errorEventDefinition, true, (ActivityImpl) boundaryEventActivity.getEventScope(), boundaryEventActivity);
    }
  }

  protected void addErrorEventDefinition(ErrorEventDefinition errorEventDefinition, ScopeImpl catchingScope) {
    catchingScope.getProperties().addListItem(BpmnProperties.ERROR_EVENT_DEFINITIONS, errorEventDefinition);

    List<ErrorEventDefinition> errorEventDefinitions = catchingScope.getProperties().get(BpmnProperties.ERROR_EVENT_DEFINITIONS);
    Collections.sort(errorEventDefinitions, ErrorEventDefinition.comparator);
  }

  protected void parseBoundaryEscalationEventDefinition(Element escalationEventDefinitionElement, boolean cancelActivity, ActivityImpl boundaryEventActivity) {
    boundaryEventActivity.getProperties().set(BpmnProperties.TYPE, ActivityTypes.BOUNDARY_ESCALATION);

    EscalationEventDefinition escalationEventDefinition = createEscalationEventDefinitionForEscalationHandler(escalationEventDefinitionElement, boundaryEventActivity, cancelActivity, boundaryEventActivity.getId());
    addEscalationEventDefinition(boundaryEventActivity.getEventScope(), escalationEventDefinition, escalationEventDefinitionElement, boundaryEventActivity.getId());

    for (BpmnParseListener parseListener : parseListeners) {
      parseListener.parseBoundaryEscalationEventDefinition(escalationEventDefinitionElement, cancelActivity, boundaryEventActivity);
    }
  }

  /**
   * Find the referenced escalation of the given escalation event definition.
   * Add errors if the referenced escalation not found.
   *
   * @return referenced escalation or <code>null</code>, if referenced escalation not found
   */
  protected Escalation findEscalationForEscalationEventDefinition(Element escalationEventDefinition, String escalationElementId) {
    String escalationRef = escalationEventDefinition.attribute("escalationRef");
    if (escalationRef == null) {
      addError("escalationEventDefinition does not have required attribute 'escalationRef'", escalationEventDefinition, escalationElementId);
    } else if (!escalations.containsKey(escalationRef)) {
      addError("could not find escalation with id '%s'".formatted(escalationRef), escalationEventDefinition, escalationElementId);
    } else {
      return escalations.get(escalationRef);
    }
    return null;
  }

  protected EscalationEventDefinition createEscalationEventDefinitionForEscalationHandler(Element escalationEventDefinitionElement, ActivityImpl escalationHandler, boolean cancelActivity, String parentElementId) {
    EscalationEventDefinition escalationEventDefinition = new EscalationEventDefinition(escalationHandler, cancelActivity);

    String escalationRef = escalationEventDefinitionElement.attribute("escalationRef");
    if (escalationRef != null) {
      if (!escalations.containsKey(escalationRef)) {
        addError("could not find escalation with id '%s'".formatted(escalationRef), escalationEventDefinitionElement, parentElementId);
      } else {
        Escalation escalation = escalations.get(escalationRef);
        escalationEventDefinition.setEscalationCode(escalation.getEscalationCode());
      }
    }

    String escalationCodeVariable = escalationEventDefinitionElement.attributeNS(OPERATON_BPMN_EXTENSIONS_NS, "escalationCodeVariable");
    if(escalationCodeVariable != null) {
      escalationEventDefinition.setEscalationCodeVariable(escalationCodeVariable);
    }

    return escalationEventDefinition;
  }

  protected void addEscalationEventDefinition(ScopeImpl catchingScope, EscalationEventDefinition escalationEventDefinition, Element element, String escalationElementId) {
    // ensure there is only one escalation handler (e.g. escalation boundary event, escalation event subprocess) what can catch the escalation event
    for (EscalationEventDefinition existingEscalationEventDefinition : catchingScope.getProperties().get(BpmnProperties.ESCALATION_EVENT_DEFINITIONS)) {

      if (existingEscalationEventDefinition.getEscalationHandler().isSubProcessScope()
          && escalationEventDefinition.getEscalationHandler().isSubProcessScope()) {

        if (existingEscalationEventDefinition.getEscalationCode() == null && escalationEventDefinition.getEscalationCode() == null) {
          addError("The same scope can not contains more than one escalation event subprocess without escalation code. "
              + "An escalation event subprocess without escalation code catch all escalation events.", element, escalationElementId);
        } else if (existingEscalationEventDefinition.getEscalationCode() == null || escalationEventDefinition.getEscalationCode() == null) {
          addError("The same scope can not contains an escalation event subprocess without escalation code and another one with escalation code. "
              + "The escalation event subprocess without escalation code catch all escalation events.", element, escalationElementId);
        } else if (existingEscalationEventDefinition.getEscalationCode().equals(escalationEventDefinition.getEscalationCode())) {
          addError("multiple escalation event subprocesses with the same escalationCode '%s' are not supported on same scope"
              .formatted(escalationEventDefinition.getEscalationCode()), element, escalationElementId);
        }
      } else if (!existingEscalationEventDefinition.getEscalationHandler().isSubProcessScope()
          && !escalationEventDefinition.getEscalationHandler().isSubProcessScope()) {

        if (existingEscalationEventDefinition.getEscalationCode() == null && escalationEventDefinition.getEscalationCode() == null) {
          addError("The same scope can not contains more than one escalation boundary event without escalation code. "
              + "An escalation boundary event without escalation code catch all escalation events.", element, escalationElementId);
        } else if (existingEscalationEventDefinition.getEscalationCode() == null || escalationEventDefinition.getEscalationCode() == null) {
          addError("The same scope can not contains an escalation boundary event without escalation code and another one with escalation code. "
              + "The escalation boundary event without escalation code catch all escalation events.", element, escalationElementId);
        } else if (existingEscalationEventDefinition.getEscalationCode().equals(escalationEventDefinition.getEscalationCode())) {
          addError("multiple escalation boundary events with the same escalationCode '%s' are not supported on same scope"
              .formatted(escalationEventDefinition.getEscalationCode()), element, escalationElementId);
        }
      }
    }

    catchingScope.getProperties().addListItem(BpmnProperties.ESCALATION_EVENT_DEFINITIONS, escalationEventDefinition);
  }

  protected void addTimerDeclaration(ScopeImpl scope, TimerDeclarationImpl timerDeclaration) {
    scope.getProperties().putMapEntry(BpmnProperties.TIMER_DECLARATIONS, timerDeclaration.getActivityId(), timerDeclaration);
  }

  protected void addTimerListenerDeclaration(String listenerId, ScopeImpl scope, TimerDeclarationImpl timerDeclaration) {
    if (scope.getProperties().get(BpmnProperties.TIMEOUT_LISTENER_DECLARATIONS) != null && scope.getProperties().get(BpmnProperties.TIMEOUT_LISTENER_DECLARATIONS).get(timerDeclaration.getActivityId()) != null) {
      scope.getProperties().get(BpmnProperties.TIMEOUT_LISTENER_DECLARATIONS).get(timerDeclaration.getActivityId()).put(listenerId, timerDeclaration);
    } else {
      Map<String, TimerDeclarationImpl> activityDeclarations = new HashMap<>();
      activityDeclarations.put(listenerId, timerDeclaration);
      scope.getProperties().putMapEntry(BpmnProperties.TIMEOUT_LISTENER_DECLARATIONS, timerDeclaration.getActivityId(), activityDeclarations);
    }
  }

  @SuppressWarnings( {"unchecked", "deprecation" })
  protected void addVariableDeclaration(ScopeImpl scope, VariableDeclaration variableDeclaration) {
    List<VariableDeclaration> variableDeclarations = (List<VariableDeclaration>) scope.getProperty(PROPERTYNAME_VARIABLE_DECLARATIONS);
    if (variableDeclarations == null) {
      variableDeclarations = new ArrayList<>();
      scope.setProperty(PROPERTYNAME_VARIABLE_DECLARATIONS, variableDeclarations);
    }
    variableDeclarations.add(variableDeclaration);
  }

  /**
   * Parses the given element as conditional boundary event.
   *
   * @param element the XML element which contains the conditional event information
   * @param interrupting indicates if the event is interrupting or not
   * @param conditionalActivity the conditional event activity
   * @return the boundary conditional event behavior which contains the condition
   */
  public BoundaryConditionalEventActivityBehavior parseBoundaryConditionalEventDefinition(Element element, boolean interrupting, ActivityImpl conditionalActivity) {
    conditionalActivity.getProperties().set(BpmnProperties.TYPE, ActivityTypes.BOUNDARY_CONDITIONAL);

    ConditionalEventDefinition conditionalEventDefinition = parseConditionalEventDefinition(element, conditionalActivity);
    conditionalEventDefinition.setInterrupting(interrupting);
    addEventSubscriptionDeclaration(conditionalEventDefinition, conditionalActivity.getEventScope(), element);

    for (BpmnParseListener parseListener : parseListeners) {
      parseListener.parseBoundaryConditionalEventDefinition(element, interrupting, conditionalActivity);
    }

    return new BoundaryConditionalEventActivityBehavior(conditionalEventDefinition);
  }

  /**
   * Parses the given element as intermediate conditional event.
   *
   * @param element the XML element which contains the conditional event information
   * @param conditionalActivity the conditional event activity
   * @return returns the conditional activity with the parsed information
   */
  public ConditionalEventDefinition parseIntermediateConditionalEventDefinition(Element element, ActivityImpl conditionalActivity) {
    conditionalActivity.getProperties().set(BpmnProperties.TYPE, ActivityTypes.INTERMEDIATE_EVENT_CONDITIONAL);

    ConditionalEventDefinition conditionalEventDefinition = parseConditionalEventDefinition(element, conditionalActivity);
    addEventSubscriptionDeclaration(conditionalEventDefinition, conditionalActivity.getEventScope(), element);

    for (BpmnParseListener parseListener : parseListeners) {
      parseListener.parseIntermediateConditionalEventDefinition(element, conditionalActivity);
    }

    return conditionalEventDefinition;
  }

  /**
   * Parses the given element as conditional start event of an event subprocess.
   *
   * @param element the XML element which contains the conditional event information
   * @param interrupting indicates if the event is interrupting or not
   * @param conditionalActivity the conditional event activity
   * @return
   */
  public ConditionalEventDefinition parseConditionalStartEventForEventSubprocess(Element element, ActivityImpl conditionalActivity, boolean interrupting) {
    conditionalActivity.getProperties().set(BpmnProperties.TYPE, ActivityTypes.START_EVENT_CONDITIONAL);

    ConditionalEventDefinition conditionalEventDefinition = parseConditionalEventDefinition(element, conditionalActivity);
    conditionalEventDefinition.setInterrupting(interrupting);
    addEventSubscriptionDeclaration(conditionalEventDefinition, conditionalActivity.getEventScope(), element);

    for (BpmnParseListener parseListener : parseListeners) {
      parseListener.parseConditionalStartEventForEventSubprocess(element, conditionalActivity, interrupting);
    }

    return conditionalEventDefinition;
  }

  /**
   * Parses the given element and returns an ConditionalEventDefinition object.
   *
   * @param element the XML element which contains the conditional event information
   * @param conditionalActivity the conditional event activity
   * @return the conditional event definition which was parsed
   */
  protected ConditionalEventDefinition parseConditionalEventDefinition(Element element, ActivityImpl conditionalActivity) {
    ConditionalEventDefinition conditionalEventDefinition = null;

    Element conditionExprElement = element.element(CONDITION);
    String conditionalActivityId = conditionalActivity.getId();
    if (conditionExprElement != null) {
      Condition condition = parseConditionExpression(conditionExprElement, conditionalActivityId);
      conditionalEventDefinition = new ConditionalEventDefinition(condition, conditionalActivity);

      String expression = conditionExprElement.getText().trim();
      conditionalEventDefinition.setConditionAsString(expression);

      conditionalActivity.getProcessDefinition().getProperties().set(BpmnProperties.HAS_CONDITIONAL_EVENTS, true);

      final String variableName = element.attributeNS(OPERATON_BPMN_EXTENSIONS_NS, "variableName");
      conditionalEventDefinition.setVariableName(variableName);

      final String variableEvents = element.attributeNS(OPERATON_BPMN_EXTENSIONS_NS, "variableEvents");
      final List<String> variableEventsList = parseCommaSeparatedList(variableEvents);
      conditionalEventDefinition.setVariableEvents(new HashSet<>(variableEventsList));

      for (String variableEvent : variableEventsList) {
        if (!VARIABLE_EVENTS.contains(variableEvent)) {
          addWarning("Variable event: %s is not valid. Possible variable change events are: %s:"
              .formatted(variableEvent, Arrays.toString(VARIABLE_EVENTS.toArray())), element, conditionalActivityId);
        }
      }

    } else {
      addError("Conditional event must contain an expression for evaluation.", element, conditionalActivityId);
    }

    return conditionalEventDefinition;
  }

  /**
   * Parses a subprocess (formally known as an embedded subprocess): a
   * subprocess defined within another process definition.
   *
   * @param subProcessElement
   *          The XML element corresponding with the subprocess definition
   * @param scope
   *          The current scope on which the subprocess is defined.
   */
  public ActivityImpl parseSubProcess(Element subProcessElement, ScopeImpl scope) {
    ActivityImpl subProcessActivity = createActivityOnScope(subProcessElement, scope);
    subProcessActivity.setSubProcessScope(true);

    parseAsynchronousContinuationForActivity(subProcessElement, subProcessActivity);

    Boolean isTriggeredByEvent = parseBooleanAttribute(subProcessElement.attribute("triggeredByEvent"), false);
    subProcessActivity.getProperties().set(BpmnProperties.TRIGGERED_BY_EVENT, isTriggeredByEvent);
    subProcessActivity.setProperty(PROPERTYNAME_CONSUMES_COMPENSATION, Boolean.FALSE.equals(isTriggeredByEvent));

    subProcessActivity.setScope(true);
    if (Boolean.TRUE.equals(isTriggeredByEvent)) {
      subProcessActivity.setActivityBehavior(new EventSubProcessActivityBehavior());
      subProcessActivity.setEventScope(scope);
    } else {
      subProcessActivity.setActivityBehavior(new SubProcessActivityBehavior());
    }
    parseScope(subProcessElement, subProcessActivity);

    for (BpmnParseListener parseListener : parseListeners) {
      parseListener.parseSubProcess(subProcessElement, scope, subProcessActivity);
    }
    return subProcessActivity;
  }

  protected ActivityImpl parseTransaction(Element transactionElement, ScopeImpl scope) {
    ActivityImpl activity = createActivityOnScope(transactionElement, scope);

    parseAsynchronousContinuationForActivity(transactionElement, activity);

    activity.setScope(true);
    activity.setSubProcessScope(true);
    activity.setActivityBehavior(new SubProcessActivityBehavior());
    activity.getProperties().set(BpmnProperties.TRIGGERED_BY_EVENT, false);
    parseScope(transactionElement, activity);

    for (BpmnParseListener parseListener : parseListeners) {
      parseListener.parseTransaction(transactionElement, scope, activity);
    }
    return activity;
  }

  /**
   * Parses a call activity (currently only supporting calling subprocesses).
   *
   * @param callActivityElement
   *          The XML element defining the call activity
   * @param scope
   *          The current scope on which the call activity is defined.
   */
  public ActivityImpl parseCallActivity(Element callActivityElement, ScopeImpl scope, boolean isMultiInstance) {
    ActivityImpl activity = createActivityOnScope(callActivityElement, scope);

    // parse async
    parseAsynchronousContinuationForActivity(callActivityElement, activity);

    // parse definition key (and behavior)
    String calledElement = callActivityElement.attribute("calledElement");
    String caseRef = callActivityElement.attributeNS(OPERATON_BPMN_EXTENSIONS_NS, "caseRef");
    String className = callActivityElement.attributeNS(OPERATON_BPMN_EXTENSIONS_NS, PROPERTYNAME_VARIABLE_MAPPING_CLASS);
    String delegateExpression = callActivityElement.attributeNS(OPERATON_BPMN_EXTENSIONS_NS, PROPERTYNAME_VARIABLE_MAPPING_DELEGATE_EXPRESSION);

    if (calledElement == null && caseRef == null) {
      addError("Missing attribute 'calledElement' or 'caseRef'", callActivityElement);
    } else if (calledElement != null && caseRef != null) {
      addError("The attributes 'calledElement' or 'caseRef' cannot be used together: Use either 'calledElement' or 'caseRef'", callActivityElement);
    }

    String bindingAttributeName = "calledElementBinding";
    String versionAttributeName = "calledElementVersion";
    String versionTagAttributeName = "calledElementVersionTag";
    String tenantIdAttributeName = "calledElementTenantId";

    String deploymentId = deployment.getId();

    CallableElement callableElement = new CallableElement();
    callableElement.setDeploymentId(deploymentId);

    CallableElementActivityBehavior behavior = null;

    if (calledElement != null) {
      if (className != null) {
          behavior = new CallActivityBehavior(className);
      } else if (delegateExpression != null) {
         Expression exp = expressionManager.createExpression(delegateExpression);
         behavior = new CallActivityBehavior(exp);
      } else {
        behavior = new CallActivityBehavior();
      }
      ParameterValueProvider definitionKeyProvider = createParameterValueProvider(calledElement, expressionManager);
      callableElement.setDefinitionKeyValueProvider(definitionKeyProvider);

    } else {
      behavior = new CaseCallActivityBehavior();
      ParameterValueProvider definitionKeyProvider = createParameterValueProvider(caseRef, expressionManager);
      callableElement.setDefinitionKeyValueProvider(definitionKeyProvider);
      bindingAttributeName = "caseBinding";
      versionAttributeName = "caseVersion";
      tenantIdAttributeName = "caseTenantId";
    }

    behavior.setCallableElement(callableElement);

    // parse binding
    parseBinding(callActivityElement, activity, callableElement, bindingAttributeName);

    // parse version
    parseVersion(callActivityElement, activity, callableElement, bindingAttributeName, versionAttributeName);

    // parse versionTag
    parseVersionTag(callActivityElement, activity, callableElement, bindingAttributeName, versionTagAttributeName);

    // parse tenant id
    parseTenantId(callActivityElement, activity, callableElement, tenantIdAttributeName);

    // parse input parameter
    parseInputParameter(callActivityElement, callableElement);

    // parse output parameter
    parseOutputParameter(callActivityElement, callableElement);

    if (!isMultiInstance) {
      // turn activity into a scope unless it is a multi instance activity, in
      // that case this
      // is not necessary because there is already the multi instance body scope
      // and concurrent
      // child executions are sufficient
      activity.setScope(true);
    }
    activity.setActivityBehavior(behavior);

    parseExecutionListenersOnScope(callActivityElement, activity);

    for (BpmnParseListener parseListener : parseListeners) {
      parseListener.parseCallActivity(callActivityElement, scope, activity);
    }
    return activity;
  }

  @SuppressWarnings("unused")
  protected void parseBinding(Element callActivityElement, ActivityImpl activity, BaseCallableElement callableElement, String bindingAttributeName) {
    String binding = callActivityElement.attributeNS(OPERATON_BPMN_EXTENSIONS_NS, bindingAttributeName);

    if (CallableElementBinding.DEPLOYMENT.getValue().equals(binding)) {
      callableElement.setBinding(CallableElementBinding.DEPLOYMENT);
    } else if (CallableElementBinding.LATEST.getValue().equals(binding)) {
      callableElement.setBinding(CallableElementBinding.LATEST);
    } else if (CallableElementBinding.VERSION.getValue().equals(binding)) {
      callableElement.setBinding(CallableElementBinding.VERSION);
    } else if (CallableElementBinding.VERSION_TAG.getValue().equals(binding)) {
      callableElement.setBinding(CallableElementBinding.VERSION_TAG);
    }
  }

  @SuppressWarnings("unused")
  protected void parseTenantId(Element callingActivityElement, ActivityImpl activity, BaseCallableElement callableElement, String attrName) {
    ParameterValueProvider tenantIdValueProvider = null;

    String tenantId = callingActivityElement.attributeNS(OPERATON_BPMN_EXTENSIONS_NS, attrName);
    if (tenantId != null && !tenantId.isEmpty()) {
      tenantIdValueProvider = createParameterValueProvider(tenantId, expressionManager);
    }

    callableElement.setTenantIdProvider(tenantIdValueProvider);
  }

  @SuppressWarnings("unused")
  protected void parseVersion(Element callingActivityElement, ActivityImpl activity, BaseCallableElement callableElement, String bindingAttributeName, String versionAttributeName) {
    String version = null;

    CallableElementBinding binding = callableElement.getBinding();
    version = callingActivityElement.attributeNS(OPERATON_BPMN_EXTENSIONS_NS, versionAttributeName);

    if (binding != null && binding.equals(CallableElementBinding.VERSION) && version == null) {
      addError("Missing attribute '%s' when '%s' has value '%s'"
          .formatted(versionAttributeName, bindingAttributeName, CallableElementBinding.VERSION.getValue()), callingActivityElement);
    }

    ParameterValueProvider versionProvider = createParameterValueProvider(version, expressionManager);
    callableElement.setVersionValueProvider(versionProvider);
  }

  @SuppressWarnings("unused")
  protected void parseVersionTag(Element callingActivityElement, ActivityImpl activity, BaseCallableElement callableElement, String bindingAttributeName, String versionTagAttributeName) {
    String versionTag = null;

    CallableElementBinding binding = callableElement.getBinding();
    versionTag = callingActivityElement.attributeNS(OPERATON_BPMN_EXTENSIONS_NS, versionTagAttributeName);

    if (binding != null && binding.equals(CallableElementBinding.VERSION_TAG) && versionTag == null) {
      addError("Missing attribute '%s' when '%s' has value '%s'"
          .formatted(versionTagAttributeName, bindingAttributeName, CallableElementBinding.VERSION_TAG.getValue()), callingActivityElement);
    }

    ParameterValueProvider versionTagProvider = createParameterValueProvider(versionTag, expressionManager);
    callableElement.setVersionTagValueProvider(versionTagProvider);
  }

  protected void parseInputParameter(Element elementWithParameters, CallableElement callableElement) {
    Element extensionsElement = elementWithParameters.element(EXTENSION_ELEMENTS_TAG);

    if (extensionsElement != null) {
      // input data elements
      for (Element inElement : extensionsElement.elementsNS(OPERATON_BPMN_EXTENSIONS_NS, "in")) {

        String businessKey = inElement.attribute("businessKey");

        if (businessKey != null && !businessKey.isEmpty()) {
          ParameterValueProvider businessKeyValueProvider = createParameterValueProvider(businessKey, expressionManager);
          callableElement.setBusinessKeyValueProvider(businessKeyValueProvider);

        } else {

          CallableElementParameter parameter = parseCallableElementProvider(inElement, elementWithParameters.attribute("id"));

          if (attributeValueEquals(inElement, "local", TRUE)) {
            parameter.setReadLocal(true);
          }

          callableElement.addInput(parameter);
        }
      }
    }
  }

  protected void parseOutputParameter(Element callActivityElement, CallableElement callableElement) {
    Element extensionsElement = callActivityElement.element(EXTENSION_ELEMENTS_TAG);

    if (extensionsElement != null) {
      // output data elements
      for (Element outElement : extensionsElement.elementsNS(OPERATON_BPMN_EXTENSIONS_NS, "out")) {

        CallableElementParameter parameter = parseCallableElementProvider(outElement, callActivityElement.attribute("id"));

        if (attributeValueEquals(outElement, "local", TRUE)) {
          callableElement.addOutputLocal(parameter);
        }
        else {
          callableElement.addOutput(parameter);
        }

      }
    }
  }

  protected boolean attributeValueEquals(Element element, String attribute, String comparisonValue) {
    String value = element.attribute(attribute);

    return comparisonValue.equals(value);
  }

  protected CallableElementParameter parseCallableElementProvider(Element parameterElement, String ancestorElementId) {
    CallableElementParameter parameter = new CallableElementParameter();

    String variables = parameterElement.attribute("variables");

    if (ALL.equals(variables)) {
      parameter.setAllVariables(true);
    } else {
      boolean strictValidation = !Context.getProcessEngineConfiguration().getDisableStrictCallActivityValidation();

      ParameterValueProvider sourceValueProvider = new NullValueProvider();

      String source = parameterElement.attribute("source");
      if (source != null) {
        if (!source.isEmpty()) {
          sourceValueProvider = new ConstantValueProvider(source);
        }
        else {
          if (strictValidation) {
            addError("Empty attribute 'source' when passing variables", parameterElement, ancestorElementId);
          }
          else {
            source = null;
          }
        }
      }

      if (source == null) {
        source = parameterElement.attribute("sourceExpression");

        if (source != null) {
          if (!source.isEmpty()) {
            Expression expression = expressionManager.createExpression(source);
            sourceValueProvider = new ElValueProvider(expression);
          }
          else if (strictValidation) {
            addError("Empty attribute 'sourceExpression' when passing variables", parameterElement, ancestorElementId);
          }
        }
      }

      if (strictValidation && source == null) {
        addError("Missing parameter 'source' or 'sourceExpression' when passing variables", parameterElement, ancestorElementId);
      }

      parameter.setSourceValueProvider(sourceValueProvider);

      String target = parameterElement.attribute("target");
      if ((strictValidation || source != null && !source.isEmpty()) && target == null) {
        addError("Missing attribute 'target' when attribute 'source' or 'sourceExpression' is set", parameterElement, ancestorElementId);
      }
      else if (strictValidation && target != null && target.isEmpty()) {
        addError("Empty attribute 'target' when attribute 'source' or 'sourceExpression' is set", parameterElement, ancestorElementId);
      }
      parameter.setTarget(target);
    }

    return parameter;
  }

  /**
   * Parses the properties of an element (if any) that can contain properties
   * (processes, activities, etc.)
   *
   * <p>
   * Returns true if property subelemens are found.
   * </p>
   *
   * @param element
   *          The element that can contain properties.
   * @param activity
   *          The activity where the property declaration is done.
   */
  public void parseProperties(Element element, ActivityImpl activity) {
    List<Element> propertyElements = element.elements("property");
    for (Element propertyElement : propertyElements) {
      parseProperty(propertyElement, activity);
    }
  }

  /**
   * Parses one property definition.
   *
   * @param propertyElement
   *          The 'property' element that defines how a property looks like and
   *          is handled.
   */
  public void parseProperty(Element propertyElement, ActivityImpl activity) {
    String id = propertyElement.attribute("id");
    String name = propertyElement.attribute("name");

    // If name isn't given, use the id as name
    if (name == null) {
      if (id == null) {
        addError("Invalid property usage on line %s: no id or name specified."
            .formatted(propertyElement.getLine()), propertyElement, activity.getId());
      } else {
        name = id;
      }
    }

    String type = null;
    parsePropertyCustomExtensions(activity, propertyElement, name, type);
  }

  /**
   * Parses the custom extensions for properties.
   *
   * @param activity
   *          The activity where the property declaration is done.
   * @param propertyElement
   *          The 'property' element defining the property.
   * @param propertyName
   *          The name of the property.
   * @param propertyType
   *          The type of the property.
   */
  public void parsePropertyCustomExtensions(ActivityImpl activity, Element propertyElement, String propertyName, String propertyType) {

    if (propertyType == null) {
      String type = propertyElement.attributeNS(OPERATON_BPMN_EXTENSIONS_NS, TYPE);
      propertyType = type != null ? type : "string"; // default is string
    }

    VariableDeclaration variableDeclaration = new VariableDeclaration(propertyName, propertyType);
    addVariableDeclaration(activity, variableDeclaration);
    activity.setScope(true);

    String src = propertyElement.attributeNS(OPERATON_BPMN_EXTENSIONS_NS, "src");
    if (src != null) {
      variableDeclaration.setSourceVariableName(src);
    }

    String srcExpr = propertyElement.attributeNS(OPERATON_BPMN_EXTENSIONS_NS, "srcExpr");
    if (srcExpr != null) {
      Expression sourceExpression = expressionManager.createExpression(srcExpr);
      variableDeclaration.setSourceExpression(sourceExpression);
    }

    String dst = propertyElement.attributeNS(OPERATON_BPMN_EXTENSIONS_NS, "dst");
    if (dst != null) {
      variableDeclaration.setDestinationVariableName(dst);
    }

    String destExpr = propertyElement.attributeNS(OPERATON_BPMN_EXTENSIONS_NS, "dstExpr");
    if (destExpr != null) {
      Expression destinationExpression = expressionManager.createExpression(destExpr);
      variableDeclaration.setDestinationExpression(destinationExpression);
    }

    String link = propertyElement.attributeNS(OPERATON_BPMN_EXTENSIONS_NS, "link");
    if (link != null) {
      variableDeclaration.setLink(link);
    }

    String linkExpr = propertyElement.attributeNS(OPERATON_BPMN_EXTENSIONS_NS, "linkExpr");
    if (linkExpr != null) {
      Expression linkExpression = expressionManager.createExpression(linkExpr);
      variableDeclaration.setLinkExpression(linkExpression);
    }

    for (BpmnParseListener parseListener : parseListeners) {
      parseListener.parseProperty(propertyElement, variableDeclaration, activity);
    }
  }

  /**
   * Parses all sequence flow of a scope.
   *
   * @param processElement
   *          The 'process' element wherein the sequence flow are defined.
   * @param scope
   *          The scope to which the sequence flow must be added.
   * @param compensationHandlers
   */
  public void parseSequenceFlow(Element processElement, ScopeImpl scope, Map<String, Element> compensationHandlers) {
    for (Element sequenceFlowElement : processElement.elements("sequenceFlow")) {

      String id = sequenceFlowElement.attribute("id");
      String sourceRef = sequenceFlowElement.attribute(SOURCE_REF_ATTRIBUTE);
      String destinationRef = sequenceFlowElement.attribute(TARGET_REF_ATTRIBUTE);

      // check if destination is a throwing link event (event source) which mean
      // we have
      // to target the catching link event (event target) here:
      if (eventLinkSources.containsKey(destinationRef)) {
        String linkName = eventLinkSources.get(destinationRef);
        destinationRef = eventLinkTargets.get(linkName);
        if (destinationRef == null) {
          addError("sequence flow points to link event source with name '%s' but no event target with that name exists. Most probably your link events are not configured correctly."
              .formatted(linkName), sequenceFlowElement);
          // we cannot do anything useful now
          return;
        }
        // Reminder: Maybe we should log a warning if we use intermediate link
        // events which are not used?
        // e.g. we have a catching event without the corresponding throwing one.
        // not done for the moment as it does not break executability
      }

      // Implicit check: sequence flow cannot cross (sub) process boundaries: we
      // don't do a processDefinition.findActivity here
      ActivityImpl sourceActivity = scope.findActivityAtLevelOfSubprocess(sourceRef);
      ActivityImpl destinationActivity = scope.findActivityAtLevelOfSubprocess(destinationRef);

      if ((sourceActivity == null && compensationHandlers.containsKey(sourceRef))
          || (sourceActivity != null && sourceActivity.isCompensationHandler())) {
        addError("Invalid outgoing sequence flow of compensation activity '%s'. A compensation activity should not have an incoming or outgoing sequence flow."
                .formatted(sourceRef),
            sequenceFlowElement,
            sourceRef,
            id);
      } else if ((destinationActivity == null && compensationHandlers.containsKey(destinationRef))
          || (destinationActivity != null && destinationActivity.isCompensationHandler())) {
        addError("Invalid incoming sequence flow of compensation activity '%s'. A compensation activity should not have an incoming or outgoing sequence flow."
            .formatted(destinationRef),
            sequenceFlowElement,
            destinationRef,
            id);
      } else if (sourceActivity == null) {
        addError("Invalid source '%s' of sequence flow '%s'".formatted(sourceRef, id), sequenceFlowElement);
      } else if (destinationActivity == null) {
        addError("Invalid destination '%s' of sequence flow '%s'".formatted(destinationRef, id), sequenceFlowElement);
      } else if (sourceActivity.getActivityBehavior() instanceof EventBasedGatewayActivityBehavior) {
        // ignore
      } else if (destinationActivity.getActivityBehavior() instanceof IntermediateCatchEventActivityBehavior && (destinationActivity.getEventScope() != null)
          && (destinationActivity.getEventScope().getActivityBehavior() instanceof EventBasedGatewayActivityBehavior)) {
        addError("Invalid incoming sequenceflow for intermediateCatchEvent with id '%s' connected to an event-based gateway."
                .formatted(destinationActivity.getId()),
            sequenceFlowElement);
      } else if (sourceActivity.getActivityBehavior() instanceof SubProcessActivityBehavior
          && sourceActivity.isTriggeredByEvent()) {
        addError("Invalid outgoing sequence flow of event subprocess", sequenceFlowElement);
      } else if (destinationActivity.getActivityBehavior() instanceof SubProcessActivityBehavior
          && destinationActivity.isTriggeredByEvent()) {
        addError("Invalid incoming sequence flow of event subprocess", sequenceFlowElement);
      }
      else {

        if(getMultiInstanceScope(sourceActivity) != null) {
          sourceActivity = getMultiInstanceScope(sourceActivity);
        }
        if(getMultiInstanceScope(destinationActivity) != null) {
          destinationActivity = getMultiInstanceScope(destinationActivity);
        }

        TransitionImpl transition = sourceActivity.createOutgoingTransition(id);
        sequenceFlows.put(id, transition);
        transition.setProperty("name", sequenceFlowElement.attribute("name"));
        transition.setProperty(PROPERTYNAME_DOCUMENTATION, parseDocumentation(sequenceFlowElement));
        transition.setDestination(destinationActivity);
        parseSequenceFlowConditionExpression(sequenceFlowElement, transition);
        parseExecutionListenersOnTransition(sequenceFlowElement, transition);

        for (BpmnParseListener parseListener : parseListeners) {
          parseListener.parseSequenceFlow(sequenceFlowElement, scope, transition);
        }
      }
    }
  }

  /**
   * Parses a condition expression on a sequence flow.
   *
   * @param seqFlowElement
   *          The 'sequenceFlow' element that can contain a condition.
   * @param seqFlow
   *          The sequenceFlow object representation to which the condition must
   *          be added.
   */
  public void parseSequenceFlowConditionExpression(Element seqFlowElement, TransitionImpl seqFlow) {
    Element conditionExprElement = seqFlowElement.element(CONDITION_EXPRESSION);
    if (conditionExprElement != null) {
      Condition condition = parseConditionExpression(conditionExprElement, seqFlow.getId());
      seqFlow.setProperty(PROPERTYNAME_CONDITION_TEXT, conditionExprElement.getText().trim());
      seqFlow.setProperty(PROPERTYNAME_CONDITION, condition);
    }
  }

  protected Condition parseConditionExpression(Element conditionExprElement, String ancestorElementId) {
    String expression = conditionExprElement.getText().trim();
    String type = conditionExprElement.attributeNS(XSI_NS, TYPE);
    String language = conditionExprElement.attribute(PROPERTYNAME_LANGUAGE);
    String resource = conditionExprElement.attributeNS(OPERATON_BPMN_EXTENSIONS_NS, PROPERTYNAME_RESOURCE);
    if (type != null) {
      String value = type.contains(":") ? resolveName(type) : BpmnParser.BPMN20_NS + ":" + type;
      if (!ATTRIBUTEVALUE_T_FORMAL_EXPRESSION.equals(value)) {
        addError("Invalid type, only tFormalExpression is currently supported", conditionExprElement, ancestorElementId);
      }
    }
    Condition condition = null;
    if (language == null) {
      condition = new UelExpressionCondition(expressionManager.createExpression(expression));
    } else {
      try {
        ExecutableScript script = ScriptUtil.getScript(language, expression, resource, expressionManager);
        condition = new ScriptCondition(script);
      } catch (ProcessEngineException e) {
        addError("Unable to process condition expression:" + e.getMessage(), conditionExprElement, ancestorElementId);
      }
    }
    return condition;
  }

  /**
   * Parses all execution-listeners on a scope.
   *
   * @param scopeElement
   *          the XML element containing the scope definition.
   * @param scope
   *          the scope to add the executionListeners to.
   */
  public void parseExecutionListenersOnScope(Element scopeElement, ScopeImpl scope) {
    Element extentionsElement = scopeElement.element(EXTENSION_ELEMENTS_TAG);
    String scopeElementId = scopeElement.attribute("id");
    if (extentionsElement != null) {
      List<Element> listenerElements = extentionsElement.elementsNS(OPERATON_BPMN_EXTENSIONS_NS, "executionListener");
      for (Element listenerElement : listenerElements) {
        String eventName = listenerElement.attribute("event");
        if (isValidEventNameForScope(eventName, listenerElement, scopeElementId)) {
          ExecutionListener listener = parseExecutionListener(listenerElement, scopeElementId);
          if (listener != null) {
            scope.addListener(eventName, listener);
          }
        }
      }
    }
  }

  /**
   * Check if the given event name is valid. If not, an appropriate error is
   * added.
   */
  protected boolean isValidEventNameForScope(String eventName, Element listenerElement, String ancestorElementId) {
    if (eventName != null && !eventName.trim().isEmpty()) {
      if ("start".equals(eventName) || "end".equals(eventName)) {
        return true;
      } else {
        addError("Attribute 'event' must be one of {start|end}", listenerElement, ancestorElementId);
      }
    } else {
      addError("Attribute 'event' is mandatory on listener", listenerElement, ancestorElementId);
    }
    return false;
  }

  public void parseExecutionListenersOnTransition(Element activitiElement, TransitionImpl activity) {
    Element extensionElements = activitiElement.element(EXTENSION_ELEMENTS_TAG);
    if (extensionElements != null) {
      List<Element> listenerElements = extensionElements.elementsNS(OPERATON_BPMN_EXTENSIONS_NS, "executionListener");
      for (Element listenerElement : listenerElements) {
        ExecutionListener listener = parseExecutionListener(listenerElement, activity.getId());
        if (listener != null) {
          // Since a transition only fires event 'take', we don't parse the
          // event attribute, it is ignored
          activity.addListener(ExecutionListener.EVENTNAME_TAKE, listener);
        }
      }
    }
  }

  /**
   * Parses an {@link ExecutionListener} implementation for the given
   * executionListener element.
   *
   * @param executionListenerElement
   *          the XML element containing the executionListener definition.
   */
  public ExecutionListener parseExecutionListener(Element executionListenerElement, String ancestorElementId) {
    ExecutionListener executionListener = null;

    String className = executionListenerElement.attribute(PROPERTYNAME_CLASS);
    String expression = executionListenerElement.attribute(PROPERTYNAME_EXPRESSION);
    String delegateExpression = executionListenerElement.attribute(PROPERTYNAME_DELEGATE_EXPRESSION);
    Element scriptElement = executionListenerElement.elementNS(OPERATON_BPMN_EXTENSIONS_NS, SCRIPT_TAG);

    if (className != null) {
      if (className.isEmpty()) {
        addError("Attribute 'class' cannot be empty", executionListenerElement, ancestorElementId);
      } else {
        executionListener = new ClassDelegateExecutionListener(className, parseFieldDeclarations(executionListenerElement));
      }
    } else if (expression != null) {
      executionListener = new ExpressionExecutionListener(expressionManager.createExpression(expression));
    } else if (delegateExpression != null) {
      if (delegateExpression.isEmpty()) {
        addError("Attribute 'delegateExpression' cannot be empty", executionListenerElement, ancestorElementId);
      } else {
        executionListener = new DelegateExpressionExecutionListener(expressionManager.createExpression(delegateExpression), parseFieldDeclarations(executionListenerElement));
      }
    } else if (scriptElement != null) {
      try {
        ExecutableScript executableScript = parseOperatonScript(scriptElement);
        if (executableScript != null) {
          executionListener = new ScriptExecutionListener(executableScript);
        }
      } catch (BpmnParseException e) {
        addError(e, ancestorElementId);
      }
    } else {
      addError("Element 'class', 'expression', 'delegateExpression' or 'script' is mandatory on executionListener", executionListenerElement, ancestorElementId);
    }
    return executionListener;
  }

  // Diagram interchange
  // /////////////////////////////////////////////////////////////////

  public void parseDiagramInterchangeElements() {
    // Multiple BPMNDiagram possible
    List<Element> diagrams = rootElement.elementsNS(BPMN_DI_NS, "BPMNDiagram");
    if (!diagrams.isEmpty()) {
      for (Element diagramElement : diagrams) {
        parseBPMNDiagram(diagramElement);
      }
    }
  }

  public void parseBPMNDiagram(Element bpmndiagramElement) {
    // Each BPMNdiagram needs to have exactly one BPMNPlane
    Element bpmnPlane = bpmndiagramElement.elementNS(BPMN_DI_NS, "BPMNPlane");
    if (bpmnPlane != null) {
      parseBPMNPlane(bpmnPlane);
    }
  }

  public void parseBPMNPlane(Element bpmnPlaneElement) {
    String bpmnElement = bpmnPlaneElement.attribute(BPMN_ELEMENT_ATTRIBUTE);
    if (bpmnElement != null && !"".equals(bpmnElement)) {
      // there seems to be only on process without collaboration
      if (getProcessDefinition(bpmnElement) != null) {
        getProcessDefinition(bpmnElement).setGraphicalNotationDefined(true);
      }

      List<Element> shapes = bpmnPlaneElement.elementsNS(BPMN_DI_NS, "BPMNShape");
      for (Element shape : shapes) {
        parseBPMNShape(shape);
      }

      List<Element> edges = bpmnPlaneElement.elementsNS(BPMN_DI_NS, "BPMNEdge");
      for (Element edge : edges) {
        parseBPMNEdge(edge);
      }

    } else {
      addError("'bpmnElement' attribute is required on BPMNPlane ", bpmnPlaneElement);
    }
  }

  public void parseBPMNShape(Element bpmnShapeElement) {
    String bpmnElement = bpmnShapeElement.attribute(BPMN_ELEMENT_ATTRIBUTE);

    if (bpmnElement != null && !"".equals(bpmnElement)) {
      // For collaborations, their are also shape definitions for the
      // participants / processes
      if (participantProcesses.get(bpmnElement) != null) {
        ProcessDefinitionEntity procDef = getProcessDefinition(participantProcesses.get(bpmnElement));
        procDef.setGraphicalNotationDefined(true);

        // The participation that references this process, has a bounds to be
        // rendered + a name as wel
        parseDIBounds(bpmnShapeElement, procDef.getParticipantProcess());
        return;
      }

      for (ProcessDefinitionEntity processDefinition : getProcessDefinitions()) {
        ActivityImpl activity = processDefinition.findActivity(bpmnElement);
        if (activity != null) {
          parseDIBounds(bpmnShapeElement, activity);

          // collapsed or expanded
          String isExpanded = bpmnShapeElement.attribute(PROPERTYNAME_ISEXPANDED);
          if (isExpanded != null) {
            activity.setProperty(PROPERTYNAME_ISEXPANDED, parseBooleanAttribute(isExpanded));
          }
        } else {
          Lane lane = processDefinition.getLaneForId(bpmnElement);

          if (lane != null) {
            // The shape represents a lane
            parseDIBounds(bpmnShapeElement, lane);
          } else if (!elementIds.contains(bpmnElement)) { // It might not be an
                                                          // activity nor a
                                                          // lane, but it might
                                                          // still reference
                                                          // 'something'
            addError("Invalid reference in 'bpmnElement' attribute, activity %s not found".formatted(bpmnElement), bpmnShapeElement);
          }
        }
      }
    } else {
      addError("'bpmnElement' attribute is required on BPMNShape", bpmnShapeElement);
    }
  }

  protected void parseDIBounds(Element bpmnShapeElement, HasDIBounds target) {
    Element bounds = bpmnShapeElement.elementNS(BPMN_DC_NS, "Bounds");
    if (bounds != null) {
      target.setX(parseDoubleAttribute(bpmnShapeElement, "x", bounds.attribute("x"), true).intValue());
      target.setY(parseDoubleAttribute(bpmnShapeElement, "y", bounds.attribute("y"), true).intValue());
      target.setWidth(parseDoubleAttribute(bpmnShapeElement, "width", bounds.attribute("width"), true).intValue());
      target.setHeight(parseDoubleAttribute(bpmnShapeElement, "height", bounds.attribute("height"), true).intValue());
    } else {
      addError("'Bounds' element is required", bpmnShapeElement);
    }
  }

  public void parseBPMNEdge(Element bpmnEdgeElement) {
    String sequenceFlowId = bpmnEdgeElement.attribute(BPMN_ELEMENT_ATTRIBUTE);
    if (sequenceFlowId != null && !"".equals(sequenceFlowId)) {
      if (sequenceFlows != null && sequenceFlows.containsKey(sequenceFlowId)) {

        TransitionImpl sequenceFlow = sequenceFlows.get(sequenceFlowId);
        List<Element> waypointElements = bpmnEdgeElement.elementsNS(OMG_DI_NS, "waypoint");
        if (waypointElements.size() >= 2) {
          List<Integer> waypoints = new ArrayList<>();
          for (Element waypointElement : waypointElements) {
            waypoints.add(parseDoubleAttribute(waypointElement, "x", waypointElement.attribute("x"), true).intValue());
            waypoints.add(parseDoubleAttribute(waypointElement, "y", waypointElement.attribute("y"), true).intValue());
          }
          sequenceFlow.setWaypoints(waypoints);
        } else {
          addError("Minimum 2 waypoint elements must be definted for a 'BPMNEdge'", bpmnEdgeElement);
        }
      } else if (!elementIds.contains(sequenceFlowId)) { // it might not be a
                                                         // sequenceFlow but it
                                                         // might still
                                                         // reference
                                                         // 'something'
        addError("Invalid reference in 'bpmnElement' attribute, sequenceFlow %s not found".formatted(sequenceFlowId), bpmnEdgeElement);
      }
    } else {
      addError("'bpmnElement' attribute is required on BPMNEdge", bpmnEdgeElement);
    }
  }

  // Getters, setters and Parser overridden operations
  // ////////////////////////////////////////

  public List<ProcessDefinitionEntity> getProcessDefinitions() {
    return processDefinitions;
  }

  public ProcessDefinitionEntity getProcessDefinition(String processDefinitionKey) {
    for (ProcessDefinitionEntity processDefinition : processDefinitions) {
      if (processDefinition.getKey().equals(processDefinitionKey)) {
        return processDefinition;
      }
    }
    return null;
  }

  @Override
  public BpmnParse name(String name) {
    super.name(name);
    return this;
  }

  @Override
  public BpmnParse sourceInputStream(InputStream inputStream) {
    super.sourceInputStream(inputStream);
    return this;
  }

  @Override
  public BpmnParse sourceResource(String resource, ClassLoader classLoader) {
    super.sourceResource(resource, classLoader);
    return this;
  }

  @Override
  public BpmnParse sourceResource(String resource) {
    super.sourceResource(resource);
    return this;
  }

  @Override
  public BpmnParse sourceString(String string) {
    super.sourceString(string);
    return this;
  }

  @Override
  public BpmnParse sourceUrl(String url) {
    super.sourceUrl(url);
    return this;
  }

  @Override
  public BpmnParse sourceUrl(URL url) {
    super.sourceUrl(url);
    return this;
  }

  public Boolean parseBooleanAttribute(String booleanText, boolean defaultValue) {
    if (booleanText == null) {
      return defaultValue;
    } else {
      return parseBooleanAttribute(booleanText);
    }
  }

  public Boolean parseBooleanAttribute(String booleanText) {
    if (TRUE.equals(booleanText) || "enabled".equals(booleanText) || "on".equals(booleanText) || "active".equals(booleanText) || "yes".equals(booleanText)) {
      return Boolean.TRUE;
    }
    if (FALSE.equals(booleanText) || "disabled".equals(booleanText) || "off".equals(booleanText) || "inactive".equals(booleanText)
        || "no".equals(booleanText)) {
      return Boolean.FALSE;
    }
    return null;
  }

  public Double parseDoubleAttribute(Element element, String attributeName, String doubleText, boolean required) {
    if (required && (doubleText == null || "".equals(doubleText))) {
      addError(attributeName + " is required", element);
    } else {
      try {
        return Double.parseDouble(doubleText);
      } catch (NumberFormatException e) {
        addError("Cannot parse %s: %s".formatted(attributeName, e.getMessage()), element);
      }
    }
    return -1.0;
  }

  protected boolean isStartable(Element element) {
    return TRUE.equalsIgnoreCase(element.attributeNS(OPERATON_BPMN_EXTENSIONS_NS, "isStartableInTasklist", TRUE));
  }

  protected boolean isExclusive(Element element) {
    return TRUE.equals(element.attributeNS(OPERATON_BPMN_EXTENSIONS_NS, "exclusive", String.valueOf(DEFAULT_EXCLUSIVE)));
  }

  protected boolean isAsyncBefore(Element element) {
    return TRUE.equals(element.attributeNS(OPERATON_BPMN_EXTENSIONS_NS, "async"))
        || TRUE.equals(element.attributeNS(OPERATON_BPMN_EXTENSIONS_NS, "asyncBefore"));
  }

  protected boolean isAsyncAfter(Element element) {
    return TRUE.equals(element.attributeNS(OPERATON_BPMN_EXTENSIONS_NS, "asyncAfter"));
  }

  protected boolean isServiceTaskLike(Element element) {

    return element != null && (
          element.attributeNS(OPERATON_BPMN_EXTENSIONS_NS, PROPERTYNAME_CLASS) != null
        || element.attributeNS(OPERATON_BPMN_EXTENSIONS_NS, PROPERTYNAME_EXPRESSION) != null
        || element.attributeNS(OPERATON_BPMN_EXTENSIONS_NS, PROPERTYNAME_DELEGATE_EXPRESSION) != null
        || element.attributeNS(OPERATON_BPMN_EXTENSIONS_NS, TYPE) != null
        || hasConnector(element));
  }

  protected boolean hasConnector(Element element) {
    Element extensionElements = element.element(EXTENSION_ELEMENTS_TAG);
    return extensionElements != null && extensionElements.element("connector") != null;
  }

  public Map<String, List<JobDeclaration<?, ?>>> getJobDeclarations() {
    return jobDeclarations;
  }

  public List<JobDeclaration<?, ?>> getJobDeclarationsByKey(String processDefinitionKey) {
    return jobDeclarations.get(processDefinitionKey);
  }

  // IoMappings ////////////////////////////////////////////////////////

  protected void parseActivityInputOutput(Element activityElement, ActivityImpl activity) {
    Element extensionElements = activityElement.element(EXTENSION_ELEMENTS_TAG);
    if (extensionElements != null) {
      IoMapping inputOutput = null;
      try {
        inputOutput = parseInputOutput(extensionElements);
      } catch (BpmnParseException e) {
        addError(e, activity.getId());
      }

      if (inputOutput != null) {
        if (checkActivityInputOutputSupported(activityElement, activity, inputOutput)) {

          activity.setIoMapping(inputOutput);

          if (getMultiInstanceScope(activity) == null) {
            // turn activity into a scope (->local, isolated scope for
            // variables) unless it is a multi instance activity, in that case
            // this
            // is not necessary because:
            // A scope is already created for the multi instance body which
            // isolates the local variables from other executions in the same
            // scope, and
            // * parallel: the individual concurrent executions are isolated
            // even if they are not scope themselves
            // * sequential: after each iteration local variables are purged
            activity.setScope(true);
          }
        }

        for (BpmnParseListener parseListener : parseListeners) {
          parseListener.parseIoMapping(extensionElements, activity, inputOutput);
        }
      }
    }
  }

  protected boolean checkActivityInputOutputSupported(Element activityElement, ActivityImpl activity, IoMapping inputOutput) {
    String tagName = activityElement.getTagName();

    if (!(tagName.toLowerCase().contains("task")
        || tagName.contains("Event")
        || TRANSACTION_TAG.equals(tagName)
        || SUB_PROCESS_TAG.equals(tagName)
        || "callActivity".equals(tagName))) {
      addError("operaton:inputOutput mapping unsupported for element type '%s'.".formatted(tagName), activityElement);
      return false;
    }

    if (SUB_PROCESS_TAG.equals(tagName) && TRUE.equals(activityElement.attribute("triggeredByEvent"))) {
      addError("operaton:inputOutput mapping unsupported for element type '%s' with attribute 'triggeredByEvent = true'.".formatted(tagName), activityElement);
      return false;
    }

    if (!inputOutput.getOutputParameters().isEmpty()) {
      return checkActivityOutputParameterSupported(activityElement, activity);
    } else {
      return true;
    }
  }

  protected boolean checkActivityOutputParameterSupported(Element activityElement, ActivityImpl activity) {
    String tagName = activityElement.getTagName();

    if ("endEvent".equals(tagName)) {
      addError("operaton:outputParameter not allowed for element type '%s'.".formatted(tagName), activityElement);
      return true;
    } else if (getMultiInstanceScope(activity) != null) {
      addError("operaton:outputParameter not allowed for multi-instance constructs", activityElement);
      return false;
    } else {
      return true;
    }
  }

  protected void ensureNoIoMappingDefined(Element element) {
    Element inputOutput = findOperatonExtensionElement(element, "inputOutput");
    if (inputOutput != null) {
      addError("operaton:inputOutput mapping unsupported for element type '%s'.".formatted(element.getTagName()), element);
    }
  }

  protected ParameterValueProvider createParameterValueProvider(Object value, ExpressionManager expressionManager) {
    if (value == null) {
      return new NullValueProvider();

    } else if (value instanceof String string) {
      Expression expression = expressionManager.createExpression(string);
      return new ElValueProvider(expression);

    } else {
      return new ConstantValueProvider(value);
    }
  }

  protected void addTimeCycleWarning(Element timeCycleElement, String type, String timerElementId) {
    String warning = "It is not recommended to use a %s timer event with a time cycle.".formatted(type);
    addWarning(warning, timeCycleElement, timerElementId);
  }

  protected void ensureNoExpressionInMessageStartEvent(Element element,
                                                       EventSubscriptionDeclaration messageStartEventSubscriptionDeclaration,
                                                       String parentElementId) {
    boolean eventNameContainsExpression = false;
    if(messageStartEventSubscriptionDeclaration.hasEventName()) {
      eventNameContainsExpression = !messageStartEventSubscriptionDeclaration.isEventNameLiteralText();
    }
    if (eventNameContainsExpression) {
      String messageStartName = messageStartEventSubscriptionDeclaration.getUnresolvedEventName();
      addError("Invalid message name '%s' for element '%s': expressions in the message start event name are not allowed!"
          .formatted(messageStartName, element.getTagName()), element, parentElementId);
    }
  }

}

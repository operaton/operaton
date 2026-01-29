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
package org.operaton.bpm.engine.impl;

import java.net.URI;

import org.operaton.bpm.application.impl.ProcessApplicationLogger;
import org.operaton.bpm.container.impl.ContainerIntegrationLogger;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.impl.bpmn.behavior.BpmnBehaviorLogger;
import org.operaton.bpm.engine.impl.bpmn.parser.BpmnParseLogger;
import org.operaton.bpm.engine.impl.cfg.ConfigurationLogger;
import org.operaton.bpm.engine.impl.cfg.TransactionLogger;
import org.operaton.bpm.engine.impl.cmd.CommandLogger;
import org.operaton.bpm.engine.impl.cmmn.behavior.CmmnBehaviorLogger;
import org.operaton.bpm.engine.impl.cmmn.operation.CmmnOperationLogger;
import org.operaton.bpm.engine.impl.cmmn.transformer.CmmnTransformerLogger;
import org.operaton.bpm.engine.impl.core.CoreLogger;
import org.operaton.bpm.engine.impl.db.EnginePersistenceLogger;
import org.operaton.bpm.engine.impl.digest.SecurityLogger;
import org.operaton.bpm.engine.impl.dmn.DecisionLogger;
import org.operaton.bpm.engine.impl.externaltask.ExternalTaskLogger;
import org.operaton.bpm.engine.impl.identity.IndentityLogger;
import org.operaton.bpm.engine.impl.incident.IncidentLogger;
import org.operaton.bpm.engine.impl.interceptor.ContextLogger;
import org.operaton.bpm.engine.impl.jobexecutor.JobExecutorLogger;
import org.operaton.bpm.engine.impl.metrics.MetricsLogger;
import org.operaton.bpm.engine.impl.migration.MigrationLogger;
import org.operaton.bpm.engine.impl.persistence.entity.JobEntity;
import org.operaton.bpm.engine.impl.plugin.AdministratorAuthorizationPluginLogger;
import org.operaton.bpm.engine.impl.pvm.PvmLogger;
import org.operaton.bpm.engine.impl.scripting.ScriptLogger;
import org.operaton.bpm.engine.impl.test.TestLogger;
import org.operaton.bpm.engine.impl.util.EngineUtilLogger;
import org.operaton.commons.logging.BaseLogger;

/**
 * @author Stefan Hentschel.
 */
public class ProcessEngineLogger extends BaseLogger {

  public static final String PROJECT_CODE = "ENGINE";

  public static final ProcessEngineLogger INSTANCE = BaseLogger.createLogger(
      ProcessEngineLogger.class, PROJECT_CODE, "org.operaton.bpm.engine", "00");

  public static final BpmnParseLogger BPMN_PARSE_LOGGER = BaseLogger.createLogger(
      BpmnParseLogger.class, PROJECT_CODE, "org.operaton.bpm.engine.bpmn.parser", "01");

  public static final BpmnBehaviorLogger BPMN_BEHAVIOR_LOGGER = BaseLogger.createLogger(
      BpmnBehaviorLogger.class, PROJECT_CODE, "org.operaton.bpm.engine.bpmn.behavior", "02");

  public static final EnginePersistenceLogger PERSISTENCE_LOGGER = BaseLogger.createLogger(
      EnginePersistenceLogger.class, PROJECT_CODE, "org.operaton.bpm.engine.persistence", "03");

  public static final CmmnTransformerLogger CMMN_TRANSFORMER_LOGGER = BaseLogger.createLogger(
      CmmnTransformerLogger.class, PROJECT_CODE, "org.operaton.bpm.engine.cmmn.transformer", "04");

  public static final CmmnBehaviorLogger CMNN_BEHAVIOR_LOGGER = BaseLogger.createLogger(
      CmmnBehaviorLogger.class, PROJECT_CODE, "org.operaton.bpm.engine.cmmn.behavior", "05");

  public static final CmmnOperationLogger CMMN_OPERATION_LOGGER = BaseLogger.createLogger(
      CmmnOperationLogger.class, PROJECT_CODE, "org.operaton.bpm.engine.cmmn.operation", "06");

  public static final ProcessApplicationLogger PROCESS_APPLICATION_LOGGER = BaseLogger.createLogger(
      ProcessApplicationLogger.class, PROJECT_CODE, "org.operaton.bpm.application", "07");

  public static final ContainerIntegrationLogger CONTAINER_INTEGRATION_LOGGER = BaseLogger.createLogger(
      ContainerIntegrationLogger.class, PROJECT_CODE, "org.operaton.bpm.container", "08");

  public static final EngineUtilLogger UTIL_LOGGER = BaseLogger.createLogger(
      EngineUtilLogger.class, PROJECT_CODE, "org.operaton.bpm.engine.util", "09");

  public static final TransactionLogger TX_LOGGER = BaseLogger.createLogger(
      TransactionLogger.class, PROJECT_CODE, "org.operaton.bpm.engine.tx", "11");

  public static final ConfigurationLogger CONFIG_LOGGER = BaseLogger.createLogger(
      ConfigurationLogger.class, PROJECT_CODE, "org.operaton.bpm.engine.cfg", "12");

  public static final CommandLogger CMD_LOGGER = BaseLogger.createLogger(
      CommandLogger.class, PROJECT_CODE, "org.operaton.bpm.engine.cmd", "13");

  public static final JobExecutorLogger JOB_EXECUTOR_LOGGER = BaseLogger.createLogger(
      JobExecutorLogger.class, PROJECT_CODE, "org.operaton.bpm.engine.jobexecutor", "14");

  public static final TestLogger TEST_LOGGER = BaseLogger.createLogger(
      TestLogger.class, PROJECT_CODE, "org.operaton.bpm.engine.test", "15");

  public static final ContextLogger CONTEXT_LOGGER = BaseLogger.createLogger(
      ContextLogger.class, PROJECT_CODE, "org.operaton.bpm.engine.context", "16");

  public static final CoreLogger CORE_LOGGER = BaseLogger.createLogger(
      CoreLogger.class, PROJECT_CODE, "org.operaton.bpm.engine.core", "17");

  public static final MetricsLogger METRICS_LOGGER = BaseLogger.createLogger(
      MetricsLogger.class, PROJECT_CODE, "org.operaton.bpm.engine.metrics", "18");

  public static final AdministratorAuthorizationPluginLogger ADMIN_PLUGIN_LOGGER = BaseLogger.createLogger(
      AdministratorAuthorizationPluginLogger.class, PROJECT_CODE, "org.operaton.bpm.engine.plugin.admin", "19");

  public static final PvmLogger PVM_LOGGER = BaseLogger.createLogger(
      PvmLogger.class, PROJECT_CODE, "org.operaton.bpm.engine.pvm", "20");

  public static final ScriptLogger SCRIPT_LOGGER = BaseLogger.createLogger(
      ScriptLogger.class, PROJECT_CODE, "org.operaton.bpm.engine.script", "21");

  public static final DecisionLogger DECISION_LOGGER = BaseLogger.createLogger(
      DecisionLogger.class, PROJECT_CODE, "org.operaton.bpm.engine.dmn", "22");

  public static final MigrationLogger MIGRATION_LOGGER = BaseLogger.createLogger(
      MigrationLogger.class, PROJECT_CODE, "org.operaton.bpm.engine.migration", "23");

  public static final ExternalTaskLogger EXTERNAL_TASK_LOGGER = BaseLogger.createLogger(
      ExternalTaskLogger.class, PROJECT_CODE, "org.operaton.bpm.engine.externaltask", "24");

  public static final SecurityLogger SECURITY_LOGGER = BaseLogger.createLogger(
      SecurityLogger.class, PROJECT_CODE, "org.operaton.bpm.engine.security", "25");

  public static final IncidentLogger INCIDENT_LOGGER = BaseLogger.createLogger(
      IncidentLogger.class, PROJECT_CODE, "org.operaton.bpm.engine.incident", "26");

  public static final IndentityLogger INDENTITY_LOGGER = BaseLogger.createLogger(
      IndentityLogger.class, PROJECT_CODE, "org.operaton.bpm.engine.identity", "27");
  // Use code 29 for the next logger. Skip 28 as it's previously used for telemetry feature that is removed from the code base now

  public static boolean shouldLogJobException(ProcessEngineConfiguration processEngineConfiguration, JobEntity currentJob) {
    boolean enableReducedJobExceptionLogging = processEngineConfiguration.isEnableReducedJobExceptionLogging();
    return currentJob == null || !enableReducedJobExceptionLogging || enableReducedJobExceptionLogging && currentJob.getRetries() <= 1;
  }

  public static boolean shouldLogCmdException(ProcessEngineConfiguration processEngineConfiguration) {
    return processEngineConfiguration.isEnableCmdExceptionLogging();
  }

  public void processEngineCreated(String name) {
    logInfo("001", "Process Engine {} created.", name);
  }

  public void processEngineAlreadyInitialized() {
    logInfo("002", "Process engine already initialized");
  }

  public void initializingProcessEngineForResource(URI resourceUri) {
    logInfo(
        "003", "Initializing process engine for resource {}", resourceUri);
  }

  public void initializingProcessEngine(String name) {
    logInfo(
        "004", "Initializing process engine {}", name);
  }

  public void exceptionWhileInitializingProcessengine(Throwable e) {
    logError("005", "Exception while initializing process engine {}", e.getMessage(), e);
  }

  public void exceptionWhileClosingProcessEngine(String string, Exception e) {
    logError("006", "Exception while closing process engine {}", string, e);
  }

  public void processEngineClosed(String name) {
    logInfo("007", "Process Engine {} closed", name);
  }

  public void historyCleanupJobReconfigurationFailure(Exception exception) {
    logInfo(
      "008",
      "History Cleanup Job reconfiguration failed on Process Engine Bootstrap. Possible concurrent execution with the JobExecutor: {}",
      exception.getMessage()
    );
  }

  public void couldNotDetermineIp(Exception e) {
    logWarn(
        "009", "Could not determine local IP address for generating a host name", e);
  }

}


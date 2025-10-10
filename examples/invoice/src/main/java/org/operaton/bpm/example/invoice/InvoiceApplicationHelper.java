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
package org.operaton.bpm.example.invoice;

import java.io.InputStream;
import java.util.Calendar;
import java.util.List;
import org.slf4j.Logger;

import org.operaton.bpm.application.ProcessApplicationReference;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.authorization.Groups;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.impl.util.IoUtil;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.repository.ProcessDefinitionQuery;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.slf4j.LoggerFactory;

import static org.operaton.bpm.engine.variable.Variables.createVariables;
import static org.operaton.bpm.engine.variable.Variables.fileValue;

public final class InvoiceApplicationHelper {

  private static final Logger LOGGER = LoggerFactory.getLogger(InvoiceApplicationHelper.class);

  private static final String PROCDEFKEY_INVOICE = "invoice";
  private static final String RESOURCE_INVOICE_PDF = "invoice.pdf";

  private static final String VAR_CREDITOR = "creditor";
  private static final String VAR_AMOUNT = "amount";
  private static final String VAR_INVOICE_CATEGORY = "invoiceCategory";
  private static final String VAR_INVOICE_NUMBER = "invoiceNumber";
  private static final String VAR_INVOICE_DOCUMENT = "invoiceDocument";
  public static final String MIME_TYPE_APPLICATION_PDF = "application/pdf";

  private InvoiceApplicationHelper() {
  }

  public static void startFirstProcess(ProcessEngine processEngine) {
    createUsers(processEngine);

    //enable metric reporting
    ProcessEngineConfigurationImpl processEngineConfiguration = (ProcessEngineConfigurationImpl) processEngine.getProcessEngineConfiguration();
    processEngineConfiguration.setDbMetricsReporterActivate(true);
    processEngineConfiguration.getDbMetricsReporter().setReporterId("REPORTER");

    startInvoiceProcessInstances(processEngine, 1);
    startInvoiceProcessInstances(processEngine, null);

    //disable reporting
    processEngineConfiguration.setDbMetricsReporterActivate(false);
  }

  public static void createDeployment(ProcessEngine processEngine, ClassLoader classLoader, ProcessApplicationReference applicationReference) {
    // Hack: deploy the first version of the invoice process once before the process application
    //   is deployed the first time
    if (processEngine != null) {

      RepositoryService repositoryService = processEngine.getRepositoryService();

      if (!isInvoiceProcessDeployed(repositoryService)) {
        repositoryService.createDeployment(applicationReference)
          .addInputStream("invoice.v1.bpmn", classLoader.getResourceAsStream("invoice.v1.bpmn"))
          .addInputStream("invoiceBusinessDecisions.dmn", classLoader.getResourceAsStream("invoiceBusinessDecisions.dmn"))
          .addInputStream("reviewInvoice.bpmn", classLoader.getResourceAsStream("reviewInvoice.bpmn"))
          .deploy();
      }
    }
  }

  private static boolean isInvoiceProcessDeployed(RepositoryService repositoryService) {
    return repositoryService.createProcessDefinitionQuery().processDefinitionKey(PROCDEFKEY_INVOICE).count() > 0;
  }

  private static void startInvoiceProcessInstances(ProcessEngine processEngine, Integer version) {

    ProcessEngineConfigurationImpl processEngineConfiguration = (ProcessEngineConfigurationImpl) processEngine.getProcessEngineConfiguration();
    ProcessDefinitionQuery processDefinitionQuery = processEngine
      .getRepositoryService()
      .createProcessDefinitionQuery()
      .processDefinitionKey(PROCDEFKEY_INVOICE);

    if (version != null) {
      processDefinitionQuery.processDefinitionVersion(version);
    }
    else {
      processDefinitionQuery.latestVersion();
    }

    ProcessDefinition processDefinition = processDefinitionQuery.singleResult();

    InputStream invoiceInputStream = InvoiceApplicationHelper.class.getClassLoader().getResourceAsStream(
      RESOURCE_INVOICE_PDF);

    long numberOfRunningProcessInstances = processEngine.getRuntimeService().createProcessInstanceQuery().processDefinitionId(processDefinition.getId()).count();

    if (numberOfRunningProcessInstances == 0) { // start three process instances

      LOGGER.info("Start 3 instances of {}, version {}", processDefinition.getName(), processDefinition.getVersion());
      // process instance 1
      processEngine.getRuntimeService().startProcessInstanceById(processDefinition.getId(), createVariables()
          .putValue(VAR_CREDITOR, "Great Pizza for Everyone Inc.")
          .putValue(VAR_AMOUNT, 30.00d)
          .putValue(VAR_INVOICE_CATEGORY, "Travel Expenses")
          .putValue(VAR_INVOICE_NUMBER, "GPFE-23232323")
          .putValue(VAR_INVOICE_DOCUMENT, fileValue(RESOURCE_INVOICE_PDF)
              .file(invoiceInputStream)
              .mimeType(MIME_TYPE_APPLICATION_PDF)
              .create()));

      IoUtil.closeSilently(invoiceInputStream);
      invoiceInputStream = InvoiceApplicationHelper.class.getClassLoader().getResourceAsStream(RESOURCE_INVOICE_PDF);
      processEngineConfiguration.getDbMetricsReporter().reportNow();

      // process instance 2
      try {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, -14);
        ClockUtil.setCurrentTime(calendar.getTime());

        ProcessInstance pi = processEngine.getRuntimeService().startProcessInstanceById(processDefinition.getId(), createVariables()
            .putValue(VAR_CREDITOR, "Bobby's Office Supplies")
            .putValue(VAR_AMOUNT, 900.00d)
            .putValue(VAR_INVOICE_CATEGORY, "Misc")
            .putValue(VAR_INVOICE_NUMBER, "BOS-43934")
            .putValue(VAR_INVOICE_DOCUMENT, fileValue(RESOURCE_INVOICE_PDF)
                .file(invoiceInputStream)
                .mimeType(MIME_TYPE_APPLICATION_PDF)
                .create()));

        processEngineConfiguration.getDbMetricsReporter().reportNow();
        calendar.add(Calendar.DAY_OF_MONTH, 14);
        ClockUtil.setCurrentTime(calendar.getTime());

        processEngine.getIdentityService().setAuthentication("demo", List.of(Groups.OPERATON_ADMIN));
        Task task = processEngine.getTaskService().createTaskQuery().processInstanceId(pi.getId()).singleResult();
        processEngine.getTaskService().claim(task.getId(), "demo");
        processEngine.getTaskService().complete(task.getId(), createVariables().putValue("approved", true));
      }
      finally{
        processEngineConfiguration.getDbMetricsReporter().reportNow();
        ClockUtil.reset();
        processEngine.getIdentityService().clearAuthentication();
      }

      IoUtil.closeSilently(invoiceInputStream);
      invoiceInputStream = InvoiceApplicationHelper.class.getClassLoader().getResourceAsStream(RESOURCE_INVOICE_PDF);

      // process instance 3
      try {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, -5);
        ClockUtil.setCurrentTime(calendar.getTime());

        ProcessInstance pi = processEngine.getRuntimeService().startProcessInstanceById(processDefinition.getId(), createVariables()
            .putValue(VAR_CREDITOR, "Papa Steve's all you can eat")
            .putValue(VAR_AMOUNT, 10.99d)
            .putValue(VAR_INVOICE_CATEGORY, "Travel Expenses")
            .putValue(VAR_INVOICE_NUMBER, "PSACE-5342")
            .putValue(VAR_INVOICE_DOCUMENT, fileValue(RESOURCE_INVOICE_PDF)
                .file(invoiceInputStream)
                .mimeType(MIME_TYPE_APPLICATION_PDF)
                .create()));

        processEngineConfiguration.getDbMetricsReporter().reportNow();
        calendar.add(Calendar.DAY_OF_MONTH, 5);
        ClockUtil.setCurrentTime(calendar.getTime());

        processEngine.getIdentityService().setAuthenticatedUserId("mary");
        Task task = processEngine.getTaskService().createTaskQuery().processInstanceId(pi.getId()).singleResult();
        processEngine.getTaskService().createComment(null, pi.getId(), "I cannot approve this invoice: the amount is missing.\n\n Could you please provide the amount?");
        processEngine.getTaskService().complete(task.getId(), createVariables().putValue("approved", false));
      }
      finally{
        processEngineConfiguration.getDbMetricsReporter().reportNow();
        ClockUtil.reset();
        processEngine.getIdentityService().clearAuthentication();
      }
    } else {
      LOGGER.info("No new instances of {} version {} started, there are {} instances running",
          processDefinition.getName(), processDefinition.getVersion(), numberOfRunningProcessInstances);
    }
  }

  private static void createUsers(ProcessEngine processEngine) {
    // create demo users
    new DemoDataGenerator().createUsers(processEngine);
  }
}

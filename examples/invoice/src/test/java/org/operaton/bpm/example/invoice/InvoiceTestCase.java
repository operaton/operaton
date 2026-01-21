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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.IdentityLink;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.Variables;

import static org.operaton.bpm.engine.impl.test.ProcessEngineAssert.assertProcessEnded;
import static org.operaton.bpm.engine.variable.Variables.fileValue;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(ProcessEngineExtension.class)
class InvoiceTestCase {
  ProcessEngine processEngine;
  RuntimeService runtimeService;
  TaskService taskService;
  ManagementService managementService;

  @Deployment(resources = {"invoice.v1.bpmn", "invoiceBusinessDecisions.dmn"})
  @Test
  void testHappyPathV1() {
    InputStream invoiceInputStream = InvoiceProcessApplication.class.getClassLoader().getResourceAsStream("invoice.pdf");
    VariableMap variables = Variables.createVariables()
      .putValue("creditor", "Great Pizza for Everyone Inc.")
      .putValue("amount", 300.0d)
      .putValue("invoiceCategory", "Travel Expenses")
      .putValue("invoiceNumber", "GPFE-23232323")
      .putValue("invoiceDocument", fileValue("invoice.pdf")
        .file(invoiceInputStream)
        .mimeType("application/pdf")
        .create());

    ProcessInstance pi = runtimeService.startProcessInstanceByKey("invoice", variables);

    Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();
    assertThat(task.getTaskDefinitionKey()).isEqualTo("approveInvoice");

    List<IdentityLink> links = taskService.getIdentityLinksForTask(task.getId());
    Set<String> approverGroups = new HashSet<>();
    for (IdentityLink link : links) {
      approverGroups.add(link.getGroupId());
    }
    assertThat(approverGroups).containsExactly("accounting", "sales");

    variables.clear();
    variables.put("approved", Boolean.TRUE);
    taskService.complete(task.getId(), variables);

    task = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();

    assertThat(task.getTaskDefinitionKey()).isEqualTo("prepareBankTransfer");
    taskService.complete(task.getId());

    Job archiveInvoiceJob = managementService.createJobQuery().singleResult();
    assertThat(archiveInvoiceJob).isNotNull();
    managementService.executeJob(archiveInvoiceJob.getId());

    assertProcessEnded(processEngine, pi.getId());
  }

  @Deployment(resources = {"invoice.v2.bpmn", "invoiceBusinessDecisions.dmn"})
  @Test
  void testHappyPathV2() {
    InputStream invoiceInputStream = InvoiceProcessApplication.class.getClassLoader().getResourceAsStream("invoice.pdf");
    VariableMap variables = Variables.createVariables()
      .putValue("creditor", "Great Pizza for Everyone Inc.")
      .putValue("amount", 300.0d)
      .putValue("invoiceCategory", "Travel Expenses")
      .putValue("invoiceNumber", "GPFE-23232323")
      .putValue("invoiceDocument", fileValue("invoice.pdf")
        .file(invoiceInputStream)
        .mimeType("application/pdf")
        .create());

    ProcessInstance pi = runtimeService.startProcessInstanceByKey("invoice", variables);

    Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();
    assertThat(task.getTaskDefinitionKey()).isEqualTo("approveInvoice");

    List<IdentityLink> links = taskService.getIdentityLinksForTask(task.getId());
    Set<String> approverGroups = new HashSet<>();
    for (IdentityLink link : links) {
      approverGroups.add(link.getGroupId());
    }
    assertThat(approverGroups).containsExactly("accounting", "sales");

    variables.clear();
    variables.put("approved", Boolean.TRUE);
    taskService.complete(task.getId(), variables);

    task = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();

    assertThat(task.getTaskDefinitionKey()).isEqualTo("prepareBankTransfer");
    taskService.complete(task.getId());

    Job archiveInvoiceJob = managementService.createJobQuery().singleResult();
    assertThat(archiveInvoiceJob).isNotNull();
    managementService.executeJob(archiveInvoiceJob.getId());

    assertProcessEnded(processEngine, pi.getId());
  }

  @Deployment(resources = {"invoice.v2.bpmn", "invoiceBusinessDecisions.dmn"})
  @Test
  void testApproveInvoiceAssignment() {
    InputStream invoiceInputStream = InvoiceProcessApplication.class.getClassLoader().getResourceAsStream("invoice.pdf");

    VariableMap variables = Variables.createVariables()
      .putValue("creditor", "Great Pizza for Everyone Inc.")
      .putValue("amount", 300.0d)
      .putValue("invoiceCategory", "Travel Expenses")
      .putValue("invoiceNumber", "GPFE-23232323")
      .putValue("invoiceDocument", fileValue("invoice.pdf")
        .file(invoiceInputStream)
        .mimeType("application/pdf")
        .create())
      .putValue("approverGroups", List.of("sales", "accounting"));

    ProcessInstance pi = runtimeService.createProcessInstanceByKey("invoice")
      .setVariables(variables)
      .startBeforeActivity("approveInvoice")
      .execute();

    // given that the process instance is waiting at task "approveInvoice"
    Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();
    assertThat(task.getTaskDefinitionKey()).isEqualTo("approveInvoice");

    // and task has candidate groups
    List<IdentityLink> links = taskService.getIdentityLinksForTask(task.getId());
    Set<String> approverGroups = new HashSet<>();
    for (IdentityLink link : links) {
      approverGroups.add(link.getGroupId());
    }
    assertThat(approverGroups).containsExactly("accounting", "sales");

    // and variable approver is null
    assertThat(taskService.getVariable(task.getId(), "approver")).isNull();

    // if mary claims the task
    taskService.claim(task.getId(), "mary");

    // then the variable "approver" exists and is set to mary
    assertThat(taskService.getVariable(task.getId(), "approver")).isEqualTo("mary");
  }

  @Deployment(resources = {"invoice.v2.bpmn", "reviewInvoice.bpmn", "invoiceBusinessDecisions.dmn"})
  @Test
  void testNonSuccessfulPath() {
    InputStream invoiceInputStream = InvoiceProcessApplication.class.getClassLoader().getResourceAsStream("invoice.pdf");
    VariableMap variables = Variables.createVariables()
      .putValue("creditor", "Great Pizza for Everyone Inc.")
      .putValue("amount", 300.0d)
      .putValue("invoiceCategory", "Travel Expenses")
      .putValue("invoiceNumber", "GPFE-23232323")
      .putValue("invoiceDocument", fileValue("invoice.pdf")
        .file(invoiceInputStream)
        .mimeType("application/pdf")
        .create());

    ProcessInstance pi = runtimeService.startProcessInstanceByKey("invoice", variables);

    Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();
    assertThat(task.getTaskDefinitionKey()).isEqualTo("approveInvoice");

    List<IdentityLink> links = taskService.getIdentityLinksForTask(task.getId());
    Set<String> approverGroups = new HashSet<>();
    for (IdentityLink link : links) {
      approverGroups.add(link.getGroupId());
    }
    assertThat(approverGroups).containsExactly("accounting", "sales");

    variables.clear();
    variables.put("approved", Boolean.FALSE);
    taskService.complete(task.getId(), variables);

    task = taskService.createTaskQuery().singleResult();

    assertThat(task.getTaskDefinitionKey()).isEqualTo("assignReviewer");
    variables.clear();
    variables.put("reviewer", "peter");
    taskService.complete(task.getId(), variables);

    task = taskService.createTaskQuery().singleResult();

    assertThat(task.getTaskDefinitionKey()).isEqualTo("reviewInvoice");
    variables.clear();
    variables.put("clarified", Boolean.FALSE);
    taskService.complete(task.getId(), variables);

    assertProcessEnded(processEngine, task.getProcessInstanceId());
    assertProcessEnded(processEngine, pi.getId());
  }

}



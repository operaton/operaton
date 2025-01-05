/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.engine.test.api.runtime;

import java.util.Comparator;
import java.util.Date;
import java.util.List;

import junit.framework.TestCase;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.batch.Batch;
import org.operaton.bpm.engine.batch.BatchStatistics;
import org.operaton.bpm.engine.batch.history.HistoricBatch;
import org.operaton.bpm.engine.externaltask.ExternalTask;
import org.operaton.bpm.engine.history.HistoricActivityInstance;
import org.operaton.bpm.engine.history.HistoricCaseActivityInstance;
import org.operaton.bpm.engine.history.HistoricDecisionInstance;
import org.operaton.bpm.engine.history.HistoricDetail;
import org.operaton.bpm.engine.history.HistoricExternalTaskLog;
import org.operaton.bpm.engine.history.HistoricIncident;
import org.operaton.bpm.engine.history.HistoricJobLog;
import org.operaton.bpm.engine.history.HistoricProcessInstance;
import org.operaton.bpm.engine.history.HistoricTaskInstance;
import org.operaton.bpm.engine.history.HistoricVariableInstance;
import org.operaton.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.operaton.bpm.engine.impl.persistence.entity.HistoricJobLogEventEntity;
import org.operaton.bpm.engine.management.SchemaLogEntry;
import org.operaton.bpm.engine.query.Query;
import org.operaton.bpm.engine.repository.CaseDefinition;
import org.operaton.bpm.engine.repository.DecisionDefinition;
import org.operaton.bpm.engine.repository.Deployment;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.CaseExecution;
import org.operaton.bpm.engine.runtime.Execution;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;

/**
 * This class provides utils to verify the sorting of queries of engine entities.
 * Assuming we sort over a property x, there are two valid orderings when some entities
 * have values where x = null: Either, these values precede the overall list, or they trail it.
 * Thus, this class does not use regular comparators but a {@link NullTolerantComparator}
 * that can be used to assert a list of entites in both ways.
 *
 * @author Thorben Lindhauer
 *
 */
public class TestOrderingUtil {

  // EXECUTION

  public static NullTolerantComparator<Execution> executionByProcessInstanceId() {
    return propertyComparator(obj -> obj.getProcessInstanceId());
  }

  public static NullTolerantComparator<Execution> executionByProcessDefinitionId() {
    return propertyComparator(obj -> ((ExecutionEntity) obj).getProcessDefinitionId());
  }

  public static NullTolerantComparator<Execution> executionByProcessDefinitionKey(ProcessEngine processEngine) {
    final RuntimeService runtimeService = processEngine.getRuntimeService();
    final RepositoryService repositoryService = processEngine.getRepositoryService();

    return propertyComparator(obj -> {
      ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
          .processInstanceId(obj.getProcessInstanceId()).singleResult();
      ProcessDefinition processDefinition = repositoryService.getProcessDefinition(processInstance.getProcessDefinitionId());
      return processDefinition.getKey();
    });
  }

  //PROCESS INSTANCE

  public static NullTolerantComparator<ProcessInstance> processInstanceByProcessInstanceId() {
    return propertyComparator(obj -> obj.getProcessInstanceId());
  }

  public static NullTolerantComparator<ProcessInstance> processInstanceByProcessDefinitionId() {
    return propertyComparator(obj -> obj.getProcessDefinitionId());
  }

  public static NullTolerantComparator<ProcessInstance> processInstanceByBusinessKey() {
    return propertyComparator(obj -> obj.getBusinessKey());
  }

  // PROCESS DEFINITION

  public static NullTolerantComparator<ProcessDefinition> processDefinitionByDeployTime(ProcessEngine processEngine){
    RepositoryService repositoryService = processEngine.getRepositoryService();
    return propertyComparator(obj -> {
      Deployment deployment = repositoryService.createDeploymentQuery().deploymentId(obj.getDeploymentId()).singleResult();
      return deployment.getDeploymentTime();
    });
  }

  // DECISION DEFINITION

  public static NullTolerantComparator<DecisionDefinition> decisionDefinitionByDeployTime(ProcessEngine processEngine) {
    RepositoryService repositoryService = processEngine.getRepositoryService();
    return propertyComparator(obj -> {
      Deployment deployment = repositoryService.createDeploymentQuery().deploymentId(obj.getDeploymentId()).singleResult();
      return deployment.getDeploymentTime();
    });
  }

  //HISTORIC PROCESS INSTANCE

  public static NullTolerantComparator<HistoricProcessInstance> historicProcessInstanceByProcessDefinitionId() {
    return propertyComparator(obj -> obj.getProcessDefinitionId());
  }

  public static NullTolerantComparator<HistoricProcessInstance> historicProcessInstanceByProcessDefinitionKey() {
    return propertyComparator(obj -> obj.getProcessDefinitionKey());
  }

  public static NullTolerantComparator<HistoricProcessInstance> historicProcessInstanceByProcessDefinitionName() {
    return propertyComparator(obj -> obj.getProcessDefinitionName());
  }

  public static NullTolerantComparator<HistoricProcessInstance> historicProcessInstanceByProcessDefinitionVersion() {
    return propertyComparator(obj -> obj.getProcessDefinitionVersion());
  }

  public static NullTolerantComparator<HistoricProcessInstance> historicProcessInstanceByProcessInstanceId() {
    return propertyComparator(obj -> obj.getId());
  }

  // CASE EXECUTION

  public static NullTolerantComparator<CaseExecution> caseExecutionByDefinitionId() {
    return propertyComparator(obj -> obj.getCaseDefinitionId());
  }

  public static NullTolerantComparator<CaseExecution> caseExecutionByDefinitionKey(ProcessEngine processEngine) {
    final RepositoryService repositoryService = processEngine.getRepositoryService();
    return propertyComparator(obj -> {
      CaseDefinition caseDefinition = repositoryService.getCaseDefinition(obj.getCaseDefinitionId());
      return caseDefinition.getKey();
    });
  }

  public static NullTolerantComparator<CaseExecution> caseExecutionById() {
    return propertyComparator(obj -> obj.getId());
  }

  // TASK

  public static NullTolerantComparator<Task> taskById() {
    return propertyComparator(obj -> obj.getId());
  }

  public static NullTolerantComparator<Task> taskByName() {
    return propertyComparator(obj -> obj.getName());
  }

  public static NullTolerantComparator<Task> taskByPriority() {
    return propertyComparator(obj -> obj.getPriority());
  }

  public static NullTolerantComparator<Task> taskByAssignee() {
    return propertyComparator(obj -> obj.getAssignee());
  }

  public static NullTolerantComparator<Task> taskByDescription() {
    return propertyComparator(obj -> obj.getDescription());
  }

  public static NullTolerantComparator<Task> taskByProcessInstanceId() {
    return propertyComparator(obj -> obj.getProcessInstanceId());
  }

  public static NullTolerantComparator<Task> taskByExecutionId() {
    return propertyComparator(obj -> obj.getExecutionId());
  }

  public static NullTolerantComparator<Task> taskByCreateTime() {
    return propertyComparator(obj -> obj.getCreateTime());
  }

  public static NullTolerantComparator<Task> taskByDueDate() {
    return propertyComparator(obj -> obj.getDueDate());
  }

  public static NullTolerantComparator<Task> taskByFollowUpDate() {
    return propertyComparator(obj -> obj.getFollowUpDate());
  }

  public static NullTolerantComparator<Task> taskByCaseInstanceId() {
    return propertyComparator(obj -> obj.getCaseInstanceId());
  }

  public static NullTolerantComparator<Task> taskByCaseExecutionId() {
    return propertyComparator(obj -> obj.getCaseExecutionId());
  }

  // HISTORIC JOB LOG

  public static NullTolerantComparator<HistoricJobLog> historicJobLogByTimestamp() {
    return propertyComparator(obj -> obj.getTimestamp());
  }

  public static NullTolerantComparator<HistoricJobLog> historicJobLogByJobId() {
    return propertyComparator(obj -> obj.getJobId());
  }

  public static NullTolerantComparator<HistoricJobLog> historicJobLogByJobDefinitionId() {
    return propertyComparator(obj -> obj.getJobDefinitionId());
  }

  public static NullTolerantComparator<HistoricJobLog> historicJobLogByJobDueDate() {
    return propertyComparator(obj -> obj.getJobDueDate());
  }

  public static NullTolerantComparator<HistoricJobLog> historicJobLogByJobRetries() {
    return propertyComparator(obj -> obj.getJobRetries());
  }

  public static NullTolerantComparator<HistoricJobLog> historicJobLogByActivityId() {
    return propertyComparator(obj -> obj.getActivityId());
  }

  public static NullTolerantComparator<HistoricJobLog> historicJobLogByExecutionId() {
    return propertyComparator(obj -> obj.getExecutionId());
  }

  public static NullTolerantComparator<HistoricJobLog> historicJobLogByProcessInstanceId() {
    return propertyComparator(obj -> obj.getProcessInstanceId());
  }

  public static NullTolerantComparator<HistoricJobLog> historicJobLogByProcessDefinitionId() {
    return propertyComparator(obj -> obj.getProcessDefinitionId());
  }

  public static NullTolerantComparator<HistoricJobLog> historicJobLogByProcessDefinitionKey(ProcessEngine processEngine) {
    final RepositoryService repositoryService = processEngine.getRepositoryService();

    return propertyComparator(obj -> {
      ProcessDefinition processDefinition = repositoryService.getProcessDefinition(obj.getProcessDefinitionId());
      return processDefinition.getKey();
    });
  }

  public static NullTolerantComparator<HistoricJobLog> historicJobLogByDeploymentId() {
    return propertyComparator(obj -> obj.getDeploymentId());
  }

  public static NullTolerantComparator<HistoricJobLog> historicJobLogByJobPriority() {
    return propertyComparator(obj -> obj.getJobPriority());
  }

  public static NullTolerantComparator<HistoricJobLog> historicJobLogPartiallyByOccurence() {
    return propertyComparator(obj -> ((HistoricJobLogEventEntity) obj).getSequenceCounter());
  }

  public static NullTolerantComparator<HistoricJobLog> historicJobLogByTenantId() {
    return propertyComparator(obj -> obj.getTenantId());
  }

  // jobs

  public static NullTolerantComparator<Job> jobByPriority() {
    return propertyComparator(obj -> obj.getPriority());
  }

  // external task

  public static NullTolerantComparator<ExternalTask> externalTaskById() {
    return propertyComparator(obj -> obj.getId());
  }

  public static NullTolerantComparator<ExternalTask> externalTaskByProcessInstanceId() {
    return propertyComparator(obj -> obj.getProcessInstanceId());
  }

  public static NullTolerantComparator<ExternalTask> externalTaskByProcessDefinitionId() {
    return propertyComparator(obj -> obj.getProcessDefinitionId());
  }

  public static NullTolerantComparator<ExternalTask> externalTaskByProcessDefinitionKey() {
    return propertyComparator(obj -> obj.getProcessDefinitionKey());
  }

  public static NullTolerantComparator<ExternalTask> externalTaskByLockExpirationTime() {
    return propertyComparator(obj -> obj.getLockExpirationTime());
  }

  public static NullTolerantComparator<ExternalTask> externalTaskByPriority() {
    return propertyComparator(obj -> obj.getPriority());
  }

  // batch

  public static NullTolerantComparator<Batch> batchById() {
    return propertyComparator(obj -> obj.getId());
  }

  public static NullTolerantComparator<Batch> batchByTenantId() {
    return propertyComparator(obj -> obj.getTenantId());
  }

  public static NullTolerantComparator<HistoricBatch> historicBatchById() {
    return propertyComparator(obj -> obj.getId());
  }

  public static NullTolerantComparator<HistoricBatch> historicBatchByTenantId() {
    return propertyComparator(obj -> obj.getTenantId());
  }

  public static NullTolerantComparator<HistoricBatch> historicBatchByStartTime() {
    return propertyComparator(obj -> obj.getStartTime());
  }

  public static NullTolerantComparator<HistoricBatch> historicBatchByEndTime() {
    return propertyComparator(obj -> obj.getEndTime());
  }

  public static NullTolerantComparator<BatchStatistics> batchStatisticsById() {
    return propertyComparator(obj -> obj.getId());
  }

  public static NullTolerantComparator<BatchStatistics> batchStatisticsByTenantId() {
    return propertyComparator(obj -> obj.getTenantId());
  }

  public static NullTolerantComparator<BatchStatistics> batchStatisticsByStartTime() {
    return propertyComparator(Batch::getStartTime);
  }

  // HISTORIC EXTERNAL TASK LOG

  public static NullTolerantComparator<HistoricExternalTaskLog> historicExternalTaskByTimestamp() {
    return propertyComparator(obj -> obj.getTimestamp());
  }

  public static NullTolerantComparator<HistoricExternalTaskLog> historicExternalTaskLogByExternalTaskId() {
    return propertyComparator(obj -> obj.getExternalTaskId());
  }

  public static NullTolerantComparator<HistoricExternalTaskLog> historicExternalTaskLogByRetries() {
    return propertyComparator(obj -> obj.getRetries());
  }

  public static NullTolerantComparator<HistoricExternalTaskLog> historicExternalTaskLogByPriority() {
    return propertyComparator(obj -> obj.getPriority());
  }

  public static NullTolerantComparator<HistoricExternalTaskLog> historicExternalTaskLogByTopicName() {
    return propertyComparator(obj -> obj.getTopicName());
  }

  public static NullTolerantComparator<HistoricExternalTaskLog> historicExternalTaskLogByWorkerId() {
    return propertyComparator(obj -> obj.getWorkerId());
  }

  public static NullTolerantComparator<HistoricExternalTaskLog> historicExternalTaskLogByActivityId() {
    return propertyComparator(obj -> obj.getActivityId());
  }

  public static NullTolerantComparator<HistoricExternalTaskLog> historicExternalTaskLogByActivityInstanceId() {
    return propertyComparator(obj -> obj.getActivityInstanceId());
  }

  public static NullTolerantComparator<HistoricExternalTaskLog> historicExternalTaskLogByExecutionId() {
    return propertyComparator(obj -> obj.getExecutionId());
  }

  public static NullTolerantComparator<HistoricExternalTaskLog> historicExternalTaskLogByProcessInstanceId() {
    return propertyComparator(obj -> obj.getProcessInstanceId());
  }

  public static NullTolerantComparator<HistoricExternalTaskLog> historicExternalTaskLogByProcessDefinitionId() {
    return propertyComparator(obj -> obj.getProcessDefinitionId());
  }

  public static NullTolerantComparator<HistoricExternalTaskLog> historicExternalTaskLogByProcessDefinitionKey(ProcessEngine processEngine) {
    final RepositoryService repositoryService = processEngine.getRepositoryService();

    return propertyComparator(obj -> {
      ProcessDefinition processDefinition = repositoryService.getProcessDefinition(obj.getProcessDefinitionId());
      return processDefinition.getKey();
    });
  }

  // HISTORIC ENTITIES

  public static NullTolerantComparator<HistoricActivityInstance> historicActivityInstanceByTenantId() {
    return propertyComparator(obj -> obj.getTenantId());
  }

  public static NullTolerantComparator<HistoricIncident> historicIncidentByTenantId() {
    return propertyComparator(obj -> obj.getTenantId());
  }

  public static NullTolerantComparator<HistoricDecisionInstance> historicDecisionInstanceByTenantId() {
    return propertyComparator(obj -> obj.getTenantId());
  }

  public static NullTolerantComparator<HistoricDetail> historicDetailByTenantId() {
    return propertyComparator(obj -> obj.getTenantId());
  }

  public static NullTolerantComparator<HistoricTaskInstance> historicTaskInstanceByTenantId() {
    return propertyComparator(obj -> obj.getTenantId());
  }

  public static NullTolerantComparator<HistoricVariableInstance> historicVariableInstanceByTenantId() {
    return propertyComparator(obj -> obj.getTenantId());
  }

  public static NullTolerantComparator<HistoricCaseActivityInstance> historicCaseActivityInstanceByTenantId() {
    return propertyComparator(obj -> obj.getTenantId());
  }

  public static NullTolerantComparator<HistoricExternalTaskLog> historicExternalTaskLogByTenantId() {
    return propertyComparator(obj -> obj.getTenantId());
  }

  // SCHEMA LOG
  public static NullTolerantComparator<SchemaLogEntry> schemaLogEntryByTimestamp() {
    return propertyComparator(obj -> obj.getTimestamp());
  }

  // general

  public static <T, P extends Comparable<P>> NullTolerantComparator<T> propertyComparator(
      final PropertyAccessor<T, P> accessor) {
    return new NullTolerantComparator<>() {

      @Override
      public int compare(T o1, T o2) {
        P prop1 = accessor.getProperty(o1);
        P prop2 = accessor.getProperty(o2);

        return prop1.compareTo(prop2);
      }

      @Override
      public boolean hasNullProperty(T object) {
        return accessor.getProperty(object) == null;
      }
    };
  }

  protected interface PropertyAccessor<T, P extends Comparable<P>> {
    P getProperty(T obj);
  }


  public static <T> NullTolerantComparator<T> inverted(final NullTolerantComparator<T> comparator) {
    return new NullTolerantComparator<>() {
      @Override
      public int compare(T o1, T o2) {
        return - comparator.compare(o1, o2);
      }

      @Override
      public boolean hasNullProperty(T object) {
        return comparator.hasNullProperty(object);
      }
    };
  }


  public static <T> NullTolerantComparator<T> hierarchical(final NullTolerantComparator<T> baseComparator,
      final NullTolerantComparator<T>... minorOrderings) {
    return new NullTolerantComparator<>() {
      @Override
      public int compare(T o1, T o2, boolean nullPrecedes) {
        int comparison = baseComparator.compare(o1, o2, nullPrecedes);

        int i = 0;
        while (comparison == 0 && i < minorOrderings.length) {
          NullTolerantComparator<T> comparator = minorOrderings[i];
          comparison = comparator.compare(o1, o2, nullPrecedes);
          i++;
        }

        return comparison;
      }

      @Override
      public int compare(T o1, T o2) {
        throw new UnsupportedOperationException();
      }

      @Override
      public boolean hasNullProperty(T object) {
        throw new UnsupportedOperationException();
      }
    };
  }

  public abstract static class NullTolerantComparator<T> implements Comparator<T> {

    public int compare(T o1, T o2, boolean nullPrecedes) {
      boolean o1Null = hasNullProperty(o1);
      boolean o2Null = hasNullProperty(o2);

      if (o1Null) {
        if (o2Null) {
          return 0;
        } else {
          if (nullPrecedes) {
            return -1;
          } else {
            return 1;
          }
        }
      } else {

        if (o2Null) {
          if (nullPrecedes) {
            return 1;
          } else {
            return -1;
          }
        }
      }

      return compare(o1, o2);
    }

    public abstract boolean hasNullProperty(T object);
  }

  public static <T> void verifySorting(List<T> actualElements, NullTolerantComparator<T> expectedOrdering) {
    // check two orderings: one in which values with null properties are at the front of the list
    boolean leadingNullOrdering = orderingConsistent(actualElements, expectedOrdering, true);

    if (leadingNullOrdering) {
      return;
    }

    // and one where the values with null properties are at the end of the list
    boolean trailingNullOrdering = orderingConsistent(actualElements, expectedOrdering, false);
    TestCase.assertTrue("Ordering not consistent with comparator", trailingNullOrdering);
  }

  public static <T> boolean orderingConsistent(List<T> actualElements, NullTolerantComparator<T> expectedOrdering, boolean nullPrecedes) {
    for (int i = 0; i < actualElements.size() - 1; i++) {
      T currentExecution = actualElements.get(i);
      T nextExecution = actualElements.get(i + 1);

      int comparison = expectedOrdering.compare(currentExecution, nextExecution, nullPrecedes);
      if (comparison > 0) {
        return false;
      }
    }

    return true;
  }

  public static <T> void verifySortingAndCount(Query<?, T> query, int expectedCount, NullTolerantComparator<T> expectedOrdering) {
    List<T> elements = query.list();
    TestCase.assertEquals(expectedCount, elements.size());

    verifySorting(elements, expectedOrdering);
  }

}

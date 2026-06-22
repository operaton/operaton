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
package org.operaton.bpm.engine.impl.task;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.delegate.Expression;
import org.operaton.bpm.engine.delegate.VariableScope;
import org.operaton.bpm.engine.impl.calendar.BusinessCalendar;
import org.operaton.bpm.engine.impl.calendar.DueDateBusinessCalendar;
import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.impl.el.ExpressionManager;
import org.operaton.bpm.engine.impl.persistence.entity.TaskEntity;

/**
 * @author Roman Smirnov
 *
 */
public class TaskDecorator {

  protected TaskDefinition taskDefinition;
  protected ExpressionManager expressionManager;

  public TaskDecorator(TaskDefinition taskDefinition, ExpressionManager expressionManager) {
    this.taskDefinition = taskDefinition;
    this.expressionManager = expressionManager;
  }

  public void decorate(TaskEntity task, VariableScope variableScope) {
    // set the taskDefinition
    task.setTaskDefinition(taskDefinition);

    // name
    initializeTaskName(task, variableScope);
    // description
    initializeTaskDescription(task, variableScope);
    // dueDate
    initializeTaskDueDate(task, variableScope);
    // followUpDate
    initializeTaskFollowUpDate(task, variableScope);
    // priority
    initializeTaskPriority(task, variableScope);
    // assignments
    initializeTaskAssignments(task, variableScope);
  }

  protected void initializeTaskName(TaskEntity task, VariableScope variableScope) {
    Expression nameExpression = taskDefinition.getNameExpression();
    if (nameExpression != null) {
      String name = (String) nameExpression.getValue(variableScope);
      task.setName(name);
    }
  }

  protected void initializeTaskDescription(TaskEntity task, VariableScope variableScope) {
    Expression descriptionExpression = taskDefinition.getDescriptionExpression();
    if (descriptionExpression != null) {
      String description = (String) descriptionExpression.getValue(variableScope);
      task.setDescription(description);
    }
  }

  protected void initializeTaskDueDate(TaskEntity task, VariableScope variableScope) {
    Expression dueDateExpression = taskDefinition.getDueDateExpression();
    if(dueDateExpression != null) {
      Object dueDate = dueDateExpression.getValue(variableScope);
      if(dueDate != null) {
        if (dueDate instanceof Date date) {
          task.setDueDate(date);

        } else if (dueDate instanceof String string) {
          BusinessCalendar businessCalendar = getBusinessCalender();
          task.setDueDate(businessCalendar.resolveDuedate(string, task));
        } else {
          throw new ProcessEngineException("Due date expression does not resolve to a Date or Date string: %s".formatted(
              dueDateExpression.getExpressionText()));
        }
      }
    }
  }

  protected void initializeTaskFollowUpDate(TaskEntity task, VariableScope variableScope) {
    Expression followUpDateExpression = taskDefinition.getFollowUpDateExpression();
    if(followUpDateExpression != null) {
      Object followUpDate = followUpDateExpression.getValue(variableScope);
      if(followUpDate != null) {
        if (followUpDate instanceof Date date) {
          task.setFollowUpDate(date);

        } else if (followUpDate instanceof String string) {
          BusinessCalendar businessCalendar = getBusinessCalender();
          task.setFollowUpDate(businessCalendar.resolveDuedate(string, task));

        } else {
          throw new ProcessEngineException("Follow up date expression does not resolve to a Date or Date string: %s".formatted(
              followUpDateExpression.getExpressionText()));
        }
      }
    }
  }

  protected void initializeTaskPriority(TaskEntity task, VariableScope variableScope) {
    Expression priorityExpression = taskDefinition.getPriorityExpression();
    if (priorityExpression != null) {
      Object priority = priorityExpression.getValue(variableScope);

      if (priority != null) {
        if (priority instanceof String string) {
          try {
            task.setPriority(Integer.parseInt(string));

          } catch (NumberFormatException e) {
            throw new ProcessEngineException("Priority does not resolve to a number: %s".formatted(priority), e);
          }
        } else if (priority instanceof Number number) {
          task.setPriority(number.intValue());

        } else {
          throw new ProcessEngineException("Priority expression does not resolve to a number: %s".formatted(
                  priorityExpression.getExpressionText()));
        }
      }
    }
  }

  protected void initializeTaskAssignments(TaskEntity task, VariableScope variableScope) {
    // assignee
    initializeTaskAssignee(task, variableScope);
    // candidateUsers
    initializeTaskCandidateUsers(task, variableScope);
    // candidateGroups
    initializeTaskCandidateGroups(task, variableScope);
  }

  protected void initializeTaskAssignee(TaskEntity task, VariableScope variableScope) {
    Expression assigneeExpression = taskDefinition.getAssigneeExpression();
    if (assigneeExpression != null) {
      task.setAssignee((String) assigneeExpression.getValue(variableScope));
    }
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  protected void initializeTaskCandidateGroups(TaskEntity task, VariableScope variableScope) {
    Set<Expression> candidateGroupIdExpressions = taskDefinition.getCandidateGroupIdExpressions();

    for (Expression groupIdExpr : candidateGroupIdExpressions) {
      Object value = groupIdExpr.getValue(variableScope);

      if (value instanceof String string) {
        List<String> candiates = extractCandidates(string);
        task.addCandidateGroups(candiates);

      } else if (value instanceof Collection collection) {
        task.addCandidateGroups(collection);

      } else {
        throw new ProcessEngineException("Expression did not resolve to a string or collection of strings");
      }
    }
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  protected void initializeTaskCandidateUsers(TaskEntity task, VariableScope variableScope) {
    Set<Expression> candidateUserIdExpressions = taskDefinition.getCandidateUserIdExpressions();
    for (Expression userIdExpr : candidateUserIdExpressions) {
      Object value = userIdExpr.getValue(variableScope);

      if (value instanceof String string) {
        List<String> candiates = extractCandidates(string);
        task.addCandidateUsers(candiates);

      } else if (value instanceof Collection collection) {
        task.addCandidateUsers(collection);

      } else {
        throw new ProcessEngineException("Expression did not resolve to a string or collection of strings");
      }
    }
  }


  /**
   * Extract a candidate list from a string.
   */
  protected List<String> extractCandidates(String str) {
    return Arrays.asList(str.split("[\\s]*,[\\s]*"));
  }

  // getters ///////////////////////////////////////////////////////////////

  public TaskDefinition getTaskDefinition() {
    return taskDefinition;
  }

  public ExpressionManager getExpressionManager() {
    return expressionManager;
  }

  protected BusinessCalendar getBusinessCalender() {
    return Context
        .getProcessEngineConfiguration()
        .getBusinessCalendarManager()
        .getBusinessCalendar(DueDateBusinessCalendar.NAME);
  }

}

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
package org.operaton.bpm.engine.history;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.query.Query;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.runtime.ProcessInstanceQuery;

/**
 * Allows programmatic querying of {@link HistoricProcessInstance}s.
 *
 * @author Tom Baeyens
 * @author Joram Barrez
 * @author Falko Menge
 */
public interface HistoricProcessInstanceQuery extends Query<HistoricProcessInstanceQuery, HistoricProcessInstance> {

  /**
   * Only select historic process instances with the given process instance.
   * {@link ProcessInstance} ids and {@link HistoricProcessInstance} ids match.
   *
   * @return HistoricProcessInstanceQuery A modified query with applied filter
   */
  HistoricProcessInstanceQuery processInstanceId(String processInstanceId);

  /**
   * Only select historic process instances whose id is in the given set of ids.
   * {@link ProcessInstance} ids and {@link HistoricProcessInstance} ids match.
   *
   * @return HistoricProcessInstanceQuery A modified query with applied filter
   */
  HistoricProcessInstanceQuery processInstanceIds(Set<String> processInstanceIds);

  /**
   * Only select historic process instances whose id is not in the given set of ids.
   * {@link ProcessInstance} ids and {@link HistoricProcessInstance} ids match.
   */
  default HistoricProcessInstanceQuery processInstanceIdNotIn(String... processInstanceIdNotIn) {
    return this;
  }

  /**
   * Only select historic process instances for the given process definition
   *
   * @return HistoricProcessInstanceQuery A modified query with applied filter
   */
  HistoricProcessInstanceQuery processDefinitionId(String processDefinitionId);

  /**
   * Only select historic process instances that are defined by a process
   * definition with the given key.
   *
   * @return HistoricProcessInstanceQuery A modified query with applied filter
   */
  HistoricProcessInstanceQuery processDefinitionKey(String processDefinitionKey);

  /**
   * Only select historic process instances that are defined by any given process
   * definition key.
   *
   * @return HistoricProcessInstanceQuery A modified query with applied filter
   */
  HistoricProcessInstanceQuery processDefinitionKeyIn(String... processDefinitionKeys);

  /**
   * Only select historic process instances that don't have a process-definition of which the key is present in the given list
   *
   * @return HistoricProcessInstanceQuery A modified query with applied filter
   */
  HistoricProcessInstanceQuery processDefinitionKeyNotIn(List<String> processDefinitionKeys);

  /**
   * Only select historic process instances that are defined by a process
   * definition with the given name.
   *
   * @return HistoricProcessInstanceQuery A modified query with applied filter
   */
  HistoricProcessInstanceQuery processDefinitionName(String processDefinitionName);

  /**
   * Only select historic process instances that are defined by process definition which name
   * is like the given value.
   *
   * @param nameLike The string can include the wildcard character '%' to express
   *                 like-strategy: starts with (string%), ends with (%string) or contains (%string%).
   * @return HistoricProcessInstanceQuery A modified query with applied filter
   */
  HistoricProcessInstanceQuery processDefinitionNameLike(String nameLike);

  /**
   * Only select historic process instances with the given business key
   * @return HistoricProcessInstanceQuery A modified query with applied filter
   */
  HistoricProcessInstanceQuery processInstanceBusinessKey(String processInstanceBusinessKey);

  /**
   * Only select historic process instances whose business key is in the given set.
   * @return HistoricProcessInstanceQuery A modified query with applied filter
   */
  HistoricProcessInstanceQuery processInstanceBusinessKeyIn(String... processInstanceBusinessKeyIn);

  /**
   * Only select historic process instances which had a business key like the given value.
   *
   * @param processInstanceBusinessKeyLike The string can include the wildcard character '%' to express
   *                                       like-strategy: starts with (string%), ends with (%string) or contains (%string%).
   * @return HistoricProcessInstanceQuery A modified query with applied filter
   */
  HistoricProcessInstanceQuery processInstanceBusinessKeyLike(String processInstanceBusinessKeyLike);

  /**
   * Only select historic process instances that are completely finished.
   * @return HistoricProcessInstanceQuery A modified query with applied filter
   */
  HistoricProcessInstanceQuery finished();

  /**
   * Only select historic process instance that are not yet finished.
   * @return HistoricProcessInstanceQuery A modified query with applied filter
   */
  HistoricProcessInstanceQuery unfinished();

  /**
   * Only select historic process instances with incidents
   *
   * @return HistoricProcessInstanceQuery A modified query with applied filter
   */
  HistoricProcessInstanceQuery withIncidents();

  /**
   * Only select historic process instances with root incidents
   *
   * @return HistoricProcessInstanceQuery A modified query with applied filter
   */
  HistoricProcessInstanceQuery withRootIncidents();

  /**
   * Only select historic process instances that have incidents with given ids.
   * @return HistoricProcessInstanceQuery A modified query with applied filter
   */
  HistoricProcessInstanceQuery incidentIdIn(String... incidentIds);

  /**
   * Only select historic process instances with incident status either 'open' or 'resolved'.
   * To get all process instances with incidents, use {@link HistoricProcessInstanceQuery#withIncidents()}.
   *
   * @param status indicates the incident status, which is either 'open' or 'resolved'
   * @return HistoricProcessInstanceQuery A modified query with applied filter
   */
  HistoricProcessInstanceQuery incidentStatus(String status);

  /**
   * Only selects process instances with the given incident type.
   * @return HistoricProcessInstanceQuery A modified query with applied filter
   */
  HistoricProcessInstanceQuery incidentType(String incidentType);

  /**
   * Only select historic process instances with the given incident message.
   *
   * @param incidentMessage Incidents Message for which the historic process instances should be selected
   * @return HistoricProcessInstanceQuery A modified query with applied filter
   */
  HistoricProcessInstanceQuery incidentMessage(String incidentMessage);

  /**
   * Only select historic process instances which had an incident message like the given value.
   *
   * @param incidentMessageLike The string can include the wildcard character '%' to express
   *                            like-strategy: starts with (string%), ends with (%string) or contains (%string%).
   * @return HistoricProcessInstanceQuery A modified query with applied filter
   */
  HistoricProcessInstanceQuery incidentMessageLike(String incidentMessageLike);

  /**
   * Only select historic process instances which are associated with jobs that have exceptions and retries left.
   *
   * @return HistoricProcessInstanceQuery A modified query with applied filter
   */
  default HistoricProcessInstanceQuery withJobsRetrying() {
    return this;
  }

  /**
   * Only select historic process instances which are associated with the given case instance id.
   * @return HistoricProcessInstanceQuery A modified query with applied filter
   */
  HistoricProcessInstanceQuery caseInstanceId(String caseInstanceId);

  /**
   * The query will match the names of variables in a case-insensitive way.
   */
  HistoricProcessInstanceQuery matchVariableNamesIgnoreCase();

  /**
   * The query will match the values of variables in a case-insensitive way.
   * @return HistoricProcessInstanceQuery A modified query with applied filter
   */
  HistoricProcessInstanceQuery matchVariableValuesIgnoreCase();

  /**
   * Only select process instances which had a global variable with the given value
   * when they ended. Only select process instances which have a variable value
   * greater than the passed value. The type only applies to already ended
   * process instances, otherwise use a {@link ProcessInstanceQuery} instead! The type of the
   * variable is determined based on the value, using types configured in
   * {@link ProcessEngineConfigurationImpl#getVariableSerializers()}. Byte-arrays and
   * {@link Serializable} objects (which are not primitive type wrappers) are
   * not supported.
   *
   * @param name of the variable, cannot be null.
   * @return HistoricProcessInstanceQuery A modified query with applied filter
   */
  HistoricProcessInstanceQuery variableValueEquals(String name, Object value);

  /**
   * Only select process instances which had a global variable with the given name, but
   * with a different value than the passed value when they ended. Only select
   * process instances which have a variable value greater than the passed
   * value. Byte-arrays and {@link Serializable} objects (which are not
   * primitive type wrappers) are not supported.
   *
   * @param name of the variable, cannot be null.
   * @return HistoricProcessInstanceQuery A modified query with applied filter
   */
  HistoricProcessInstanceQuery variableValueNotEquals(String name, Object value);

  /**
   * Only select process instances which had a global variable value greater than the
   * passed value when they ended. Booleans, Byte-arrays and
   * {@link Serializable} objects (which are not primitive type wrappers) are
   * not supported. Only select process instances which have a variable value
   * greater than the passed value.
   *
   * @param name  cannot be null.
   * @param value cannot be null.
   * @return HistoricProcessInstanceQuery A modified query with applied filter
   */
  HistoricProcessInstanceQuery variableValueGreaterThan(String name, Object value);

  /**
   * Only select process instances which had a global variable value greater than or
   * equal to the passed value when they ended. Booleans, Byte-arrays and
   * {@link Serializable} objects (which are not primitive type wrappers) are
   * not supported. Only applies to already ended process instances, otherwise
   * use a {@link ProcessInstanceQuery} instead!
   *
   * @param name  cannot be null.
   * @param value cannot be null.
   * @return HistoricProcessInstanceQuery A modified query with applied filter
   */
  HistoricProcessInstanceQuery variableValueGreaterThanOrEqual(String name, Object value);

  /**
   * Only select process instances which had a global variable value less than the
   * passed value when the ended. Only applies to already ended process
   * instances, otherwise use a {@link ProcessInstanceQuery} instead! Booleans,
   * Byte-arrays and {@link Serializable} objects (which are not primitive type
   * wrappers) are not supported.
   *
   * @param name  cannot be null.
   * @param value cannot be null.
   * @return HistoricProcessInstanceQuery A modified query with applied filter
   */
  HistoricProcessInstanceQuery variableValueLessThan(String name, Object value);

  /**
   * Only select process instances which has a global variable value less than or equal
   * to the passed value when they ended. Only applies to already ended process
   * instances, otherwise use a {@link ProcessInstanceQuery} instead! Booleans,
   * Byte-arrays and {@link Serializable} objects (which are not primitive type
   * wrappers) are not supported.
   *
   * @param name  cannot be null.
   * @param value cannot be null.
   * @return HistoricProcessInstanceQuery A modified query with applied filter
   */
  HistoricProcessInstanceQuery variableValueLessThanOrEqual(String name, Object value);

  /**
   * Only select process instances which had global variable value like the given value
   * when they ended. Only applies to already ended process instances, otherwise
   * use a {@link ProcessInstanceQuery} instead! This can be used on string
   * variables only.
   *
   * @param name  cannot be null.
   * @param value cannot be null. The string can include the
   *              wildcard character '%' to express like-strategy: starts with
   *              (string%), ends with (%string) or contains (%string%).
   * @return HistoricProcessInstanceQuery A modified query with applied filter
   */
  HistoricProcessInstanceQuery variableValueLike(String name, String value);

  /**
   * Only select historic process instances that were started before the given date.
   * @return HistoricProcessInstanceQuery A modified query with applied filter
   */
  HistoricProcessInstanceQuery startedBefore(Date date);

  /**
   * Only select historic process instances that were started after the given date.
   * @return HistoricProcessInstanceQuery A modified query with applied filter
   */
  HistoricProcessInstanceQuery startedAfter(Date date);

  /**
   * Only select historic process instances that were started before the given date.
   * @return HistoricProcessInstanceQuery A modified query with applied filter
   */
  HistoricProcessInstanceQuery finishedBefore(Date date);

  /**
   * Only select historic process instances that were started after the given date.
   * @return HistoricProcessInstanceQuery A modified query with applied filter
   */
  HistoricProcessInstanceQuery finishedAfter(Date date);

  /**
   * Only select historic process instance that are started by the given user.
   * @return HistoricProcessInstanceQuery A modified query with applied filter
   */
  HistoricProcessInstanceQuery startedBy(String userId);

  /**
   * Order by the process instance id (needs to be followed by {@link #asc()} or {@link #desc()}).
   */
  HistoricProcessInstanceQuery orderByProcessInstanceId();

  /**
   * Order by the process definition id (needs to be followed by {@link #asc()} or {@link #desc()}).
   */
  HistoricProcessInstanceQuery orderByProcessDefinitionId();

  /**
   * Order by the process definition key (needs to be followed by {@link #asc()} or {@link #desc()}).
   */
  HistoricProcessInstanceQuery orderByProcessDefinitionKey();

  /**
   * Order by the process definition name (needs to be followed by {@link #asc()} or {@link #desc()}).
   */
  HistoricProcessInstanceQuery orderByProcessDefinitionName();

  /**
   * Order by the process definition version (needs to be followed by {@link #asc()} or {@link #desc()}).
   */
  HistoricProcessInstanceQuery orderByProcessDefinitionVersion();

  /**
   * Order by the business key (needs to be followed by {@link #asc()} or {@link #desc()}).
   */
  HistoricProcessInstanceQuery orderByProcessInstanceBusinessKey();

  /**
   * Order by the start time (needs to be followed by {@link #asc()} or {@link #desc()}).
   */
  HistoricProcessInstanceQuery orderByProcessInstanceStartTime();

  /**
   * Order by the end time (needs to be followed by {@link #asc()} or {@link #desc()}).
   */
  HistoricProcessInstanceQuery orderByProcessInstanceEndTime();

  /**
   * Order by the duration of the process instance (needs to be followed by {@link #asc()} or {@link #desc()}).
   */
  HistoricProcessInstanceQuery orderByProcessInstanceDuration();

  /**
   * Only select historic process instances that are top level process instances.
   * @return HistoricProcessInstanceQuery A modified query with applied filter
   */
  HistoricProcessInstanceQuery rootProcessInstances();

  /**
   * Only select historic process instances started by the given process
   * instance. {@link ProcessInstance) ids and {@link HistoricProcessInstance}
   * ids match.
   * @return HistoricProcessInstanceQuery A modified query with applied filter
   */
  HistoricProcessInstanceQuery superProcessInstanceId(String superProcessInstanceId);

  /**
   * Only select historic process instances having a sub process instance
   * with the given process instance id.
   * <p>
   * Note that there will always be maximum only <b>one</b>
   * such process instance that can be the result of this query.
   * @return HistoricProcessInstanceQuery A modified query with applied filter
   */
  HistoricProcessInstanceQuery subProcessInstanceId(String subProcessInstanceId);

  /**
   * Only select historic process instances started by the given case
   * instance.
   * @return HistoricProcessInstanceQuery A modified query with applied filter
   */
  HistoricProcessInstanceQuery superCaseInstanceId(String superCaseInstanceId);

  /**
   * Only select historic process instances having a sub case instance
   * with the given case instance id.
   * <p>
   * Note that there will always be maximum only <b>one</b>
   * such process instance that can be the result of this query.
   * @return HistoricProcessInstanceQuery A modified query with applied filter
   */
  HistoricProcessInstanceQuery subCaseInstanceId(String subCaseInstanceId);

  /**
   * Only select historic process instances with one of the given tenant ids.
   * @return HistoricProcessInstanceQuery A modified query with applied filter
   */
  HistoricProcessInstanceQuery tenantIdIn(String... tenantIds);

  /**
   * Only selects historic process instances which have no tenant id.
   * @return HistoricProcessInstanceQuery A modified query with applied filter
   */
  HistoricProcessInstanceQuery withoutTenantId();

  /**
   * Order by tenant id (needs to be followed by {@link #asc()} or {@link #desc()}).
   * Note that the ordering of historic process instances without tenant id is database-specific.
   * @return HistoricProcessInstanceQuery A modified query with applied filter
   */
  HistoricProcessInstanceQuery orderByTenantId();

  /**
   * Only select historic process instances that were started as of the provided
   * date. (Date will be adjusted to reflect midnight)
   *
   * @return HistoricProcessInstanceQuery A modified query with applied filter
   * @deprecated Use {@link #startedAfter(Date)} and {@link #startedBefore(Date)} instead.
   */
  @Deprecated(forRemoval = true, since = "1.0")
  HistoricProcessInstanceQuery startDateBy(Date date);

  /**
   * Only select historic process instances that were started on the provided date.
   *
   * @return HistoricProcessInstanceQuery A modified query with applied filter
   * @deprecated Use {@link #startedAfter(Date)} and {@link #startedBefore(Date)} instead.
   */
  @Deprecated(forRemoval = true, since = "1.0")
  HistoricProcessInstanceQuery startDateOn(Date date);

  /**
   * Only select historic process instances that were finished as of the
   * provided date. (Date will be adjusted to reflect one second before midnight)
   *
   * @return HistoricProcessInstanceQuery A modified query with applied filter
   * @deprecated Use {@link #startedAfter(Date)} and {@link #startedBefore(Date)} instead.
   */
  @Deprecated(forRemoval = true, since = "1.0")
  HistoricProcessInstanceQuery finishDateBy(Date date);

  /**
   * Only select historic process instances that were finished on provided date.
   *
   * @return HistoricProcessInstanceQuery A modified query with applied filter
   * @deprecated Use {@link #startedAfter(Date)} and {@link #startedBefore(Date)} instead.
   */
  @Deprecated(forRemoval = true, since = "1.0")
  HistoricProcessInstanceQuery finishDateOn(Date date);

  /**
   * Only select historic process instances that executed an activity after the given date.
   * @return HistoricProcessInstanceQuery A modified query with applied filter
   */
  HistoricProcessInstanceQuery executedActivityAfter(Date date);

  /**
   * Only select historic process instances that executed an activity before the given date.
   * @return HistoricProcessInstanceQuery A modified query with applied filter
   */
  HistoricProcessInstanceQuery executedActivityBefore(Date date);

  /**
   * Only select historic process instances that executed activities with given ids.
   * @return HistoricProcessInstanceQuery A modified query with applied filter
   */
  HistoricProcessInstanceQuery executedActivityIdIn(String... ids);

  /**
   * Only select historic process instances that have active activities with given ids.
   * @return HistoricProcessInstanceQuery A modified query with applied filter
   */
  HistoricProcessInstanceQuery activeActivityIdIn(String... ids);

  /**
   * Only select historic process instances with an active activity with one of the given ids.
   * In contrast to the `activeActivityIdIn` filter, it can query for async and incident activities.
   * @return HistoricProcessInstanceQuery A modified query with applied filter
   */
  HistoricProcessInstanceQuery activityIdIn(String... ids);

  /**
   * Only select historic process instances that executed an job after the given date.
   * @return HistoricProcessInstanceQuery A modified query with applied filter
   */
  HistoricProcessInstanceQuery executedJobAfter(Date date);

  /**
   * Only select historic process instances that executed an job before the given date.
   * @return HistoricProcessInstanceQuery A modified query with applied filter
   */
  HistoricProcessInstanceQuery executedJobBefore(Date date);

  /**
   * Only select historic process instances that are active.
   * @return HistoricProcessInstanceQuery A modified query with applied filter
   */
  HistoricProcessInstanceQuery active();

  /**
   * Only select historic process instances that are suspended.
   * @return HistoricProcessInstanceQuery A modified query with applied filter
   */
  HistoricProcessInstanceQuery suspended();

  /**
   * Only select historic process instances that are completed.
   * @return HistoricProcessInstanceQuery A modified query with applied filter
   */
  HistoricProcessInstanceQuery completed();

  /**
   * Only select historic process instances that are externallyTerminated.
   * @return HistoricProcessInstanceQuery A modified query with applied filter
   */
  HistoricProcessInstanceQuery externallyTerminated();

  /**
   * Only select historic process instances that are internallyTerminated.
   * @return HistoricProcessInstanceQuery A modified query with applied filter
   */
  HistoricProcessInstanceQuery internallyTerminated();

  /**
   * <p>After calling or(), a chain of several filter criteria could follow. Each filter criterion that follows or()
   * will be linked together with an OR expression until the OR query is terminated. To terminate the OR query right
   * after the last filter criterion was applied, {@link #endOr()} must be invoked.</p>
   *
   * @return an object of the type {@link HistoricProcessInstanceQuery} on which an arbitrary amount of filter criteria could be applied.
   * The several filter criteria will be linked together by an OR expression.
   * @throws ProcessEngineException when or() has been invoked directly after or() or after or() and trailing filter
   *                                criteria. To prevent throwing this exception, {@link #endOr()} must be invoked after a chain of filter criteria to
   *                                mark the end of the OR query.
   */
  HistoricProcessInstanceQuery or();

  /**
   * <p>endOr() terminates an OR query on which an arbitrary amount of filter criteria were applied. To terminate the
   * OR query which has been started by invoking {@link #or()}, endOr() must be invoked. Filter criteria which are
   * applied after calling endOr() are linked together by an AND expression.</p>
   *
   * @return an object of the type {@link HistoricProcessInstanceQuery} on which an arbitrary amount of filter criteria could be applied.
   * The filter criteria will be linked together by an AND expression.
   * @throws ProcessEngineException when endOr() has been invoked before {@link #or()} was invoked. To prevent throwing
   *                                this exception, {@link #or()} must be invoked first.
   */
  HistoricProcessInstanceQuery endOr();
}

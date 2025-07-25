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
package org.operaton.bpm.cockpit.impl.plugin.base.dto.query;

import org.operaton.bpm.cockpit.impl.plugin.base.dto.ProcessInstanceDto;
import org.operaton.bpm.cockpit.rest.dto.AbstractRestQueryParametersDto;
import org.operaton.bpm.engine.impl.QueryVariableValue;
import org.operaton.bpm.engine.impl.variable.serializer.VariableSerializers;
import org.operaton.bpm.engine.rest.dto.OperatonQueryParam;
import org.operaton.bpm.engine.rest.dto.ConditionQueryParameterDto;
import org.operaton.bpm.engine.rest.dto.VariableQueryParameterDto;
import org.operaton.bpm.engine.rest.dto.converter.BooleanConverter;
import org.operaton.bpm.engine.rest.dto.converter.DateConverter;
import org.operaton.bpm.engine.rest.dto.converter.StringArrayConverter;
import org.operaton.bpm.engine.rest.dto.converter.VariableListConverter;

import jakarta.ws.rs.core.MultivaluedMap;

import java.util.*;

public abstract class AbstractProcessInstanceQueryDto<T extends ProcessInstanceDto>
  extends AbstractRestQueryParametersDto<T> {

  private static final long serialVersionUID = 1L;

  private static final String SORT_BY_PROCESS_INSTANCE_START_TIME = "startTime";

  private static final List<String> VALID_SORT_BY_VALUES;
  static {
    VALID_SORT_BY_VALUES = new ArrayList<>();
    VALID_SORT_BY_VALUES.add(SORT_BY_PROCESS_INSTANCE_START_TIME);
  }

  private static final Map<String, String> ORDER_BY_VALUES;
  static {
    ORDER_BY_VALUES = new HashMap<>();
    ORDER_BY_VALUES.put(SORT_BY_PROCESS_INSTANCE_START_TIME, "START_TIME_");
  }

  protected String processDefinitionId;
  protected String parentProcessDefinitionId;
  protected String[] activityIdIn;
  protected String[] activityInstanceIdIn;
  protected String businessKey;
  protected String parentProcessInstanceId;
  protected Date startedBefore;
  protected Date startedAfter;
  protected Boolean withIncident;

  private List<VariableQueryParameterDto> variables;

  /**
   * Process instance compatible wrapper for query variables
   */
  private List<QueryVariableValue> queryVariableValues;

  protected AbstractProcessInstanceQueryDto() {
  }

  protected AbstractProcessInstanceQueryDto(MultivaluedMap<String, String> queryParameter) {
    super(queryParameter);
  }

  public String getProcessDefinitionId() {
    return processDefinitionId;
  }

  @OperatonQueryParam("processDefinitionId")
  public void setProcessDefinitionId(String processDefinitionId) {
    this.processDefinitionId = processDefinitionId;
  }

  public String getParentProcessDefinitionId() {
    return parentProcessDefinitionId;
  }

  @OperatonQueryParam("parentProcessDefinitionId")
  public void setParentProcessDefinitionId(String parentProcessDefinitionId) {
    this.parentProcessDefinitionId = parentProcessDefinitionId;
  }

  @OperatonQueryParam(value = "variables", converter = VariableListConverter.class)
  public void setVariables(List<VariableQueryParameterDto> variables) {
    this.variables = variables;
  }

  public List<QueryVariableValue> getQueryVariableValues() {
    return queryVariableValues;
  }

  public void initQueryVariableValues(VariableSerializers variableTypes, String dbType) {
    queryVariableValues = createQueryVariableValues(variableTypes, variables, dbType);
  }

  public String getParentProcessInstanceId() {
    return parentProcessInstanceId;
  }

  @OperatonQueryParam("parentProcessInstanceId")
  public void setParentProcessInstanceId(String parentProcessInstanceId) {
    this.parentProcessInstanceId = parentProcessInstanceId;
  }

  public String[] getActivityIdIn() {
    return activityIdIn;
  }

  @OperatonQueryParam(value="activityIdIn", converter = StringArrayConverter.class)
  public void setActivityIdIn(String[] activityIdIn) {
    this.activityIdIn = activityIdIn;
  }

  public String[] getActivityInstanceIdIn() {
    return activityInstanceIdIn;
  }

  @OperatonQueryParam(value="activityInstanceIdIn", converter = StringArrayConverter.class)
  public void setActivityInstanceIdIn(String[] activityInstanceIdIn) {
    this.activityInstanceIdIn = activityInstanceIdIn;
  }

  public String getBusinessKey() {
    return businessKey;
  }

  @OperatonQueryParam("businessKey")
  public void setBusinessKey(String businessKey) {
    this.businessKey = businessKey;
  }

  public Date getStartedBefore() {
    return startedBefore;
  }

  @OperatonQueryParam(value="startedBefore", converter = DateConverter.class)
  public void setStartedBefore(Date startedBefore) {
    this.startedBefore = startedBefore;
  }

  public Date getStartedAfter() {
    return startedAfter;
  }

  @OperatonQueryParam(value="startedAfter", converter = DateConverter.class)
  public void setStartedAfter(Date startedAfter) {
    this.startedAfter = startedAfter;
  }

  public Boolean getWithIncident() {
    return withIncident;
  }

  @OperatonQueryParam(value="withIncident", converter = BooleanConverter.class)
  public void setWithIncident(Boolean withIncident) {
    this.withIncident = withIncident;
  }

  @Override
  protected String getOrderByValue(String sortBy) {
    return ORDER_BY_VALUES.get(sortBy);
  }

  @Override
  protected boolean isValidSortByValue(String value) {
    return VALID_SORT_BY_VALUES.contains(value);
  }

  public String getOuterOrderBy() {
    String outerOrderBy = getOrderBy();
    if (outerOrderBy == null || outerOrderBy.isEmpty()) {
      return "ID_ asc";
    }
    else if (outerOrderBy.contains(".")) {
      return outerOrderBy.substring(outerOrderBy.lastIndexOf(".") + 1);
    }
    else {
      return outerOrderBy;
    }
  }

  private List<QueryVariableValue> createQueryVariableValues(VariableSerializers variableTypes, List<VariableQueryParameterDto> variables, String dbType) {

    List<QueryVariableValue> values = new ArrayList<>();

    if (variables == null) {
      return values;
    }

    for (VariableQueryParameterDto variable : variables) {
      QueryVariableValue value = new QueryVariableValue(
          variable.getName(),
          resolveVariableValue(variable.getValue()),
          ConditionQueryParameterDto.getQueryOperator(variable.getOperator()),
          false);

      value.initialize(variableTypes, dbType);
      values.add(value);
    }

    return values;
  }
}

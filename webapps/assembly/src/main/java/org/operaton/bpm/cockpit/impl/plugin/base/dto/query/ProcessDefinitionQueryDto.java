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

import java.util.ArrayList;
import java.util.List;

import jakarta.ws.rs.core.MultivaluedMap;

import org.operaton.bpm.cockpit.impl.plugin.base.dto.ProcessDefinitionDto;
import org.operaton.bpm.cockpit.rest.dto.AbstractRestQueryParametersDto;
import org.operaton.bpm.engine.impl.QueryVariableValue;
import org.operaton.bpm.engine.impl.variable.serializer.VariableSerializers;
import org.operaton.bpm.engine.rest.dto.OperatonQueryParam;
import org.operaton.bpm.engine.rest.dto.ConditionQueryParameterDto;
import org.operaton.bpm.engine.rest.dto.VariableQueryParameterDto;
import org.operaton.bpm.engine.rest.dto.converter.StringArrayConverter;
import org.operaton.bpm.engine.rest.dto.converter.VariableListConverter;

public class ProcessDefinitionQueryDto extends AbstractRestQueryParametersDto<ProcessDefinitionDto> {

  private static final long serialVersionUID = 1L;

  protected String parentProcessDefinitionId;
  protected String superProcessDefinitionId;
  protected String[] activityIdIn;
  protected String businessKey;

  private List<VariableQueryParameterDto> variables;

  /**
   * Process instance compatible wrapper for query variables
   */
  private List<QueryVariableValue> queryVariableValues;

  public ProcessDefinitionQueryDto() { }

  public ProcessDefinitionQueryDto(MultivaluedMap<String, String> queryParameters) {
    super(queryParameters);
  }

  public String getParentProcessDefinitionId() {
    return parentProcessDefinitionId;
  }

  @OperatonQueryParam("parentProcessDefinitionId")
  public void setParentProcessDefinitionId(String parentProcessDefinitionId) {
    this.parentProcessDefinitionId = parentProcessDefinitionId;
  }

  public String getSuperProcessDefinitionId() {
    return superProcessDefinitionId;
  }

  @OperatonQueryParam("superProcessDefinitionId")
  public void setSuperProcessDefinitionId(String superProcessDefinitionId) {
    this.superProcessDefinitionId = superProcessDefinitionId;
  }

  public String[] getActivityIdIn() {
    return activityIdIn;
  }

  @OperatonQueryParam(value="activityIdIn", converter = StringArrayConverter.class)
  public void setActivityIdIn(String[] activityIdIn) {
    this.activityIdIn = activityIdIn;
  }

  public String getBusinessKey() {
    return businessKey;
  }

  @OperatonQueryParam("businessKey")
  public void setBusinessKey(String businessKey) {
    this.businessKey = businessKey;
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

  @Override
  protected String getOrderByValue(String sortBy) {
    return super.getOrderBy();
  }

  @Override
  protected boolean isValidSortByValue(String value) {
    return false;
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

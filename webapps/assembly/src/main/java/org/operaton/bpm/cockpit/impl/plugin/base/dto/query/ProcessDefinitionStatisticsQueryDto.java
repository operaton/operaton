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
package org.operaton.bpm.cockpit.impl.plugin.base.dto.query;

import org.operaton.bpm.cockpit.impl.plugin.base.dto.ProcessDefinitionStatisticsDto;
import org.operaton.bpm.cockpit.rest.dto.AbstractRestQueryParametersDto;
import org.operaton.bpm.engine.rest.dto.OperatonQueryParam;

import javax.ws.rs.core.MultivaluedMap;
import java.util.HashMap;
import java.util.Map;

public class ProcessDefinitionStatisticsQueryDto extends AbstractRestQueryParametersDto<ProcessDefinitionStatisticsDto> {

  private static final Map<String, String> VALID_SORT_VALUES;
  static {
    VALID_SORT_VALUES = new HashMap<>();
    VALID_SORT_VALUES.put("incidents", "INCIDENT_COUNT_");
    VALID_SORT_VALUES.put("instances", "INSTANCE_COUNT_");
    VALID_SORT_VALUES.put("key", "KEY_");
    VALID_SORT_VALUES.put("name", "NAME_");
    VALID_SORT_VALUES.put("tenantId", "TENANT_ID_");
  }

  protected String key;
  protected String keyLike;
  protected String name;
  protected String nameLike;

  public ProcessDefinitionStatisticsQueryDto(MultivaluedMap<String, String> queryParameters) {
    super(queryParameters);
  }

  @OperatonQueryParam("key")
  public void setKey(String key) {
    this.key = key;
  }

  @OperatonQueryParam("keyLike")
  public void setKeyLike(String keyLike) {
    this.keyLike = keyLike;
  }

  @OperatonQueryParam("name")
  public void setName(String name) {
    this.name = name;
  }

  @OperatonQueryParam("nameLike")
  public void setNameLike(String nameLike) {
    this.nameLike = nameLike;
  }

  @Override
  protected boolean isValidSortByValue(String value) {
    return VALID_SORT_VALUES.containsKey(value);
  }

  @Override
  protected String getOrderByValue(String sortBy) {
    return VALID_SORT_VALUES.get(sortBy);
  }

}

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
package org.operaton.bpm.engine.rest.dto.authorization;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.MultivaluedMap;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.authorization.AuthorizationQuery;
import org.operaton.bpm.engine.rest.dto.AbstractQueryDto;
import org.operaton.bpm.engine.rest.dto.OperatonQueryParam;
import org.operaton.bpm.engine.rest.dto.converter.IntegerConverter;
import org.operaton.bpm.engine.rest.dto.converter.StringArrayConverter;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 *
 * @author Daniel Meyer
 */
public class AuthorizationQueryDto extends AbstractQueryDto<AuthorizationQuery> {

  private static final String SORT_BY_RESOURCE_TYPE = "resourceType";
  private static final String SORT_BY_RESOURCE_ID = "resourceId";

  private static final List<String> VALID_SORT_BY_VALUES;
  static {
    VALID_SORT_BY_VALUES = new ArrayList<>();
    VALID_SORT_BY_VALUES.add(SORT_BY_RESOURCE_TYPE);
    VALID_SORT_BY_VALUES.add(SORT_BY_RESOURCE_ID);
  }

  protected String id;
  protected Integer type;
  protected String[] userIdIn;
  protected String[] groupIdIn;
  protected Integer resourceType;
  protected String resourceId;

  public AuthorizationQueryDto() {

  }

  public AuthorizationQueryDto(ObjectMapper objectMapper, MultivaluedMap<String, String> queryParameters) {
    super(objectMapper, queryParameters);
  }

  @OperatonQueryParam("id")
  public void setId(String id) {
    this.id = id;
  }

  @OperatonQueryParam(value="type", converter = IntegerConverter.class)
  public void setType(Integer type) {
    this.type = type;
  }

  @OperatonQueryParam(value="userIdIn", converter = StringArrayConverter.class)
  public void setUserIdIn(String[] userIdIn) {
    this.userIdIn = userIdIn;
  }

  @OperatonQueryParam(value="groupIdIn", converter = StringArrayConverter.class)
  public void setGroupIdIn(String[] groupIdIn) {
    this.groupIdIn = groupIdIn;
  }

  @OperatonQueryParam(value="resourceType", converter = IntegerConverter.class)
  public void setResourceType(int resourceType) {
    this.resourceType = resourceType;
  }

  @OperatonQueryParam("resourceId")
  public void setResourceId(String resourceId) {
    this.resourceId = resourceId;
  }

  protected boolean isValidSortByValue(String value) {
    return VALID_SORT_BY_VALUES.contains(value);
  }

  protected AuthorizationQuery createNewQuery(ProcessEngine engine) {
    return engine.getAuthorizationService().createAuthorizationQuery();
  }

  protected void applyFilters(AuthorizationQuery query) {

    if (id != null) {
      query.authorizationId(id);
    }
    if (type != null) {
      query.authorizationType(type);
    }
    if (userIdIn != null) {
      query.userIdIn(userIdIn);
    }
    if (groupIdIn != null) {
      query.groupIdIn(groupIdIn);
    }
    if (resourceType != null) {
      query.resourceType(resourceType);
    }
    if (resourceId != null) {
      query.resourceId(resourceId);
    }
  }

  @Override
  protected void applySortBy(AuthorizationQuery query, String sortBy, Map<String, Object> parameters, ProcessEngine engine) {
    if (SORT_BY_RESOURCE_ID.equals(sortBy)) {
      query.orderByResourceId();
    } else if (SORT_BY_RESOURCE_TYPE.equals(sortBy)) {
      query.orderByResourceType();
    }
  }

}

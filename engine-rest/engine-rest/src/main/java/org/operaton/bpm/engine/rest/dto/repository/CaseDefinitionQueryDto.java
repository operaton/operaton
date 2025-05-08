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
package org.operaton.bpm.engine.rest.dto.repository;

import static java.lang.Boolean.TRUE;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.MultivaluedMap;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.repository.CaseDefinitionQuery;
import org.operaton.bpm.engine.rest.dto.AbstractQueryDto;
import org.operaton.bpm.engine.rest.dto.OperatonQueryParam;
import org.operaton.bpm.engine.rest.dto.converter.BooleanConverter;
import org.operaton.bpm.engine.rest.dto.converter.IntegerConverter;
import org.operaton.bpm.engine.rest.dto.converter.StringListConverter;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 *
 * @author Roman Smirnov
 *
 */
public class CaseDefinitionQueryDto extends AbstractQueryDto<CaseDefinitionQuery> {

  private static final String SORT_BY_ID_VALUE = "id";
  private static final String SORT_BY_KEY_VALUE = "key";
  private static final String SORT_BY_NAME_VALUE = "name";
  private static final String SORT_BY_VERSION_VALUE = "version";
  private static final String SORT_BY_DEPLOYMENT_ID_VALUE = "deploymentId";
  private static final String SORT_BY_CATEGORY_VALUE = "category";
  private static final String SORT_BY_TENANT_ID = "tenantId";

  private static final List<String> VALID_SORT_BY_VALUES;

  static {
    VALID_SORT_BY_VALUES = new ArrayList<>();

    VALID_SORT_BY_VALUES.add(SORT_BY_CATEGORY_VALUE);
    VALID_SORT_BY_VALUES.add(SORT_BY_KEY_VALUE);
    VALID_SORT_BY_VALUES.add(SORT_BY_ID_VALUE);
    VALID_SORT_BY_VALUES.add(SORT_BY_NAME_VALUE);
    VALID_SORT_BY_VALUES.add(SORT_BY_VERSION_VALUE);
    VALID_SORT_BY_VALUES.add(SORT_BY_DEPLOYMENT_ID_VALUE);
    VALID_SORT_BY_VALUES.add(SORT_BY_TENANT_ID);
  }

  protected String caseDefinitionId;
  protected List<String> caseDefinitionIdIn;
  protected String category;
  protected String categoryLike;
  protected String name;
  protected String nameLike;
  protected String deploymentId;
  protected String key;
  protected String keyLike;
  protected String resourceName;
  protected String resourceNameLike;
  protected Integer version;
  protected Boolean latestVersion;
  protected List<String> tenantIds;
  protected Boolean withoutTenantId;
  protected Boolean includeDefinitionsWithoutTenantId;

  public CaseDefinitionQueryDto() {}

  public CaseDefinitionQueryDto(ObjectMapper objectMapper, MultivaluedMap<String, String> queryParameters) {
    super(objectMapper, queryParameters);
  }

  @OperatonQueryParam("caseDefinitionId")
  public void setCaseDefinitionId(String caseDefinitionId) {
    this.caseDefinitionId = caseDefinitionId;
  }

  @OperatonQueryParam(value = "caseDefinitionIdIn", converter = StringListConverter.class)
  public void setCaseDefinitionIdIn(List<String> caseDefinitionIdIn) {
    this.caseDefinitionIdIn = caseDefinitionIdIn;
  }

  @OperatonQueryParam("category")
  public void setCategory(String category) {
    this.category = category;
  }

  @OperatonQueryParam("categoryLike")
  public void setCategoryLike(String categoryLike) {
    this.categoryLike = categoryLike;
  }

  @OperatonQueryParam("name")
  public void setName(String name) {
    this.name = name;
  }

  @OperatonQueryParam("nameLike")
  public void setNameLike(String nameLike) {
    this.nameLike = nameLike;
  }

  @OperatonQueryParam("deploymentId")
  public void setDeploymentId(String deploymentId) {
    this.deploymentId = deploymentId;
  }

  @OperatonQueryParam("key")
  public void setKey(String key) {
    this.key = key;
  }

  @OperatonQueryParam("keyLike")
  public void setKeyLike(String keyLike) {
    this.keyLike = keyLike;
  }

  @OperatonQueryParam("resourceName")
  public void setResourceName(String resourceName) {
    this.resourceName = resourceName;
  }

  @OperatonQueryParam("resourceNameLike")
  public void setResourceNameLike(String resourceNameLike) {
    this.resourceNameLike = resourceNameLike;
  }

  @OperatonQueryParam(value = "version", converter = IntegerConverter.class)
  public void setVersion(Integer version) {
    this.version = version;
  }

  /**
   * @deprecated Use {@link #setLatestVersion(Boolean)} instead.
   */
  @Deprecated(forRemoval = true, since = "1.0.0-beta-1")
  @OperatonQueryParam(value = "latest", converter = BooleanConverter.class)
  public void setLatest(Boolean latest) {
    setLatestVersion(latest);
  }

  @OperatonQueryParam(value = "latestVersion", converter = BooleanConverter.class)
  public void setLatestVersion(Boolean latestVersion) {
    this.latestVersion = latestVersion;
  }

  @OperatonQueryParam(value = "tenantIdIn", converter = StringListConverter.class)
  public void setTenantIdIn(List<String> tenantIds) {
    this.tenantIds = tenantIds;
  }

  @OperatonQueryParam(value = "withoutTenantId", converter = BooleanConverter.class)
  public void setWithoutTenantId(Boolean withoutTenantId) {
    this.withoutTenantId = withoutTenantId;
  }

  @OperatonQueryParam(value = "includeCaseDefinitionsWithoutTenantId", converter = BooleanConverter.class)
  public void setIncludeCaseDefinitionsWithoutTenantId(Boolean includeDefinitionsWithoutTenantId) {
    this.includeDefinitionsWithoutTenantId = includeDefinitionsWithoutTenantId;
  }

  @Override
  protected boolean isValidSortByValue(String value) {
    return VALID_SORT_BY_VALUES.contains(value);
  }

  @Override
  protected CaseDefinitionQuery createNewQuery(ProcessEngine engine) {
    return engine.getRepositoryService().createCaseDefinitionQuery();
  }

  @Override
  protected void applyFilters(CaseDefinitionQuery query) {
    if (caseDefinitionId != null) {
      query.caseDefinitionId(caseDefinitionId);
    }
    if (caseDefinitionIdIn != null && !caseDefinitionIdIn.isEmpty()) {
      query.caseDefinitionIdIn(caseDefinitionIdIn.toArray(new String[caseDefinitionIdIn.size()]));
    }
    if (category != null) {
      query.caseDefinitionCategory(category);
    }
    if (categoryLike != null) {
      query.caseDefinitionCategoryLike(categoryLike);
    }
    if (name != null) {
      query.caseDefinitionName(name);
    }
    if (nameLike != null) {
      query.caseDefinitionNameLike(nameLike);
    }
    if (deploymentId != null) {
      query.deploymentId(deploymentId);
    }
    if (key != null) {
      query.caseDefinitionKey(key);
    }
    if (keyLike != null) {
      query.caseDefinitionKeyLike(keyLike);
    }
    if (resourceName != null) {
      query.caseDefinitionResourceName(resourceName);
    }
    if (resourceNameLike != null) {
      query.caseDefinitionResourceNameLike(resourceNameLike);
    }
    if (version != null) {
      query.caseDefinitionVersion(version);
    }
    if (TRUE.equals(latestVersion)) {
      query.latestVersion();
    }
    if (tenantIds != null && !tenantIds.isEmpty()) {
      query.tenantIdIn(tenantIds.toArray(new String[tenantIds.size()]));
    }
    if (TRUE.equals(withoutTenantId)) {
      query.withoutTenantId();
    }
    if (TRUE.equals(includeDefinitionsWithoutTenantId)) {
      query.includeCaseDefinitionsWithoutTenantId();
    }
  }

  @Override
  protected void applySortBy(CaseDefinitionQuery query, String sortBy, Map<String, Object> parameters, ProcessEngine engine) {
    if (sortBy.equals(SORT_BY_CATEGORY_VALUE)) {
      query.orderByCaseDefinitionCategory();
    } else if (sortBy.equals(SORT_BY_KEY_VALUE)) {
      query.orderByCaseDefinitionKey();
    } else if (sortBy.equals(SORT_BY_ID_VALUE)) {
      query.orderByCaseDefinitionId();
    } else if (sortBy.equals(SORT_BY_VERSION_VALUE)) {
      query.orderByCaseDefinitionVersion();
    } else if (sortBy.equals(SORT_BY_NAME_VALUE)) {
      query.orderByCaseDefinitionName();
    } else if (sortBy.equals(SORT_BY_DEPLOYMENT_ID_VALUE)) {
      query.orderByDeploymentId();
    } else if (sortBy.equals(SORT_BY_TENANT_ID)) {
      query.orderByTenantId();
    }
  }

}

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
package org.operaton.bpm.engine.rest.dto;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response.Status;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.impl.Direction;
import org.operaton.bpm.engine.query.Query;
import org.operaton.bpm.engine.rest.exception.InvalidRequestException;
import org.operaton.bpm.engine.rest.exception.RestException;

/**
 * Defines common query operations, such as sorting options and validation.
 * Also allows to access its setter methods based on {@link OperatonQueryParam} annotations which is
 * used for processing Http query parameters.
 *
 * @author Thorben Lindhauer
 *
 */
public abstract class AbstractQueryDto<T extends Query<?, ?>>  extends AbstractSearchQueryDto {
  private static final String SORT_ORDER_ASC_VALUE = "asc";
  private static final String SORT_ORDER_DESC_VALUE = "desc";

  private static final List<String> VALID_SORT_ORDER_VALUES = List.of(SORT_ORDER_ASC_VALUE, SORT_ORDER_DESC_VALUE);

  protected String sortBy;
  protected String sortOrder;

  protected List<SortingDto> sortings;

  protected Map<String, String> expressions = new HashMap<>();

  // required for populating via jackson
  protected AbstractQueryDto() {

  }

  protected AbstractQueryDto(ObjectMapper objectMapper, MultivaluedMap<String, String> queryParameters) {
    super(objectMapper, queryParameters);
  }


  @OperatonQueryParam("sortBy")
  public void setSortBy(String sortBy) {
    if (!isValidSortByValue(sortBy)) {
      throw new InvalidRequestException(Status.BAD_REQUEST, "sortBy parameter has invalid value: %s".formatted(sortBy));
    }
    this.sortBy = sortBy;
  }

  @OperatonQueryParam("sortOrder")
  public void setSortOrder(String sortOrder) {
    if (!VALID_SORT_ORDER_VALUES.contains(sortOrder)) {
      throw new InvalidRequestException(Status.BAD_REQUEST, "sortOrder parameter has invalid value: %s".formatted(sortOrder));
    }
    this.sortOrder = sortOrder;
  }

  public void setSorting(List<SortingDto> sorting) {
    this.sortings = sorting;
  }

  public List<SortingDto> getSorting() {
    return sortings;
  }

  protected abstract boolean isValidSortByValue(String value);

  protected boolean sortOptionsValid() {
    return (sortBy != null && sortOrder != null) || (sortBy == null && sortOrder == null);
  }

  public T toQuery(ProcessEngine engine) {
    T query = createNewQuery(engine);
    applyFilters(query);

    if (!sortOptionsValid()) {
      throw new InvalidRequestException(Status.BAD_REQUEST, "Only a single sorting parameter specified. sortBy and sortOrder required");
    }

    applySortingOptions(query, engine);

    return query;
  }

  protected abstract T createNewQuery(ProcessEngine engine);

  protected abstract void applyFilters(T query);

  protected void applySortingOptions(T query, ProcessEngine engine) {
    if (sortBy != null) {
      applySortBy(query, sortBy, null, engine);
    }
    if (sortOrder != null) {
      applySortOrder(query, sortOrder);
    }

    if (sortings != null) {
      for (SortingDto sorting : sortings) {
        String sortingOrder = sorting.getSortOrder();
        String sortingBy = sorting.getSortBy();

        if (sortingBy != null) {
          applySortBy(query, sortingBy, sorting.getParameters(), engine);
        }
        if (sortingOrder != null) {
          applySortOrder(query, sortingOrder);
        }
      }
    }
  }

  protected abstract void applySortBy(T query, String sortBy, Map<String, Object> parameters, ProcessEngine engine);

  protected void applySortOrder(T query, String sortOrder) {
    if (sortOrder != null) {
      if (SORT_ORDER_ASC_VALUE.equals(sortOrder)) {
        query.asc();
      } else if (SORT_ORDER_DESC_VALUE.equals(sortOrder)) {
        query.desc();
      }
    }
  }

  public static String sortOrderValueForDirection(Direction direction) {
    if (Direction.ASCENDING.equals(direction)) {
      return SORT_ORDER_ASC_VALUE;
    }
    else if (Direction.DESCENDING.equals(direction)) {
      return SORT_ORDER_DESC_VALUE;
    }
    else {
      throw new RestException("Unknown query sorting direction %s".formatted(direction));
    }
  }

}

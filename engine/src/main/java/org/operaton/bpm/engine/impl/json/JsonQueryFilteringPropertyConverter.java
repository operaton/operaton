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
package org.operaton.bpm.engine.impl.json;

import java.util.List;

import com.google.gson.JsonObject;

import org.operaton.bpm.engine.impl.QueryEntityRelationCondition;
import org.operaton.bpm.engine.impl.QueryPropertyImpl;
import org.operaton.bpm.engine.impl.util.JsonUtil;
import org.operaton.bpm.engine.query.QueryProperty;

/**
 * @author Thorben Lindhauer
 *
 */
public class JsonQueryFilteringPropertyConverter implements JsonObjectConverter<QueryEntityRelationCondition> {

  protected static final JsonQueryFilteringPropertyConverter INSTANCE =
      new JsonQueryFilteringPropertyConverter();

  protected static final JsonArrayConverter<List<QueryEntityRelationCondition>> ARRAY_CONVERTER =
    new JsonArrayOfObjectsConverter<>(INSTANCE);

  public static final String BASE_PROPERTY = "baseField";
  public static final String COMPARISON_PROPERTY = "comparisonField";
  public static final String SCALAR_VALUE = "value";

  @Override
  public JsonObject toJsonObject(QueryEntityRelationCondition filteringProperty) {
    JsonObject jsonObject = JsonUtil.createObject();

    JsonUtil.addField(jsonObject, BASE_PROPERTY, filteringProperty.getProperty().getName());

    QueryProperty comparisonProperty = filteringProperty.getComparisonProperty();
    if (comparisonProperty != null) {
      JsonUtil.addField(jsonObject, COMPARISON_PROPERTY, comparisonProperty.getName());
    }

    Object scalarValue = filteringProperty.getScalarValue();
    if (scalarValue != null) {
      JsonUtil.addFieldRawValue(jsonObject, SCALAR_VALUE, scalarValue);
    }

    return jsonObject;
  }

  @Override
  public QueryEntityRelationCondition toObject(JsonObject jsonObject) {
    // this is limited in that it allows only String values;
    // that is sufficient for current use case with task filters
    // but could be extended by a data type in the future
    String scalarValue = null;
    if (jsonObject.has(SCALAR_VALUE)) {
      scalarValue = JsonUtil.getString(jsonObject, SCALAR_VALUE);
    }

    QueryProperty baseProperty = null;
    if (jsonObject.has(BASE_PROPERTY)) {
      baseProperty = new QueryPropertyImpl(JsonUtil.getString(jsonObject, BASE_PROPERTY));
    }

    QueryProperty comparisonProperty = null;
    if (jsonObject.has(COMPARISON_PROPERTY)) {
      comparisonProperty = new QueryPropertyImpl(JsonUtil.getString(jsonObject, COMPARISON_PROPERTY));
    }

    return new QueryEntityRelationCondition(baseProperty, comparisonProperty, scalarValue);
  }

}

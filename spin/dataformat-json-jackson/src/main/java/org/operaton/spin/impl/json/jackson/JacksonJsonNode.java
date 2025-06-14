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
package org.operaton.spin.impl.json.jackson;

import static org.operaton.commons.utils.EnsureUtil.ensureNotNull;

import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.operaton.spin.SpinList;
import org.operaton.spin.impl.SpinListImpl;
import org.operaton.spin.impl.json.jackson.format.JacksonJsonDataFormat;
import org.operaton.spin.impl.json.jackson.format.JacksonJsonDataFormatMapper;
import org.operaton.spin.impl.json.jackson.query.JacksonJsonPathQuery;
import org.operaton.spin.json.SpinJsonDataFormatException;
import org.operaton.spin.json.SpinJsonException;
import org.operaton.spin.json.SpinJsonNode;
import org.operaton.spin.json.SpinJsonPathQuery;
import org.operaton.spin.spi.DataFormatMapper;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jayway.jsonpath.InvalidPathException;
import com.jayway.jsonpath.JsonPath;

/**
 * Wrapper for a Jackson Json Tree Node.
 *
 * @author Thorben Lindhauer
 * @author Stefan Hentschel
 */
public class JacksonJsonNode extends SpinJsonNode {

  private static final JacksonJsonLogger LOG = JacksonJsonLogger.JSON_TREE_LOGGER;

  protected final JsonNode jsonNode;
  protected final JacksonJsonDataFormat dataFormat;


  public JacksonJsonNode(JsonNode jsonNode, JacksonJsonDataFormat dataFormat) {
    this.jsonNode = jsonNode;
    this.dataFormat = dataFormat;
  }

  @Override
  public String getDataFormatName() {
    return dataFormat.getName();
  }

  @Override
  public JsonNode unwrap() {
    return jsonNode;
  }

  @Override
  public String toString() {
    StringWriter writer = new StringWriter();
    writeToWriter(writer);
    return writer.toString();
  }

  @Override
  public void writeToWriter(Writer writer) {
    dataFormat.getWriter().writeToWriter(writer, jsonNode);
  }

  /**
   * fetch correct array index if index is less than 0
   *
   * ArrayNode will convert all negative integers into 0...
   *
   * @param index wanted index
   * @return {@link Integer} new index
   */
  protected Integer getCorrectIndex(Integer index) {
    Integer size = jsonNode.size();
    Integer newIndex = index;

    // reverse walking through the array
    if(index < 0) {
      newIndex = size + index;
    }

    // the negative index would be greater than the size a second time!
    if(newIndex < 0) {
      throw LOG.indexOutOfBounds(index, size);
    }

    // the index is greater as the actual size
    if(index > size) {
     throw LOG.indexOutOfBounds(index, size);
    }

    return newIndex;
  }

  protected int lookupArray(JsonNode searchNode, int direction) {
    if(!this.isArray()) {
      throw LOG.unableToGetIndex(jsonNode.getNodeType().name());
    }
    int i = direction>0 ? 0 : jsonNode.size() - 1;
    for (; i < jsonNode.size() && i >= 0; i += direction) {
      if (jsonNode.get(i).equals(searchNode)){
        return i;
      }
    }
    return -1;
  }

  @Override
  public Integer indexOf(Object searchObject) {
    ensureNotNull("searchObject", searchObject);
    JsonNode node = dataFormat.createJsonNode(searchObject);
    int res = lookupArray(node, 1);
    if(res == -1){
      throw LOG.unableToFindProperty(node.asText());
    }
    return res;
  }

  @Override
  public Integer lastIndexOf(Object searchObject) {
    ensureNotNull("searchObject", searchObject);
    JsonNode node = dataFormat.createJsonNode(searchObject);
    int res = lookupArray(node, -1);
    if(res == -1){
      throw LOG.unableToFindProperty(node.asText());
    }
    return res;
  }

  @Override
  public boolean contains(Object searchObject) {
    ensureNotNull("searchObject", searchObject);
    JsonNode node = dataFormat.createJsonNode(searchObject);
    int res = lookupArray(node, 1);
    return res != -1;
  }

  @Override
  public boolean isObject() {
    return jsonNode.isObject();
  }

  @Override
  public boolean hasProp(String name) {
    return jsonNode.has(name);
  }

  @Override
  public SpinJsonNode prop(String name) {
    ensureNotNull("name", name);
    if(jsonNode.has(name)) {
      JsonNode property = jsonNode.get(name);
      return dataFormat.createWrapperInstance(property);
    } else {
      throw LOG.unableToFindProperty(name);
    }
  }

  @Override
  public SpinJsonNode prop(String name, String newProperty) {
    ObjectNode node = (ObjectNode) jsonNode;

    node.put(name, newProperty);

    return this;
  }

  @Override
  public SpinJsonNode prop(String name, Number newProperty) {
    ObjectNode node = (ObjectNode) jsonNode;

    // Numbers magic because Jackson has no native .put(Number value)
    if (newProperty == null) {
      node.putNull(name);
    } else if(newProperty instanceof Long) {
      node.put(name, newProperty.longValue());
    } else if(newProperty instanceof Integer) {
      node.put(name, newProperty.intValue());
    } else if(newProperty instanceof Float) {
        node.put(name, newProperty.floatValue());
    } else {
      // convert any other sub class of Number into Float
      node.put(name, newProperty.floatValue());
    }

    return this;
  }

  @Override
  public SpinJsonNode prop(String name, int newProperty) {
    return prop(name, (Number) newProperty);
  }

  @Override
  public SpinJsonNode prop(String name, float newProperty) {
    return prop(name, (Number) newProperty);
  }

  @Override
  public SpinJsonNode prop(String name, long newProperty) {
    return prop(name, (Number) newProperty);
  }

  @Override
  public SpinJsonNode prop(String name, boolean newProperty) {
    return prop(name, (Boolean) newProperty);
  }

  @Override
  public SpinJsonNode prop(String name, Boolean newProperty) {
    ObjectNode node = (ObjectNode) jsonNode;

    node.put(name, newProperty);

    return this;
  }

  @Override
  public SpinJsonNode prop(String name, List<Object> newProperty) {
    ObjectNode node = (ObjectNode) jsonNode;

    node.set(name, dataFormat.createJsonNode(newProperty));

    return this;
  }

  public SpinJsonNode propList(String name, List<Object> newProperty) {
    return prop(name, newProperty);
  }

  @Override
  public SpinJsonNode prop(String name, Map<String, Object> newProperty) {
    ObjectNode node = (ObjectNode) jsonNode;

    node.set(name, dataFormat.createJsonNode(newProperty));

    return this;
  }

  @Override
  public SpinJsonNode prop(String name, SpinJsonNode newProperty) {
    ObjectNode node = (ObjectNode) jsonNode;

    if (newProperty != null) {
      node.set(name, (JsonNode) newProperty.unwrap());
    }
    else {
      node.putNull(name);
    }

    return this;
  }

  @Override
  public SpinJsonNode deleteProp(String name) {
    ensureNotNull("name", name);

    if(jsonNode.has(name)) {
      ObjectNode node = (ObjectNode) jsonNode;
      node.remove(name);
      return this;
    } else {
      throw LOG.unableToFindProperty(name);
    }
  }

  @Override
  public SpinJsonNode deleteProp(List<String> names) {
    ensureNotNull("names", names);

    for(String name: names) {
      deleteProp(name);
    }

    return this;
  }

  @Override
  public SpinJsonNode append(Object property) {
    ensureNotNull("property", property);
    if(jsonNode.isArray()) {
      ArrayNode node = (ArrayNode) jsonNode;

      node.add(dataFormat.createJsonNode(property));

      return this;
    } else {
      throw LOG.unableToModifyNode(jsonNode.getNodeType().name());
    }
  }

  @Override
  public SpinJsonNode insertAt(int index, Object property) {
    ensureNotNull("property", property);

    if(jsonNode.isArray()) {
      index = getCorrectIndex(index);
      ArrayNode node = (ArrayNode) jsonNode;

      node.insert(index, dataFormat.createJsonNode(property));

      return this;
    } else {
      throw LOG.unableToModifyNode(jsonNode.getNodeType().name());
    }
  }

  @Override
  public SpinJsonNode insertBefore(Object searchObject, Object insertObject) {
    ensureNotNull("searchObject", searchObject);
    ensureNotNull("insertObject", insertObject);
    if(this.isArray()) {
      Integer i = indexOf(searchObject);

      return insertAt(i, insertObject);

    } else {
      throw LOG.unableToCreateNode(jsonNode.getNodeType().name());
    }
  }

  @Override
  public SpinJsonNode insertAfter(Object searchObject, Object insertObject) {
    ensureNotNull("searchObject", searchObject);
    ensureNotNull("insertObject", insertObject);
    if(this.isArray()) {
      Integer i = indexOf(searchObject);

      return insertAt(i + 1, insertObject);

    } else {
      throw LOG.unableToCreateNode(jsonNode.getNodeType().name());
    }
  }

  @Override
  public SpinJsonNode remove(Object property) {
    return removeAt(indexOf(property));
  }

  @Override
  public SpinJsonNode removeLast(Object property) {
    return removeAt(lastIndexOf(property));
  }

  @Override
  public SpinJsonNode removeAt(int index) {
    if(this.isArray()) {
      ArrayNode node = (ArrayNode) jsonNode;

      node.remove(getCorrectIndex(index));

      return this;
    } else {
      throw LOG.unableToModifyNode(jsonNode.getNodeType().name());
    }
  }

  @Override
  public Boolean isBoolean() {
    return jsonNode.isBoolean();
  }

  @Override
  public Boolean boolValue() {
    if(jsonNode.isBoolean()) {
      return jsonNode.booleanValue();
    } else {
      throw LOG.unableToParseValue(Boolean.class.getSimpleName(), jsonNode.getNodeType());
    }
  }

  @Override
  public Boolean isNumber() {
    return jsonNode.isNumber();
  }

  @Override
  public Number numberValue() {
    if(jsonNode.isNumber()) {
      return jsonNode.numberValue();
    } else {
      throw LOG.unableToParseValue(Number.class.getSimpleName(), jsonNode.getNodeType());
    }
  }

  @Override
  public Boolean isString() {
    return jsonNode.isTextual();
  }

  @Override
  public String stringValue() {
    if(jsonNode.isTextual()) {
      return jsonNode.textValue();
    } else {
      throw LOG.unableToParseValue(String.class.getSimpleName(), jsonNode.getNodeType());
    }
  }

  @Override
  public Boolean isNull() {
    return jsonNode.isNull();
  }

  @Override
  public Boolean isValue() {
    return jsonNode.isValueNode();
  }

  @Override
  public Object value() {
    if(jsonNode.isBoolean()) {
      return jsonNode.booleanValue();
    }

    if(jsonNode.isNumber()) {
      return jsonNode.numberValue();
    }

    if(jsonNode.isTextual()) {
      return jsonNode.textValue();
    }

    if (jsonNode.isNull()) {
      return null;
    }

    throw LOG.unableToParseValue("String/Number/Boolean/Null", jsonNode.getNodeType());
  }

  @Override
  public Boolean isArray() {
    return jsonNode.isArray();
  }

  @Override
  public SpinList<SpinJsonNode> elements() {
    if(jsonNode.isArray()) {
      Iterator<JsonNode> iterator = jsonNode.elements();
      SpinList<SpinJsonNode> list = new SpinListImpl<>();
      while(iterator.hasNext()) {
        SpinJsonNode node = dataFormat.createWrapperInstance(iterator.next());
        list.add(node);
      }

      return list;
    } else {
      throw LOG.unableToParseValue(SpinList.class.getSimpleName(), jsonNode.getNodeType());
    }
  }

  @Override
  public List<String> fieldNames() {
    if(jsonNode.isContainerNode()) {
      Iterator<String> iterator = jsonNode.fieldNames();
      List<String> list = new ArrayList<>();
      while(iterator.hasNext()) {
        list.add(iterator.next());
      }

      return list;
    } else {
      throw LOG.unableToParseValue("Array/Object", jsonNode.getNodeType());
    }
  }

  public JsonNodeType getNodeType() {
    return jsonNode.getNodeType();
  }

  @Override
  public SpinJsonPathQuery jsonPath(String expression) {
    ensureNotNull("expression", expression);
    try {
      JsonPath query = JsonPath.compile(expression);
      return new JacksonJsonPathQuery(this, query, dataFormat);
    } catch(InvalidPathException pex) {
      throw LOG.unableToCompileJsonPathExpression(expression, pex);
    } catch(IllegalArgumentException aex) {
      throw LOG.unableToCompileJsonPathExpression(expression, aex);
    }
  }

  /**
   * Maps the json represented by this object to a java object of the given type.<br>
   * Note: the desired target type is not validated and needs to be trusted.
   *
   * @throws SpinJsonException if the json representation cannot be mapped to the specified type
   */
  @Override
  public <C> C mapTo(Class<C> type) {
    DataFormatMapper mapper = dataFormat.getMapper();
    return mapper.mapInternalToJava(jsonNode, type);
  }

  /**
   * Maps the json represented by this object to a java object of the given type.
   * Argument is to be supplied in Jackson's canonical type string format
   * (see {@link JavaType#toCanonical()}).<br>
   * Note: the desired target type is not validated and needs to be trusted.
   *
   * @throws SpinJsonException if the json representation cannot be mapped to the specified type
   * @throws SpinJsonDataFormatException if the parameter does not match a valid type
   */
  @Override
  public <C> C mapTo(String type) {
    DataFormatMapper mapper = dataFormat.getMapper();
    return mapper.mapInternalToJava(jsonNode, type);
  }

  /**
   * Maps the json represented by this object to a java object of the given type.<br>
   * Note: the desired target type is not validated and needs to be trusted.
   *
   * @throws SpinJsonException if the json representation cannot be mapped to the specified type
   */
  public <C> C mapTo(JavaType type) {
    JacksonJsonDataFormatMapper mapper = dataFormat.getMapper();
    return mapper.mapInternalToJava(jsonNode, type);
  }
}

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
package org.operaton.bpm.dmn.engine.impl;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.operaton.bpm.dmn.engine.impl.spi.type.DmnTypeDefinition;
import org.operaton.bpm.engine.variable.value.TypedValue;

public class DmnDecisionTableOutputImpl {

  protected String id;
  protected String name;
  protected String outputName;
  protected DmnTypeDefinition typeDefinition;
  protected List<TypedValue> outputValues;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getOutputName() {
    return outputName;
  }

  public void setOutputName(String outputName) {
    this.outputName = outputName;
  }

  public DmnTypeDefinition getTypeDefinition() {
    return typeDefinition;
  }

  public void setTypeDefinition(DmnTypeDefinition typeDefinition) {
    this.typeDefinition = typeDefinition;
  }

  public List<TypedValue> getOutputValues() {
    return outputValues;
  }

  public void setOutputValues(List<TypedValue> outputValues) {
    this.outputValues = outputValues;
  }

  @Override
  public String toString() {
    return "DmnDecisionTableOutputImpl{" +
      "id='" + id + '\'' +
      ", name='" + name + '\'' +
      ", outputName='" + outputName + '\'' +
      ", typeDefinition=" + typeDefinition +
      ", outputValues=[" +
      Optional.ofNullable(outputValues).orElse(List.of())
        .stream().map(t -> t.getValue().toString())
        .collect(Collectors.joining(" ,")) +
      "]" +
      '}';
  }

}

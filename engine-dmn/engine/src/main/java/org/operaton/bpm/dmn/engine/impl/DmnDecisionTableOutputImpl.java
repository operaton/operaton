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
package org.operaton.bpm.dmn.engine.impl;

import org.operaton.bpm.dmn.engine.impl.spi.type.DmnTypeDefinition;

public class DmnDecisionTableOutputImpl {

  protected String id;
  protected String name;
  protected String outputName;
  protected DmnTypeDefinition typeDefinition;

    /**
   * Returns the id of the object.
   *
   * @return the id
   */
  public String getId() {
    return id;
  }

    /**
   * Sets the ID of the object.
   *
   * @param id the new ID to set
   */
  public void setId(String id) {
    this.id = id;
  }

    /**
   * Returns the name of the object.
   *
   * @return the name of the object
   */
  public String getName() {
    return name;
  }

    /**
   * Sets the name of the object.
   * 
   * @param name the name to be set
   */
  public void setName(String name) {
    this.name = name;
  }

    /**
   * Returns the output name.
   *
   * @return the output name
   */
  public String getOutputName() {
    return outputName;
  }

    /**
   * Sets the output name for the method.
   *
   * @param outputName the new output name to set
   */
  public void setOutputName(String outputName) {
    this.outputName = outputName;
  }

    /**
   * Returns the type definition associated with this object.
   *
   * @return the type definition
   */
  public DmnTypeDefinition getTypeDefinition() {
    return typeDefinition;
  }

    /**
   * Sets the type definition for a DMN element.
   * 
   * @param typeDefinition the type definition to be set
   */
  public void setTypeDefinition(DmnTypeDefinition typeDefinition) {
    this.typeDefinition = typeDefinition;
  }

    /**
   * Returns a string representation of the DmnDecisionTableOutputImpl object
   */
  @Override
  public String toString() {
    return "DmnDecisionTableOutputImpl{" +
      "id='" + id + '\'' +
      ", name='" + name + '\'' +
      ", outputName='" + outputName + '\'' +
      ", typeDefinition=" + typeDefinition +
      '}';
  }

}

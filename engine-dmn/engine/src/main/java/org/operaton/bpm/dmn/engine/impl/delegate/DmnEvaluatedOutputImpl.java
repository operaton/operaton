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
package org.operaton.bpm.dmn.engine.impl.delegate;

import org.operaton.bpm.dmn.engine.delegate.DmnEvaluatedOutput;
import org.operaton.bpm.dmn.engine.impl.DmnDecisionTableOutputImpl;
import org.operaton.bpm.engine.variable.value.TypedValue;

public class DmnEvaluatedOutputImpl implements DmnEvaluatedOutput {

  protected String id;
  protected String name;
  protected String outputName;
  protected TypedValue value;

  public DmnEvaluatedOutputImpl(DmnDecisionTableOutputImpl decisionTableOutput, TypedValue value) {
    this.id = decisionTableOutput.getId();
    this.name = decisionTableOutput.getName();
    this.outputName = decisionTableOutput.getOutputName();
    this.value = value;
  }
    /**
   * Returns the id of the object.
   *
   * @return the id of the object
   */
  public String getId() {
    return id;
  }

    /**
   * Sets the id of the object.
   * 
   * @param id the new id to set
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
   * @param name the new name to set
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
   * @param outputName the name of the output
   */
  public void setOutputName(String outputName) {
      this.outputName = outputName;
    }

    /**
   * Returns the value stored in the TypedValue object.
   *
   * @return the stored value
   */
  public TypedValue getValue() {
    return value;
  }

    /**
   * Sets the value of the TypedValue object.
   *
   * @param value the TypedValue object to be set
   */
  public void setValue(TypedValue value) {
    this.value = value;
  }

    /**
   * Compares this DmnEvaluatedOutputImpl instance with the specified object for equality.
   * Returns true if the objects are equal based on their id, name, outputName, and value.
   * Otherwise, returns false.
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    DmnEvaluatedOutputImpl that = (DmnEvaluatedOutputImpl) o;

    if (id != null ? !id.equals(that.id) : that.id != null) return false;
    if (name != null ? !name.equals(that.name) : that.name != null) return false;
    if (outputName != null ? !outputName.equals(that.outputName) : that.outputName != null) return false;
    return !(value != null ? !value.equals(that.value) : that.value != null);

  }

    /**
   * Returns the hash code value for this object based on its id, name, outputName, and value fields.
   *
   * @return the hash code value for this object
   */
  @Override
  public int hashCode() {
    int result = id != null ? id.hashCode() : 0;
    result = 31 * result + (name != null ? name.hashCode() : 0);
    result = 31 * result + (outputName != null ? outputName.hashCode() : 0);
    result = 31 * result + (value != null ? value.hashCode() : 0);
    return result;
  }

    /**
   * Returns a string representation of the DmnEvaluatedOutputImpl object, including its id, name, outputName, and value.
   * 
   * @return a string representation of the DmnEvaluatedOutputImpl object
   */
  @Override
  public String toString() {
    return "DmnEvaluatedOutputImpl{" +
      "id='" + id + '\'' +
      ", name='" + name + '\'' +
      ", outputName='" + outputName + '\'' +
      ", value=" + value +
      '}';
  }
}

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

import org.operaton.bpm.dmn.engine.delegate.DmnEvaluatedInput;
import org.operaton.bpm.dmn.engine.impl.DmnDecisionTableInputImpl;
import org.operaton.bpm.engine.variable.value.TypedValue;

public class DmnEvaluatedInputImpl implements DmnEvaluatedInput {

  protected String id;
  protected String name;
  protected String inputVariable;
  protected TypedValue value;

  public DmnEvaluatedInputImpl(DmnDecisionTableInputImpl input) {
    this.id = input.getId();
    this.name = input.getName();
    this.inputVariable = input.getInputVariable();
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
   * Sets the id for the object.
   * 
   * @param id the id to be set
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
   * Returns the value of the input variable.
   *
   * @return the value of the input variable
   */
  public String getInputVariable() {
    return inputVariable;
  }

    /**
   * Sets the input variable for the object.
   *
   * @param inputVariable the new input variable value
   */
  public void setInputVariable(String inputVariable) {
    this.inputVariable = inputVariable;
  }

    /**
   * Returns the value of this TypedValue.
   *
   * @return the value of this TypedValue
   */
  public TypedValue getValue() {
    return value;
  }

    /**
   * Sets the value of the TypedValue object.
   * 
   * @param value the TypedValue object to set
   */
  public void setValue(TypedValue value) {
    this.value = value;
  }

    /**
   * Compares this DmnEvaluatedInputImpl object with the specified object for equality.
   * Returns true if the objects are equal based on their id, name, inputVariable, and value.
   *
   * @param o the object to compare to
   * @return true if the objects are equal, false otherwise
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    DmnEvaluatedInputImpl that = (DmnEvaluatedInputImpl) o;

    if (id != null ? !id.equals(that.id) : that.id != null) return false;
    if (name != null ? !name.equals(that.name) : that.name != null) return false;
    if (inputVariable != null ? !inputVariable.equals(that.inputVariable) : that.inputVariable != null) return false;
    return !(value != null ? !value.equals(that.value) : that.value != null);

  }

    /**
   * Returns a hash code value for the object based on its id, name, inputVariable, and value.
   *
   * @return the hash code value for the object
   */
  @Override
  public int hashCode() {
    int result = id != null ? id.hashCode() : 0;
    result = 31 * result + (name != null ? name.hashCode() : 0);
    result = 31 * result + (inputVariable != null ? inputVariable.hashCode() : 0);
    result = 31 * result + (value != null ? value.hashCode() : 0);
    return result;
  }

    /**
   * Returns a string representation of the DmnEvaluatedInputImpl object, including its id, name, input variable, and value.
   */
  @Override
  public String toString() {
    return "DmnEvaluatedInputImpl{" +
      "id='" + id + '\'' +
      ", name='" + name + '\'' +
      ", inputVariable='" + inputVariable + '\'' +
      ", value=" + value +
      '}';
  }

}

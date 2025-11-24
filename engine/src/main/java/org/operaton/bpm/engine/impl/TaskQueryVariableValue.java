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
package org.operaton.bpm.engine.impl;

import java.io.Serial;
import java.util.Objects;

/**
 * Represents a variable value used in a task query.
 *
 * @author Frederik Heremans
 */
public class TaskQueryVariableValue extends QueryVariableValue {

  @Serial
  private static final long serialVersionUID = 1L;

  protected boolean isProcessInstanceVariable;

  /**
   * <p>
   * The parameters <code>isTaskVariable</code> and
   * <code>isProcessInstanceVariable</code> have the following meaning:
   * </p>
   *
   * <ul>
   * <li>if <code>isTaskVariable == true</code>: only query after task variables</li>
   * <li>if <code>isTaskVariable == false && isProcessInstanceVariable == true</code>:
   *     only query after process instance variables</li>
   * <li>if <code>isTaskVariable == false && isProcessInstanceVariable == false</code>:
   *     only query after case instance variables</li>
   * </ul>
   */
  public TaskQueryVariableValue(String name, Object value, QueryOperator operator, boolean isTaskVariable,
      boolean isProcessInstanceVariable) {
    this(name, value, operator, isTaskVariable, isProcessInstanceVariable, false, false);
  }

  public TaskQueryVariableValue(String name, Object value, QueryOperator operator, boolean isTaskVariable,
      boolean isProcessInstanceVariable, boolean variableNameIgnoreCase, boolean variableValueIgnoreCase) {
    super(name, value, operator, isTaskVariable, variableNameIgnoreCase, variableValueIgnoreCase);
    this.isProcessInstanceVariable = isProcessInstanceVariable;
  }

  public boolean isProcessInstanceVariable() {
    return isProcessInstanceVariable;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + Objects.hash(isProcessInstanceVariable);
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (!super.equals(obj))
      return false;
    if (getClass() != obj.getClass())
      return false;
    TaskQueryVariableValue other = (TaskQueryVariableValue) obj;
    return isProcessInstanceVariable == other.isProcessInstanceVariable;
  }
}

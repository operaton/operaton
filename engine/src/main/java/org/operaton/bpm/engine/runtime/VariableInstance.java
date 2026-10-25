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
package org.operaton.bpm.engine.runtime;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import org.operaton.bpm.engine.variable.value.TypedValue;

/**
 * A {@link VariableInstance} represents a variable in the execution of
 * a process instance.
 *
 * @author roman.smirnov
 *
 */
public @NullMarked interface VariableInstance {

  /**
   * @return the Id of this variable instance
   */
  @Nullable String getId();

  /**
   * Returns the name of this variable instance.
   */
  @Nullable String getName();

  /**
   * Returns the name of the type of this variable instance
   *
   * @return the type name of the variable
   */
  @Nullable String getTypeName();

  /**
   * Returns the value of this variable instance.
   */
  @Nullable Object getValue();

  /**
   * Returns the TypedValue of this variable instance.
   */
  @Nullable TypedValue getTypedValue();

  /**
   * Returns the corresponding process instance id.
   */
  @Nullable String getProcessInstanceId();

  /**
   * Returns the corresponding execution id.
   */
  @Nullable String getExecutionId();

  /**
   * Return the corresponding process definition id.
   */
  @Nullable String getProcessDefinitionId();

  /**
   * Returns the corresponding case instance id.
   */
  @Nullable String getCaseInstanceId();

  /**
   * Returns the corresponding case execution id.
   */
  @Nullable String getCaseExecutionId();

  /**
   * Returns the corresponding task id.
   */
  @Nullable String getTaskId();

  /**
   * Returns the corresponding batch id.
   */
  @Nullable String getBatchId();

  /**
   * Returns the corresponding activity instance id.
   */
  @Nullable String getActivityInstanceId();

  /**
   * If the variable value could not be loaded, this returns the error message.
   * @return an error message indicating why the variable value could not be loaded.
   */
  @Nullable String getErrorMessage();

  /**
   * The id of the tenant this variable belongs to. Can be <code>null</code>
   * if the variable belongs to no single tenant.
   */
  @Nullable String getTenantId();

}

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
package org.operaton.bpm.engine.form;


/**
 * Used to indicate the type on a {@link FormProperty}.
 * @author Tom Baeyens
 */
public interface FormType {

  /** Name for the form type. */
  String getName();

  /** Retrieve type specific extra information like
   * the list of values for enum types or the format
   * for date types. Look in the userguide for
   * which extra information keys each type provides
   * and what return type they give. */
  Object getInformation(String key);

}

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

import org.operaton.bpm.engine.FormService;


/**
 * Represents a single property on a form.
 *
 * @deprecated since 1.0, form properties are deprecated. Use {@link FormField} instead,
 *             which provides a more flexible and type-safe form field API.
 *
 * @author Tom Baeyens
 */
@Deprecated(since = "1.0")
public interface FormProperty {

  /** The key used to submit the property in {@link FormService#submitStartFormData(String, java.util.Map)}
   * or {@link FormService#submitTaskFormData(String, java.util.Map)} */
  String getId();

  /** The display label */
  String getName();

  /** Type of the property. */
  FormType getType();

  /** Optional value that should be used to display in this property */
  String getValue();

  /** Is this property read to be displayed in the form and made accessible with the methods
   * {@link FormService#getStartFormData(String)} and {@link FormService#getTaskFormData(String)}. */
  boolean isReadable();

  /** Is this property expected when a user submits the form? */
  boolean isWritable();

  /** Is this property a required input field */
  boolean isRequired();
}

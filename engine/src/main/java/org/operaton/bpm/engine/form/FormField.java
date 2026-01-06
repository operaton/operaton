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

import java.util.List;
import java.util.Map;

import org.operaton.bpm.engine.variable.value.TypedValue;

/**
 * <p>Represents an individual field in a form.</p>
 *
 * @author Michael Siebers
 * @author Daniel Meyer
 *
 */
public interface FormField {

  /**
   * @return the Id of a form property. Must be unique for a given form.
   * The id is used for mapping the form field to a process variable.
   */
  String getId();

  /**
   * @return the human-readable display name of a form property.
   */
  String getLabel();

  /**
   * @return the type of this form field.
   */
  FormType getType();

  /**
   * @return the name of the type of this form field
   */
  String getTypeName();

  /**
   * @deprecated since 1.0, use {@link #getValue()} instead, which provides type-safe access to the form field value.
   *
   * @return the default value for this form field.
   */
  @Deprecated(since = "1.0")
  Object getDefaultValue();

  /**
   * @return the value for this form field
   */
  TypedValue getValue();

  /**
   * @return a list of {@link FormFieldValidationConstraint ValidationConstraints}.
   */
  List<FormFieldValidationConstraint> getValidationConstraints();

  /**
   * @return a {@link Map} of additional properties. This map may be used for adding additional configuration
   * to a form field. An example may be layout hints such as the size of the rendered form field or information
   * about an icon to prepend or append to the rendered form field.
   */
  Map<String, String> getProperties();

  /**
   * @return true if field is defined as businessKey, false otherwise
   */
  boolean isBusinessKey();

}

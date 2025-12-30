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
package org.operaton.bpm.engine.impl.form.type;

import org.operaton.bpm.engine.form.FormType;
import org.operaton.bpm.engine.variable.value.TypedValue;


/**
 * @author Tom Baeyens
 * @author Daniel Meyer
 */
public abstract class AbstractFormFieldType implements FormType {

  public abstract TypedValue convertToFormValue(TypedValue propertyValue);

  public abstract TypedValue convertToModelValue(TypedValue propertyValue);

  /**
   * @deprecated since 1.0, use {@link #convertToModelValue(TypedValue)} instead,
   *             which provides type-safe conversion.
   */
  @Deprecated(since = "1.0")
  public abstract Object convertFormValueToModelValue(Object propertyValue);

  /**
   * @deprecated since 1.0, use {@link #convertToFormValue(TypedValue)} instead,
   *             which provides type-safe conversion.
   */
  @Deprecated(since = "1.0")
  public abstract String convertModelValueToFormValue(Object modelValue);

  @Override
  public Object getInformation(String key) {
    return null;
  }

}

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
package org.operaton.bpm.engine.rest.helper.variable;

import org.operaton.bpm.engine.variable.value.TypedValue;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

/**
 * @author Thorben Lindhauer
 *
 */
public class EqualsUntypedValue extends BaseMatcher<TypedValue> {

  protected Object value;

  public EqualsUntypedValue value(Object value) {
    this.value = value;
    return this;
  }

  @Override
  public boolean matches(Object argument) {
    if (argument == null || !TypedValue.class.isAssignableFrom(argument.getClass())) {
      return false;
    }

    TypedValue typedValue = (TypedValue) argument;

    if (typedValue.getType() != null) {
      return false;
    }

    if (value == null) {
      if (typedValue.getValue() != null) {
        return false;
      }
    } else {
      if (!value.equals(typedValue.getValue())) {
        return false;
      }
    }

    return true;
  }

  public static EqualsUntypedValue matcher() {
    return new EqualsUntypedValue();
  }

  @Override
  public void describeTo(Description description) {
    StringBuilder sb = new StringBuilder();
    sb.append(this.getClass().getSimpleName());
    sb.append(": ");
    sb.append("value=");
    sb.append(value);

    description.appendText(sb.toString());
  }

}

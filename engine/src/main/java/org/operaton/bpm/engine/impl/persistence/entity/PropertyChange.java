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
package org.operaton.bpm.engine.impl.persistence.entity;

import java.util.Date;
import org.jspecify.annotations.Nullable;

/**
 * Contains data about a property change.
 *
 * @author Daniel Meyer
 * @author Danny Gräf
 *
 */
public class PropertyChange {

  /** the empty change */
  public static final PropertyChange EMPTY_CHANGE = new PropertyChange(null, null, null);

  /** the name of the property which has been changed */
  protected String propertyName;

  /** the original value */
  protected @Nullable Object orgValue;

  /** the new value */
  protected @Nullable Object newValue;

  public PropertyChange(String propertyName, @Nullable Object orgValue, @Nullable Object newValue) {
    this.propertyName = propertyName;
    this.orgValue = orgValue;
    this.newValue = newValue;
  }

  public String getPropertyName() {
    return propertyName;
  }

  public void setPropertyName(String propertyName) {
    this.propertyName = propertyName;
  }

  public @Nullable Object getOrgValue() {
    return orgValue;
  }

  public void setOrgValue(@Nullable Object orgValue) {
    this.orgValue = orgValue;
  }

  public @Nullable Object getNewValue() {
    return newValue;
  }

  public void setNewValue(@Nullable Object newValue) {
    this.newValue = newValue;
  }

  public @Nullable String getNewValueString() {
    return valueAsString(newValue);
  }

  public @Nullable String getOrgValueString() {
    return valueAsString(orgValue);
  }

  protected @Nullable String valueAsString(@Nullable Object value) {
    if(value == null) {
      return null;

    } else if(value instanceof Date date){
      return String.valueOf(date.getTime());

    } else {
      return value.toString();

    }
  }

}

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

import org.operaton.bpm.engine.history.HistoricFormField;
import org.operaton.bpm.engine.history.HistoricFormProperty;
import org.operaton.bpm.engine.impl.history.event.HistoricFormPropertyEventEntity;


/**
 * @author Tom Baeyens
 */
public class HistoricFormPropertyEntity extends HistoricFormPropertyEventEntity implements HistoricFormProperty, HistoricFormField {

  private static final long serialVersionUID = 1L;

  @Override
  public String getPropertyValue() {
    if(propertyValue != null) {
      return propertyValue.toString();
    } else {
      return null;
    }
  }

  @Override
  public String getFieldId() {
    return propertyId;
  }

  @Override
  public Object getFieldValue() {
    return propertyValue;
  }

}

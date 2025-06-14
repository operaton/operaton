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
package org.operaton.bpm.model.bpmn.instance.operaton;

import org.operaton.bpm.model.bpmn.instance.BpmnModelElementInstance;

import java.util.Collection;

/**
 * The BPMN formProperty operaton extension element
 *
 * @author Sebastian Menski
 */
public interface OperatonFormProperty extends BpmnModelElementInstance {

  String getOperatonId();

  void setOperatonId(String operatonId);

  String getOperatonName();

  void setOperatonName(String operatonName);

  String getOperatonType();

  void setOperatonType(String operatonType);

  boolean isOperatonRequired();

  void setOperatonRequired(boolean isOperatonRequired);

  boolean isOperatonReadable();

  void setOperatonReadable(boolean isOperatonReadable);

  boolean isOperatonWriteable();

  void setOperatonWriteable(boolean isOperatonWriteable);

  String getOperatonVariable();

  void setOperatonVariable(String operatonVariable);

  String getOperatonExpression();

  void setOperatonExpression(String operatonExpression);

  String getOperatonDatePattern();

  void setOperatonDatePattern(String operatonDatePattern);

  String getOperatonDefault();

  void setOperatonDefault(String operatonDefault);

  Collection<OperatonValue> getOperatonValues();

}

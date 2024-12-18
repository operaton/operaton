/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.model.bpmn.instance;

import org.operaton.bpm.model.bpmn.builder.ServiceTaskBuilder;

/**
 * The BPMN serviceTask element
 *
 * @author Sebastian Menski
 */
public interface ServiceTask extends Task {

  @Override
  ServiceTaskBuilder builder();

  String getImplementation();

  void setImplementation(String implementation);

  Operation getOperation();

  void setOperation(Operation operation);

  /** operaton extensions */

  String getOperatonClass();

  void setOperatonClass(String operatonClass);

  String getOperatonDelegateExpression();

  void setOperatonDelegateExpression(String operatonExpression);

  String getOperatonExpression();

  void setOperatonExpression(String operatonExpression);

  String getOperatonResultVariable();

  void setOperatonResultVariable(String operatonResultVariable);

  String getOperatonType();

  void setOperatonType(String operatonType);

  String getOperatonTopic();

  void setOperatonTopic(String operatonTopic);
  
  String getOperatonTaskPriority();
  
  void setOperatonTaskPriority(String taskPriority);
}

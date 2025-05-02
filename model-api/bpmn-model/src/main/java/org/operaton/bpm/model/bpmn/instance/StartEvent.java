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

import org.operaton.bpm.model.bpmn.builder.StartEventBuilder;

/**
 * The BPMN startEvent element
 *
 * @author Sebastian Menski
 *
 */
public interface StartEvent extends CatchEvent {

  @Override
  StartEventBuilder builder();

  boolean isInterrupting();

  void setInterrupting(boolean isInterrupting);

  /** operaton extensions */

  /**
   * @deprecated Use isOperatonAsyncBefore() instead.
   */
  @Deprecated(forRemoval = true, since = "1.0.0-beta-1")
  boolean isOperatonAsync();

  /**
   * @deprecated Use setOperatonAsyncBefore(isOperatonAsyncBefore) instead.
   */
  @Deprecated(forRemoval = true, since = "1.0.0-beta-1")
  void setOperatonAsync(boolean isOperatonAsync);

  String getOperatonFormHandlerClass();

  void setOperatonFormHandlerClass(String operatonFormHandlerClass);

  String getOperatonFormKey();

  void setOperatonFormKey(String operatonFormKey);

  String getOperatonFormRef();

  void setOperatonFormRef(String operatonFormRef);

  String getOperatonFormRefBinding();

  void setOperatonFormRefBinding(String operatonFormRefBinding);

  String getOperatonFormRefVersion();

  void setOperatonFormRefVersion(String operatonFormRefVersion);

  String getOperatonInitiator();

  void setOperatonInitiator(String operatonInitiator);
}

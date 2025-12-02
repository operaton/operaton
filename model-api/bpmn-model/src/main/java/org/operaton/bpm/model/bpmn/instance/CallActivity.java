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
package org.operaton.bpm.model.bpmn.instance;

import org.operaton.bpm.model.bpmn.builder.CallActivityBuilder;

/**
 * The BPMN callActivity element
 *
 * @author Sebastian Menski
 */
public interface CallActivity extends Activity {

  @Override
  CallActivityBuilder builder();

  String getCalledElement();

  void setCalledElement(String calledElement);

  /** operaton extensions */

  /**
   * @deprecated Use {@link #isOperatonAsyncBefore()} instead.
   */
  @Deprecated(forRemoval = true, since = "1.0")
  default boolean isOperatonAsync() {
    return isOperatonAsyncBefore();
  }

  /**
   * @deprecated Use {@link #setOperatonAsyncBefore(boolean)} instead.
   */
  @Deprecated(forRemoval = true, since = "1.0")
  default void setOperatonAsync(boolean isOperatonAsync) {
    setOperatonAsyncBefore(isOperatonAsync);
  }

  String getOperatonCalledElementBinding();

  void setOperatonCalledElementBinding(String operatonCalledElementBinding);

  String getOperatonCalledElementVersion();

  void setOperatonCalledElementVersion(String operatonCalledElementVersion);

  String getOperatonCalledElementVersionTag();

  void setOperatonCalledElementVersionTag(String operatonCalledElementVersionTag);

  String getOperatonCaseRef();

  void setOperatonCaseRef(String operatonCaseRef);

  String getOperatonCaseBinding();

  void setOperatonCaseBinding(String operatonCaseBinding);

  String getOperatonCaseVersion();

  void setOperatonCaseVersion(String operatonCaseVersion);

  String getOperatonCalledElementTenantId();

  void setOperatonCalledElementTenantId(String tenantId);

  String getOperatonCaseTenantId();

  void setOperatonCaseTenantId(String tenantId);

  String getOperatonVariableMappingClass();

  void setOperatonVariableMappingClass(String operatonClass);

  String getOperatonVariableMappingDelegateExpression();

  void setOperatonVariableMappingDelegateExpression(String operatonExpression);

}

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

/**
 * A {@link OperatonFormRef} represents a reference to a deployed Operaton Form.
 */
public interface OperatonFormRef {

  /**
   * The key of a {@link OperatonFormRef} corresponds to the {@code id} attribute
   * in the Operaton Forms JSON.
   */
  String getKey();

  /**
   * The binding of {@link OperatonFormRef} specifies which version of the form
   * to reference. Possible values are: {@code latest}, {@code deployment} and
   * {@code version} (specific version value can be retrieved with {@link #getVersion()}).
   */
  String getBinding();

  /**
   * If the {@link #getBinding() binding} of a {@link OperatonFormRef} is set to
   * {@code version}, the specific version is returned.
   */
  Integer getVersion();
}

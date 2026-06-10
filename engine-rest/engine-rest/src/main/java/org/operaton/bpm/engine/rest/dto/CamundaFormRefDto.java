/*
 * Copyright 2026 the Operaton contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.engine.rest.dto;

import org.operaton.bpm.engine.form.OperatonFormRef;

/** @deprecated Use {@link OperatonFormRef} through the {@code operatonFormRef} REST field instead. */
@Deprecated
public class CamundaFormRefDto {

  protected String key;
  protected String binding;
  protected Integer version;

  public CamundaFormRefDto() {
  }

  public CamundaFormRefDto(String key, String binding, Integer version) {
    this.key = key;
    this.binding = binding;
    this.version = version;
  }

  public String getKey() {
    return key;
  }

  public String getBinding() {
    return binding;
  }

  public Integer getVersion() {
    return version;
  }

  public static CamundaFormRefDto from(OperatonFormRef operatonFormRef) {
    if (operatonFormRef == null) {
      return null;
    }
    return new CamundaFormRefDto(operatonFormRef.getKey(), operatonFormRef.getBinding(), operatonFormRef.getVersion());
  }

}

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
package org.operaton.bpm.model.bpmn.instance.dc;

import org.operaton.bpm.model.bpmn.instance.BpmnModelElementInstance;

/**
 * The DC font element
 *
 * @author Sebastian Menski
 */
public interface Font extends BpmnModelElementInstance {

  String getName();

  void setName(String name);

  Double getSize();

  void setSize(Double size);

  Boolean isBold();

  void setBold(boolean isBold);

  Boolean isItalic();

  void setItalic(boolean isItalic);

  Boolean isUnderline();

  /**
   * @deprecated use {@link #setUnderline(boolean)} instead
   */
  @Deprecated(since="1.1.0", forRemoval=true)
  @SuppressWarnings({"java:S100", "java:S1133"})
  default void SetUnderline(boolean isUnderline) {
    setUnderline(isUnderline);
  }

  @SuppressWarnings("java:S1845")
  void setUnderline(boolean isUnderline);

  Boolean isStrikeThrough();

  void setStrikeTrough(boolean isStrikeTrough);

}

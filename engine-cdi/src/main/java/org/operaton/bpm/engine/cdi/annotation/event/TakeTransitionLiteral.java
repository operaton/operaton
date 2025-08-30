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
package org.operaton.bpm.engine.cdi.annotation.event;


import java.util.Objects;
import jakarta.enterprise.util.AnnotationLiteral;

public class TakeTransitionLiteral extends AnnotationLiteral<TakeTransition> implements TakeTransition {

  protected final String transitionName;

  public TakeTransitionLiteral(String transitionName) {
    this.transitionName = transitionName;
  }
  @Override
  public String value() {
    return transitionName != null ? transitionName : "";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    TakeTransitionLiteral that = (TakeTransitionLiteral) o;
    return Objects.equals(transitionName, that.transitionName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), transitionName);
  }
}

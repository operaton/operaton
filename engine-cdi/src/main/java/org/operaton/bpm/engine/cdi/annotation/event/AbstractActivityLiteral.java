/*
 * Copyright 2025 the Operaton contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.engine.cdi.annotation.event;

import jakarta.enterprise.util.AnnotationLiteral;
import java.lang.annotation.Annotation;
import java.util.Objects;

public abstract class AbstractActivityLiteral<T extends Annotation> extends AnnotationLiteral<T> {
  protected final String activityId;

  protected AbstractActivityLiteral(String activityId) {
    this.activityId = activityId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    if (!super.equals(o))
      return false;
    DeleteTaskLiteral that = (DeleteTaskLiteral) o;
    return Objects.equals(activityId, that.taskDefinitionKey);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), activityId);
  }

}

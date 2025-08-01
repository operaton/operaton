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
package org.operaton.bpm.engine.variable.impl.value;

import java.io.Serial;


import org.operaton.bpm.engine.variable.type.ValueType;
import org.operaton.bpm.engine.variable.value.TypedValue;

/**
 * Untyped Null
 *
 * @author Daniel Meyer
 *
 */
public final class NullValueImpl implements TypedValue {

  @Serial private static final long serialVersionUID = 1L;

  private final boolean isTransient;

  // null is always null
  public static final NullValueImpl INSTANCE = new NullValueImpl(false);
  public static final NullValueImpl INSTANCE_TRANSIENT = new NullValueImpl(true);

  private NullValueImpl(boolean isTransient) {
    this.isTransient = isTransient;
  }

  @Override
  public Object getValue() {
    return null;
  }

  @Override
  public ValueType getType() {
    return ValueType.NULL;
  }

  @Override
  public String toString() {
    return "Untyped 'null' value";
  }

  @Override
  public boolean isTransient() {
    return isTransient;
  }

}

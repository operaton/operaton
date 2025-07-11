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
package org.operaton.bpm.engine.impl.core.variable.scope;

import org.operaton.bpm.engine.impl.core.variable.CoreVariableInstance;
import org.operaton.bpm.engine.variable.value.TypedValue;

/**
 * @author Daniel Meyer
 * @author Roman Smirnov
 * @author Sebastian Menski
 *
 */
public class SimpleVariableInstance implements CoreVariableInstance {

  protected String name;
  protected TypedValue value;

  public SimpleVariableInstance(String name, TypedValue value) {
    this.name = name;
    this.value = value;
  }

  @Override
  public String getName() {
    return name;
  }
  public void setName(String name) {
    this.name = name;
  }

  @Override
  public TypedValue getTypedValue(boolean deserialize) {
    return value;
  }

  @Override
  public void setValue(TypedValue value) {
    this.value = value;
  }

  public static class SimpleVariableInstanceFactory implements VariableInstanceFactory<SimpleVariableInstance> {

    public static final SimpleVariableInstanceFactory INSTANCE = new SimpleVariableInstanceFactory();

    @Override
    public SimpleVariableInstance build(String name, TypedValue value, boolean isTransient) {
      return new SimpleVariableInstance(name, value);
    }

  }

}

/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements.
 * Modifications Copyright the Operaton contributors.
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
package org.operaton.bpm.pa.service;

import java.io.ObjectInputStream;
import java.io.Serializable;

/**
 * Objects of this class cannot be de-serialized.
 *
 * @author Daniel Meyer
 *
 */
public class FailingSerializable implements Serializable {

  protected String foo;

  private static final long serialVersionUID = 1L;

  private void readObject(ObjectInputStream in) throws ClassNotFoundException {
    throw new ClassNotFoundException("Cannot load class FailingSerializable");
  }

  public String getFoo() {
    return foo;
  }

  public void setFoo(String foo) {
    this.foo = foo;
  }
}

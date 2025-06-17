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
package org.operaton.bpm.integrationtest.functional.spin.dataformat;

import java.io.Writer;

import org.operaton.spin.Spin;

/**
 * @author Thorben Lindhauer
 *
 */
public class FooSpin extends Spin<FooSpin> {

  @Override
  public String getDataFormatName() {
    return null;
  }

  @Override
  public Object unwrap() {
    return null;
  }

  @Override
  public String toString() {
    return null;
  }

  @Override
  public void writeToWriter(Writer writer) {
    // no-op
  }

  @Override
  public <C> C mapTo(Class<C> type) {
    return null;
  }

  @Override
  public <C> C mapTo(String type) {
    return null;
  }

}

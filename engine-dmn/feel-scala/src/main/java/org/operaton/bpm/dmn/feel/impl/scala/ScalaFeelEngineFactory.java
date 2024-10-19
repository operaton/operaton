/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.dmn.feel.impl.scala;

import org.operaton.bpm.dmn.feel.impl.FeelEngine;
import org.operaton.bpm.dmn.feel.impl.FeelEngineFactory;
import org.operaton.bpm.dmn.feel.impl.scala.function.FeelCustomFunctionProvider;

import java.util.List;

public class ScalaFeelEngineFactory implements FeelEngineFactory {

  protected List<FeelCustomFunctionProvider> customFunctionProviders;

  public ScalaFeelEngineFactory() {
  }

  public ScalaFeelEngineFactory(List<FeelCustomFunctionProvider> customFunctionProviders) {
    this.customFunctionProviders = customFunctionProviders;
  }

    /**
   * Creates a new instance of FeelEngine using custom function providers.
   * 
   * @return a new instance of FeelEngine
   */
  public FeelEngine createInstance() {
      return new ScalaFeelEngine(customFunctionProviders);
   }

    /**
   * Sets the list of custom function providers for the FEEL engine.
   * 
   * @param customFunctionProviders the list of custom function providers to be set
   */
  public void setCustomFunctionProviders(List<FeelCustomFunctionProvider> customFunctionProviders) {
    this.customFunctionProviders = customFunctionProviders;
  }

    /**
   * Retrieves the list of custom function providers.
   *
   * @return the list of custom function providers
   */
  public List<FeelCustomFunctionProvider> getCustomFunctionProviders() {
    return customFunctionProviders;
  }

}


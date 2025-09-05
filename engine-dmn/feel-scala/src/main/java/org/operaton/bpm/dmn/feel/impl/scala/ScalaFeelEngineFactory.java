/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements.
 * Modifications Copyright 2025 the Operaton contributors.
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
package org.operaton.bpm.dmn.feel.impl.scala;

import java.util.List;

import org.operaton.bpm.dmn.feel.impl.FeelEngine;
import org.operaton.bpm.dmn.feel.impl.FeelEngineFactory;
import org.operaton.bpm.dmn.feel.impl.scala.function.FeelCustomFunctionProvider;

/**
 * Factory class to create instances of {@link FeelEngine}, specifically {@link ScalaFeelEngine}.
 * Allows configuration of custom function providers for extending the functionality of
 * the FEEL engine.
 */
public class ScalaFeelEngineFactory implements FeelEngineFactory {

  /**
   * A protected field that stores a list of custom function providers for the FEEL engine.
   * Custom function providers are instances of {@link FeelCustomFunctionProvider} that define
   * additional custom functions to be made available in FEEL expressions.
   */
  protected List<FeelCustomFunctionProvider> customFunctionProviders;

  /**
   * Default constructor for the ScalaFeelEngineFactory.
   * Initializes the factory with default settings and no custom function providers.
   */
  public ScalaFeelEngineFactory() {
  }

  /**
   * Constructs a new instance of ScalaFeelEngineFactory with the given list of custom function providers.
   * The custom function providers allow extending the FEEL engine by adding custom functions
   * to FEEL expressions.
   *
   * @param customFunctionProviders a list of {@link FeelCustomFunctionProvider} that supply additional
   *                                custom functions to be available in the FEEL engine.
   */
  public ScalaFeelEngineFactory(List<FeelCustomFunctionProvider> customFunctionProviders) {
    this.customFunctionProviders = customFunctionProviders;
  }

  /**
   * Creates a new instance of {@link ScalaFeelEngine} configured with the optional
   * custom function providers. The instance provides an implementation of the
   * {@link FeelEngine} interface for evaluating FEEL expressions.
   *
   * @return a new instance of {@link ScalaFeelEngine}, which implements {@link FeelEngine}.
   */
  @Override
  public FeelEngine createInstance() {
      return new ScalaFeelEngine(customFunctionProviders);
   }

  /**
   * Sets the custom function providers for the engine factory. Custom function providers
   * allow extending the FEEL engine with additional custom functions that can be used
   * in FEEL expressions.
   *
   * @param customFunctionProviders a list of {@link FeelCustomFunctionProvider} instances,
   *                                each defining custom functions for the FEEL engine.
   */
  public void setCustomFunctionProviders(List<FeelCustomFunctionProvider> customFunctionProviders) {
    this.customFunctionProviders = customFunctionProviders;
  }

  /**
   * Retrieves the list of custom function providers that have been configured for the factory.
   * Custom function providers supply additional custom functions that can be used in FEEL expressions.
   *
   * @return a list of {@link FeelCustomFunctionProvider} instances, which define custom functions
   *         to extend the functionality of the FEEL engine.
   */
  public List<FeelCustomFunctionProvider> getCustomFunctionProviders() {
    return customFunctionProviders;
  }

}


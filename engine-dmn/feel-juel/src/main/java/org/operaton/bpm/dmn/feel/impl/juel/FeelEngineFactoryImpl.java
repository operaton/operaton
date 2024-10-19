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
package org.operaton.bpm.dmn.feel.impl.juel;

import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.operaton.bpm.impl.juel.jakarta.el.ELException;
import org.operaton.bpm.impl.juel.jakarta.el.ExpressionFactory;

import org.operaton.bpm.dmn.feel.impl.FeelEngine;
import org.operaton.bpm.dmn.feel.impl.FeelEngineFactory;
import org.operaton.bpm.dmn.feel.impl.juel.el.ElContextFactory;
import org.operaton.bpm.dmn.feel.impl.juel.el.FeelElContextFactory;
import org.operaton.bpm.dmn.feel.impl.juel.el.FeelTypeConverter;
import org.operaton.bpm.dmn.feel.impl.juel.transform.FeelToJuelFunctionTransformer;
import org.operaton.bpm.dmn.feel.impl.juel.transform.FeelToJuelTransform;
import org.operaton.bpm.dmn.feel.impl.juel.transform.FeelToJuelTransformImpl;
import org.operaton.commons.utils.cache.Cache;
import org.operaton.commons.utils.cache.ConcurrentLruCache;

import org.operaton.bpm.impl.juel.ExpressionFactoryImpl;

public class FeelEngineFactoryImpl implements FeelEngineFactory {

  public static final FeelEngineLogger LOG = FeelLogger.ENGINE_LOGGER;

  public static final int DEFAULT_EXPRESSION_CACHE_SIZE = 1000;

  protected final FeelEngine feelEngine;

  protected final int expressionCacheSize;
  protected final List<FeelToJuelFunctionTransformer> customFunctionTransformers;

  public FeelEngineFactoryImpl() {
    this(DEFAULT_EXPRESSION_CACHE_SIZE);
  }

  public FeelEngineFactoryImpl(int expressionCacheSize) {
      this(expressionCacheSize, Collections.<FeelToJuelFunctionTransformer> emptyList());
  }

  public FeelEngineFactoryImpl(List<FeelToJuelFunctionTransformer> customFunctionTransformers) {
      this(DEFAULT_EXPRESSION_CACHE_SIZE, customFunctionTransformers);
  }

  public FeelEngineFactoryImpl(int expressionCacheSize, List<FeelToJuelFunctionTransformer> customFunctionTransformers) {
    this.expressionCacheSize = expressionCacheSize;
    this.customFunctionTransformers = customFunctionTransformers;

    feelEngine = createFeelEngine();
  }

    /**
   * Creates and returns an instance of FeelEngine.
   *
   * @return the created instance of FeelEngine
   */
  public FeelEngine createInstance() {
    return feelEngine;
  }

    /**
   * Creates and returns a FeelEngine instance with the necessary dependencies.
   *
   * @return a new instance of FeelEngine
   */
  protected FeelEngine createFeelEngine() {
    FeelToJuelTransform transform = createFeelToJuelTransform();
    ExpressionFactory expressionFactory = createExpressionFactory();
    ElContextFactory elContextFactory = createElContextFactory();
    Cache<TransformExpressionCacheKey, String> transformExpressionCache = createTransformExpressionCache();
    return new FeelEngineImpl(transform, expressionFactory, elContextFactory, transformExpressionCache);
  }

    /**
   * Creates a FeelToJuelTransform object and adds custom function transformers to it based on the customFunctionTransformers list.
   *
   * @return the created FeelToJuelTransform object
   */
  protected FeelToJuelTransform createFeelToJuelTransform() {
    FeelToJuelTransformImpl transformer = new FeelToJuelTransformImpl();

    for (FeelToJuelFunctionTransformer functionTransformer : customFunctionTransformers) {
      transformer.addCustomFunctionTransformer(functionTransformer);
    }

    return transformer;
  }

    /**
   * Creates a new ExpressionFactory with the specified expression cache size.
   *
   * @return the newly created ExpressionFactory
   */
  protected ExpressionFactory createExpressionFactory() {
    Properties properties = new Properties();
    properties.put(ExpressionFactoryImpl.PROP_CACHE_SIZE, String.valueOf(expressionCacheSize));

    try {
      return new ExpressionFactoryImpl(properties, createTypeConverter());
    }
    catch (ELException e) {
      throw LOG.unableToInitializeFeelEngine(e);
    }
  }

    /**
   * Creates a new instance of FeelTypeConverter.
   *
   * @return a new instance of FeelTypeConverter
   */
  protected FeelTypeConverter createTypeConverter() {
    return new FeelTypeConverter();
  }

    /**
   * Creates a new ElContextFactory by initializing a FeelElContextFactory and adding custom functions from the list of customFunctionTransformers.
   * 
   * @return The newly created ElContextFactory
   */
  protected ElContextFactory createElContextFactory() {
    FeelElContextFactory factory = new FeelElContextFactory();

    for (FeelToJuelFunctionTransformer functionTransformer : customFunctionTransformers) {
      factory.addCustomFunction(functionTransformer.getName(), functionTransformer.getMethod());
    }

    return factory;
  }

    /**
   * Creates a cache for storing transformation expressions, using ConcurrentLruCache implementation.
   *
   * @return the created Cache instance
   */
  protected Cache<TransformExpressionCacheKey, String> createTransformExpressionCache() {
    return new ConcurrentLruCache<TransformExpressionCacheKey, String>(expressionCacheSize);
  }

}

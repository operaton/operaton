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
package org.operaton.bpm.dmn.engine.impl.el;

import org.operaton.bpm.impl.juel.jakarta.el.ArrayELResolver;
import org.operaton.bpm.impl.juel.jakarta.el.BeanELResolver;
import org.operaton.bpm.impl.juel.jakarta.el.CompositeELResolver;
import org.operaton.bpm.impl.juel.jakarta.el.ELContext;
import org.operaton.bpm.impl.juel.jakarta.el.ELResolver;
import org.operaton.bpm.impl.juel.jakarta.el.ListELResolver;
import org.operaton.bpm.impl.juel.jakarta.el.MapELResolver;
import org.operaton.bpm.impl.juel.jakarta.el.ResourceBundleELResolver;

import org.operaton.bpm.dmn.engine.impl.spi.el.ElExpression;
import org.operaton.bpm.dmn.engine.impl.spi.el.ElProvider;

import org.operaton.bpm.impl.juel.ExpressionFactoryImpl;
import org.operaton.bpm.impl.juel.TreeValueExpression;
import org.operaton.bpm.impl.juel.SimpleContext;

/**
 * A simple implementation of {@link ElProvider} using Juel.
 *
 * @author Daniel Meyer
 *
 */
public class JuelElProvider implements ElProvider {

  protected final ExpressionFactoryImpl factory;
  protected final JuelElContextFactory elContextFactory;
  protected final ELContext parsingElContext;

  public JuelElProvider() {
    this(new ExpressionFactoryImpl(), new JuelElContextFactory(createDefaultResolver()));
  }

  public JuelElProvider(ExpressionFactoryImpl expressionFactory, JuelElContextFactory elContextFactory) {
    this.factory = expressionFactory;
    this.elContextFactory = elContextFactory;
    this.parsingElContext = createDefaultParsingElContext();
  }

    /**
   * Creates and returns a new SimpleContext object.
   * 
   * @return a new SimpleContext object
   */
  protected SimpleContext createDefaultParsingElContext() {
    return new SimpleContext();
  }

    /**
   * Creates an EL expression from the given string representation.
   * 
   * @param expression the string representation of the EL expression
   * @return the created EL expression
   */
  public ElExpression createExpression(String expression) {
    TreeValueExpression juelExpr = factory.createValueExpression(parsingElContext, expression, Object.class);
    return new JuelExpression(juelExpr, elContextFactory);
  }

    /**
   * Returns the ExpressionFactoryImpl instance.
   *
   * @return the ExpressionFactoryImpl instance
   */
  public ExpressionFactoryImpl getFactory() {
    return factory;
  }

    /**
   * Returns the JuelElContextFactory used by this class.
   *
   * @return the JuelElContextFactory instance
   */
  public JuelElContextFactory getElContextFactory() {
    return elContextFactory;
  }

    /**
   * Returns the ELContext used for parsing.
   *
   * @return the ELContext used for parsing
   */
  public ELContext getParsingElContext() {
    return parsingElContext;
  }

    /**
   * Creates a default ELResolver with a CompositeELResolver that includes VariableContextElResolver, ArrayELResolver, 
   * ListELResolver, MapELResolver, ResourceBundleELResolver, and BeanELResolver.
   * 
   * @return the default ELResolver
   */
  protected static ELResolver createDefaultResolver() {
    CompositeELResolver resolver = new CompositeELResolver();
    resolver.add(new VariableContextElResolver());
    resolver.add(new ArrayELResolver(true));
    resolver.add(new ListELResolver(true));
    resolver.add(new MapELResolver(true));
    resolver.add(new ResourceBundleELResolver());
    resolver.add(new BeanELResolver());
    return resolver;
  }
}

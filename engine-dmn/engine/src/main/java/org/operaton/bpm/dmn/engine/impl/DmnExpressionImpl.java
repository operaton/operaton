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
package org.operaton.bpm.dmn.engine.impl;

import javax.script.CompiledScript;

import org.operaton.bpm.dmn.engine.impl.spi.el.ElExpression;
import org.operaton.bpm.dmn.engine.impl.spi.type.DmnTypeDefinition;

public class DmnExpressionImpl implements CachedCompiledScriptSupport, CachedExpressionSupport {

  protected String id;
  protected String name;

  protected DmnTypeDefinition typeDefinition;
  protected String expressionLanguage;
  protected String expression;

  protected CompiledScript cachedCompiledScript;
  protected ElExpression cachedExpression;

    /**
   * Returns the id of the object.
   *
   * @return the id
   */
  public String getId() {
    return id;
  }

    /**
   * Sets the id of the object.
   * 
   * @param id the new id to set
   */
  public void setId(String id) {
    this.id = id;
  }

    /**
   * Returns the name.
   *
   * @return the name
   */
  public String getName() {
    return name;
  }

    /**
   * Sets the name of the object.
   * 
   * @param name the new name to set
   */
  public void setName(String name) {
    this.name = name;
  }

    /**
   * Returns the type definition of this object.
   *
   * @return the type definition
   */
  public DmnTypeDefinition getTypeDefinition() {
    return typeDefinition;
  }

    /**
   * Sets the type definition for the DMN element.
   * 
   * @param typeDefinition the type definition to be set
   */
  public void setTypeDefinition(DmnTypeDefinition typeDefinition) {
    this.typeDefinition = typeDefinition;
  }

    /**
   * Returns the expression language of the object.
   * 
   * @return the expression language
   */
  public String getExpressionLanguage() {
    return expressionLanguage;
  }

    /**
   * Sets the expression language for the method.
   * 
   * @param expressionLanguage the new expression language to be set
   */
  public void setExpressionLanguage(String expressionLanguage) {
    this.expressionLanguage = expressionLanguage;
  }

    /**
   * Returns the expression stored in the object.
   *
   * @return the expression
   */
  public String getExpression() {
    return expression;
  }

    /**
   * Sets the expression for the object.
   * 
   * @param expression the expression to be set
   */
  public void setExpression(String expression) {
    this.expression = expression;
  }

    /**
   * Returns a string representation of the DmnExpressionImpl object, including its id, name, typeDefinition, expressionLanguage, and expression.
   *
   * @return a string representation of the DmnExpressionImpl object
   */
  @Override
  public String toString() {
    return "DmnExpressionImpl{" +
      "id='" + id + '\'' +
      ", name='" + name + '\'' +
      ", typeDefinition=" + typeDefinition +
      ", expressionLanguage='" + expressionLanguage + '\'' +
      ", expression='" + expression + '\'' +
      '}';
  }

    /**
   * Caches the provided compiled script.
   * 
   * @param compiledScript the compiled script to be cached
   */
  public void cacheCompiledScript(CompiledScript compiledScript) {
    this.cachedCompiledScript = compiledScript;
  }

    /**
   * Returns the cached compiled script.
   *
   * @return the cached compiled script
   */
  public CompiledScript getCachedCompiledScript() {
    return this.cachedCompiledScript;
  }

    /**
   * Returns the cached EL expression.
   *
   * @return the cached EL expression
   */
  public ElExpression getCachedExpression() {
    return this.cachedExpression;
  }

    /**
   * Sets the cached EL expression.
   * 
   * @param expression the EL expression to be cached
   */
  public void setCachedExpression(ElExpression expression) {
    this.cachedExpression = expression;
  }
}

/*
 * Copyright 2025 the Operaton contributors.
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
package org.operaton.bpm.dmn.engine.impl;

import java.util.concurrent.Callable;
import java.util.concurrent.locks.ReentrantLock;
import javax.script.CompiledScript;

import org.operaton.bpm.dmn.engine.impl.spi.el.ElExpression;
import org.operaton.bpm.dmn.engine.impl.spi.type.DmnTypeDefinition;

/**
 * Implementation of a DMN (Decision Model and Notation) expression.
 * This class provides support for caching compiled scripts and expressions,
 * as well as thread-safe execution of operations using a lock.
 */
public class DmnExpressionImpl implements CachedCompiledScriptSupport, CachedExpressionSupport {

  // Unique identifier for the expression
  protected String id;

  // Name of the expression
  protected String name;

  // Type definition of the expression
  protected DmnTypeDefinition typeDefinition;

  // Language in which the expression is written
  protected String expressionLanguage;

  // The actual expression as a string
  protected String expression;

  // Cached compiled script for the expression
  protected CompiledScript cachedCompiledScript;

  // Cached EL (Expression Language) expression
  protected ElExpression cachedExpression;

  // Lock for thread-safe operations
  private final ReentrantLock lock = new ReentrantLock();

  /**
   * Gets the unique identifier of the expression.
   *
   * @return the ID of the expression
   */
  public String getId() {
    return id;
  }

  /**
   * Sets the unique identifier of the expression.
   *
   * @param id the ID to set
   */
  public void setId(String id) {
    this.id = id;
  }

  /**
   * Gets the name of the expression.
   *
   * @return the name of the expression
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the name of the expression.
   *
   * @param name the name to set
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Gets the type definition of the expression.
   *
   * @return the type definition
   */
  public DmnTypeDefinition getTypeDefinition() {
    return typeDefinition;
  }

  /**
   * Sets the type definition of the expression.
   *
   * @param typeDefinition the type definition to set
   */
  public void setTypeDefinition(DmnTypeDefinition typeDefinition) {
    this.typeDefinition = typeDefinition;
  }

  /**
   * Gets the language of the expression.
   *
   * @return the expression language
   */
  public String getExpressionLanguage() {
    return expressionLanguage;
  }

  /**
   * Sets the language of the expression.
   *
   * @param expressionLanguage the language to set
   */
  public void setExpressionLanguage(String expressionLanguage) {
    this.expressionLanguage = expressionLanguage;
  }

  /**
   * Gets the actual expression as a string.
   *
   * @return the expression
   */
  public String getExpression() {
    return expression;
  }

  /**
   * Sets the actual expression as a string.
   *
   * @param expression the expression to set
   */
  public void setExpression(String expression) {
    this.expression = expression;
  }

  /**
   * Returns a string representation of the DMN expression.
   *
   * @return a string representation of the object
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
   * Executes a callable operation with a lock to ensure thread safety.
   *
   * @param <T> the type of the result
   * @param callable the operation to execute
   * @return the result of the callable
   * @throws IllegalStateException if an exception occurs during execution
   */
  @Override
  public <T> T executeWithLock(Callable<T> callable) {
    lock.lock();
    try {
      return callable.call();
    } catch (Exception e) {
      throw new IllegalStateException("Error executing callable with lock", e);
    } finally {
      lock.unlock();
    }
  }

  /**
   * Caches a compiled script for the expression.
   *
   * @param compiledScript the compiled script to cache
   */
  @Override
  public void cacheCompiledScript(CompiledScript compiledScript) {
    this.cachedCompiledScript = compiledScript;
  }

  /**
   * Retrieves the cached compiled script for the expression.
   *
   * @return the cached compiled script
   */
  @Override
  public CompiledScript getCachedCompiledScript() {
    return this.cachedCompiledScript;
  }

  /**
   * Retrieves the cached EL expression.
   *
   * @return the cached EL expression
   */
  @Override
  public ElExpression getCachedExpression() {
    return this.cachedExpression;
  }

  /**
   * Caches an EL expression for the expression.
   *
   * @param expression the EL expression to cache
   */
  @Override
  public void setCachedExpression(ElExpression expression) {
    this.cachedExpression = expression;
  }
}

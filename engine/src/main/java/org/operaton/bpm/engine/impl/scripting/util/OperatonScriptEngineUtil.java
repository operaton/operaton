/*
 * Copyright CIB software GmbH and/or licensed to CIB software GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. CIB software licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.operaton.bpm.engine.impl.scripting.util;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import org.operaton.bpm.engine.impl.scripting.engine.OperatonScriptEngineManager;

/**
 * Utility class for creating script engines with Operaton namespace support.
 * This class provides helper methods for users who want to create their own
 * script engines with the OperatonClassLoader without having compile-time
 * dependencies on specific script engine implementations.
 * 
 * @author Operaton Team
 */
public class OperatonScriptEngineUtil {
  
  /**
   * Creates a Groovy script engine with OperatonClassLoader support.
   * This method uses reflection to avoid compile-time dependencies on Groovy.
   * 
   * Usage example:
   * <pre>
   * ScriptEngine groovyEngine = OperatonScriptEngineUtil.createGroovyScriptEngine();
   * if (groovyEngine != null) {
   *   // Use the script engine
   *   groovyEngine.eval("println 'Hello from Operaton!'");
   * }
   * </pre>
   * 
   * @return A Groovy ScriptEngine with OperatonClassLoader, or null if Groovy is not available
   */
  public static ScriptEngine createGroovyScriptEngine() {
    return createGroovyScriptEngine(Thread.currentThread().getContextClassLoader());
  }
  
  /**
   * Creates a Groovy script engine with OperatonClassLoader support using a specific parent ClassLoader.
   * 
   * @param parentClassLoader The parent ClassLoader to use
   * @return A Groovy ScriptEngine with OperatonClassLoader, or null if Groovy is not available
   */
  public static ScriptEngine createGroovyScriptEngine(ClassLoader parentClassLoader) {
    try {
      ClassLoader cibSevenClassLoader = OperatonScriptEngineManager.createOperatonClassLoader(parentClassLoader);
      
      // Use reflection to create GroovyClassLoader
      Class<?> groovyClassLoaderClass = Class.forName("groovy.lang.GroovyClassLoader", true, cibSevenClassLoader);
      var groovyClassLoaderConstructor = groovyClassLoaderClass.getConstructor(ClassLoader.class);
      var groovyClassLoader = groovyClassLoaderConstructor.newInstance(cibSevenClassLoader);
      
      // Use reflection to create GroovyScriptEngineImpl
      Class<?> groovyScriptEngineClass = Class.forName("org.codehaus.groovy.jsr223.GroovyScriptEngineImpl", true, cibSevenClassLoader);
      var groovyScriptEngineConstructor = groovyScriptEngineClass.getConstructor(groovyClassLoaderClass);
      return (ScriptEngine) groovyScriptEngineConstructor.newInstance(groovyClassLoader);
      
    } catch (Exception e) {
      // Return null if Groovy is not available
      return null;
    }
  }
  
  /**
   * Creates a custom ScriptEngineManager that uses OperatonClassLoader for script engines.
   * This can be used to get any script engine with Operaton namespace support.
   * 
   * @return A ScriptEngineManager with Operaton support
   */
  public static ScriptEngineManager createOperatonScriptEngineManager() {
    return new OperatonScriptEngineManager();
  }
  
  /**
   * Gets the OperatonClassLoader that can be used with any script engine.
   * This classloader automatically translates Camunda namespace classes to Operaton namespace.
   * 
   * Usage example with any script engine:
   * <pre>
   * ClassLoader operatonClassLoader = OperatonScriptEngineUtil.getOperatonClassLoader();
   * // Use with any script engine that accepts a ClassLoader
   * </pre>
   * 
   * @return A ClassLoader that handles Operaton namespace translation
   */
  public static ClassLoader getOperatonClassLoader() {
    return OperatonScriptEngineManager.createOperatonClassLoader();
  }
  
  /**
   * Gets the OperatonClassLoader with a specific parent ClassLoader.
   * 
   * @param parentClassLoader The parent ClassLoader
   * @return A ClassLoader that handles Operaton namespace translation
   */
  public static ClassLoader getOperatonClassLoader(ClassLoader parentClassLoader) {
    return OperatonScriptEngineManager.createOperatonClassLoader(parentClassLoader);
  }
}

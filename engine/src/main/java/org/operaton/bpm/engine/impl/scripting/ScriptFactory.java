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
package org.operaton.bpm.engine.impl.scripting;

import org.operaton.bpm.engine.delegate.Expression;

/**
 * <p>A script factory is responsible for creating {@link ExecutableScript}
 * instances. Users may customize (subclass) this class in order to customize script
 * creation.</p>
 *
 * <p>The default executable script implementations created by this factory preserve
 * configured script preprocessing behavior automatically. Custom subclasses that
 * return their own {@link ExecutableScript} implementations should ensure that
 * configured script preprocessors are still applied before evaluation if they
 * want those custom implementations to participate in the script preprocessing
 * feature.</p>
 *
 * @author Daniel Meyer
 *
 */
public class ScriptFactory {

  public ExecutableScript createScriptFromResource(String language, String resource) {
    return new ResourceExecutableScript(language, resource);
  }

  public ExecutableScript createScriptFromResource(String language, Expression resourceExpression) {
    return new DynamicResourceExecutableScript(language, resourceExpression);
  }

  public ExecutableScript createScriptFromSource(String language, String source) {
    return new SourceExecutableScript(language, source);
  }

  public ExecutableScript createScriptFromSource(String language, Expression sourceExpression) {
    return new DynamicSourceExecutableScript(language, sourceExpression);
  }

}

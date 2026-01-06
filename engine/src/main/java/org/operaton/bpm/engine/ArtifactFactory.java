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
package org.operaton.bpm.engine;

/**
 * Create and destroy artifacts of a given class in a container specific way.
 * This SPI hides differences between CDI, Spring, etc.
 * <br/>
 * Samples:
 * <pre>
 *     &lt;operaton:taskListener class="org.mypackage.MyListener".../&gt;
 *     or
 *     &lt;serviceTask operaton:class=""org.mypackage.MyJavaDelegate".. /&gt;
 * </pre>
 *
 * <p>
 * The default implementation uses Class.newInstance to create artifacts.
 * The CDI specific version utilizes the BeanManager to resolve the
 * Contextual Instances.
 * </p>
 *
 * @author <a href="mailto:struberg@yahoo.de">Mark Struberg</a>
 */
public interface ArtifactFactory {
  /**
   *
   * @param clazz of the artifact to create
   * @return the instance of the fullyQualifiedClassName
   */
  <T> T getArtifact(Class<T> clazz);

}

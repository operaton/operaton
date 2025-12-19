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
package org.operaton.bpm.engine.impl;

import org.operaton.bpm.engine.ArtifactFactory;
import org.operaton.bpm.engine.ProcessEngineException;

/**
 * Default ArtifactService implementation.
 * This version uses Class.newInstance() to create
 * new Artifacts.
 * This is the default behaviour like has been in old
 * operaton/activity versions.
 *
 * @since 7.2.0
 * @author <a href="mailto:struberg@yahoo.de">Mark Struberg</a>
 */
public class DefaultArtifactFactory implements ArtifactFactory {
  @Override
  public <T> T getArtifact(Class<T> clazz) {
    try {
      return clazz.getDeclaredConstructor().newInstance();
    } catch (Exception e) {
      throw new ProcessEngineException("couldn't instantiate class %s".formatted(clazz.getName()), e);
    }
  }
}

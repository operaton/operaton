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
package org.operaton.bpm.dmn.engine.impl.spi.type;

/**
 * Provide {@link DmnDataTypeTransformer}s for specific type names.
 *
 * @author Philipp Ossler
 */
public interface DmnDataTypeTransformerRegistry {

  /**
   * Returns the matching transformer for the given type.
   *
   * @param typeName name of the type
   * @return the matching transformer
   */
  DmnDataTypeTransformer getTransformer(String typeName);

    /**
   * Adds a custom transformer for a specific data type.
   *
   * @param typeName the name of the data type
   * @param transformer the custom transformer to be added
   */
  void addTransformer(String typeName, DmnDataTypeTransformer transformer);

}

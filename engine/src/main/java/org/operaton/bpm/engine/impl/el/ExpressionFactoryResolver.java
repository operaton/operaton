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
package org.operaton.bpm.engine.impl.el;

import jakarta.el.ExpressionFactory;
import org.operaton.bpm.impl.juel.ExpressionFactoryImpl;



/**
 * Class used to get hold of a {@link ExpressionFactory}.
 *
 * @author Frederik Heremans
 */
public class ExpressionFactoryResolver {
  private ExpressionFactoryResolver() {
    // private constructor to prevent instantiation
  }

  public static ExpressionFactory resolveExpressionFactory() {
    // Return instance of custom JUEL implementation
    return new ExpressionFactoryImpl();
  }
}

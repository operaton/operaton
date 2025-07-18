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
package org.operaton.bpm.engine.impl.tree;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.operaton.bpm.engine.impl.persistence.entity.ExecutionEntity;

/**
 *
 * @author Christopher Zell <christopher.zell@camunda.com>
 */
public class ExecutionTopDownWalker extends ReferenceWalker<ExecutionEntity> {

  public ExecutionTopDownWalker(ExecutionEntity initialElement) {
    super(initialElement);
  }

  public ExecutionTopDownWalker(List<ExecutionEntity> initialElements) {
    super(initialElements);
  }

  @Override
  protected Collection<ExecutionEntity> nextElements() {
    List<ExecutionEntity> executions = getCurrentElement().getExecutions();
    if (executions == null) {
      executions = new ArrayList<>();
    }
    return executions;
  }
}

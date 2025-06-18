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
package org.operaton.bpm.engine.impl.bpmn.parser;

import java.util.List;

import org.operaton.bpm.engine.impl.core.variable.mapping.IoMapping;
import org.operaton.bpm.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.operaton.bpm.engine.impl.pvm.process.ActivityImpl;
import org.operaton.bpm.engine.impl.pvm.process.ScopeImpl;
import org.operaton.bpm.engine.impl.pvm.process.TransitionImpl;
import org.operaton.bpm.engine.impl.util.xml.Element;
import org.operaton.bpm.engine.impl.variable.VariableDeclaration;

/**
 * Abstract base class for implementing a {@link BpmnParseListener} without being forced
 * to implement all methods provided, which makes the implementation more robust to
 * future changes.
 *
 * @author ruecker
 *
 * @deprecated Will be removed in a future version. Implement BpmnParseListener directly.
 */
@Deprecated(forRemoval = true)
public class AbstractBpmnParseListener implements BpmnParseListener {

  // no-op implementation retained for backwards compatibility
}

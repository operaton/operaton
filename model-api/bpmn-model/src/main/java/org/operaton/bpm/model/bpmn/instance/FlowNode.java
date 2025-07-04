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
package org.operaton.bpm.model.bpmn.instance;

import org.operaton.bpm.model.bpmn.Query;
import org.operaton.bpm.model.bpmn.builder.AbstractFlowNodeBuilder;

import java.util.Collection;

/**
 * The BPMN flowNode element
 *
 * @author Sebastian Menski
 */
public interface FlowNode extends FlowElement {

  @Override
  @SuppressWarnings("rawtypes")
  AbstractFlowNodeBuilder builder();

  Collection<SequenceFlow> getIncoming();

  Collection<SequenceFlow> getOutgoing();

  Query<FlowNode> getPreviousNodes();

  Query<FlowNode> getSucceedingNodes();

  boolean isOperatonAsyncBefore();

  void setOperatonAsyncBefore(boolean isOperatonAsyncBefore);

  boolean isOperatonAsyncAfter();

  void setOperatonAsyncAfter(boolean isOperatonAsyncAfter);

  boolean isOperatonExclusive();

  void setOperatonExclusive(boolean isOperatonExclusive);

  String getOperatonJobPriority();

  void setOperatonJobPriority(String jobPriority);

}

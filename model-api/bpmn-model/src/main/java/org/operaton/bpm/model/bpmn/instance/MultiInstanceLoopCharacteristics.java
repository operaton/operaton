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

import java.util.Collection;

import org.operaton.bpm.model.bpmn.MultiInstanceFlowCondition;
import org.operaton.bpm.model.bpmn.builder.MultiInstanceLoopCharacteristicsBuilder;

/**
 * The BPMN 2.0 multiInstanceLoopCharacteristics element type
 *
 * @author Filip Hrisafov
 *
 */
public interface MultiInstanceLoopCharacteristics extends LoopCharacteristics {

  LoopCardinality getLoopCardinality();

  void setLoopCardinality(LoopCardinality loopCardinality);

  DataInput getLoopDataInputRef();

  void setLoopDataInputRef(DataInput loopDataInputRef);

  DataOutput getLoopDataOutputRef();

  void setLoopDataOutputRef(DataOutput loopDataOutputRef);

  InputDataItem getInputDataItem();

  void setInputDataItem(InputDataItem inputDataItem);

  OutputDataItem getOutputDataItem();

  void setOutputDataItem(OutputDataItem outputDataItem);

  Collection<ComplexBehaviorDefinition> getComplexBehaviorDefinitions();

  CompletionCondition getCompletionCondition();

  void setCompletionCondition(CompletionCondition completionCondition);

  boolean isSequential();

  void setSequential(boolean sequential);

  MultiInstanceFlowCondition getBehavior();

  void setBehavior(MultiInstanceFlowCondition behavior);

  EventDefinition getOneBehaviorEventRef();

  void setOneBehaviorEventRef(EventDefinition oneBehaviorEventRef);

  EventDefinition getNoneBehaviorEventRef();

  void setNoneBehaviorEventRef(EventDefinition noneBehaviorEventRef);

  String getOperatonCollection();

  void setOperatonCollection(String expression);

  String getOperatonElementVariable();

  void setOperatonElementVariable(String variableName);

  boolean isOperatonAsyncBefore();

  void setOperatonAsyncBefore(boolean isOperatonAsyncBefore);

  boolean isOperatonAsyncAfter();

  void setOperatonAsyncAfter(boolean isOperatonAsyncAfter);

  boolean isOperatonExclusive();

  void setOperatonExclusive(boolean isOperatonExclusive);

  @Override
  MultiInstanceLoopCharacteristicsBuilder builder();

}

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

import org.operaton.bpm.model.bpmn.builder.SubProcessBuilder;

/**
 * The BPMN subProcess element
 *
 * @author Sebastian Menski
 */
public interface SubProcess extends Activity {

  @Override
  SubProcessBuilder builder();

  boolean triggeredByEvent();

  void setTriggeredByEvent(boolean triggeredByEvent);

  Collection<LaneSet> getLaneSets();

  Collection<FlowElement> getFlowElements();

  Collection<Artifact> getArtifacts();

  /** operaton extensions */

  /**
   * @deprecated Use isOperatonAsyncBefore() instead.
   */
  @Deprecated(forRemoval = true, since = "1.0")
  boolean isOperatonAsync();

  /**
   * @deprecated Use setOperatonAsyncBefore(isOperatonAsyncBefore) instead.
   */
  @Deprecated(forRemoval = true, since = "1.0")
  void setOperatonAsync(boolean isOperatonAsync);
}

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
package org.operaton.bpm.engine.impl.pvm.process;

import java.util.ArrayList;
import java.util.List;

import org.operaton.bpm.engine.delegate.ExecutionListener;
import org.operaton.bpm.engine.impl.core.model.CoreModelElement;
import org.operaton.bpm.engine.impl.pvm.PvmActivity;
import org.operaton.bpm.engine.impl.pvm.PvmProcessDefinition;
import org.operaton.bpm.engine.impl.pvm.PvmTransition;


/**
 * @author Tom Baeyens
 */
public class TransitionImpl extends CoreModelElement implements PvmTransition {

  private static final long serialVersionUID = 1L;

  protected ActivityImpl source;
  protected ActivityImpl destination;

  protected ProcessDefinitionImpl processDefinition;

  /** Graphical information: a list of waypoints: x1, y1, x2, y2, x3, y3, .. */
  protected List<Integer> waypoints = new ArrayList<>();


  public TransitionImpl(String id, ProcessDefinitionImpl processDefinition) {
    super(id);
    this.processDefinition = processDefinition;
  }

  @Override
  public ActivityImpl getSource() {
    return source;
  }

  public void setDestination(ActivityImpl destination) {
    this.destination = destination;
    destination.getIncomingTransitions().add(this);
  }

  /**
   * @deprecated Use {@link #addListener(ExecutionListener.EVENTNAME_TAKE, executionListener)} instead.
   */
  @Deprecated(forRemoval = true, since = "1.0")
  public void addExecutionListener(ExecutionListener executionListener) {
    super.addListener(ExecutionListener.EVENTNAME_TAKE, executionListener);
  }

  /**
   * @deprecated Use {@link #getListeners(ExecutionListener.EVENTNAME_TAKE)} instead.
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  @Deprecated(forRemoval = true, since = "1.0")
  public List<ExecutionListener> getExecutionListeners() {
    return (List) super.getListeners(ExecutionListener.EVENTNAME_TAKE);
  }

  /**
   * @deprecated Use {@link #addListener(ExecutionListener.EVENTNAME_TAKE, executionListener)} in a loop instead.
   */
  @Deprecated(forRemoval = true, since = "1.0")
  public void setExecutionListeners(List<ExecutionListener> executionListeners) {
    for (ExecutionListener executionListener : executionListeners) {
      addListener(ExecutionListener.EVENTNAME_TAKE, executionListener);
    }
  }

  @Override
  public String toString() {
    return "("+source.getId()+")--"+(id!=null?id+"-->(":">(")+destination.getId()+")";
  }

  // getters and setters //////////////////////////////////////////////////////

  @Override
  public PvmProcessDefinition getProcessDefinition() {
    return processDefinition;
  }

  protected void setSource(ActivityImpl source) {
    this.source = source;
  }

  @Override
  public PvmActivity getDestination() {
    return destination;
  }

  public List<Integer> getWaypoints() {
    return waypoints;
  }

  public void setWaypoints(List<Integer> waypoints) {
    this.waypoints = waypoints;
  }

}

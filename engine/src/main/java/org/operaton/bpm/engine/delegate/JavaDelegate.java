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
package org.operaton.bpm.engine.delegate;


/**
 * Convience class that should be used when a Java delegation in a BPMN 2.0
 * process is required (for example, to call custom business logic).
 *
 * This class can be used for both service tasks and event listeners.
 *
 * This class does not allow to influence the control flow. It follows the
 * default BPMN 2.0 behavior of taking every outgoing sequence flow (which has a
 * condition that evaluates to true if there is a condition defined) If you are
 * in need of influencing the flow in your process, use the
 * class 'org.operaton.bpm.engine.impl.pvm.delegate.ActivityBehavior' instead.
 *
 * @author Joram Barrez
 */
public interface JavaDelegate {

  void execute(DelegateExecution execution) throws Exception;

}

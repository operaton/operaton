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
package org.operaton.bpm.integrationtest.functional.event.beans;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.operaton.bpm.engine.cdi.BusinessProcess;
import org.operaton.bpm.engine.cdi.BusinessProcessEvent;

/**
 * @author Daniel Meyer
 *
 */
public class EventObserverCdiBean {

  public static final String LISTENER_INVOCATION_COUNT = "listenerInvocationCount";

  @Inject
  private BusinessProcess businessProcess;

  public void handleAllEvents(@Observes BusinessProcessEvent businessProcessEvent) {

    Integer listenerInvocationCount = businessProcess.getVariable(LISTENER_INVOCATION_COUNT);
    if(listenerInvocationCount == null) {
      listenerInvocationCount = 0;
    }

    listenerInvocationCount += 1;

    businessProcess.setVariable(LISTENER_INVOCATION_COUNT, listenerInvocationCount);
  }

}

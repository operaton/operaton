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
package org.operaton.bpm.engine.test.api.runtime.migration.models;

import org.operaton.bpm.model.bpmn.BpmnModelInstance;

/**
 * @author Thorben Lindhauer
 *
 */
public class TimerCatchModels {

  public static final BpmnModelInstance ONE_TIMER_CATCH_PROCESS = ProcessModels.newModel()
    .startEvent()
    .intermediateCatchEvent("timerCatch")
      .timerWithDuration("PT10M")
    .userTask("userTask")
    .endEvent()
    .done();

  public static final BpmnModelInstance SUBPROCESS_TIMER_CATCH_PROCESS = ProcessModels.newModel()
      .startEvent()
      .subProcess("subProcess")
      .embeddedSubProcess()
        .startEvent()
        .intermediateCatchEvent("timerCatch")
          .timerWithDuration("PT10M")
        .userTask("userTask")
        .endEvent()
      .subProcessDone()
      .endEvent()
      .done();
}

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
package org.operaton.bpm.engine.test.util;

import java.util.ArrayList;
import java.util.List;

import org.operaton.bpm.engine.delegate.DelegateExecution;
import org.operaton.bpm.engine.delegate.ExecutionListener;

/**
 * @author Daniel Meyer
 *
 */
public class TestExecutionListener implements ExecutionListener {

  public static List<String> collectedEvents = new ArrayList<>();

  @Override
  public void notify(DelegateExecution execution) throws Exception {
    String counterKey = execution.getCurrentActivityId() + "-" +execution.getEventName();
    collectedEvents.add(counterKey);
  }

  public static void reset() {
    collectedEvents.clear();
  }

}

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
package org.operaton.bpm.engine.test.api.mock;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNull;

import java.util.HashMap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.delegate.DelegateExecution;
import org.operaton.bpm.engine.impl.mock.Mocks;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;

/**
 * @author Tassilo Weidner
 */
@ExtendWith(ProcessEngineExtension.class)
class MocksTest {

  protected RuntimeService runtimeService;
  protected TaskService taskService;

  @Test
  void testMethodsOfMocksAPI() {
    //given
    HashMap<String, Object> map = new HashMap<>();

    for (int i = 0; i < 5; i++) {
      map.put("key" + i, new Object());
    }

    //when
    for (String key : map.keySet()) {
      Mocks.register(key, map.get(key));
    }

    //then
    for (String key : map.keySet()) {
      assertEquals(map.get(key), Mocks.get(key));
    }

    assertEquals(map, Mocks.getMocks());

    Mocks.reset();

    for (String key : map.keySet()) {
      assertNull(Mocks.get(key));
    }

    assertEquals(0, Mocks.getMocks().size());
  }

  @Test
  @Deployment
  void testMockAvailabilityInScriptTask() {
    testMockAvailability();
  }

  @Test
  @Deployment
  void testMockAvailabilityInExpressionLanguage() {
    testMockAvailability();
  }

  //helper ////////////////////////////////////////////////////////////
  private void testMockAvailability() {
    //given
    Mocks.register("myMock", new MyPojo());

    //when
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("mocksTest");
    Mocks.reset();

    //then
    assertEquals("testValue", runtimeService.getVariable(pi.getId(), "testVar"));
  }

  public static class MyPojo {

    public String test = "testValue";

    public String getTest() {
      return test;
    }

    public void testMethod(DelegateExecution execution, String str) {
      execution.setVariable("testVar", str);
    }

  }
}

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
package org.operaton.bpm.engine.test.bpmn.exclusive;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.operaton.bpm.engine.test.Deployment;

/**
 * @author Daniel Meyer
 *
 */
class ExclusiveTaskReuseCacheTest extends ExclusiveTaskTest {

  @BeforeEach
  void setUp() {
    processEngineConfiguration.setDbEntityCacheReuseEnabled(true);
  }

  @AfterEach
  void tearDown() {
    processEngineConfiguration.setDbEntityCacheReuseEnabled(false);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/exclusive/ExclusiveTaskTest.testNonExclusiveService.bpmn20.xml"})
  @Override
  @Test
  public void testNonExclusiveService() {
    super.testNonExclusiveService();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/exclusive/ExclusiveTaskTest.testExclusiveService.bpmn20.xml"})
  @Override
  @Test
  public void testExclusiveService() {
    super.testExclusiveService();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/exclusive/ExclusiveTaskTest.testExclusiveServiceConcurrent.bpmn20.xml"})
  @Override
  @Test
  public void testExclusiveServiceConcurrent() {
    super.testExclusiveServiceConcurrent();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/exclusive/ExclusiveTaskTest.testExclusiveSequence2.bpmn20.xml"})
  @Override
  @Test
  public void testExclusiveSequence2() {
    super.testExclusiveSequence2();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/exclusive/ExclusiveTaskTest.testExclusiveSequence3.bpmn20.xml"})
  @Override
  @Test
  public void testExclusiveSequence3() {
    super.testExclusiveSequence3();
  }

}

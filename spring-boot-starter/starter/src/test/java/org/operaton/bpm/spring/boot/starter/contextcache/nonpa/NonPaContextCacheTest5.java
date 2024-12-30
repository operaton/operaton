/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.spring.boot.starter.contextcache.nonpa;

import org.operaton.bpm.engine.ProcessEngines;
import org.operaton.bpm.spring.boot.starter.contextcache.AbstractContextCacheTest;
import org.operaton.bpm.spring.boot.starter.test.nonpa.TestApplication;
import static org.operaton.bpm.engine.test.assertions.bpmn.AbstractAssertions.init;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests {@link NonPaContextCacheTest1}, {@link NonPaContextCacheTest2}, {@link NonPaContextCacheTest3},
 * {@link NonPaContextCacheTest4}and {@link NonPaContextCacheTest5} are meant to be run together so that
 * ApplicationContext caching is tested.
 * See {@link NonPaContextCacheSuiteTest} for a detailed explanation.
 *
 * @author Nikola Koevski
 */
@ActiveProfiles("contextcaching")
@SpringBootTest(
  classes = { TestApplication.class },
  properties = {
    "operaton.bpm.generate-unique-process-engine-name=true",
    "spring.datasource.generate-unique-name=true",
    "test5Property=test5Value"
  },
  webEnvironment = SpringBootTest.WebEnvironment.NONE
)
class NonPaContextCacheTest5 extends AbstractContextCacheTest {

  @BeforeEach
  void setUp() {
    this.testName = "nonPaTest5";
    contextMap.put(this.testName, applicationContext.hashCode());

    // ensure that Operaton Assert is using the non-default engine
    init(processEngine);
  }

  @Test
  void testContextCaching() {
    assertThat(applicationContext.hashCode())
        .isNotEqualTo(contextMap.get("nonPaTest1"))
        .isNotEqualTo(contextMap.get("nonPaTest2"));
  }

  @Test
  void testEngineName()
  {
    assertThat(processEngine.getName())
        .isNotEqualTo(ProcessEngines.NAME_DEFAULT)
        .containsPattern("processEngine\\w{10}");
  }
}

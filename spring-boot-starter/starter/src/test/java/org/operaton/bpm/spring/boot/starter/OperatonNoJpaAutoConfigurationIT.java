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
package org.operaton.bpm.spring.boot.starter;

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.spring.boot.starter.test.nonpa.TestApplication;
import org.operaton.bpm.spring.boot.starter.test.nonpa.jpa.domain.TestEntity;
import org.operaton.bpm.spring.boot.starter.test.nonpa.jpa.repository.TestEntityRepository;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {TestApplication.class},
  webEnvironment = WebEnvironment.NONE,
  properties = {
    "operaton.bpm.generate-unique-process-application-name=true",
    "spring.datasource.generate-unique-name=true",
  })
@ActiveProfiles("nojpa")
class OperatonNoJpaAutoConfigurationIT extends AbstractOperatonAutoConfigurationIT {

  private final TestEntityRepository testEntityRepository;

  @Autowired
  public OperatonNoJpaAutoConfigurationIT(TestEntityRepository testEntityRepository) {
      this.testEntityRepository = testEntityRepository;
  }

  @Test
  void jpaDisabledTest() {
    TestEntity testEntity = testEntityRepository.save(new TestEntity());
    Map<String, Object> variables = new HashMap<>();
    variables.put("test", testEntity);
    assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("TestProcess", variables), "")
        .isNotNull()
        .isInstanceOf(ProcessEngineException.class);
  }

  @Test
  void pojoTest() {
    Map<String, Object> variables = new HashMap<>();
    Pojo pojo = new Pojo();
    variables.put("test", pojo);
    assertThat(runtimeService.startProcessInstanceByKey("TestProcess", variables)).isNotNull();
  }

  public static class Pojo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
  }
}

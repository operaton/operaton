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
package org.operaton.bpm.spring.boot.starter;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.spring.boot.starter.test.nonpa.TestApplication;
import org.operaton.bpm.spring.boot.starter.test.nonpa.jpa.domain.TestEntity;
import org.operaton.bpm.spring.boot.starter.test.nonpa.jpa.repository.TestEntityRepository;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest(classes = { TestApplication.class },
  webEnvironment = WebEnvironment.NONE,
  properties = {
    "operaton.bpm.generate-unique-process-application-name=true",
    "spring.datasource.generate-unique-name=true",
  })
@ActiveProfiles("nojpa")
public class OperatonNoJpaAutoConfigurationIT extends AbstractOperatonAutoConfigurationIT {

  @Autowired
  private TestEntityRepository testEntityRepository;

  @Test
  public void jpaDisabledTest() {
    TestEntity testEntity = testEntityRepository.save(new TestEntity());
    Map<String, Object> variables = new HashMap<>();
    variables.put("test", testEntity);
    try {
      runtimeService.startProcessInstanceByKey("TestProcess", variables);
      fail();
    } catch (ProcessEngineException e) {
      assertNotNull(e);
    }
  }

  @Test
  public void pojoTest() {
    Map<String, Object> variables = new HashMap<>();
    Pojo pojo = new Pojo();
    variables.put("test", pojo);
    assertNotNull(runtimeService.startProcessInstanceByKey("TestProcess", variables));
  }

  public static class Pojo implements Serializable {

    private static final long serialVersionUID = 1L;

  }
}

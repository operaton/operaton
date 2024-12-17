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
package org.operaton.bpm.spring.boot.starter.configuration.id;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.impl.cfg.IdGenerator;
import org.operaton.bpm.spring.boot.starter.property.OperatonBpmProperties;
import org.operaton.bpm.spring.boot.starter.test.nonpa.TestApplication;
import org.operaton.bpm.spring.boot.starter.util.OperatonSpringBootUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import static org.assertj.core.api.Assertions.assertThat;
import static org.operaton.bpm.spring.boot.starter.configuration.id.IdGeneratorConfiguration.PREFIXED;

@RunWith(SpringRunner.class)
@SpringBootTest(
  classes = {TestApplication.class},
  properties = {
    "operaton.bpm.id-generator=" + PREFIXED,
    "spring.application.name=myapp"
  })
public class PrefixedUuidGeneratorIT {

  @Autowired
  private IdGenerator idGenerator;

  @Autowired
  private OperatonBpmProperties properties;

  @Autowired
  private ProcessEngine processEngine;

  @Test
  public void property_is_set() {
    assertThat(properties.getIdGenerator()).isEqualTo(IdGeneratorConfiguration.PREFIXED);
  }

  @Test
  public void configured_idGenerator_is_uuid() {
    final IdGenerator idGenerator = OperatonSpringBootUtil.get(processEngine).getIdGenerator();

    assertThat(idGenerator).isOfAnyClassIn(PrefixedUuidGenerator.class);
  }

  @Test
  public void nextId_is_uuid() {
    assertThat(idGenerator.getNextId().split("-")).hasSize(6).startsWith("myapp");
  }
}

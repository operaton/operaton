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

import org.operaton.bpm.application.ProcessApplicationInfo;
import org.operaton.bpm.engine.spring.application.SpringProcessApplication;
import org.operaton.bpm.spring.boot.starter.test.pa.TestProcessApplication;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
  classes = { TestProcessApplication.class },
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ActiveProfiles("customContextPath")
class CustomContextPathWebProcessApplicationIT {

  private final SpringProcessApplication application;

  @Autowired
  public CustomContextPathWebProcessApplicationIT(SpringProcessApplication application) {
      this.application = application;
  }

  @Test
  void postDeployEvent() {
    assertThat(application).isNotNull();
    assertThat(application.getProperties()).containsEntry(ProcessApplicationInfo.PROP_SERVLET_CONTEXT_PATH, "/my/custom/context/path");
  }

}

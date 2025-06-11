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
package org.operaton.bpm.run.test.config.filter;

import org.operaton.bpm.engine.rest.dto.runtime.FilterDto;
import org.operaton.bpm.engine.rest.dto.task.TaskQueryDto;
import org.operaton.bpm.run.OperatonApp;
import org.operaton.bpm.run.test.AbstractRestTest;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {OperatonApp.class}, webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles(profiles = {"test-default-task-filter"})
class DefaultTaskFilterTest extends AbstractRestTest {

  @Test
  void shouldCreateDefaultTaskFilter() {

    // given default task filter enabled
    String url = "http://localhost:" + localPort + CONTEXT_PATH + "/filter";

    // when
    ResponseEntity<List<FilterDto>> response = testRestTemplate.exchange(url, HttpMethod.GET, null,
        new ParameterizedTypeReference<>() {
        });

    // then
    List<FilterDto> filters = response.getBody();
    assertThat(filters).hasSize(1);
    FilterDto filter = filters.get(0);
    assertThat(filter.getName()).isEqualTo("All tasks");
    assertThat(filter.getResourceType()).isEqualTo("Task");
    assertThat(filter.getQuery()).isInstanceOf(TaskQueryDto.class);
  }
}

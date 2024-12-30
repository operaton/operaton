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
package org.operaton.bpm.spring.boot.starter.property;

import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Nikola Koevski
 */
@TestPropertySource(properties = {
  "operaton.bpm.webapp.indexRedirectEnabled=false",
  "operaton.bpm.webapp.csrf.targetOrigin=localhost:8080",
  "operaton.bpm.webapp.csrf.denyStatus=405",
  "operaton.bpm.webapp.csrf.randomClass=java.util.Random",
  "operaton.bpm.webapp.csrf.entryPoints[0]=/api/engine/engine/default/history/task/count",
  "operaton.bpm.webapp.csrf.entryPoints[1]=/api/engine/engine/default/history/variable/count"
})
public class WebappPropertyTest extends ParsePropertiesHelper {

  @Test
  public void testIndexRedirectEnabled() {
    // given Operaton properties are set
    // then
    assertThat(webapp.isIndexRedirectEnabled()).isFalse();
  }

  @Test
  public void testCsrfProperty() {
    // given Operaton properties are set
    // then
    assertThat(webapp.getCsrf()).isNotNull();
  }

  @Test
  public void testCsrfTargetOriginProperty() {
    // given the Operaton CSRF TargetOrigin property is defined
    // then
    assertThat(webapp.getCsrf().getTargetOrigin()).isNotNull();
    assertThat(webapp.getCsrf().getTargetOrigin()).isNotBlank();
    assertThat(webapp.getCsrf().getTargetOrigin()).isEqualTo("localhost:8080");
  }

  @Test
  public void testCsrfDenyStatusProperty() {
    // given the Operaton CSRF DenyStatus property is defined
    // then
    assertThat(webapp.getCsrf().getDenyStatus()).isNotNull();
    assertThat(webapp.getCsrf().getDenyStatus()).isEqualTo(405);
  }

  @Test
  public void testCsrfRandomClassProperty() {
    // given the Operaton CSRF RandomClass property is defined
    // then
    assertThat(webapp.getCsrf().getRandomClass()).isNotNull();
    assertThat(webapp.getCsrf().getRandomClass()).isEqualTo("java.util.Random");
  }

  @Test
  public void testCsrfEntryPointsProperty() {
    // given the Operaton CSRF EntryPoints property is defined
    // then
    assertThat(webapp.getCsrf().getEntryPoints()).isNotNull();
    assertThat(webapp.getCsrf().getEntryPoints()).isNotEmpty();
    assertThat(webapp.getCsrf().getEntryPoints()).isInstanceOf(List.class);
    assertThat(webapp.getCsrf().getEntryPoints()).hasSize(2);
    assertThat(webapp.getCsrf().getEntryPoints().get(0)).isEqualTo("/api/engine/engine/default/history/task/count");
    assertThat(webapp.getCsrf().getEntryPoints().get(1)).isEqualTo("/api/engine/engine/default/history/variable/count");
  }
}

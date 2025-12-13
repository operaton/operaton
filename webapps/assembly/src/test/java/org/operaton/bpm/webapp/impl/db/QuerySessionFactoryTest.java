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
package org.operaton.bpm.webapp.impl.db;

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;

import static org.assertj.core.api.Assertions.assertThat;

class QuerySessionFactoryTest {

  @RegisterExtension
  static ProcessEngineExtension processEngineExtension = ProcessEngineExtension.builder()
    .randomEngineName()
    .configurationResource("operaton-skip-check.cfg.xml")
    .build();

  private ProcessEngineConfigurationImpl processEngineConfiguration;

  @Test
  void querySessionFactoryInitializationFromEngineConfig() {
    // given
    QuerySessionFactory querySessionFactory = new QuerySessionFactory();
    processEngineConfiguration = processEngineExtension.getProcessEngineConfiguration();

    // when
    querySessionFactory.initFromProcessEngineConfiguration(processEngineConfiguration, Collections.emptyList());

    // then
    assertThat(querySessionFactory.getWrappedConfiguration()).isEqualTo(processEngineConfiguration);
    assertThat(querySessionFactory.getDatabaseType()).isEqualTo(processEngineConfiguration.getDatabaseType());
    assertThat(querySessionFactory.getDataSource()).isEqualTo(processEngineConfiguration.getDataSource());
    assertThat(querySessionFactory.getDatabaseTablePrefix()).isEqualTo(
      processEngineConfiguration.getDatabaseTablePrefix());
    assertThat(querySessionFactory.getSkipIsolationLevelCheck()).isTrue();
    assertThat(querySessionFactory.getHistoryLevel()).isEqualTo(processEngineConfiguration.getHistoryLevel());
    assertThat(querySessionFactory.getHistory()).isEqualTo(processEngineConfiguration.getHistory());

  }

  @Test
  void buildMappingsShouldBuildValidXml() throws Exception {
    // given
    QuerySessionFactory querySessionFactory = new QuerySessionFactory();
    processEngineConfiguration = processEngineExtension.getProcessEngineConfiguration();
    querySessionFactory.initFromProcessEngineConfiguration(processEngineConfiguration, Collections.emptyList());
    var mappingFiles = List.of("foo/mapping1.xml", "bar/mapping2.xml");
    var documentBuildFactory = DocumentBuilderFactory.newDefaultInstance();
    var builder = documentBuildFactory.newDocumentBuilder();

    // when
    String mappings = querySessionFactory.buildMappings(mappingFiles);

    // then
    var document = builder.parse(new ByteArrayInputStream(mappings.getBytes()));
    assertThat(document.getDocumentElement().getNodeName()).isEqualTo("configuration");

    // normalize newlines only for the assertion
    assertThat(mappings.replace("\r\n", "\n"))
      .contains("<mapper resource=\"foo/mapping1.xml\" />\n")
      .contains("<mapper resource=\"bar/mapping2.xml\" />\n");
  }
}

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
package org.operaton.bpm.application.impl.deployment.parser;

import java.net.URL;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.operaton.bpm.application.impl.metadata.ProcessesXmlParse;
import org.operaton.bpm.application.impl.metadata.ProcessesXmlParser;
import org.operaton.bpm.application.impl.metadata.spi.ProcessArchiveXml;
import org.operaton.bpm.application.impl.metadata.spi.ProcessesXml;
import org.operaton.bpm.container.impl.metadata.spi.ProcessEngineXml;
import org.operaton.bpm.engine.ProcessEngineException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * <p>The testcases for the {@link ProcessesXmlParser}</p>
 *
 * @author Daniel Meyer
 *
 */
class ProcessesXmlParserTest {

  private ProcessesXmlParser parser;

  @BeforeEach
  void setUp() {
    parser = new ProcessesXmlParser();
  }

  protected URL getStreamUrl(String filename) {
    return ProcessesXmlParserTest.class.getResource(filename);
  }

  @Test
  void testParseProcessesXmlOneEngine() {

    ProcessesXml processesXml = parser.createParse()
      .sourceUrl(getStreamUrl("process_xml_one_engine.xml"))
      .execute()
      .getProcessesXml();

    assertThat(processesXml).isNotNull();

    assertThat(processesXml.getProcessEngines()).hasSize(1);
    assertThat(processesXml.getProcessArchives()).isEmpty();

    ProcessEngineXml engineXml = processesXml.getProcessEngines().get(0);
    assertThat(engineXml.getName()).isEqualTo("default");
    assertThat(engineXml.getJobAcquisitionName()).isEqualTo("default");
    assertThat(engineXml.getConfigurationClass()).isEqualTo("configuration");
    assertThat(engineXml.getDatasource()).isEqualTo("datasource");

    Map<String, String> properties = engineXml.getProperties();
    assertThat(properties)
      .isNotNull()
      .hasSize(2)
      .containsEntry("prop1", "value1")
      .containsEntry("prop2", "value2");
  }

  @Test
  void testParseProcessesXmlTwoEngines() {

    ProcessesXml processesXml = parser.createParse()
      .sourceUrl(getStreamUrl("process_xml_two_engines.xml"))
      .execute()
      .getProcessesXml();

    assertThat(processesXml).isNotNull();

    assertThat(processesXml.getProcessEngines()).hasSize(2);
    assertThat(processesXml.getProcessArchives()).isEmpty();

    ProcessEngineXml engineXml1 = processesXml.getProcessEngines().get(0);
    assertThat(engineXml1.getName()).isEqualTo("engine1");
    assertThat(engineXml1.getConfigurationClass()).isEqualTo("configuration");
    assertThat(engineXml1.getDatasource()).isEqualTo("datasource");

    Map<String, String> properties1 = engineXml1.getProperties();
    assertThat(properties1)
      .isNotNull()
      .hasSize(2)
      .containsEntry("prop1", "value1")
      .containsEntry("prop2", "value2");

    ProcessEngineXml engineXml2 = processesXml.getProcessEngines().get(1);
    assertThat(engineXml2.getName()).isEqualTo("engine2");
    assertThat(engineXml2.getConfigurationClass()).isEqualTo("configuration");
    assertThat(engineXml2.getDatasource()).isEqualTo("datasource");

    // the second engine has no properties
    Map<String, String> properties2 = engineXml2.getProperties();
    assertThat(properties2).isNotNull().isEmpty();

  }

  @Test
  void testParseProcessesXmlOneArchive() {

    ProcessesXml processesXml = parser.createParse()
      .sourceUrl(getStreamUrl("process_xml_one_archive.xml"))
      .execute()
      .getProcessesXml();

    assertThat(processesXml).isNotNull();

    assertThat(processesXml.getProcessEngines()).isEmpty();
    assertThat(processesXml.getProcessArchives()).hasSize(1);

    ProcessArchiveXml archiveXml1 = processesXml.getProcessArchives().get(0);
    assertThat(archiveXml1.getName()).isEqualTo("pa1");
    assertThat(archiveXml1.getProcessEngineName()).isEqualTo("default");

    List<String> resourceNames = archiveXml1.getProcessResourceNames();
    assertThat(resourceNames).hasSize(2);
    assertThat(resourceNames.get(0)).isEqualTo("process1.bpmn");
    assertThat(resourceNames.get(1)).isEqualTo("process2.bpmn");

    Map<String, String> properties1 = archiveXml1.getProperties();
    assertThat(properties1)
      .isNotNull()
      .hasSize(2)
      .containsEntry("prop1", "value1")
      .containsEntry("prop2", "value2");
  }

  @Test
  void testParseProcessesXmlTwoArchives() {

    ProcessesXml processesXml = parser.createParse()
      .sourceUrl(getStreamUrl("process_xml_two_archives.xml"))
      .execute()
      .getProcessesXml();

    assertThat(processesXml).isNotNull();

    assertThat(processesXml.getProcessEngines()).isEmpty();
    assertThat(processesXml.getProcessArchives()).hasSize(2);


    ProcessArchiveXml archiveXml1 = processesXml.getProcessArchives().get(0);
    assertThat(archiveXml1.getName()).isEqualTo("pa1");
    assertThat(archiveXml1.getProcessEngineName()).isEqualTo("default");

    List<String> resourceNames = archiveXml1.getProcessResourceNames();
    assertThat(resourceNames).hasSize(2);
    assertThat(resourceNames.get(0)).isEqualTo("process1.bpmn");
    assertThat(resourceNames.get(1)).isEqualTo("process2.bpmn");

    Map<String, String> properties1 = archiveXml1.getProperties();
    assertThat(properties1)
      .isNotNull()
      .hasSize(2)
      .containsEntry("prop1", "value1")
      .containsEntry("prop2", "value2");

    ProcessArchiveXml archiveXml2 = processesXml.getProcessArchives().get(1);
    assertThat(archiveXml2.getName()).isEqualTo("pa2");
    assertThat(archiveXml2.getProcessEngineName()).isEqualTo("default");

    List<String> resourceNames2 = archiveXml2.getProcessResourceNames();
    assertThat(resourceNames).hasSize(2);
    assertThat(resourceNames2.get(0)).isEqualTo("process1.bpmn");
    assertThat(resourceNames2.get(1)).isEqualTo("process2.bpmn");

    Map<String, String> properties2 = archiveXml2.getProperties();
    assertThat(properties2).isNotNull().isEmpty();
  }

  @Test
  void testParseProcessesXmlTwoArchivesAndTwoEngines() {
    ProcessesXml processesXml = parser.createParse()
      .sourceUrl(getStreamUrl("process_xml_two_archives_two_engines.xml"))
      .execute()
      .getProcessesXml();

    assertThat(processesXml).isNotNull();

    assertThat(processesXml.getProcessEngines()).hasSize(2);
    assertThat(processesXml.getProcessArchives()).hasSize(2);

    // validate archives

    ProcessArchiveXml archiveXml1 = processesXml.getProcessArchives().get(0);
    assertThat(archiveXml1.getName()).isEqualTo("pa1");
    assertThat(archiveXml1.getProcessEngineName()).isEqualTo("default");

    List<String> resourceNames = archiveXml1.getProcessResourceNames();
    assertThat(resourceNames).hasSize(2);
    assertThat(resourceNames.get(0)).isEqualTo("process1.bpmn");
    assertThat(resourceNames.get(1)).isEqualTo("process2.bpmn");

    Map<String, String> properties1 = archiveXml1.getProperties();
    assertThat(properties1)
      .isNotNull()
      .hasSize(2)
      .containsEntry("prop1", "value1")
      .containsEntry("prop2", "value2");

    ProcessArchiveXml archiveXml2 = processesXml.getProcessArchives().get(1);
    assertThat(archiveXml2.getName()).isEqualTo("pa2");
    assertThat(archiveXml2.getProcessEngineName()).isEqualTo("default");

    List<String> resourceNames2 = archiveXml2.getProcessResourceNames();
    assertThat(resourceNames).hasSize(2);
    assertThat(resourceNames2.get(0)).isEqualTo("process1.bpmn");
    assertThat(resourceNames2.get(1)).isEqualTo("process2.bpmn");

    Map<String, String> properties2 = archiveXml2.getProperties();
    assertThat(properties2).isNotNull().isEmpty();

    // validate engines

    ProcessEngineXml engineXml1 = processesXml.getProcessEngines().get(0);
    assertThat(engineXml1.getName()).isEqualTo("engine1");
    assertThat(engineXml1.getConfigurationClass()).isEqualTo("configuration");
    assertThat(engineXml1.getDatasource()).isEqualTo("datasource");

    properties1 = engineXml1.getProperties();
    assertThat(properties1)
      .isNotNull()
      .hasSize(2)
      .containsEntry("prop1", "value1")
      .containsEntry("prop2", "value2");

    ProcessEngineXml engineXml2 = processesXml.getProcessEngines().get(1);
    assertThat(engineXml2.getName()).isEqualTo("engine2");
    assertThat(engineXml2.getConfigurationClass()).isEqualTo("configuration");
    assertThat(engineXml2.getDatasource()).isEqualTo("datasource");

    // the second engine has no properties
    properties2 = engineXml2.getProperties();
    assertThat(properties2).isNotNull().isEmpty();
  }

  @Test
  void testParseProcessesXmlEngineNoName() {

    // this test is to make sure that XML Schema Validation works.
    ProcessesXmlParse processesXmlParse = parser
      .createParse()
      .sourceUrl(getStreamUrl("process_xml_engine_no_name.xml"));

    assertThatThrownBy(processesXmlParse::execute)
      .isInstanceOf(ProcessEngineException.class);
  }

  @Test
  @Disabled("FIXME")
  void testParseProcessesXmlClassLineBreak() {
    ProcessesXml processesXml = parser.createParse()
        .sourceUrl(getStreamUrl("process_xml_one_archive_with_line_break.xml"))
        .execute()
        .getProcessesXml();

    assertThat(processesXml).isNotNull();

    ProcessArchiveXml archiveXml1 = processesXml.getProcessArchives().get(0);
    List<String> resourceNames = archiveXml1.getProcessResourceNames();
    assertThat(resourceNames).hasSize(2);
    assertThat(resourceNames.get(0)).isEqualTo("process1.bpmn");

  }

  @Test
  void testParseProcessesXmlNsPrefix() {

    ProcessesXml processesXml = parser.createParse()
      .sourceUrl(getStreamUrl("process_xml_ns_prefix.xml"))
      .execute()
      .getProcessesXml();

    assertThat(processesXml).isNotNull();

    assertThat(processesXml.getProcessEngines()).hasSize(1);
    assertThat(processesXml.getProcessArchives()).hasSize(1);

  }

  @Test
  void testParseProcessesXmlTenantId() {

    ProcessesXml processesXml = parser.createParse()
      .sourceUrl(getStreamUrl("process_xml_tenant_id.xml"))
      .execute()
      .getProcessesXml();

    assertThat(processesXml).isNotNull();
    assertThat(processesXml.getProcessArchives()).hasSize(2);

    ProcessArchiveXml archiveXmlWithoutTenantId = processesXml.getProcessArchives().get(0);
    assertThat(archiveXmlWithoutTenantId.getTenantId()).isNull();

    ProcessArchiveXml archiveXmlWithTenantId = processesXml.getProcessArchives().get(1);
    assertThat(archiveXmlWithTenantId.getTenantId()).isEqualTo("tenant1");
  }

}

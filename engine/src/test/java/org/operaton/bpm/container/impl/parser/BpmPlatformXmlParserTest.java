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
package org.operaton.bpm.container.impl.parser;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.util.List;
import java.util.Map;

import org.operaton.bpm.container.impl.metadata.BpmPlatformXmlParser;
import org.operaton.bpm.container.impl.metadata.spi.BpmPlatformXml;
import org.operaton.bpm.container.impl.metadata.spi.JobAcquisitionXml;
import org.operaton.bpm.container.impl.metadata.spi.JobExecutorXml;
import org.operaton.bpm.container.impl.metadata.spi.ProcessEnginePluginXml;
import org.operaton.bpm.container.impl.metadata.spi.ProcessEngineXml;
import org.junit.Before;
import org.junit.Test;

/**
 * <p>The testcases for the {@link BpmPlatformXmlParser}</p>
 *
 * @author Daniel Meyer
 *
 */
public class BpmPlatformXmlParserTest {

  private BpmPlatformXmlParser parser;

  @Before
  public void setUp() {
    parser = new BpmPlatformXmlParser();
  }

  protected URL getStreamUrl(String filename) {
    return BpmPlatformXmlParserTest.class.getResource(filename);
  }

  @Test
  public void testParseBpmPlatformXmlNoEngine() {

    BpmPlatformXml bpmPlatformXml = parser.createParse()
      .sourceUrl(getStreamUrl("bpmplatform_xml_no_engine.xml"))
      .execute()
      .getBpmPlatformXml();

    assertThat(bpmPlatformXml).isNotNull();
    assertThat(bpmPlatformXml.getJobExecutor()).isNotNull();
    assertThat(bpmPlatformXml.getProcessEngines()).isEmpty();

    JobExecutorXml jobExecutorXml = bpmPlatformXml.getJobExecutor();
    assertThat(jobExecutorXml.getJobAcquisitions()).hasSize(1);

    JobAcquisitionXml jobAcquisitionXml = jobExecutorXml.getJobAcquisitions().get(0);
    assertThat(jobAcquisitionXml.getName()).isEqualTo("default");
    assertThat(jobAcquisitionXml.getJobExecutorClassName()).isEqualTo("org.operaton.bpm.engine.impl.jobexecutor.DefaultJobExecutor");

    assertThat(jobAcquisitionXml.getProperties()).hasSize(2);

  }

  @Test
  public void testParseBpmPlatformXmlOneEngine() {

    BpmPlatformXml bpmPlatformXml = parser.createParse()
      .sourceUrl(getStreamUrl("bpmplatform_xml_one_engine.xml"))
      .execute()
      .getBpmPlatformXml();

    assertThat(bpmPlatformXml).isNotNull();
    assertThat(bpmPlatformXml.getJobExecutor()).isNotNull();
    assertThat(bpmPlatformXml.getProcessEngines()).hasSize(1);

    JobExecutorXml jobExecutorXml = bpmPlatformXml.getJobExecutor();
    assertThat(jobExecutorXml.getJobAcquisitions()).hasSize(1);
    assertThat(jobExecutorXml.getProperties()).hasSize(2);

    JobAcquisitionXml jobAcquisitionXml = jobExecutorXml.getJobAcquisitions().get(0);
    assertThat(jobAcquisitionXml.getName()).isEqualTo("default");
    assertThat(jobAcquisitionXml.getJobExecutorClassName()).isEqualTo("org.operaton.bpm.engine.impl.jobexecutor.DefaultJobExecutor");

    assertThat(jobAcquisitionXml.getProperties()).hasSize(2);

    ProcessEngineXml engineXml = bpmPlatformXml.getProcessEngines().get(0);
    assertThat(engineXml.getName()).isEqualTo("engine1");
    assertThat(engineXml.getJobAcquisitionName()).isEqualTo("default");

    Map<String, String> properties = engineXml.getProperties();
    assertThat(properties).isNotNull();
    assertThat(properties).isEmpty();

    List<ProcessEnginePluginXml> plugins = engineXml.getPlugins();
    assertThat(plugins).isNotNull();
    assertThat(plugins).isEmpty();

  }

  @Test
  public void testParseBpmPlatformXmlEnginePlugin() {

    BpmPlatformXml bpmPlatformXml = parser.createParse()
      .sourceUrl(getStreamUrl("bpmplatform_xml_engine_plugin.xml"))
      .execute()
      .getBpmPlatformXml();

    assertThat(bpmPlatformXml).isNotNull();
    assertThat(bpmPlatformXml.getProcessEngines()).hasSize(1);

    ProcessEngineXml engineXml = bpmPlatformXml.getProcessEngines().get(0);
    assertThat(engineXml.getName()).isEqualTo("engine1");
    assertThat(engineXml.getJobAcquisitionName()).isEqualTo("default");

    List<ProcessEnginePluginXml> plugins = engineXml.getPlugins();
    assertThat(plugins).hasSize(1);

    ProcessEnginePluginXml plugin1 = plugins.get(0);
    assertThat(plugin1).isNotNull();

    assertThat(plugin1.getPluginClass()).isEqualTo("org.operaton.bpm.MyAwesomePlugin");

    Map<String, String> properties = plugin1.getProperties();
    assertThat(properties)
            .isNotNull()
            .hasSize(2);

    String val1 = properties.get("prop1");
    assertThat(val1).isNotNull();
    assertThat(val1).isEqualTo("val1");

    String val2 = properties.get("prop2");
    assertThat(val2).isNotNull();
    assertThat(val2).isEqualTo("val2");

  }

  @Test
  public void testParseBpmPlatformXmlMultipleEnginePlugins() {

    BpmPlatformXml bpmPlatformXml = parser.createParse()
      .sourceUrl(getStreamUrl("bpmplatform_xml_multiple_engine_plugins.xml"))
      .execute()
      .getBpmPlatformXml();

    assertThat(bpmPlatformXml).isNotNull();
    assertThat(bpmPlatformXml.getProcessEngines()).hasSize(1);

    ProcessEngineXml engineXml = bpmPlatformXml.getProcessEngines().get(0);
    assertThat(engineXml.getName()).isEqualTo("engine1");
    assertThat(engineXml.getJobAcquisitionName()).isEqualTo("default");

    List<ProcessEnginePluginXml> plugins = engineXml.getPlugins();
    assertThat(plugins).hasSize(2);

  }

  @Test
  public void testParseProcessesXmlAntStyleProperties() {

    BpmPlatformXml platformXml = parser.createParse()
        .sourceUrl(getStreamUrl("bpmplatform_xml_ant_style_properties.xml"))
        .execute()
        .getBpmPlatformXml();

    assertThat(platformXml).isNotNull();

    ProcessEngineXml engineXml = platformXml.getProcessEngines().get(0);

    assertThat(engineXml.getPlugins()).hasSize(1);
    ProcessEnginePluginXml pluginXml = engineXml.getPlugins().get(0);

    Map<String, String> properties = pluginXml.getProperties();
    assertThat(properties)
            .hasSize(2)
            .containsEntry("prop1", System.getProperty("java.version"))
            .containsEntry("prop2", "prefix-" + System.getProperty("os.name"));
  }

}

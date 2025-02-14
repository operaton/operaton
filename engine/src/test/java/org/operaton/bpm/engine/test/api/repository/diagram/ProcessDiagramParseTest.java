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
package org.operaton.bpm.engine.test.api.repository.diagram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import org.operaton.bpm.engine.impl.bpmn.diagram.ProcessDiagramLayoutFactory;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.test.ProcessEngineRule;
import org.operaton.bpm.engine.test.util.ProvidedProcessEngineRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Nikola Koevski
 */
public class ProcessDiagramParseTest {

  private static final String RESOURCE_PATH = "src/test/resources/org/operaton/bpm/engine/test/api/repository/diagram/testXxeParsingIsDisabled";

  @Rule
  public ProcessEngineRule engineRule = new ProvidedProcessEngineRule();
  protected ProcessEngineConfigurationImpl processEngineConfiguration;

  boolean xxeProcessingValue;

  @Before
  public void setUp() {
    processEngineConfiguration = engineRule.getProcessEngineConfiguration();
    xxeProcessingValue = processEngineConfiguration.isEnableXxeProcessing();
  }

  @After
  public void tearDown() {
    processEngineConfiguration.setEnableXxeProcessing(xxeProcessingValue);
  }

  @Test
  public void testXxeParsingIsDisabled() {
    processEngineConfiguration.setEnableXxeProcessing(false);
    final InputStream bpmnXmlStream = getResourceInputStream(RESOURCE_PATH + ".bpmn20.xml");
    final InputStream imageStream = getResourceInputStream(RESOURCE_PATH + ".png");
    assertThat(bpmnXmlStream).isNotNull();
    var processEngineConfigurationImpl = engineRule.getProcessEngineConfiguration()
        .getCommandExecutorTxRequired();

    try {

      // when we run this in the ProcessEngine context
      processEngineConfigurationImpl.execute(commandContext -> new ProcessDiagramLayoutFactory().getProcessDiagramLayout(bpmnXmlStream, imageStream));
      fail("The test model contains a DOCTYPE declaration! The test should fail.");
    } catch (Exception e) {
      // then
      assertThat(e.getMessage()).contains("Error while parsing BPMN model");
      assertThat(e.getCause().getMessage()).contains("http://apache.org/xml/features/disallow-doctype-decl");
    }
  }

  @Test
  public void testXxeParsingIsEnabled() {
    processEngineConfiguration.setEnableXxeProcessing(true);
    final InputStream bpmnXmlStream = getResourceInputStream(RESOURCE_PATH + ".bpmn20.xml");
    final InputStream imageStream = getResourceInputStream(RESOURCE_PATH + ".png");
    assertThat(bpmnXmlStream).isNotNull();
    var processEngineConfigurationImpl = engineRule.getProcessEngineConfiguration()
        .getCommandExecutorTxRequired();

    try {

      // when we run this in the ProcessEngine context
      processEngineConfigurationImpl.execute(commandContext -> new ProcessDiagramLayoutFactory().getProcessDiagramLayout(bpmnXmlStream, imageStream));
      fail("The test model contains a DOCTYPE declaration! The test should fail.");
    } catch (Exception e) {
      // then
      assertThat(e.getMessage()).contains("Error while parsing BPMN model");
      assertThat(e.getCause().getMessage()).contains("file.txt");
    }
  }

  private InputStream getResourceInputStream(String path) {
    try {
      return new FileInputStream(path);
    } catch (FileNotFoundException ex) {
      throw new AssertionError("The test BPMN model file is missing. " + ex.getMessage());
    }
  }
}

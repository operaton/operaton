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
package org.operaton.bpm.application.impl.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.operaton.bpm.application.impl.metadata.spi.ProcessArchiveXml;
import org.operaton.bpm.application.impl.metadata.spi.ProcessesXml;
import org.operaton.bpm.engine.repository.ResumePreviousBy;
import org.junit.Test;

/**
 * <p>Testcase verifying the default properties in the empty processes.xml</p>
 *
 * @author Daniel Meyer
 *
 */
public class EmptyProcessesXmlTest {

  @Test
  public void testDefaultValues() {

    ProcessesXml emptyProcessesXml = ProcessesXml.EMPTY_PROCESSES_XML;
    assertThat(emptyProcessesXml).isNotNull();

    assertThat(emptyProcessesXml.getProcessEngines()).isNotNull().isEmpty();

    assertThat(emptyProcessesXml.getProcessArchives()).isNotNull().hasSize(1);

    ProcessArchiveXml processArchiveXml = emptyProcessesXml.getProcessArchives().get(0);

    assertThat(processArchiveXml.getName()).isNull();
    assertThat(processArchiveXml.getProcessEngineName()).isNull();

    assertThat(processArchiveXml.getProcessResourceNames()).isNotNull().isEmpty();

    Map<String, String> properties = processArchiveXml.getProperties();

    assertThat(properties)
            .isNotNull()
            .hasSize(4);

    String isDeleteUponUndeploy = properties.get(ProcessArchiveXml.PROP_IS_DELETE_UPON_UNDEPLOY);
    assertThat(isDeleteUponUndeploy).isNotNull().isEqualTo(Boolean.FALSE.toString());

    String isScanForProcessDefinitions = properties.get(ProcessArchiveXml.PROP_IS_SCAN_FOR_PROCESS_DEFINITIONS);
    assertThat(isScanForProcessDefinitions).isNotNull().isEqualTo(Boolean.TRUE.toString());

    String isDeployChangedOnly = properties.get(ProcessArchiveXml.PROP_IS_DEPLOY_CHANGED_ONLY);
    assertThat(isDeployChangedOnly).isNotNull().isEqualTo(Boolean.FALSE.toString());

    String resumePreviousBy = properties.get(ProcessArchiveXml.PROP_RESUME_PREVIOUS_BY);
    assertThat(resumePreviousBy)
      .isNotNull()
      .isSameAs(ResumePreviousBy.RESUME_BY_PROCESS_DEFINITION_KEY);
  }

}

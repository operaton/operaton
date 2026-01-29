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
package org.operaton.bpm.container.impl.deployment;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.operaton.bpm.application.impl.metadata.spi.ProcessesXml;
import org.operaton.bpm.container.impl.metadata.spi.ProcessEngineXml;
import org.operaton.bpm.container.impl.spi.DeploymentOperation;

import static org.operaton.bpm.container.impl.deployment.Attachments.PROCESSES_XML_RESOURCES;

/**
 * <p>Retrieves the List of ProcessEngines from an attached {@link ProcessesXml}.</p>
 *
 * @see AbstractParseBpmPlatformXmlStep
 *
 */
public class ProcessesXmlStartProcessEnginesStep extends AbstractStartProcessEnginesStep {

  @Override
  protected List<ProcessEngineXml> getProcessEnginesXmls(DeploymentOperation operationContext) {

    final Map<URI, ProcessesXml> processesXmls = operationContext.getAttachment(PROCESSES_XML_RESOURCES);

    List<ProcessEngineXml> processEngines = new ArrayList<>();

    for (ProcessesXml processesXml : processesXmls.values()) {
      processEngines.addAll(processesXml.getProcessEngines());

    }

    return processEngines;
  }

}

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
import java.net.URL;
import java.util.Map;
import java.util.Map.Entry;

import org.operaton.bpm.application.impl.metadata.spi.ProcessArchiveXml;
import org.operaton.bpm.application.impl.metadata.spi.ProcessesXml;
import org.operaton.bpm.container.impl.spi.DeploymentOperation;
import org.operaton.bpm.container.impl.spi.DeploymentOperationStep;
import org.operaton.bpm.engine.impl.util.ReflectUtil;

import static org.operaton.bpm.container.impl.deployment.Attachments.PROCESSES_XML_RESOURCES;

/**
 * <p>
 * Deployment step responsible for creating individual
 * {@link DeployProcessArchiveStep} instances for each process archive
 * configured in the META-INF/processes.xml file.
 * </p>
 *
 * @author Daniel Meyer
 *
 */
public class DeployProcessArchivesStep extends DeploymentOperationStep {

  @Override
  public String getName() {
    return "Deploy process archives";
  }

  @Override
  public void performOperationStep(DeploymentOperation operationContext) {

    Map<URL, ProcessesXml> processesXmls = operationContext.getAttachment(PROCESSES_XML_RESOURCES);

    for (Entry<URL, ProcessesXml> processesXml : processesXmls.entrySet()) {
      for (ProcessArchiveXml processArchive : processesXml.getValue().getProcessArchives()) {
        // for each process archive add an individual operation step
        operationContext.addStep(createDeployProcessArchiveStep(processArchive, ReflectUtil.urlToURI(processesXml.getKey())));
      }
    }
  }

  protected DeployProcessArchiveStep createDeployProcessArchiveStep(ProcessArchiveXml parsedProcessArchive, URI uri) {
    return new DeployProcessArchiveStep(parsedProcessArchive, uri);
  }
}

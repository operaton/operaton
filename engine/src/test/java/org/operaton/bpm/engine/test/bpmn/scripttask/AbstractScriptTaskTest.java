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
package org.operaton.bpm.engine.test.bpmn.scripttask;

import static org.assertj.core.api.Assertions.fail;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.operaton.bpm.engine.repository.Deployment;
import org.operaton.bpm.engine.test.util.PluggableProcessEngineTest;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.junit.After;

public abstract class AbstractScriptTaskTest extends PluggableProcessEngineTest {

  private final List<String> deploymentIds = new ArrayList<>();

  @After
  public void tearDown() {
    deploymentIds.forEach(deploymentId -> repositoryService.deleteDeployment(deploymentId, true));
  }

  protected void deployProcess(BpmnModelInstance process) {
    Deployment deployment = repositoryService.createDeployment()
        .addModelInstance("testProcess.bpmn", process)
        .deploy();
      deploymentIds.add(deployment.getId());
  }

  protected void deployProcess(String scriptFormat, String scriptText) {
    BpmnModelInstance process = createProcess(scriptFormat, scriptText);
    deployProcess(process);
  }

  protected BpmnModelInstance createProcess(String scriptFormat, String scriptText) {

    return Bpmn.createExecutableProcess("testProcess")
      .startEvent()
      .scriptTask()
        .scriptFormat(scriptFormat)
        .scriptText(scriptText)
      .userTask()
      .endEvent()
    .done();

  }

  protected String getNormalizedResourcePath(String classPathResource) {
    try {
      return Paths.get(getClass().getResource(classPathResource).toURI()).toString().replace('\\', '/');
    } catch (Exception e) {
      fail("Cannot read path of '" + classPathResource + "': " + e.getMessage());
      return null;
    }
  }

}

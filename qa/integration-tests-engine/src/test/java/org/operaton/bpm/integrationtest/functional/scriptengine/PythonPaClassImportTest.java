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
package org.operaton.bpm.integrationtest.functional.scriptengine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.operaton.bpm.integrationtest.functional.scriptengine.classes.CustomClass;
import org.operaton.bpm.integrationtest.util.AbstractFoxPlatformIntegrationTest;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

/**
 * @author Daniel Meyer
 *
 */
@ExtendWith(ArquillianExtension.class)
public class PythonPaClassImportTest extends AbstractFoxPlatformIntegrationTest {

  public static final String SCRIPT_WITH_IMPORT =
      "from org.operaton.bpm.integrationtest.functional.scriptengine.classes import CustomClass\n"
    + "execution.setVariable('greeting', CustomClass().greet())";

  public static final String JYTHON_MODULE_DEPENDENCY =
      "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
    + "<jboss-deployment-structure>"
    + "  <deployment>"
    + "    <dependencies>"
    + "      <module name=\"org.python.jython\" services=\"import\" />"
    + "    </dependencies>"
    + "  </deployment>"
    + "</jboss-deployment-structure>";

  protected static StringAsset createScriptTaskProcess(String scriptFormat, String scriptText, String pdk) {
    BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(pdk)
        .operatonHistoryTimeToLive(180)
      .startEvent()
      .scriptTask()
        .scriptFormat(scriptFormat)
        .scriptText(scriptText)
        .userTask()
      .endEvent()
      .done();
    return new StringAsset(Bpmn.convertToString(modelInstance));
  }

  @Deployment(name="pa1")
  public static WebArchive createProcessApplication1() {
    return initWebArchiveDeployment("pa1.war")
      .addAsWebInfResource(new StringAsset(JYTHON_MODULE_DEPENDENCY),"jboss-deployment-structure.xml")
      .addAsResource(createScriptTaskProcess("python", "", "process1"), "process1.bpmn20.xml");
  }

  @Deployment(name="pa2")
  public static WebArchive createProcessApplication2() {
    return initWebArchiveDeployment("pa2.war")
      .addClass(CustomClass.class)
      .addAsWebInfResource(new StringAsset(JYTHON_MODULE_DEPENDENCY),"jboss-deployment-structure.xml")
      .addAsResource(createScriptTaskProcess("python", SCRIPT_WITH_IMPORT, "process2"), "process2.bpmn20.xml");
  }

  @Test
  @OperateOnDeployment("pa1")
  void shouldSetVariable() {
    // first start process 1 (this creates and caches the python engine)
    runtimeService.startProcessInstanceByKey("process1").getId();

    // then start process 2
    String processInstanceId = runtimeService.startProcessInstanceByKey("process2").getId();
    Object foo = runtimeService.getVariable(processInstanceId, "greeting");
    assertThat(foo).isNotNull();
    assertEquals("Hi Ho", foo);
  }

}

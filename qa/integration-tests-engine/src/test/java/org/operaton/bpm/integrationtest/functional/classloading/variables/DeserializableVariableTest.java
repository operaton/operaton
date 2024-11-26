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
package org.operaton.bpm.integrationtest.functional.classloading.variables;

import org.operaton.bpm.engine.history.HistoricVariableInstance;
import org.operaton.bpm.engine.impl.persistence.entity.HistoricVariableInstanceEntity;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.integrationtest.functional.classloading.variables.beans.GetDeserializableVariableDelegate;
import org.operaton.bpm.integrationtest.functional.classloading.variables.beans.SerializableVariable;
import org.operaton.bpm.integrationtest.functional.classloading.variables.beans.SetVariableDelegate;
import org.operaton.bpm.integrationtest.util.AbstractFoxPlatformIntegrationTest;
import org.operaton.bpm.integrationtest.util.TestContainer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

/**
 * <p>Ensures that process variables can be deserialized on a
 * process instance restart, when running on an appication server</p>
 *
 * @author Nikola Koevski
 */
@RunWith(Arquillian.class)
public class DeserializableVariableTest extends AbstractFoxPlatformIntegrationTest {

  @Deployment
  public static WebArchive createProcessArchiveDeplyoment() {
    return initWebArchiveDeployment()
      .addClass(GetDeserializableVariableDelegate.class)
      .addClass(SetVariableDelegate.class)
      .addClass(SerializableVariable.class)
      .addAsResource("org/operaton/bpm/integrationtest/functional/classloading/DeserializableVariableTest.testVariableDeserializationOnProcessApplicationRestart.bpmn20.xml");
  }

  @Deployment(name="clientDeployment")
  public static WebArchive clientDeployment() {
    WebArchive webArchive = ShrinkWrap.create(WebArchive.class, "client.war")
      .addClass(AbstractFoxPlatformIntegrationTest.class);

    TestContainer.addContainerSpecificResources(webArchive);

    return webArchive;
  }

  @Test
  @OperateOnDeployment("clientDeployment")
  public void testVariableDeserializationOnProcessApplicationRestart() {
    // given
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("testDeserializeVariable");

    // when
    Assert.assertNull(runtimeService.createProcessInstanceQuery().processInstanceId(pi.getId()).singleResult());
    runtimeService.restartProcessInstances(pi.getProcessDefinitionId())
      .startAfterActivity("servicetask1")
      .processInstanceIds(pi.getId())
      .execute();

    // then

    // Since the variable retrieval is done outside the Process Application Context as well,
    // custom object deserialization is disabled and a null check is performed
    List<HistoricVariableInstance> variableInstances = historyService.createHistoricVariableInstanceQuery().disableCustomObjectDeserialization().list();
    for (HistoricVariableInstance variable : variableInstances) {
      if (variable.getProcessInstanceId() != pi.getId() && variable instanceof HistoricVariableInstanceEntity variableEntity) {
        Assert.assertNotNull(variableEntity.getByteArrayValue());
      }
    }
  }
}

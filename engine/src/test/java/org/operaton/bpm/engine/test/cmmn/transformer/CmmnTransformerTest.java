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
package org.operaton.bpm.engine.test.cmmn.transformer;

import org.junit.jupiter.api.AfterEach;
import org.operaton.bpm.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration;
import org.operaton.bpm.engine.impl.cmmn.entity.repository.CaseDefinitionEntity;
import org.operaton.bpm.engine.impl.cmmn.handler.DefaultCmmnElementHandlerRegistry;
import org.operaton.bpm.engine.impl.cmmn.model.CmmnActivity;
import org.operaton.bpm.engine.impl.cmmn.model.CmmnCaseDefinition;
import org.operaton.bpm.engine.impl.cmmn.transformer.CmmnTransform;
import org.operaton.bpm.engine.impl.cmmn.transformer.CmmnTransformer;
import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.impl.persistence.entity.DeploymentEntity;
import org.operaton.bpm.engine.impl.persistence.entity.ResourceEntity;
import org.operaton.bpm.model.cmmn.Cmmn;
import org.operaton.bpm.model.cmmn.CmmnModelInstance;
import org.operaton.bpm.model.cmmn.instance.*;
import org.operaton.bpm.model.xml.impl.util.IoUtil;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import org.junit.After;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Roman Smirnov
 *
 */
public class CmmnTransformerTest {

  protected CmmnTransform transformer;
  protected CmmnModelInstance modelInstance;
  protected Definitions definitions;
  protected Case caseDefinition;
  protected CasePlanModel casePlanModel;
  protected DeploymentEntity deployment;

  @BeforeEach
  void setup() {
    CmmnTransformer transformerWrapper = new CmmnTransformer(null, new DefaultCmmnElementHandlerRegistry(), null);
    transformer = new CmmnTransform(transformerWrapper);

    deployment = new DeploymentEntity();
    deployment.setId("aDeploymentId");

    transformer.setDeployment(deployment);

    modelInstance = Cmmn.createEmptyModel();
    definitions = modelInstance.newInstance(Definitions.class);
    definitions.setTargetNamespace("http://operaton.org/examples");
    modelInstance.setDefinitions(definitions);

    caseDefinition = createElement(definitions, "aCaseDefinition", Case.class);
    casePlanModel = createElement(caseDefinition, "aCasePlanModel", CasePlanModel.class);

    Context.setProcessEngineConfiguration(new StandaloneInMemProcessEngineConfiguration()
        .setEnforceHistoryTimeToLive(false));
  }

  @AfterEach
  void tearDown() {
    Context.removeProcessEngineConfiguration();
  }

  protected <T extends CmmnModelElementInstance> T createElement(CmmnModelElementInstance parentElement, String id, Class<T> elementClass) {
    T element = modelInstance.newInstance(elementClass);
    element.setAttributeValue("id", id, true);
    parentElement.addChildElement(element);
    return element;
  }

  protected List<CaseDefinitionEntity> transform() {
    // convert the model to the XML string representation
    OutputStream outputStream = new ByteArrayOutputStream();
    Cmmn.writeModelToStream(outputStream, modelInstance);
    InputStream inputStream = IoUtil.convertOutputStreamToInputStream(outputStream);

    byte[] model = org.operaton.bpm.engine.impl.util.IoUtil.readInputStream(inputStream, "model");

    ResourceEntity resource = new ResourceEntity();
    resource.setBytes(model);
    resource.setName("test");

    transformer.setResource(resource);
    List<CaseDefinitionEntity> caseDefinitions = transformer.transform();

    IoUtil.closeSilently(outputStream);
    IoUtil.closeSilently(inputStream);

    return caseDefinitions;
  }

  /**
  *
  *   +-----------------+                    +-----------------+
  *   | Case1            \                   | aCaseDefinition |
  *   +-------------------+---+              +-----------------+
  *   |                       |                      |
  *   |                       |   ==>        +-----------------+
  *   |                       |              |  aCasePlanModel |
  *   |                       |              +-----------------+
  *   |                       |
  *   +-----------------------+
  *
  */
  @Test
  void testCasePlanModel() {
    // given

    // when
    List<CaseDefinitionEntity> caseDefinitions = transform();

    // then
    assertThat(caseDefinitions).hasSize(1);

    CmmnCaseDefinition caseModel = caseDefinitions.get(0);

    List<CmmnActivity> activities = caseModel.getActivities();

    assertThat(activities).hasSize(1);

    CmmnActivity casePlanModelActivity = activities.get(0);
    assertThat(casePlanModelActivity.getId()).isEqualTo(casePlanModel.getId());
    assertThat(casePlanModelActivity.getActivities()).isEmpty();
  }

  /**
  *
  *   +-----------------+                    +-----------------+
  *   | Case1            \                   | aCaseDefinition |
  *   +-------------------+---+              +-----------------+
  *   |                       |                      |
  *   |     +-------+         |   ==>        +-----------------+
  *   |     |   A   |         |              |  aCasePlanModel |
  *   |     +-------+         |              +-----------------+
  *   |                       |                      |
  *   +-----------------------+              +-----------------+
  *                                          |       A         |
  *                                          +-----------------+
  *
  */
  @Test
  void testActivityTreeWithOneHumanTask() {
    // given
    HumanTask humanTask = createElement(casePlanModel, "A", HumanTask.class);
    PlanItem planItem = createElement(casePlanModel, "PI_A", PlanItem.class);

    planItem.setDefinition(humanTask);

    // when
    List<CaseDefinitionEntity> caseDefinitions = transform();

    // then
    assertThat(caseDefinitions).hasSize(1);

    CaseDefinitionEntity caseDef = caseDefinitions.get(0);
    List<CmmnActivity> activities = caseDef.getActivities();

    CmmnActivity casePlanModelActivity = activities.get(0);

    List<CmmnActivity> planItemActivities = casePlanModelActivity.getActivities();
    assertThat(planItemActivities).hasSize(1);

    CmmnActivity child = planItemActivities.get(0);
    assertThat(child.getId()).isEqualTo(planItem.getId());
    assertThat(child.getActivities()).isEmpty();
  }

  /**
  *
  *   +-----------------+                                       +-----------------+
  *   | Case1            \                                      | aCaseDefinition |
  *   +-------------------+-----------------+                   +-----------------+
  *   |                                     |                            |
  *   |     +------------------------+      |                   +-----------------+
  *   |    / X                        \     |                   |  aCasePlanModel |
  *   |   +    +-------+  +-------+    +    |                   +-----------------+
  *   |   |    |   A   |  |   B   |    |    |  ==>                       |
  *   |   +    +-------+  +-------+    +    |                   +-----------------+
  *   |    \                          /     |                   |        X        |
  *   |     +------------------------+      |                   +-----------------+
  *   |                                     |                           / \
  *   +-------------------------------------+                          /   \
  *                                                 +-----------------+     +-----------------+
  *                                                 |        A        |     |        B        |
  *                                                 +-----------------+     +-----------------+
  */
  @Test
  void testActivityTreeWithOneStageAndNestedHumanTasks() {
    // given
    Stage stage = createElement(casePlanModel, "X", Stage.class);
    HumanTask humanTaskA = createElement(casePlanModel, "A", HumanTask.class);
    HumanTask humanTaskB = createElement(casePlanModel, "B", HumanTask.class);

    PlanItem planItemX = createElement(casePlanModel, "PI_X", PlanItem.class);
    PlanItem planItemA = createElement(stage, "PI_A", PlanItem.class);
    PlanItem planItemB = createElement(stage, "PI_B", PlanItem.class);

    planItemX.setDefinition(stage);
    planItemA.setDefinition(humanTaskA);
    planItemB.setDefinition(humanTaskB);

    // when
    List<CaseDefinitionEntity> caseDefinitions = transform();

    // then
    assertThat(caseDefinitions).hasSize(1);

    CaseDefinitionEntity caseDef = caseDefinitions.get(0);
    List<CmmnActivity> activities = caseDef.getActivities();

    CmmnActivity casePlanModelActivity = activities.get(0);

    List<CmmnActivity> children = casePlanModelActivity.getActivities();
    assertThat(children).hasSize(1);

    CmmnActivity planItemStage = children.get(0);
    assertThat(planItemStage.getId()).isEqualTo(planItemX.getId());

    children = planItemStage.getActivities();
    assertThat(children).hasSize(2);

    CmmnActivity childPlanItem = children.get(0);
    assertThat(childPlanItem.getId()).isEqualTo(planItemA.getId());
    assertThat(childPlanItem.getActivities()).isEmpty();

    childPlanItem = children.get(1);
    assertThat(childPlanItem.getId()).isEqualTo(planItemB.getId());
    assertThat(childPlanItem.getActivities()).isEmpty();
  }

  /**
  *
  *   +-----------------+                                                    +-----------------+
  *   | Case1            \                                                   | aCaseDefinition |
  *   +-------------------+-------------------+                              +-----------------+
  *   |                                       |                                       |
  *   |  +-------+                            |                              +-----------------+
  *   |  |  A1   |                            |              +---------------|  aCasePlanModel |---------------+
  *   |  +-------+                            |              |               +-----------------+               |
  *   |                                       |              |                        |                        |
  *   |    +------------------------+         |      +-----------------+     +-----------------+      +-----------------+
  *   |   / X1                       \        |      |       A1        |     |        X1       |      |        Y        |-----------+
  *   |  +    +-------+  +-------+    +       |      +-----------------+     +-----------------+      +-----------------+           |
  *   |  |    |  A2   |  |   B   |    |       |                                      / \                                           / \
  *   |  +    +-------+  +-------+    +       |                                     /   \                                         /   \
  *   |   \                          /        |                    +---------------+     +---------------+     +-----------------+     +-----------------+
  *   |    +------------------------+         |                    |      A2       |     |      B        |     |        C        |     |       X2        |
  *   |                                       |                    +---------------+     +---------------+     +-----------------+     +-----------------+
  *   |    +-----------------------------+    |  ==>                                                                                          / \
  *   |   / Y                             \   |                                                                              +---------------+   +---------------+
  *   |  +    +-------+                    +  |                                                                              |      A1       |   |       B       |
  *   |  |    |   C   |                    |  |                                                                              +---------------+   +---------------+
  *   |  |    +-------+                    |  |
  *   |  |                                 |  |
  *   |  |   +------------------------+    |  |
  *   |  |  / X2                       \   |  |
  *   |  | +    +-------+  +-------+    +  |  |
  *   |  | |    |  A1   |  |   B   |    |  |  |
  *   |  | +    +-------+  +-------+    +  |  |
  *   |  |  \                          /   |  |
  *   |  +   +------------------------+    +  |
  *   |   \                               /   |
  *   |    +-----------------------------+    |
  *   |                                       |
  *   +---------------------------------------+
  *
  */
  @Test
  void testNestedStages() {
    // given
    Stage stageX = createElement(casePlanModel, "X", Stage.class);
    Stage stageY = createElement(casePlanModel, "Y", Stage.class);
    HumanTask humanTaskA = createElement(casePlanModel, "A", HumanTask.class);
    HumanTask humanTaskB = createElement(casePlanModel, "B", HumanTask.class);
    HumanTask humanTaskC = createElement(casePlanModel, "C", HumanTask.class);

    PlanItem planItemA1 = createElement(casePlanModel, "PI_A1", PlanItem.class);
    planItemA1.setDefinition(humanTaskA);

    PlanItem planItemX1 = createElement(casePlanModel, "PI_X1", PlanItem.class);
    planItemX1.setDefinition(stageX);
    PlanItem planItemA2 = createElement(stageX, "PI_A2", PlanItem.class);
    planItemA2.setDefinition(humanTaskA);
    PlanItem planItemB = createElement(stageX, "PI_B", PlanItem.class);
    planItemB.setDefinition(humanTaskB);

    PlanItem planItemY = createElement(casePlanModel, "PI_Y", PlanItem.class);
    planItemY.setDefinition(stageY);
    PlanItem planItemC = createElement(stageY, "PI_C", PlanItem.class);
    planItemC.setDefinition(humanTaskC);
    PlanItem planItemX2 = createElement(stageY, "PI_X2", PlanItem.class);
    planItemX2.setDefinition(stageX);

    // when
    List<CaseDefinitionEntity> caseDefinitions = transform();

    // then
    assertThat(caseDefinitions).hasSize(1);

    CaseDefinitionEntity caseDef = caseDefinitions.get(0);
    List<CmmnActivity> activities = caseDef.getActivities();

    CmmnActivity casePlanModelActivity = activities.get(0);

    List<CmmnActivity> children = casePlanModelActivity.getActivities();
    assertThat(children).hasSize(3);

    CmmnActivity childPlanItem = children.get(0);
    assertThat(childPlanItem.getId()).isEqualTo(planItemA1.getId());
    assertThat(childPlanItem.getActivities()).isEmpty();

    childPlanItem = children.get(1);
    assertThat(childPlanItem.getId()).isEqualTo(planItemX1.getId());

    List<CmmnActivity> childrenOfX1 = childPlanItem.getActivities();
    assertThat(childrenOfX1)
            .isNotEmpty()
            .hasSize(2);

    childPlanItem = childrenOfX1.get(0);
    assertThat(childPlanItem.getId()).isEqualTo(planItemA2.getId());
    assertThat(childPlanItem.getActivities()).isEmpty();

    childPlanItem = childrenOfX1.get(1);
    assertThat(childPlanItem.getId()).isEqualTo(planItemB.getId());
    assertThat(childPlanItem.getActivities()).isEmpty();

    childPlanItem = children.get(2);
    assertThat(childPlanItem.getId()).isEqualTo(planItemY.getId());

    List<CmmnActivity> childrenOfY = childPlanItem.getActivities();
    assertThat(childrenOfY)
            .isNotEmpty()
            .hasSize(2);

    childPlanItem = childrenOfY.get(0);
    assertThat(childPlanItem.getId()).isEqualTo(planItemC.getId());
    assertThat(childPlanItem.getActivities()).isEmpty();

    childPlanItem = childrenOfY.get(1);
    assertThat(childPlanItem.getId()).isEqualTo(planItemX2.getId());

    List<CmmnActivity> childrenOfX2 = childPlanItem.getActivities();
    assertThat(childrenOfX2)
            .isNotEmpty()
            .hasSize(2);

    childPlanItem = childrenOfX2.get(0);
    assertThat(childPlanItem.getId()).isEqualTo(planItemA2.getId());
    assertThat(childPlanItem.getActivities()).isEmpty();

    childPlanItem = childrenOfX2.get(1);
    assertThat(childPlanItem.getId()).isEqualTo(planItemB.getId());
    assertThat(childPlanItem.getActivities()).isEmpty();

  }

}

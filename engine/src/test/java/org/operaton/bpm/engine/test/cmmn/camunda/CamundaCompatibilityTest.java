/*
 * Copyright 2025 the Operaton contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.engine.test.cmmn.camunda;

import org.junit.jupiter.api.Test;
import org.operaton.bpm.engine.exception.NotAllowedException;
import org.operaton.bpm.engine.runtime.*;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.cmmn.CmmnTest;
import org.operaton.bpm.engine.variable.Variables;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class CamundaCompatibilityTest extends CmmnTest {
    @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/camunda/CamundaCompatibilityTest.testOccurListenerByScript.cmmn"})
    @Test
    void testOccurListenerByScript() {
        // given
        String caseInstanceId = caseService
                .withCaseDefinitionByKey("case")
                .create()
                .getId();

        String milestoneId = caseService
                .createCaseExecutionQuery()
                .activityId("PI_Milestone_1")
                .singleResult()
                .getId();

        // when
        occur(milestoneId);

        // then
        VariableInstanceQuery query = runtimeService
                .createVariableInstanceQuery()
                .caseInstanceIdIn(caseInstanceId);

        assertThat(query.count()).isEqualTo(4);

        assertThat((Boolean) query.variableName("occur").singleResult().getValue()).isTrue();
        assertThat(query.variableName("occurEventCounter").singleResult().getValue()).isEqualTo(1);
        assertThat(query.variableName("occurOnCaseExecutionId").singleResult().getValue()).isEqualTo(milestoneId);

        assertThat(query.variableName("eventCounter").singleResult().getValue()).isEqualTo(1);

    }
}

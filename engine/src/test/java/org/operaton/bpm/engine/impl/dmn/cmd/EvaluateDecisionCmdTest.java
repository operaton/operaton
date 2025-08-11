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
package org.operaton.bpm.engine.impl.dmn.cmd;

import org.operaton.bpm.engine.impl.dmn.DecisionEvaluationBuilderImpl;
import org.operaton.bpm.engine.impl.interceptor.CommandExecutor;
import org.operaton.bpm.engine.impl.persistence.deploy.cache.DeploymentCache;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class EvaluateDecisionCmdTest {

  @ParameterizedTest
  @CsvSource(
    value = {
      "true,true",
      "true,false",
      "false,true",
      "false,false"
    })
  void findByKey_shouldQueryCorrectCacheMethod(boolean hasVersion, boolean withTenant) {
    // given
    String decisionDefinitionKey = "decisionKey";
    String tenantId = "someTenant";
    var builder = DecisionEvaluationBuilderImpl.evaluateDecisionByKey(mock(CommandExecutor.class), decisionDefinitionKey);
    if (hasVersion) {
      builder.version(1);
    }
    if (withTenant) {
      builder.decisionDefinitionTenantId(tenantId);
    }

    var cmd = new EvaluateDecisionCmd((DecisionEvaluationBuilderImpl)builder);
    var deploymentCache = mock(DeploymentCache.class);

    // when
    cmd.findByKey(deploymentCache);

    // then
    if (!hasVersion && !withTenant) {
      verify(deploymentCache).findDeployedLatestDecisionDefinitionByKey(decisionDefinitionKey);
    }
    if (hasVersion && !withTenant) {
      verify(deploymentCache).findDeployedDecisionDefinitionByKeyAndVersion(decisionDefinitionKey, 1);
    }
    if (!hasVersion && withTenant) {
      verify(deploymentCache).findDeployedLatestDecisionDefinitionByKeyAndTenantId(decisionDefinitionKey, tenantId);
    }
    if (hasVersion && withTenant) {
      verify(deploymentCache).findDeployedDecisionDefinitionByKeyVersionAndTenantId(decisionDefinitionKey, 1,
        tenantId);
    }
  }
}

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
package org.operaton.bpm.integrationtest.deployment.spring;

import org.operaton.bpm.application.PostDeploy;
import org.operaton.bpm.application.PreUndeploy;
import org.operaton.bpm.engine.spring.application.SpringServletProcessApplication;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Daniel Meyer
 *
 */
public class CustomSpringServletProcessApplication extends SpringServletProcessApplication {

  private boolean isPostDeployInvoked = false;
  private boolean isPreUndeployInvoked = false;

  @PostDeploy
  public void postDeploy() {
    isPostDeployInvoked = true;
  }

  @PreUndeploy
  public void preUndeploy() {
    isPreUndeployInvoked = true;
  }

  @Override
  public void start() {
    assertThat(isPostDeployInvoked).isFalse();
    super.start();
    assertThat(isPostDeployInvoked).as("@PostDeploy Method not invoked").isTrue();
  }

  @Override
  public void stop() {
    assertThat(isPreUndeployInvoked).isFalse();
    super.stop();
    assertThat(isPreUndeployInvoked).as("@PreUndeploy Method not invoked").isTrue();
  }

}

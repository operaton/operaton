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
package org.operaton.bpm.integrationtest.deployment.callbacks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.operaton.bpm.integrationtest.util.TestContainer.addContainerSpecificResourcesForNonPa;

import java.util.List;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.integrationtest.deployment.callbacks.apps.PostDeployInjectApp;

/**
 * @author Daniel Meyer
 *
 */
@ExtendWith(ArquillianExtension.class)
public class PostDeployInjectDefaultEngineTest {

  @Deployment
  public static WebArchive createDeployment() {
    var webArchive = ShrinkWrap.create(WebArchive.class, "test.war")
        .addClass(PostDeployInjectApp.class);

    addContainerSpecificResourcesForNonPa(webArchive);
    return webArchive;
  }

  @Test
  void test() {
    assertThat(PostDeployInjectApp.processEngine).as("processEngine must be injected").isNotNull();
    assertThat(PostDeployInjectApp.processApplicationInfo).as("processApplicationInfo must be injected").isNotNull();

    List<ProcessEngine> processEngines = PostDeployInjectApp.processEngines;
    assertThat(processEngines)
            .as("processEngines must be injected")
            .isNotNull()
            // the app did not do a deployment so no engines are in the list
            .isEmpty();
  }

}

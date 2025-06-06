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
package org.operaton.bpm.integrationtest.functional.metadata.engine;

import org.operaton.bpm.integrationtest.util.AbstractFoxPlatformIntegrationTest;

import org.jboss.arquillian.container.test.api.Deployment;

import static org.assertj.core.api.Assertions.assertThat;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * This time, we have two process-engines.xml files in separate library jars.
 *
 * @author Daniel Meyer
 *
 */
@RunWith(Arquillian.class)
public class TestMultipleProcessEnginesXmlsInLibrary extends AbstractFoxPlatformIntegrationTest {

  @Deployment
  public static WebArchive processArchive() {

    return initWebArchiveDeployment()
            .addAsLibraries(
              ShrinkWrap.create(JavaArchive.class, "engine1.jar")
                    .addAsResource("singleEngine.xml", "META-INF/processes.xml"),
              ShrinkWrap.create(JavaArchive.class, "engine2.jar")
                   .addAsResource("twoEngines.xml", "META-INF/processes.xml")
         );
  }

  @Test
  public void testDeployProcessArchive() {
    assertThat(processEngineService.getProcessEngine("engine1")).isNotNull();
    assertThat(processEngineService.getProcessEngine("engine2")).isNotNull();
    assertThat(processEngineService.getProcessEngine("engine3")).isNotNull();
  }

}

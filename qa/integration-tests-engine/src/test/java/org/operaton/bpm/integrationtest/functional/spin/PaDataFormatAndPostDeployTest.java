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
package org.operaton.bpm.integrationtest.functional.spin;

import static org.assertj.core.api.Assertions.assertThat;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.operaton.bpm.BpmPlatform;
import org.operaton.bpm.integrationtest.functional.spin.dataformat.Foo;
import org.operaton.bpm.integrationtest.functional.spin.dataformat.FooDataFormat;
import org.operaton.bpm.integrationtest.functional.spin.dataformat.FooDataFormatProvider;
import org.operaton.bpm.integrationtest.functional.spin.dataformat.FooSpin;
import org.operaton.bpm.integrationtest.util.AbstractFoxPlatformIntegrationTest;
import org.operaton.bpm.integrationtest.util.DeploymentHelper;
import org.operaton.bpm.integrationtest.util.TestConstants;
import org.operaton.spin.spi.DataFormatProvider;

/**
 * @author Daniel Meyer
 *
 */
@ExtendWith(ArquillianExtension.class)
public class PaDataFormatAndPostDeployTest extends AbstractFoxPlatformIntegrationTest {

  @Deployment
  public static WebArchive createDeployment() {
    return ShrinkWrap.create(WebArchive.class, "test.war")
            .addAsWebInfResource("org/operaton/bpm/integrationtest/beans.xml", "beans.xml")
            .addAsLibraries(DeploymentHelper.getEngineCdi())
            .addAsLibraries(DeploymentHelper.getTestingLibs())
            .addAsResource("META-INF/processes.xml", "META-INF/processes.xml")
            .addClass(AbstractFoxPlatformIntegrationTest.class)
            .addClass(TestConstants.class)
            .addClass(PaDataformatAndPostDeployApp.class)
            .addAsResource("org/operaton/bpm/integrationtest/oneTaskProcess.bpmn")
            .addClass(Foo.class)
            .addClass(FooDataFormat.class)
            .addClass(FooDataFormatProvider.class)
            .addClass(FooSpin.class)
            .addAsServiceProvider(DataFormatProvider.class, FooDataFormatProvider.class);
  }

  @Test
  void shouldDeployApp() {
    assertThat(BpmPlatform.getProcessApplicationService()
      .getProcessApplicationInfo(PaDataformatAndPostDeployApp.PA_NAME)).isNotNull();
  }

}

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
package org.operaton.bpm.integrationtest.deployment.ear;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.operaton.bpm.application.impl.ejb.DefaultEjbProcessApplication;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.cdi.impl.util.ProgrammaticBeanLookup;
import org.operaton.bpm.engine.test.util.JobExecutorWaitUtils;
import org.operaton.bpm.integrationtest.deployment.ear.beans.NamedCdiBean;
import org.operaton.bpm.integrationtest.util.AbstractFoxPlatformIntegrationTest;
import org.operaton.bpm.integrationtest.util.DeploymentHelper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Roman Smirnov
 * @author Daniel Meyer
 *
 */
@ExtendWith(ArquillianExtension.class)
public class TestPaAsEjbJar extends AbstractFoxPlatformIntegrationTest {

  /**
   * <pre>
   * test-application.ear
   *    |-- pa.jar
   *        |-- DefaultEjbProcessApplication.class
   *        |-- NamedCdiBean.class
   *        |-- AbstractFoxPlatformIntegrationTest.class
   *        |-- TestPaAsEjbJar.class
   *        |-- org/operaton/bpm/integrationtest/deployment/ear/paAsEjbJar-process.bpmn20.xml
   *        |-- META-INF/processes.xml
   *        |-- META-INF/beans.xml
   *
   * <p>
   *    |-- operaton-engine-cdi.jar
   *        |-- META-INF/MANIFEST.MF
   * </pre>
   * </p>
   */
  @Deployment
  public static EnterpriseArchive paAsEjbModule() {

    JavaArchive processArchive1Jar = ShrinkWrap.create(JavaArchive.class, "pa.jar")
      .addClass(DefaultEjbProcessApplication.class)
      .addClass(NamedCdiBean.class)
      .addClass(AbstractFoxPlatformIntegrationTest.class)
      .addClass(JobExecutorWaitUtils.class)
      .addClass(TestPaAsEjbJar.class)
      .addAsResource("org/operaton/bpm/integrationtest/deployment/ear/paAsEjbJar-process.bpmn20.xml")
      .addAsResource("META-INF/processes.xml", "META-INF/processes.xml")
      .addAsManifestResource("org/operaton/bpm/integrationtest/beans.xml", "beans.xml");

    return ShrinkWrap.create(EnterpriseArchive.class, "paAsEjbModule.ear")
      .addAsModule(processArchive1Jar)
      .addAsLibrary(DeploymentHelper.getEngineCdi())
      .addAsLibraries(DeploymentHelper.getTestingLibs());
  }

  @Test
  void testPaAsEjbModule() {
    ProcessEngine processEngine = ProgrammaticBeanLookup.lookup(ProcessEngine.class);
    assertThat(processEngine).isNotNull();

    runtimeService.startProcessInstanceByKey("paAsEjbJar-process");
    assertThat(runtimeService.createProcessInstanceQuery().count()).isOne();
    waitForJobExecutorToProcessAllJobs();

    assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();
  }

}

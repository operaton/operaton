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
package org.operaton.bpm.engine.cdi.test.impl.util;

import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Named;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.operaton.bpm.engine.cdi.impl.ProcessEngineServicesProducer;
import org.operaton.bpm.engine.cdi.impl.util.ProgrammaticBeanLookup;
import org.operaton.bpm.engine.cdi.test.impl.beans.SpecializedTestBean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 * @author Ronny Bräunlich
 *
 */
@ExtendWith(ArquillianExtension.class)
public class ProgrammaticBeanLookupTest {

  /**
   * Because of all alternatives and specializations I have to handle deployment myself
   */
  @ArquillianResource
  private Deployer deployer;

  @Deployment(name = "normal", managed = false)
  public static JavaArchive createDeployment() {
    return ShrinkWrap.create(JavaArchive.class)
        .addClass(ProgrammaticBeanLookup.class)
        .addClass(ProcessEngineServicesProducer.class)
        .addAsManifestResource("org/operaton/bpm/engine/cdi/test/impl/util/beans.xml", "beans.xml");
  }

  @Deployment(name = "withAlternative", managed = false)
  public static JavaArchive createDeploymentWithAlternative() {
    return ShrinkWrap.create(JavaArchive.class)
        .addClass(ProgrammaticBeanLookup.class)
        .addClass(ProcessEngineServicesProducer.class)
        .addClass(AlternativeTestBean.class)
        .addAsManifestResource("org/operaton/bpm/engine/cdi/test/impl/util/beansWithAlternative.xml", "beans.xml");
  }

  @Deployment(name = "withSpecialization", managed = false)
  public static JavaArchive createDeploymentWithSpecialization() {
    return ShrinkWrap.create(JavaArchive.class)
        .addClass(ProgrammaticBeanLookup.class)
        .addClass(ProcessEngineServicesProducer.class)
        .addClass(SpecializedTestBean.class)
        .addAsManifestResource("org/operaton/bpm/engine/cdi/test/impl/util/beans.xml", "beans.xml");
  }

  @Deployment(name = "withProducerMethod", managed = false)
  public static JavaArchive createDeploymentWithProducerMethod() {
    return ShrinkWrap.create(JavaArchive.class)
        .addClass(ProgrammaticBeanLookup.class)
        .addClass(ProcessEngineServicesProducer.class)
        .addClass(BeanWithProducerMethods.class)
        .addAsManifestResource("org/operaton/bpm/engine/cdi/test/impl/util/beans.xml", "beans.xml");
  }

  @Deployment(name = "annotatedDiscovery", managed = false)
  public static JavaArchive createDeploymentWithAnnotatedDiscovery() {
    StringAsset beansXml = new StringAsset(
      "<beans xmlns=\"https://jakarta.ee/xml/ns/jakartaee\" "
      + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
      + "xsi:schemaLocation=\"https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/beans_4_0.xsd\" "
      + "version=\"4.0\" bean-discovery-mode=\"annotated\"></beans>");

    return ShrinkWrap.create(JavaArchive.class)
        .addClass(ProgrammaticBeanLookup.class)
        .addClass(ProcessEngineServicesProducer.class)
        .addAsManifestResource(beansXml, "beans.xml");
  }

  @Test
  void testLookupBean() {
    deployer.deploy("normal");
    Object lookup = ProgrammaticBeanLookup.lookup("testOnly");
    assertThat(lookup).isInstanceOf(TestBean.class);
    deployer.undeploy("normal");
  }

  @Test
  void testLookupShouldFindAlternative() {
    deployer.deploy("withAlternative");
    Object lookup = ProgrammaticBeanLookup.lookup("testOnly");
    assertThat(lookup.getClass().getName()).isEqualTo(AlternativeTestBean.class.getName());
    deployer.undeploy("withAlternative");
  }

  @Test
  void testLookupShouldFindSpecialization() {
    deployer.deploy("withSpecialization");
    Object lookup = ProgrammaticBeanLookup.lookup("testOnly");
    assertThat(lookup.getClass().getName()).isEqualTo(SpecializedTestBean.class.getName());
    deployer.undeploy("withSpecialization");
  }

  @Test
  void testLookupShouldSupportProducerMethods() {
    deployer.deploy("withProducerMethod");
    assertThat(ProgrammaticBeanLookup.lookup("producedString")).isEqualTo("exampleString");
    deployer.undeploy("withProducerMethod");
  }

  @Test
  void testLookupShouldSupportAnnotatedDiscovery() {
    deployer.deploy("annotatedDiscovery");
    assertThat(ProgrammaticBeanLookup.lookup(ProcessEngineServicesProducer.class)).isNotNull();
    deployer.undeploy("annotatedDiscovery");
  }

  @Named("testOnly")
  public static class TestBean extends Object {
  }

  @Alternative
  @Named("testOnly")
  public static class AlternativeTestBean extends TestBean {
  }
}

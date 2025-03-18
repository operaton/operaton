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
package org.operaton.bpm.integrationtest.functional.spin;

import org.operaton.bpm.application.ProcessApplicationContext;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.engine.variable.value.ObjectValue;
import org.operaton.bpm.integrationtest.functional.spin.dataformat.Foo;
import org.operaton.bpm.integrationtest.functional.spin.dataformat.FooDataFormat;
import org.operaton.bpm.integrationtest.functional.spin.dataformat.FooDataFormatProvider;
import org.operaton.bpm.integrationtest.functional.spin.dataformat.FooSpin;
import org.operaton.bpm.integrationtest.util.AbstractFoxPlatformIntegrationTest;
import org.operaton.bpm.integrationtest.util.DeploymentHelper;
import org.operaton.spin.spi.DataFormatProvider;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.operaton.bpm.engine.variable.Variables.serializedObjectValue;

/**
 * @author Thorben Lindhauer
 *
 */
@RunWith(Arquillian.class)
public class PaDataFormatProviderTest extends AbstractFoxPlatformIntegrationTest {

  @Deployment
  public static WebArchive createDeployment() {
    return ShrinkWrap.create(WebArchive.class, "PaDataFormatTest.war")
        .addAsResource("META-INF/processes.xml")
        .addClass(AbstractFoxPlatformIntegrationTest.class)
        .addAsLibraries(DeploymentHelper.getAssertJ())
        .addAsResource("org/operaton/bpm/integrationtest/oneTaskProcess.bpmn")
        .addClass(Foo.class)
        .addClass(FooDataFormat.class)
        .addClass(FooDataFormatProvider.class)
        .addClass(FooSpin.class)
        .addAsServiceProvider(DataFormatProvider.class, FooDataFormatProvider.class)
        .addClass(ReferenceStoringProcessApplication.class);
  }

  /**
   * Tests that
   * 1) a serialized value can be set OUT OF process application context
   *   even if the data format is not available (using the fallback serializer)
   * 2) and that this value can be deserialized IN process application context
   *   by using the PA-local serializer
   */
  @Test
  public void customFormatCanBeUsedForVariableSerialization() {
    final ProcessInstance pi = runtimeService.startProcessInstanceByKey("testProcess",
        Variables.createVariables()
          .putValue("serializedObject",
              serializedObjectValue("foo")
              .serializationDataFormat(FooDataFormat.NAME)
              .objectTypeName(Foo.class.getName())));

    ObjectValue objectValue;
    try {
      ProcessApplicationContext.setCurrentProcessApplication(ReferenceStoringProcessApplication.INSTANCE);
      objectValue = runtimeService.getVariableTyped(pi.getId(), "serializedObject", true);
    } finally {
      ProcessApplicationContext.clear();
    }

    Object value = objectValue.getValue();
    assertThat(value).isInstanceOf(Foo.class);
  }

}

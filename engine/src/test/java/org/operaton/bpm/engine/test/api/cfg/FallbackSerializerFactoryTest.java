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
package org.operaton.bpm.engine.test.api.cfg;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration;
import org.operaton.bpm.engine.impl.variable.serializer.JavaObjectSerializer;
import org.operaton.bpm.engine.impl.variable.serializer.TypedValueSerializer;
import org.operaton.bpm.engine.impl.variable.serializer.VariableSerializerFactory;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.engine.variable.value.ObjectValue;
import org.operaton.bpm.engine.variable.value.TypedValue;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Thorben Lindhauer
 *
 */
class FallbackSerializerFactoryTest {

  protected ProcessEngine processEngine;
  protected String deployment;

  @AfterEach
  void tearDown() {

    if (processEngine != null) {
      if (deployment != null) {
        processEngine.getRepositoryService().deleteDeployment(deployment, true);
      }

      processEngine.close();
    }
  }

  @Test
  void testFallbackSerializer() {
    // given
    // that the process engine is configured with a fallback serializer factory
     ProcessEngineConfigurationImpl engineConfiguration = new StandaloneInMemProcessEngineConfiguration()
       .setJdbcUrl("jdbc:h2:mem:operaton-forceclose")
       .setProcessEngineName("engine-forceclose");

     engineConfiguration.setFallbackSerializerFactory(new ExampleSerializerFactory());

     processEngine = engineConfiguration.buildProcessEngine();
     deployOneTaskProcess(processEngine);

     // when setting a variable that no regular serializer can handle
     ObjectValue objectValue = Variables.objectValue("foo").serializationDataFormat(ExampleSerializer.FORMAT).create();

     ProcessInstance pi = processEngine.getRuntimeService().startProcessInstanceByKey("oneTaskProcess",
         Variables.createVariables().putValueTyped("var", objectValue));

     ObjectValue fetchedValue = processEngine.getRuntimeService().getVariableTyped(pi.getId(), "var", true);

    // then the fallback serializer is used
    assertThat(fetchedValue).isNotNull();
    assertThat(fetchedValue.getSerializationDataFormat()).isEqualTo(ExampleSerializer.FORMAT);
    assertThat(fetchedValue.getValue()).isEqualTo("foo");
  }

  @Test
  void testFallbackSerializerDoesNotOverrideRegularSerializer() {
    // given
    // that the process engine is configured with a serializer for a certain format
    // and a fallback serializer factory for the same format
     ProcessEngineConfigurationImpl engineConfiguration = new StandaloneInMemProcessEngineConfiguration()
       .setJdbcUrl("jdbc:h2:mem:operaton-forceclose")
       .setProcessEngineName("engine-forceclose");

     engineConfiguration.setCustomPreVariableSerializers(List.of(new ExampleConstantSerializer()));
     engineConfiguration.setFallbackSerializerFactory(new ExampleSerializerFactory());

     processEngine = engineConfiguration.buildProcessEngine();
     deployOneTaskProcess(processEngine);

     // when setting a variable that no regular serializer can handle
     ObjectValue objectValue = Variables.objectValue("foo").serializationDataFormat(ExampleSerializer.FORMAT).create();

     ProcessInstance pi = processEngine.getRuntimeService().startProcessInstanceByKey("oneTaskProcess",
         Variables.createVariables().putValueTyped("var", objectValue));

     ObjectValue fetchedValue = processEngine.getRuntimeService().getVariableTyped(pi.getId(), "var", true);

    // then the fallback serializer is used
    assertThat(fetchedValue).isNotNull();
    assertThat(fetchedValue.getSerializationDataFormat()).isEqualTo(ExampleSerializer.FORMAT);
    assertThat(fetchedValue.getValue()).isEqualTo(ExampleConstantSerializer.DESERIALIZED_VALUE);
  }

  public static class ExampleSerializerFactory implements VariableSerializerFactory {

    @Override
    public TypedValueSerializer<?> getSerializer(String serializerName) {
      return new ExampleSerializer();
    }

    @Override
    public TypedValueSerializer<?> getSerializer(TypedValue value) {
      return new ExampleSerializer();
    }

  }

  public static class ExampleSerializer extends JavaObjectSerializer {

    public static final String FORMAT = "example";

    public ExampleSerializer() {
      super();
      this.serializationDataFormat = FORMAT;
    }

    @Override
    public String getName() {
      return FORMAT;
    }

  }

  public static class ExampleConstantSerializer extends JavaObjectSerializer {

    public static final String DESERIALIZED_VALUE = "bar";

    public ExampleConstantSerializer() {
      super();
      this.serializationDataFormat = ExampleSerializer.FORMAT;
    }

    @Override
    public String getName() {
      return ExampleSerializer.FORMAT;
    }

    @Override
    protected Object deserializeFromByteArray(byte[] bytes, String objectTypeName) {
      // deserialize everything to a constant string
      return DESERIALIZED_VALUE;
    }

  }

  protected void deployOneTaskProcess(ProcessEngine engine) {
    deployment = engine.getRepositoryService()
        .createDeployment()
        .addClasspathResource("org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
        .deploy()
        .getId();
  }
}

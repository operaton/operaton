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
package org.operaton.spin.plugin.impl;

import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.variable.serializer.DefaultVariableSerializers;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.spin.DataFormats;
import org.operaton.spin.plugin.variable.type.JsonValueType;
import org.operaton.spin.plugin.variable.type.XmlValueType;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Ronny Bräunlich
 *
 */
@ExtendWith(ProcessEngineExtension.class)
class SpinProcessEnginePluginTest {
  ProcessEngineConfigurationImpl processEngineConfiguration;

  @Test
  void pluginDoesNotRegisterXmlSerializerIfNotPresentInClasspath() throws IOException {
    ClassLoader mockClassloader = Mockito.mock(ClassLoader.class);
    Mockito.when(mockClassloader.getResources(Mockito.anyString())).thenReturn(Collections.enumeration(Collections.<URL>emptyList()));
    DataFormats.loadDataFormats(mockClassloader);
    ProcessEngineConfigurationImpl mockConfig = Mockito.mock(ProcessEngineConfigurationImpl.class);
    DefaultVariableSerializers serializers = new DefaultVariableSerializers();
    Mockito.when(mockConfig.getVariableSerializers()).thenReturn(serializers);
    new SpinProcessEnginePlugin().registerSerializers(mockConfig);

    assertNull(serializers.getSerializerByName(XmlValueType.TYPE_NAME));
  }

  @Test
  void pluginDoesNotRegisterJsonSerializerIfNotPresentInClasspath() throws IOException {
    ClassLoader mockClassloader = Mockito.mock(ClassLoader.class);
    Mockito.when(mockClassloader.getResources(Mockito.anyString())).thenReturn(Collections.enumeration(Collections.<URL>emptyList()));
    DataFormats.loadDataFormats(mockClassloader);
    ProcessEngineConfigurationImpl mockConfig = Mockito.mock(ProcessEngineConfigurationImpl.class);
    DefaultVariableSerializers serializers = new DefaultVariableSerializers();
    Mockito.when(mockConfig.getVariableSerializers()).thenReturn(serializers);
    new SpinProcessEnginePlugin().registerSerializers(mockConfig);

    assertNull(serializers.getSerializerByName(JsonValueType.TYPE_NAME));
  }

  @Test
  void pluginRegistersXmlSerializerIfPresentInClasspath() {
    DataFormats.loadDataFormats(null);
    ProcessEngineConfigurationImpl mockConfig = Mockito.mock(ProcessEngineConfigurationImpl.class);
    Mockito.when(mockConfig.getVariableSerializers()).thenReturn(processEngineConfiguration.getVariableSerializers());
    new SpinProcessEnginePlugin().registerSerializers(mockConfig);

    assertTrue(processEngineConfiguration.getVariableSerializers().getSerializerByName(XmlValueType.TYPE_NAME) instanceof XmlValueSerializer);
  }

  @Test
  void pluginRegistersJsonSerializerIfPresentInClasspath() {
    DataFormats.loadDataFormats(null);
    ProcessEngineConfigurationImpl mockConfig = Mockito.mock(ProcessEngineConfigurationImpl.class);
    Mockito.when(mockConfig.getVariableSerializers()).thenReturn(processEngineConfiguration.getVariableSerializers());
    new SpinProcessEnginePlugin().registerSerializers(mockConfig);

    assertTrue(processEngineConfiguration.getVariableSerializers().getSerializerByName(JsonValueType.TYPE_NAME) instanceof JsonValueSerializer);
  }
}

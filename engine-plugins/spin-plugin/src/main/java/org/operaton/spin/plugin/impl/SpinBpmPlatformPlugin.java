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
package org.operaton.spin.plugin.impl;

import org.operaton.bpm.application.AbstractProcessApplication;
import org.operaton.bpm.application.ProcessApplicationInterface;
import org.operaton.bpm.container.impl.plugin.BpmPlatformPlugin;
import org.operaton.bpm.engine.impl.variable.serializer.DefaultVariableSerializers;
import org.operaton.bpm.engine.impl.variable.serializer.TypedValueSerializer;
import org.operaton.bpm.engine.impl.variable.serializer.VariableSerializers;
import org.operaton.spin.DataFormats;

import java.util.List;

/**
 * @author Thorben Lindhauer
 *
 */
public class SpinBpmPlatformPlugin implements BpmPlatformPlugin {

  private static final SpinPluginLogger LOG = SpinPluginLogger.LOGGER;

  @Override
  public void postProcessApplicationDeploy(ProcessApplicationInterface processApplication) {
    ProcessApplicationInterface rawPa = processApplication.getRawObject();
    if (rawPa instanceof AbstractProcessApplication application) {
      initializeVariableSerializers(application);
    }
    else {
      LOG.logNoDataFormatsInitiailized("process application data formats", "process application is not a sub class of " + AbstractProcessApplication.class.getName());
    }
  }

  protected void initializeVariableSerializers(AbstractProcessApplication abstractProcessApplication) {
    VariableSerializers paVariableSerializers = abstractProcessApplication.getVariableSerializers();

    if (paVariableSerializers == null) {
      paVariableSerializers = new DefaultVariableSerializers();
      abstractProcessApplication.setVariableSerializers(paVariableSerializers);
    }

    for (TypedValueSerializer<?> serializer : lookupSpinSerializers(abstractProcessApplication.getProcessApplicationClassloader())) {
      paVariableSerializers.addSerializer(serializer);
    }
  }

  protected List<TypedValueSerializer<?>> lookupSpinSerializers(ClassLoader classLoader) {

    DataFormats paDataFormats = new DataFormats();
    paDataFormats.registerDataFormats(classLoader);

    // does not create PA-local serializers for native Spin values;
    // this is still an open feature CAM-5246
    return SpinVariableSerializers.createObjectValueSerializers(paDataFormats);
  }

  @Override
  public void postProcessApplicationUndeploy(ProcessApplicationInterface processApplication) {
    // nothing to do
  }

}

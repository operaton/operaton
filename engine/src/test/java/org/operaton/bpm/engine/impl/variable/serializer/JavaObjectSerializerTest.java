/*
 * Copyright 2026 the Operaton contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.engine.impl.variable.serializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Proxy;

import org.junit.jupiter.api.Test;

import org.operaton.bpm.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration;
import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.impl.util.ReflectUtil;

import static org.assertj.core.api.Assertions.assertThat;

class JavaObjectSerializerTest {

  @Test
  void shouldResolveProxyInterfaceWithMappedForkClassNameWhenCompatibilityMappingIsEnabled() throws Exception {
    Context.setProcessEngineConfiguration(new StandaloneInMemProcessEngineConfiguration()
      .setEnableForkClassNameCompatibilityMapping(true));

    try (var objectInputStream = new JavaObjectSerializer.ClassloaderAwareObjectInputStream(
        new ByteArrayInputStream(objectStreamHeader()))) {
      Class<?> proxyClass = objectInputStream.resolveProxyClass(new String[] {
          "io.orqueio.bpm.engine.impl.variable.serializer.JavaObjectSerializerTest$TestProxyInterface"
      });

      assertThat(Proxy.isProxyClass(proxyClass)).isTrue();
      assertThat(proxyClass.getInterfaces()).containsExactly(TestProxyInterface.class);
    } finally {
      Context.removeProcessEngineConfiguration();
    }
  }

  @Test
  void shouldResolveArrayDescriptorWithMappedForkClassNameWhenCompatibilityMappingIsEnabled() {
    Context.setProcessEngineConfiguration(new StandaloneInMemProcessEngineConfiguration()
      .setEnableForkClassNameCompatibilityMapping(true));

    try {
      Class<?> arrayClass = ReflectUtil.loadClass(
          "[Lio.orqueio.bpm.engine.impl.variable.serializer.JavaObjectSerializerTest$TestProxyInterface;");

      assertThat(arrayClass).isEqualTo(TestProxyInterface[].class);
    } finally {
      Context.removeProcessEngineConfiguration();
    }
  }

  private static byte[] objectStreamHeader() throws Exception {
    try (var baos = new ByteArrayOutputStream(); var oos = new ObjectOutputStream(baos)) {
      oos.flush();
      return baos.toByteArray();
    }
  }

  public interface TestProxyInterface {
  }

}

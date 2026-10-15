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
package org.operaton.bpm.wildfly.modules;

import java.io.File;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.apache.logging.log4j.LogManager;
import org.jboss.modules.LocalModuleLoader;
import org.jboss.modules.ModuleLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/** Tests the packaged Netty classes and native resources through their WildFly module classloaders. */
class NettyModuleIT {

  private static ModuleLoader moduleLoader;
  private static ClassLoader httpModule;

  @BeforeAll
  static void loadAssembledModules() throws Exception {
    Path modules = Path.of("target", "modules");
    assertThat(modules.resolve("io/netty/netty-codec-http/main/module.xml")).exists();
    moduleLoader = new LocalModuleLoader(new File[] { modules.toFile(), createLog4jModule().toFile() });
    httpModule = moduleLoader.loadModule("io.netty.netty-codec-http").getClassLoader();
  }

  // WildFly supplies this API; only the Netty replacements are part of target/modules.
  private static Path createLog4jModule() throws Exception {
    Path log4jJar = Path.of(LogManager.class.getProtectionDomain().getCodeSource().getLocation().toURI());
    Path root = Path.of("target", "netty-it-modules");
    Path moduleDir = root.resolve("org/apache/logging/log4j/api/main");
    Files.createDirectories(moduleDir);
    Files.copy(log4jJar, moduleDir.resolve("log4j-api.jar"), StandardCopyOption.REPLACE_EXISTING);
    Files.writeString(moduleDir.resolve("module.xml"), """
        <module xmlns="urn:jboss:module:1.9" name="org.apache.logging.log4j.api">
          <resources>
            <resource-root path="log4j-api.jar" />
          </resources>
          <dependencies>
            <module name="java.logging" />
          </dependencies>
        </module>
        """);
    return root;
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "Transfer-Encoding: chunked, xchunked\r\n",
      "Transfer-Encoding: chunked\r\nTransfer-Encoding: gzip\r\n",
      "Transfer-Encoding: chunked\r\nTransfer-Encoding: chunked\r\n"
  })
  void rejectsAmbiguousTransferEncoding(String headers) throws Exception {
    // CVE-2026-89044: the final transfer coding must be chunked, appearing exactly once.
    assertRequestDecoded(headers, false);
  }

  @Test
  void acceptsChunkedAsFinalTransferCodingAcrossHeaderLines() throws Exception {
    assertRequestDecoded("Transfer-Encoding: gzip\r\nTransfer-Encoding: chunked\r\n", true);
  }

  private static void assertRequestDecoded(String headers, boolean valid) throws Exception {
    Class<?> handlerClass = httpModule.loadClass("io.netty.channel.ChannelHandler");
    Object handlers = Array.newInstance(handlerClass, 1);
    Array.set(handlers, 0, httpModule.loadClass("io.netty.handler.codec.http.HttpRequestDecoder")
        .getConstructor().newInstance());
    Class<?> channelClass = httpModule.loadClass("io.netty.channel.embedded.EmbeddedChannel");
    Object channel = channelClass.getConstructor(handlers.getClass()).newInstance(handlers);
    try {
      String request = "POST / HTTP/1.1\r\nHost: localhost\r\n" + headers + "\r\n0\r\n\r\n";
      Object buffer = httpModule.loadClass("io.netty.buffer.Unpooled")
          .getMethod("wrappedBuffer", byte[].class).invoke(null, (Object) request.getBytes(StandardCharsets.US_ASCII));
      channelClass.getMethod("writeInbound", Object[].class).invoke(channel, (Object) new Object[] { buffer });
      Object decoded = channelClass.getMethod("readInbound").invoke(channel);
      assertThat(decoded).isInstanceOf(httpModule.loadClass("io.netty.handler.codec.http.HttpRequest"));
      Object result = httpModule.loadClass("io.netty.handler.codec.http.HttpObject")
          .getMethod("decoderResult").invoke(decoded);
      assertThat((boolean) result.getClass().getMethod("isSuccess").invoke(result))
          .as("decoder result for %s: %s", headers, result).isEqualTo(valid);
      if (valid) {
        Object content = channelClass.getMethod("readInbound").invoke(channel);
        try {
          assertThat(content).isInstanceOf(httpModule.loadClass("io.netty.handler.codec.http.LastHttpContent"));
        } finally {
          httpModule.loadClass("io.netty.util.ReferenceCountUtil").getMethod("release", Object.class)
              .invoke(null, content);
        }
      }
    } finally {
      channelClass.getMethod("finishAndReleaseAll").invoke(channel);
    }
  }

  @Test
  @EnabledOnOs({ OS.LINUX, OS.MAC })
  @EnabledIfSystemProperty(named = "os.arch", matches = "amd64|x86_64|aarch64")
  void loadsNativeTransportForCurrentPlatform() throws Exception {
    boolean linux = System.getProperty("os.name").startsWith("Linux");
    String transport = linux ? "epoll" : "kqueue";
    ClassLoader nativeModule = moduleLoader.loadModule("io.netty.netty-transport-native-" + transport).getClassLoader();
    Class<?> nativeTransport = nativeModule.loadClass(
        "io.netty.channel." + transport + "." + (linux ? "Epoll" : "KQueue"));
    assertThat((boolean) nativeTransport.getMethod("isAvailable").invoke(null))
        .as("%s native transport: %s", transport, nativeTransport.getMethod("unavailabilityCause").invoke(null))
        .isTrue();
  }
}

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
package org.operaton.bpm.container.impl.jmx.deployment.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.operaton.bpm.container.impl.deployment.scanning.ClassPathProcessApplicationScanner;


/**
 * @author Falko Menge
 * @author Daniel Meyer
 */
class MultipleClasspathRootsClassPathScannerTest {

  @Test
  void testScanClassPath_multipleRoots() throws MalformedURLException {

    // define a classloader with multiple roots.
    URLClassLoader classLoader = new URLClassLoader(
      new URL[]{
        new URL("file:src/test/resources/org/operaton/bpm/container/impl/jmx/deployment/util/ClassPathScannerTest.testScanClassPathWithFiles/"),
        new URL("file:src/test/resources/org/operaton/bpm/container/impl/jmx/deployment/util/ClassPathScannerTest.testScanClassPathWithFilesRecursive/"),
        new URL("file:src/test/resources/org/operaton/bpm/container/impl/jmx/deployment/util/ClassPathScannerTest.testScanClassPathRecursiveTwoDirectories.jar")
      });

    ClassPathProcessApplicationScanner scanner = new ClassPathProcessApplicationScanner();

    Map<String, byte[]> scanResult = new HashMap<>();

    scanner.scanPaResourceRootPath(classLoader, null, "classpath:directory/",scanResult);

    assertThat(contains(scanResult, "testDeployProcessArchive.bpmn20.xml")).as("'testDeployProcessArchive.bpmn20.xml' not found").isTrue();
    assertThat(contains(scanResult, "testDeployProcessArchive.png")).as("'testDeployProcessArchive.png' not found").isTrue();
    assertThat(scanResult).hasSize(2); // only finds two files since the resource name of the processes (and diagrams) is the same

    scanResult.clear();
    scanner.scanPaResourceRootPath(classLoader, null, "directory/", scanResult);

    assertThat(contains(scanResult, "testDeployProcessArchive.bpmn20.xml")).as("'testDeployProcessArchive.bpmn20.xml' not found").isTrue();
    assertThat(contains(scanResult, "testDeployProcessArchive.png")).as("'testDeployProcessArchive.png' not found").isTrue();
    assertThat(scanResult).hasSize(2); // only finds two files since the resource name of the processes (and diagrams) is the same

    scanResult.clear();
    scanner.scanPaResourceRootPath(classLoader, new URL("file:src/test/resources/org/operaton/bpm/container/impl/jmx/deployment/util/ClassPathScannerTest.testScanClassPathWithFilesRecursive/META-INF/processes.xml"), "pa:directory/", scanResult);

    assertThat(contains(scanResult, "testDeployProcessArchive.bpmn20.xml")).as("'testDeployProcessArchive.bpmn20.xml' not found").isTrue();
    assertThat(contains(scanResult, "testDeployProcessArchive.png")).as("'testDeployProcessArchive.png' not found").isTrue();
    assertThat(scanResult).hasSize(2); // only finds two files since a PA-local resource root path is provided

  }

  private boolean contains(Map<String, byte[]> scanResult, String suffix) {
    for (String string : scanResult.keySet()) {
      if (string.endsWith(suffix)) {
        return true;
      }
    }
    return false;
  }

}

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

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestTemplate;

import org.operaton.bpm.container.impl.deployment.scanning.ClassPathProcessApplicationScanner;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameterized;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameters;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author Falko Menge
 * @author Daniel Meyer
 */
@Parameterized
public class ClassPathScannerTest {

  private final String url;
  private static ClassPathProcessApplicationScanner scanner;

  @Parameters
  public static List<Object[]> data() {
    return List.of(new Object[][] {
            { "file:src/test/resources/org/operaton/bpm/container/impl/jmx/deployment/util/ClassPathScannerTest.testScanClassPathWithFiles/" },
            { "file:src/test/resources/org/operaton/bpm/container/impl/jmx/deployment/util/ClassPathScannerTest.testScanClassPathWithFilesRecursive/" },
            { "file:src/test/resources/org/operaton/bpm/container/impl/jmx/deployment/util/ClassPathScannerTest.testScanClassPathWithFilesRecursiveTwoDirectories/" },
            { "file:src/test/resources/org/operaton/bpm/container/impl/jmx/deployment/util/ClassPathScannerTest.testScanClassPathWithAdditionalResourceSuffixes/" },
            { "file:src/test/resources/org/operaton/bpm/container/impl/jmx/deployment/util/ClassPathScannerTest.testScanClassPath.jar" },
            { "file:src/test/resources/org/operaton/bpm/container/impl/jmx/deployment/util/ClassPathScannerTest.testScanClassPathRecursive.jar" },
            { "file:src/test/resources/org/operaton/bpm/container/impl/jmx/deployment/util/ClassPathScannerTest.testScanClassPathRecursiveTwoDirectories.jar" },
    });
  }


  public ClassPathScannerTest(String url) {
    this.url = url;
  }

  @BeforeAll
  static void setup() {
    scanner = new ClassPathProcessApplicationScanner();
  }

  /**
   * Test method for {@link org.operaton.bpm.container.impl.deployment.scanning.ClassPathProcessApplicationScanner#scanClassPath(java.lang.ClassLoader)}.
   * @throws MalformedURLException
   */
  @TestTemplate
  void testScanClassPath() throws Exception {

    URLClassLoader classLoader = getClassloader();

    Map<String, byte[]> scanResult = new HashMap<>();

    scanner.scanPaResourceRootPath(classLoader, new URL(url+"/META-INF/processes.xml"), null, scanResult);

    assertThat(contains(scanResult, "testDeployProcessArchive.bpmn20.xml")).as("'testDeployProcessArchive.bpmn20.xml' not found").isTrue();
    assertThat(contains(scanResult, "testDeployProcessArchive.png")).as("'testDeployProcessArchive.png' not found").isTrue();
    if(url.contains("TwoDirectories")) {
      assertThat(scanResult).hasSize(4);
    } else {
      assertThat(scanResult).hasSize(2);
    }
  }

  @TestTemplate
  void testScanClassPathWithNonExistingRootPath_relativeToPa() throws Exception {

    URLClassLoader classLoader = getClassloader();

    Map<String, byte[]> scanResult = new HashMap<>();
    scanner.scanPaResourceRootPath(classLoader, new URL(url+"/META-INF/processes.xml"), "pa:nonexisting", scanResult);

    assertThat(contains(scanResult, "testDeployProcessArchive.bpmn20.xml")).as("'testDeployProcessArchive.bpmn20.xml' found").isFalse();
    assertThat(contains(scanResult, "testDeployProcessArchive.png")).as("'testDeployProcessArchive.png' found").isFalse();
    assertThat(scanResult).isEmpty();
  }

  @TestTemplate
  void testScanClassPathWithNonExistingRootPath_nonRelativeToPa() throws Exception {

    URLClassLoader classLoader = getClassloader();

    Map<String, byte[]> scanResult = new HashMap<>();
    scanner.scanPaResourceRootPath(classLoader, null, "nonexisting", scanResult);

    assertThat(contains(scanResult, "testDeployProcessArchive.bpmn20.xml")).as("'testDeployProcessArchive.bpmn20.xml' found").isFalse();
    assertThat(contains(scanResult, "testDeployProcessArchive.png")).as("'testDeployProcessArchive.png' found").isFalse();
    assertThat(scanResult).isEmpty();
  }

  @TestTemplate
  void testScanClassPathWithExistingRootPath_relativeToPa() throws Exception {

    URLClassLoader classLoader = getClassloader();

    Map<String, byte[]> scanResult = new HashMap<>();
    scanner.scanPaResourceRootPath(classLoader, new URL(url+"/META-INF/processes.xml"), "pa:directory/", scanResult);

    if(url.contains("Recursive")) {
      assertThat(contains(scanResult, "testDeployProcessArchive.bpmn20.xml")).as("'testDeployProcessArchive.bpmn20.xml' not found").isTrue();
      assertThat(contains(scanResult, "testDeployProcessArchive.png")).as("'testDeployProcessArchive.png' not found").isTrue();
      assertThat(scanResult).hasSize(2);
    } else {
      assertThat(contains(scanResult, "testDeployProcessArchive.bpmn20.xml")).as("'testDeployProcessArchive.bpmn20.xml' found").isFalse();
      assertThat(contains(scanResult, "testDeployProcessArchive.png")).as("'testDeployProcessArchive.png' found").isFalse();
      assertThat(scanResult).isEmpty();
    }
  }

  @TestTemplate
  void testScanClassPathWithExistingRootPath_nonRelativeToPa() throws Exception {

    URLClassLoader classLoader = getClassloader();

    Map<String, byte[]> scanResult = new HashMap<>();
    scanner.scanPaResourceRootPath(classLoader, null, "directory/", scanResult);

    if(url.contains("Recursive")) {
      assertThat(contains(scanResult, "testDeployProcessArchive.bpmn20.xml")).as("'testDeployProcessArchive.bpmn20.xml' not found").isTrue();
      assertThat(contains(scanResult, "testDeployProcessArchive.png")).as("'testDeployProcessArchive.png' not found").isTrue();
      assertThat(scanResult).hasSize(2);
    } else {
      assertThat(contains(scanResult, "testDeployProcessArchive.bpmn20.xml")).as("'testDeployProcessArchive.bpmn20.xml' found").isFalse();
      assertThat(contains(scanResult, "testDeployProcessArchive.png")).as("'testDeployProcessArchive.png' found").isFalse();
      assertThat(scanResult).isEmpty();
    }
  }

  @TestTemplate
  void testScanClassPathWithAdditionalResourceSuffixes() throws Exception {
    URLClassLoader classLoader = getClassloader();

    String[] additionalResourceSuffixes = new String[] {"py", "rb", "groovy"};

    Map<String, byte[]> scanResult = scanner.findResources(classLoader, null, new URL(url + "/META-INF/processes.xml"), additionalResourceSuffixes);

    if (url.contains("AdditionalResourceSuffixes")) {
      assertThat(scanResult).hasSize(5);
    }
  }


  private URLClassLoader getClassloader() throws MalformedURLException {
    return new URLClassLoader(new URL[]{new URL(url)});
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

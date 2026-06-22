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

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.operaton.bpm.container.impl.deployment.scanning.ProcessApplicationScanningUtil;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author Clint Manning
 */
class VfsProcessApplicationScannerTest {

  @Test
  void testScanProcessArchivePathForResources() throws Exception {

    // given: scanning the relative test resource root
    URLClassLoader classLoader = new URLClassLoader(new URL[]{new URL("file:")});
    String processRootPath = "classpath:org/operaton/bpm/container/impl/jmx/deployment/process/";
    Map<String, byte[]> scanResult = ProcessApplicationScanningUtil.findResources(classLoader, processRootPath, null);

    // expect: finds only the BPMN process file and not treats the 'bpmn' folder
    assertThat(scanResult).hasSize(1);
    String processFileName = "VfsProcessScannerTest.bpmn20.xml";
    assertThat(contains(scanResult, processFileName)).as("'%s' not found".formatted(processFileName)).isTrue();
    assertThat(contains(scanResult, "processResource.txt")).as("'bpmn' folder in resource path found").isFalse();
  }

  @Test
  void testScanProcessArchivePathForCmmnResources() throws Exception {

    // given: scanning the relative test resource root
    URLClassLoader classLoader = new URLClassLoader(new URL[]{new URL("file:")});
    String processRootPath = "classpath:org/operaton/bpm/container/impl/jmx/deployment/case/";
    Map<String, byte[]> scanResult = ProcessApplicationScanningUtil.findResources(classLoader, processRootPath, null);

    // expect: finds only the CMMN process file and not treats the 'cmmn' folder
    assertThat(scanResult).hasSize(1);
    String processFileName = "VfsProcessScannerTest.cmmn";
    assertThat(contains(scanResult, processFileName)).as("'%s' not found".formatted(processFileName)).isTrue();
    assertThat(contains(scanResult, "caseResource.txt")).as("'cmmn' in resource path found").isFalse();
  }

  @Test
  void testScanProcessArchivePathWithAdditionalResourceSuffixes() throws Exception {
    URLClassLoader classLoader = new URLClassLoader(new URL[]{new URL("file:")});
    String processRootPath = "classpath:org/operaton/bpm/container/impl/jmx/deployment/script/";
    String[] additionalResourceSuffixes = new String[] { "py", "groovy", "rb" };
    Map<String, byte[]> scanResult = ProcessApplicationScanningUtil.findResources(classLoader, processRootPath, null, additionalResourceSuffixes);

    assertThat(scanResult).hasSize(4);
    String processFileName = "VfsProcessScannerTest.bpmn20.xml";
    assertThat(contains(scanResult, processFileName)).as("'%s' not found".formatted(processFileName)).isTrue();
    assertThat(contains(scanResult, "hello.py")).as("'hello.py' in resource path found").isTrue();
    assertThat(contains(scanResult, "hello.rb")).as("'hello.rb' in resource path found").isTrue();
    assertThat(contains(scanResult, "hello.groovy")).as("'hello.groovy' in resource path found").isTrue();
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

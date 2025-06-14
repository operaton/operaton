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
package org.operaton.bpm.integrationtest.util;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.cdi.impl.util.ProgrammaticBeanLookup;
import org.operaton.bpm.engine.impl.util.IoUtil;
import org.operaton.bpm.engine.repository.ProcessDefinition;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.ByteArrayAsset;

import static org.assertj.core.api.Assertions.assertThat;


public abstract class TestHelper {

  public static final String PROCESS_XML =
    "<definitions xmlns=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" xmlns:operaton=\"http://operaton.org/schema/1.0/bpmn\"  targetNamespace=\"Examples\"><process id=\"PROCESS_KEY\" isExecutable=\"true\" operaton:historyTimeToLive=\"P180D\"><startEvent id=\"start\"/></process></definitions>";

  public static Asset getStringAsAssetWithReplacements(String string, String[][] replacements) {

    for (String[] replacement : replacements) {
      string = string.replaceAll(replacement[0], replacement[1]);
    }

    return new ByteArrayAsset(string.getBytes());

  }

  public static Asset[] generateProcessAssets(int amount) {

    Asset[] result = new Asset[amount];

    for (int i = 0; i < result.length; i++) {
      result[i] = getStringAsAssetWithReplacements(PROCESS_XML, new String[][]{new String[]{"PROCESS_KEY", "process-" + i}});
    }

    return result;

  }

  public static void assertDiagramIsDeployed(boolean deployed, Class<?> clazz, String expectedDiagramResource, String processDefinitionKey) throws IOException {
    ProcessEngine processEngine = ProgrammaticBeanLookup.lookup(ProcessEngine.class);
    assertThat(processEngine).isNotNull();
    RepositoryService repositoryService = processEngine.getRepositoryService();
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
      .processDefinitionKey(processDefinitionKey)
      .singleResult();
    assertThat(processDefinition).isNotNull();

    InputStream actualStream = null;
    InputStream expectedStream = null;
    try {
      actualStream = repositoryService.getProcessDiagram(processDefinition.getId());

      if (deployed) {
        byte[] actualDiagram = IoUtil.readInputStream(actualStream, "actualStream");
        assertThat(actualDiagram).isNotNull().isNotEmpty();

        expectedStream = clazz.getResourceAsStream(expectedDiagramResource);
        byte[] expectedDiagram = IoUtil.readInputStream(expectedStream, "expectedSteam");
        assertThat(expectedDiagram).isNotNull();

        assertThat(isEqual(expectedStream, actualStream)).isTrue();
      } else {
        assertThat(actualStream).isNull();
      }
    } finally {
      IoUtil.closeSilently(actualStream);
      IoUtil.closeSilently(expectedStream);
    }
  }

  protected static boolean isEqual(InputStream stream1, InputStream stream2)
    throws IOException {

    ByteBuffer buffer1 = ByteBuffer.allocateDirect(1024);
    ByteBuffer buffer2 = ByteBuffer.allocateDirect(1024);

    try (ReadableByteChannel channel1 = Channels.newChannel(stream1); ReadableByteChannel channel2 = Channels.newChannel(stream2)) {
      while (true) {

        int bytesReadFromStream1 = channel1.read(buffer1);
        int bytesReadFromStream2 = channel2.read(buffer2);

        if (bytesReadFromStream1 == -1 || bytesReadFromStream2 == -1)
          return bytesReadFromStream1 == bytesReadFromStream2;

        buffer1.flip();
        buffer2.flip();

        for (int i = 0; i < Math.min(bytesReadFromStream1, bytesReadFromStream2); i++)
          if (buffer1.get() != buffer2.get())
            return false;

        buffer1.compact();
        buffer2.compact();
      }
    }
  }

}

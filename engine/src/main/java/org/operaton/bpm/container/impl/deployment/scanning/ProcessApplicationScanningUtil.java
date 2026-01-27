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
package org.operaton.bpm.container.impl.deployment.scanning;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Map;

import org.operaton.bpm.application.impl.metadata.spi.ProcessArchiveXml;
import org.operaton.bpm.container.impl.ContainerIntegrationLogger;
import org.operaton.bpm.container.impl.deployment.scanning.spi.ProcessApplicationScanner;
import org.operaton.bpm.engine.impl.ProcessEngineLogger;

import static org.operaton.bpm.engine.impl.ResourceSuffixes.BPMN_RESOURCE_SUFFIXES;
import static org.operaton.bpm.engine.impl.ResourceSuffixes.CMMN_RESOURCE_SUFFIXES;
import static org.operaton.bpm.engine.impl.ResourceSuffixes.DIAGRAM_RESOURCE_SUFFIXES;
import static org.operaton.bpm.engine.impl.ResourceSuffixes.DMN_RESOURCE_SUFFIXES;

public final class ProcessApplicationScanningUtil {
  private static final ContainerIntegrationLogger LOG = ProcessEngineLogger.CONTAINER_INTEGRATION_LOGGER;

  private ProcessApplicationScanningUtil() {
  }

  /**
   *
   * @param classLoader
   *          the classloader to scan
   * @param paResourceRootPath
   *          see {@link ProcessArchiveXml#PROP_RESOURCE_ROOT_PATH}
   * @param metaFileUri
   *          the URI to the META-INF/processes.xml file
   * @return a Map of process definitions
   */
  public static Map<String, byte[]> findResources(ClassLoader classLoader, String paResourceRootPath, URI metaFileUri) {
    return findResources(classLoader, paResourceRootPath, metaFileUri, null);
  }

  /**
   *
   * @param classLoader
   *          the classloader to scan
   * @param paResourceRootPath
   *          see {@link ProcessArchiveXml#PROP_RESOURCE_ROOT_PATH}
   * @param metaFileUri
   *          the URI to the META-INF/processes.xml file
   * @param additionalResourceSuffixes
   *          a list of additional suffixes for resources
   * @return a Map of process definitions
   */
  public static Map<String, byte[]> findResources(ClassLoader classLoader, String paResourceRootPath, URI metaFileUri, String[] additionalResourceSuffixes) {
    ProcessApplicationScanner scanner;

    try {
      // check if we must use JBoss VFS
      classLoader.loadClass("org.jboss.vfs.VFS");
      scanner = new VfsProcessApplicationScanner();
    }
    catch (Exception t) {
      scanner = new ClassPathProcessApplicationScanner();
    }

    URL metaFileUrl;
     try {
       metaFileUrl = metaFileUri != null ? metaFileUri.toURL() : null;
     } catch (MalformedURLException e) {
       throw LOG.invalidDeploymentDescriptorLocation(metaFileUri.getPath(), e);
     }

    return scanner.findResources(classLoader, paResourceRootPath, metaFileUrl, additionalResourceSuffixes);
  }

  public static boolean isDeployable(String filename) {
    return hasSuffix(filename, BPMN_RESOURCE_SUFFIXES)
      || hasSuffix(filename, CMMN_RESOURCE_SUFFIXES)
      || hasSuffix(filename, DMN_RESOURCE_SUFFIXES);
  }

  public static boolean isDeployable(String filename, String[] additionalResourceSuffixes) {
    return isDeployable(filename) || hasSuffix(filename, additionalResourceSuffixes);
  }

  public static boolean hasSuffix(String filename, String[] suffixes) {
    if (suffixes == null || suffixes.length == 0) {
      return false;
    } else {
      for (String suffix : suffixes) {
        if (filename.endsWith(suffix)) {
          return true;
        }
      }
      return false;
    }
  }

  public static boolean isDiagram(String fileName, String modelFileName) {
    // process resources
    boolean isBpmnDiagram = checkDiagram(fileName, modelFileName, DIAGRAM_RESOURCE_SUFFIXES, BPMN_RESOURCE_SUFFIXES);
    // case resources
    boolean isCmmnDiagram = checkDiagram(fileName, modelFileName, DIAGRAM_RESOURCE_SUFFIXES, CMMN_RESOURCE_SUFFIXES);
    // decision resources
    boolean isDmnDiagram = checkDiagram(fileName, modelFileName, DIAGRAM_RESOURCE_SUFFIXES, DMN_RESOURCE_SUFFIXES);

    return isBpmnDiagram || isCmmnDiagram || isDmnDiagram;
  }

  /**
   * Checks, whether a filename is a diagram for the given modelFileName.
   *
   * @param fileName filename to check.
   * @param modelFileName model file name.
   * @param diagramSuffixes suffixes of the diagram files.
   * @param modelSuffixes suffixes of model files.
   * @return true, if a file is a diagram for the model.
   */
  protected static boolean checkDiagram(String fileName, String modelFileName, String[] diagramSuffixes, String[] modelSuffixes) {
    for (String modelSuffix : modelSuffixes) {
      if (modelFileName.endsWith(modelSuffix)) {
        String caseFilePrefix = modelFileName.substring(0, modelFileName.length() - modelSuffix.length());
        if (fileName.startsWith(caseFilePrefix)) {
          for (String diagramResourceSuffix : diagramSuffixes) {
            if (fileName.endsWith(diagramResourceSuffix)) {
              return true;
            }
          }
        }
      }
    }
    return false;
  }
}

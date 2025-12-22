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
package org.operaton.bpm.engine.impl.ant;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipInputStream;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;

import org.operaton.bpm.engine.*;
import org.operaton.bpm.engine.impl.util.LogUtil;

/**
 * @author Tom Baeyens
 */
public class DeployBarTask extends Task {

  String processEngineName = ProcessEngines.NAME_DEFAULT;
  File file;
  List<FileSet> fileSets;

  @Override
  public void execute() throws BuildException {
    List<File> files = collectFiles();

    Thread currentThread = Thread.currentThread();
    ClassLoader originalClassLoader = setupClassLoader(currentThread);

    try {
      ProcessEngine processEngine = initializeProcessEngine();
      deployFiles(files, processEngine);
    } finally {
      currentThread.setContextClassLoader(originalClassLoader);
    }
  }

  private List<File> collectFiles() {
    List<File> files = new ArrayList<>();
    if (file != null) {
      files.add(file);
    }
    if (fileSets != null) {
      collectFilesFromFileSets(files);
    }
    return files;
  }

  private void collectFilesFromFileSets(List<File> files) {
    for (FileSet fileSet : fileSets) {
      DirectoryScanner directoryScanner = fileSet.getDirectoryScanner(getProject());
      File baseDir = directoryScanner.getBasedir();
      String[] includedFiles = directoryScanner.getIncludedFiles();
      String[] excludedFiles = directoryScanner.getExcludedFiles();
      List<String> excludedFilesList = Arrays.asList(excludedFiles);

      for (String includedFile : includedFiles) {
        if (!excludedFilesList.contains(includedFile)) {
          files.add(new File(baseDir, includedFile));
        }
      }
    }
  }

  private ClassLoader setupClassLoader(Thread currentThread) {
    ClassLoader originalClassLoader = currentThread.getContextClassLoader();
    currentThread.setContextClassLoader(DeployBarTask.class.getClassLoader());
    LogUtil.readJavaUtilLoggingConfigFromClasspath();
    return originalClassLoader;
  }

  private ProcessEngine initializeProcessEngine() {
    log("Initializing process engine " + processEngineName);
    ProcessEngines.init();
    ProcessEngine processEngine = ProcessEngines.getProcessEngine(processEngineName);

    if (processEngine == null) {
      handleMissingProcessEngine();
    }

    return processEngine;
  }

  private void handleMissingProcessEngine() {
    List<ProcessEngineInfo> processEngineInfos = ProcessEngines.getProcessEngineInfos();
    if (processEngineInfos != null && !processEngineInfos.isEmpty()) {
      // Since no engine with the given name is found, we can't be 100% sure which ProcessEngineInfo
      // is causing the error. We should show ALL errors and process engine names / resource URL's.
      String message = getErrorMessage(processEngineInfos, processEngineName);
      throw new ProcessEngineException(message);
    } else {
      throw new ProcessEngineException("Could not find a process engine with name '%s', no engines found. Make sure an engine configuration is present on the classpath".formatted(processEngineName));
    }
  }

  private void deployFiles(List<File> files, ProcessEngine processEngine) {
    RepositoryService repositoryService = processEngine.getRepositoryService();
    log("Starting to deploy %s files".formatted(files.size()));

    for (File f : files) {
      deployFile(f, repositoryService);
    }
  }

  private void deployFile(File file, RepositoryService repositoryService) {
    String path = file.getAbsolutePath();
    log("Handling file " + path);

    try (FileInputStream inputStream = new FileInputStream(file)) {
      log("deploying bar " + path);
      repositoryService
          .createDeployment()
          .name(file.getName())
          .addZipInputStream(new ZipInputStream(inputStream))
          .deploy();
    } catch (Exception e) {
      throw new BuildException("couldn't deploy bar %s: %s".formatted(path, e.getMessage()), e);
    }
  }

  private String getErrorMessage(List<ProcessEngineInfo> processEngineInfos, String name) {
    StringBuilder builder = new StringBuilder("Could not find a process engine with name ");
    builder.append(name).append(", engines loaded:\n");
    for (ProcessEngineInfo engineInfo : processEngineInfos) {
      String engineName = engineInfo.getName() != null ? engineInfo.getName() : "unknown";
      builder.append("Process engine name: ").append(engineName);
      builder.append(" - resource: ").append(engineInfo.getResourceUrl());
      builder.append(" - status: ");

      if (engineInfo.getException() != null) {
        builder.append("Error while initializing engine. ");
        if (engineInfo.getException().indexOf("driver on UnpooledDataSource") != -1) {
          builder.append("Exception while initializing process engine! Database or database driver might not have been configured correctly.")
              .append("Please consult the user guide for supported database environments or build.properties. Stacktrace: ")
              .append(engineInfo.getException());
        } else {
          builder.append("Stacktrace: ").append(engineInfo.getException());
        }
      } else {
        // Process engine initialised without exception
        builder.append("Initialised");
      }
      builder.append("\n");
    }
    return builder.toString();
  }

  public String getProcessEngineName() {
    return processEngineName;
  }

  public void setProcessEngineName(String processEngineName) {
    this.processEngineName = processEngineName;
  }

  public File getFile() {
    return file;
  }

  public void setFile(File file) {
    this.file = file;
  }

  public List<FileSet> getFileSets() {
    return fileSets;
  }

  public void setFileSets(List<FileSet> fileSets) {
    this.fileSets = fileSets;
  }
}

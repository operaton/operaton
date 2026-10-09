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
package org.operaton.bpm.engine.impl.bpmn.behavior;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.NullMarked;
import org.operaton.bpm.engine.delegate.DelegateExecution;

import org.jspecify.annotations.Nullable;
import org.operaton.bpm.engine.delegate.Expression;
import org.operaton.bpm.engine.impl.ProcessEngineLogger;
import org.operaton.bpm.engine.impl.pvm.delegate.ActivityExecution;
import org.operaton.bpm.engine.impl.util.EnsureUtil;

import static java.nio.charset.StandardCharsets.UTF_8;

public @NullMarked class ShellActivityBehavior extends AbstractBpmnActivityBehavior {

  protected static final BpmnBehaviorLogger LOG = ProcessEngineLogger.BPMN_BEHAVIOR_LOGGER;

  protected @Nullable Expression command;
  protected @Nullable Expression wait;
  protected @Nullable Expression arg1;
  protected @Nullable Expression arg2;
  protected @Nullable Expression arg3;
  protected @Nullable Expression arg4;
  protected @Nullable Expression arg5;
  protected @Nullable Expression outputVariable;
  protected @Nullable Expression errorCodeVariable;
  protected @Nullable Expression redirectError;
  protected @Nullable Expression cleanEnv;
  protected @Nullable Expression directory;

  @Nullable String commandStr;
  @Nullable String arg1Str;
  @Nullable String arg2Str;
  @Nullable String arg3Str;
  @Nullable String arg4Str;
  @Nullable String arg5Str;
  @Nullable String waitStr;
  @Nullable String resultVariableStr;
  @Nullable String errorCodeVariableStr;
  @Nullable Boolean waitFlag;
  Boolean redirectErrorFlag = false;
  @Nullable Boolean cleanEnvBoolan;
  @Nullable String directoryStr;

  private void readFields(ActivityExecution execution) {
    commandStr = getStringFromField(command, execution);
    arg1Str = getStringFromField(arg1, execution);
    arg2Str = getStringFromField(arg2, execution);
    arg3Str = getStringFromField(arg3, execution);
    arg4Str = getStringFromField(arg4, execution);
    arg5Str = getStringFromField(arg5, execution);
    waitStr = getStringFromField(wait, execution);
    resultVariableStr = getStringFromField(outputVariable, execution);
    errorCodeVariableStr = getStringFromField(errorCodeVariable, execution);

    String redirectErrorStr = getStringFromField(redirectError, execution);
    String cleanEnvStr = getStringFromField(cleanEnv, execution);

    waitFlag = waitStr == null || "true".equals(waitStr);
    redirectErrorFlag = "true".equals(redirectErrorStr);
    cleanEnvBoolan = "true".equals(cleanEnvStr);
    directoryStr = getStringFromField(directory, execution);

  }

  @Override
  public void execute(ActivityExecution execution) {

    readFields(execution);

    List<String> argList = new ArrayList<>();

    EnsureUtil.ensureNotNull("Command is missing","command", commandStr);
    Objects.requireNonNull(commandStr);
    argList.add(commandStr);

    if (arg1Str != null) {
      argList.add(arg1Str);
    }
    if (arg2Str != null) {
      argList.add(arg2Str);
    }
    if (arg3Str != null) {
      argList.add(arg3Str);
    }
    if (arg4Str != null) {
      argList.add(arg4Str);
    }
    if (arg5Str != null) {
      argList.add(arg5Str);
    }

    ProcessBuilder processBuilder = new ProcessBuilder(argList);

    try {
      processBuilder.redirectErrorStream(redirectErrorFlag);
      if (Boolean.TRUE.equals(cleanEnvBoolan)) {
        Map<String, String> env = processBuilder.environment();
        env.clear();
      }
      if (directoryStr != null && !directoryStr.isEmpty()) {
        processBuilder.directory(new File(directoryStr));
      }

      Process process = processBuilder.start();

      if (Boolean.TRUE.equals(waitFlag)) {
        int errorCode = process.waitFor();

        if (resultVariableStr != null) {
          String result = convertStreamToStr(process.getInputStream());
          execution.setVariable(resultVariableStr, result);
        }

        if (errorCodeVariableStr != null) {
          execution.setVariable(errorCodeVariableStr, Integer.toString(errorCode));
        }

      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw LOG.shellExecutionException(e);
    } catch (Exception e) {
      throw LOG.shellExecutionException(e);
    }

    leave(execution);
  }

  public static String convertStreamToStr(@Nullable InputStream is) throws IOException {

    if (is != null) {
      Writer writer = new StringWriter();

      char[] buffer = new char[1024];
      try (is) {
        Reader reader = new BufferedReader(new InputStreamReader(is, UTF_8));
        int n;
        while ((n = reader.read(buffer)) != -1) {
          writer.write(buffer, 0, n);
        }
      }
      return writer.toString();
    } else {
      return "";
    }
  }

  protected @Nullable String getStringFromField(@Nullable Expression expression, DelegateExecution execution) {
    if (expression != null) {
      Object value = expression.getValue(execution);
      if (value != null) {
        return value.toString();
      }
    }
    return null;
  }

}

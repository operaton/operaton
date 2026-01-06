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
package org.operaton.bpm.client.variable.impl.value;

import java.io.InputStream;
import java.io.Serial;

import org.operaton.bpm.client.impl.EngineClient;
import org.operaton.bpm.client.impl.EngineClientException;
import org.operaton.bpm.client.impl.ExternalTaskClientLogger;
import org.operaton.bpm.client.variable.value.DeferredFileValue;
import org.operaton.bpm.engine.variable.impl.value.FileValueImpl;

import static org.operaton.bpm.engine.variable.type.ValueType.FILE;

/**
 * @author Tassilo Weidner
 */
public class DeferredFileValueImpl extends FileValueImpl implements DeferredFileValue {

  @Serial private static final long serialVersionUID = 1L;

  protected static final ExternalTaskClientLogger LOG = ExternalTaskClientLogger.CLIENT_LOGGER;

  protected boolean isLoaded;

  protected String variableName;
  protected String executionId;
  protected transient EngineClient engineClient;

  public DeferredFileValueImpl(String filename, EngineClient engineClient) {
    super(FILE, filename);
    this.engineClient = engineClient;
  }

  protected void load() {
    try {
      byte[] bytes = engineClient.getLocalBinaryVariable(variableName, executionId);
      setValue(bytes);

      this.isLoaded = true;

    } catch (EngineClientException e) {
      throw LOG.handledEngineClientException("loading deferred file", e);

    }
  }

  @Override
  public boolean isLoaded() {
    return isLoaded;
  }

  @Override
  public InputStream getValue() {
    if (!isLoaded()) {
      load();
    }

    return super.getValue();
  }

  public void setVariableName(String variableName) {
    this.variableName = variableName;
  }

  public void setExecutionId(String executionId) {
    this.executionId = executionId;
  }

  public String getExecutionId() {
    return executionId;
  }

  @Override
  public String toString() {
    return "DeferredFileValueImpl [mimeType=%s, filename=%s, type=%s, isTransient=%s, isLoaded=%s]"
        .formatted(mimeType, filename, type, isTransient, isLoaded);
  }

}

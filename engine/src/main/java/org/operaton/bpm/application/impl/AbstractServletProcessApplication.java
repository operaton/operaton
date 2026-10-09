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
package org.operaton.bpm.application.impl;

import java.util.HashMap;
import java.util.Map;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.operaton.bpm.application.AbstractProcessApplication;
import org.operaton.bpm.application.ProcessApplicationInfo;
import org.operaton.bpm.application.ProcessApplicationReference;

public @NullMarked class AbstractServletProcessApplication extends AbstractProcessApplication {

  protected @Nullable String servletContextName;
  protected @Nullable String servletContextPath;

  protected @Nullable ProcessApplicationReferenceImpl reference;

  protected @Nullable ClassLoader processApplicationClassloader;


  @Override
  protected @Nullable String autodetectProcessApplicationName() {
    String name = servletContextName != null && !servletContextName.isEmpty() ? servletContextName : servletContextPath;
    if(name != null && name.startsWith("/")) {
      name = name.substring(1);
    }
    return name;
  }

  @Override
  public ProcessApplicationReference getReference() {
     if(reference == null) {
       reference = new ProcessApplicationReferenceImpl(this);
     }
     return reference;
  }

  @Override
  public @Nullable ClassLoader getProcessApplicationClassloader() {
    return processApplicationClassloader;
  }

  @Override
  @SuppressWarnings("ConstantConditions")
  public Map<String, String> getProperties() {
    Map<String, String> properties = new HashMap<>();

    // set the servlet context path as property
    properties.put(ProcessApplicationInfo.PROP_SERVLET_CONTEXT_PATH, servletContextPath);

    return properties;
  }

}

/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.model.cmmn.util;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.operaton.bpm.model.cmmn.Cmmn;
import org.operaton.bpm.model.cmmn.CmmnModelInstance;
import org.operaton.bpm.model.xml.impl.util.IoUtil;

import java.io.InputStream;

/**
 * @author Daniel Meyer
 * @author Roman Smirnov
 *
 */
public class ParseCmmnModelRule implements BeforeEachCallback {

  protected CmmnModelInstance CmmnModelInstance;

  @Override
  public void beforeEach(ExtensionContext context){

    if(context.getTestMethod().map(method -> method.getAnnotation(CmmnModelResource.class)).isPresent()) {

      Class<?> testClass = context.getTestClass().orElseThrow();
      String methodName = context.getTestMethod().get().getName();

      String resourceFolderName = testClass.getName().replace(".", "/");
      String cmmnResourceName = resourceFolderName + "." + methodName + ".cmmn";

      InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream(cmmnResourceName);
      try {
        CmmnModelInstance = Cmmn.readModelFromStream(resourceAsStream);
      } finally {
        IoUtil.closeSilently(resourceAsStream);
      }
    }
  }

  public CmmnModelInstance getCmmnModel() {
    return CmmnModelInstance;
  }

}

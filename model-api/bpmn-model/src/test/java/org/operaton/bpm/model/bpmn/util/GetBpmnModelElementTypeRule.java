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
package org.operaton.bpm.model.bpmn.util;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.xml.Model;
import org.operaton.bpm.model.xml.ModelInstance;
import org.operaton.bpm.model.xml.instance.ModelElementInstance;
import org.operaton.bpm.model.xml.test.GetModelElementTypeRule;
import org.operaton.bpm.model.xml.type.ModelElementType;

/**
 * @author Sebastian Menski
 */
public class GetBpmnModelElementTypeRule implements GetModelElementTypeRule, BeforeAllCallback {

  private ModelInstance modelInstance;
  private Model model;
  private ModelElementType modelElementType;

  @Override
  public void beforeAll(ExtensionContext context) {
    String className = context.getTestClass().orElseThrow().getName();
    className =  className.replace("Test", "");
    Class<? extends ModelElementInstance> instanceClass = null;
    try {
      instanceClass = (Class<? extends ModelElementInstance>) Class.forName(className);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
    modelInstance = Bpmn.createEmptyModel();
    model = modelInstance.getModel();
    modelElementType = model.getType(instanceClass);
  }

  public ModelInstance getModelInstance() {
    return modelInstance;
  }

  public Model getModel() {
    return model;
  }

  public ModelElementType getModelElementType() {
    return modelElementType;
  }
}

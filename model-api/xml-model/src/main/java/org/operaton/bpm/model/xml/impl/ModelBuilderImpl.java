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
package org.operaton.bpm.model.xml.impl;

import org.operaton.bpm.model.xml.Model;
import org.operaton.bpm.model.xml.ModelBuilder;
import org.operaton.bpm.model.xml.impl.instance.ModelElementInstanceImpl;
import org.operaton.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.operaton.bpm.model.xml.impl.type.ModelElementTypeBuilderImpl;
import org.operaton.bpm.model.xml.instance.ModelElementInstance;
import org.operaton.bpm.model.xml.type.ModelElementType;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * This builder is used to define and create a new model.
 *
 * @author Daniel Meyer
 *
 */
public class ModelBuilderImpl extends ModelBuilder {

  private final List<ModelElementTypeBuilderImpl> typeBuilders = new ArrayList<>();
  private final ModelImpl model;

  public ModelBuilderImpl(String modelName) {
    model = new ModelImpl(modelName);
  }

  @Override
  public ModelBuilder alternativeNamespace(String alternativeNs, String actualNs) {
    model.declareAlternativeNamespace(alternativeNs, actualNs);
    return this;
  }

  @Override
  public ModelElementTypeBuilder defineType(Class<? extends ModelElementInstance> modelInstanceType, String typeName) {
    ModelElementTypeBuilderImpl typeBuilder = new ModelElementTypeBuilderImpl(modelInstanceType, typeName, model);
    typeBuilders.add(typeBuilder);
    return typeBuilder;
  }

  @Override
  public ModelElementType defineGenericType(String typeName, String typeNamespaceUri) {
    ModelElementTypeBuilder typeBuilder = defineType(ModelElementInstance.class, typeName)
      .namespaceUri(typeNamespaceUri)
      .instanceProvider(instanceContext -> new ModelElementInstanceImpl(instanceContext));

    return typeBuilder.build();
  }

  @Override
  public Model build() {
    for (ModelElementTypeBuilderImpl typeBuilder : typeBuilders) {
      typeBuilder.buildTypeHierarchy(model);
    }
    for (ModelElementTypeBuilderImpl typeBuilder : typeBuilders) {
      typeBuilder.performModelBuild(model);
    }
    return model;
  }

}

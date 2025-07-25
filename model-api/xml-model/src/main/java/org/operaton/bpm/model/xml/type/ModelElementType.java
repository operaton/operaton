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
package org.operaton.bpm.model.xml.type;

import org.operaton.bpm.model.xml.Model;
import org.operaton.bpm.model.xml.ModelInstance;
import org.operaton.bpm.model.xml.instance.ModelElementInstance;
import org.operaton.bpm.model.xml.type.attribute.Attribute;

import java.util.Collection;
import java.util.List;

/**
 * @author Daniel Meyer
 *
 */
public interface ModelElementType {

  String getTypeName();

  String getTypeNamespace();

  Class<? extends ModelElementInstance> getInstanceType();

  List<Attribute<?>> getAttributes();

  ModelElementInstance newInstance(ModelInstance modelInstance);

  ModelElementType getBaseType();

  boolean isAbstract();

  Collection<ModelElementType> getExtendingTypes();

  Collection<ModelElementType> getAllExtendingTypes();

  Attribute<?> getAttribute(String attributeName);

  Model getModel();

  Collection<ModelElementInstance> getInstances(ModelInstance modelInstanceImpl);

  List<ModelElementType> getChildElementTypes();

  List<ModelElementType> getAllChildElementTypes();

}

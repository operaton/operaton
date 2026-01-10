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
package org.operaton.bpm.model.xml.impl.type;

import java.util.*;

import org.operaton.bpm.model.xml.Model;
import org.operaton.bpm.model.xml.ModelException;
import org.operaton.bpm.model.xml.ModelInstance;
import org.operaton.bpm.model.xml.impl.ModelImpl;
import org.operaton.bpm.model.xml.impl.ModelInstanceImpl;
import org.operaton.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.operaton.bpm.model.xml.impl.util.ModelTypeException;
import org.operaton.bpm.model.xml.impl.util.ModelUtil;
import org.operaton.bpm.model.xml.instance.DomDocument;
import org.operaton.bpm.model.xml.instance.DomElement;
import org.operaton.bpm.model.xml.instance.ModelElementInstance;
import org.operaton.bpm.model.xml.type.ModelElementType;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;
import org.operaton.bpm.model.xml.type.attribute.Attribute;
import org.operaton.bpm.model.xml.type.child.ChildElementCollection;

/**
 * @author Daniel Meyer
 *
 */
public class ModelElementTypeImpl implements ModelElementType {

  private final ModelImpl model;

  private final String typeName;

  private final Class<? extends ModelElementInstance> instanceType;

  private String typeNamespace;

  private ModelElementTypeImpl baseType;

  private final List<ModelElementType> extendingTypes = new ArrayList<>();

  private final List<Attribute<?>> attributes = new ArrayList<>();

  private final List<ModelElementType> childElementTypes = new ArrayList<>();

  private final List<ChildElementCollection<?>> childElementCollections = new ArrayList<>();

  private ModelTypeInstanceProvider<?> instanceProvider;

  private boolean isAbstract;

  public ModelElementTypeImpl(ModelImpl model, String name, Class<? extends ModelElementInstance> instanceType) {
    this.model = model;
    this.typeName = name;
    this.instanceType = instanceType;
  }

  @Override
  public ModelElementInstance newInstance(ModelInstance modelInstance) {
    ModelInstanceImpl modelInstanceImpl = (ModelInstanceImpl) modelInstance;
    DomDocument document = modelInstanceImpl.getDocument();
    DomElement domElement = document.createElement(typeNamespace, typeName);
    return newInstance(modelInstanceImpl, domElement);
  }

  public ModelElementInstance newInstance(ModelInstanceImpl modelInstance, DomElement domElement) {
    ModelTypeInstanceContext modelTypeInstanceContext = new ModelTypeInstanceContext(domElement, modelInstance, this);
    return createModelElementInstance(modelTypeInstanceContext);
  }

  public void registerAttribute(Attribute<?> attribute) {
    if (!attributes.contains(attribute)) {
      attributes.add(attribute);
    }
  }

  public void registerChildElementType(ModelElementType childElementType) {
    if (!childElementTypes.contains(childElementType)) {
      childElementTypes.add(childElementType);
    }
  }

  public void registerChildElementCollection(ChildElementCollection<?> childElementCollection) {
    if (!childElementCollections.contains(childElementCollection)) {
      childElementCollections.add(childElementCollection);
    }
  }

  public void registerExtendingType(ModelElementType modelType) {
    if (!extendingTypes.contains(modelType)) {
      extendingTypes.add(modelType);
    }
  }

  protected ModelElementInstance createModelElementInstance(ModelTypeInstanceContext instanceContext) {
    if (isAbstract) {
      throw new ModelTypeException("Model element type %s is abstract and no instances can be created.".formatted(getTypeName()));
    }
    else {
      return instanceProvider.newInstance(instanceContext);
    }
  }

  @Override
  public final List<Attribute<?>> getAttributes() {
    return attributes;
  }

  @Override
  public String getTypeName() {
    return typeName;
  }

  @Override
  public Class<? extends ModelElementInstance> getInstanceType() {
    return instanceType;
  }

  public void setTypeNamespace(String typeNamespace) {
    this.typeNamespace = typeNamespace;
  }

  @Override
  public String getTypeNamespace() {
    return typeNamespace;
  }

  public void setBaseType(ModelElementTypeImpl baseType) {
    if (this.baseType == null) {
      this.baseType = baseType;
    }
    else if (!this.baseType.equals(baseType)) {
      throw new ModelException("Type can not have multiple base types. %s already extends type %s and can not also extend type %s".formatted(
          this.getClass(), this.baseType.getClass(), baseType.getClass()));
    }
  }

  public void setInstanceProvider(ModelTypeInstanceProvider<?> instanceProvider) {
    this.instanceProvider = instanceProvider;
  }

  @Override
  public boolean isAbstract() {
    return isAbstract;
  }

  public void setAbstract(boolean isAbstract) {
    this.isAbstract = isAbstract;
  }

  @Override
  public Collection<ModelElementType> getExtendingTypes() {
    return Collections.unmodifiableCollection(extendingTypes);
  }

  @Override
  public Collection<ModelElementType> getAllExtendingTypes() {
    HashSet<ModelElementType> result = new HashSet<>();
    result.add(this);
    resolveExtendingTypes(result);
    return result;
  }

  /**
   * Resolve all types recursively which are extending this type
   *
   * @param allExtendingTypes set of calculated extending types
   */
  public void resolveExtendingTypes(Set<ModelElementType> allExtendingTypes) {
    for(ModelElementType modelElementType : extendingTypes) {
      ModelElementTypeImpl modelElementTypeImpl = (ModelElementTypeImpl) modelElementType;
      if (!allExtendingTypes.contains(modelElementTypeImpl)) {
        allExtendingTypes.add(modelElementType);
        modelElementTypeImpl.resolveExtendingTypes(allExtendingTypes);
      }
    }
  }

  /**
   * Resolve all types which are base types of this type
   *
   * @param baseTypes list of calculated base types
   */
  public void resolveBaseTypes(List<ModelElementType> baseTypes) {
    if (baseType != null) {
      baseTypes.add(baseType);
      baseType.resolveBaseTypes(baseTypes);
    }
  }


  @Override
  public ModelElementType getBaseType() {
    return baseType;
  }

  @Override
  public Model getModel() {
    return model;
  }

  @Override
  public List<ModelElementType> getChildElementTypes() {
    return childElementTypes;
  }

  @Override
  public List<ModelElementType> getAllChildElementTypes() {
    List<ModelElementType> allChildElementTypes = new ArrayList<>();
    if (baseType != null) {
      allChildElementTypes.addAll(baseType.getAllChildElementTypes());
    }
    allChildElementTypes.addAll(childElementTypes);
    return allChildElementTypes;
  }

  public List<ChildElementCollection<?>> getChildElementCollections() {
    return childElementCollections;
  }

  public List<ChildElementCollection<?>> getAllChildElementCollections() {
    List<ChildElementCollection<?>> allChildElementCollections = new ArrayList<>();
    if (baseType != null) {
      allChildElementCollections.addAll(baseType.getAllChildElementCollections());
    }
    allChildElementCollections.addAll(childElementCollections);
    return allChildElementCollections;
  }

  @Override
  public Collection<ModelElementInstance> getInstances(ModelInstance modelInstance) {
    ModelInstanceImpl modelInstanceImpl = (ModelInstanceImpl) modelInstance;
    DomDocument document = modelInstanceImpl.getDocument();

    List<DomElement> elements = getElementsByNameNs(document, typeNamespace);

    List<ModelElementInstance> resultList = new ArrayList<>();
    for (DomElement element : elements) {
      resultList.add(ModelUtil.getModelElement(element, modelInstanceImpl, this));
    }
    return resultList;
  }

  protected List<DomElement> getElementsByNameNs(DomDocument document, String namespaceURI) {
    List<DomElement> elements = document.getElementsByNameNs(namespaceURI, typeName);

    if (elements.isEmpty()) {
      Set<String> alternativeNamespaces = getModel().getAlternativeNamespaces(namespaceURI);

      if (alternativeNamespaces != null)
      {
        Iterator<String> namespaceIt = alternativeNamespaces.iterator();
        while (elements.isEmpty() && namespaceIt.hasNext()) {
          elements = getElementsByNameNs(document, namespaceIt.next());
        }
      }
    }

    return elements;
  }

  /**
   * Test if a element type is a base type of this type. So this type extends the given element type.
   *
   * @param elementType the element type to test
   * @return true if {@code childElementTypeClass} is a base type of this type, else otherwise
   */
  public boolean isBaseTypeOf(ModelElementType elementType) {
    if (this.equals(elementType)) {
      return true;
    }
    else {
      Collection<ModelElementType> baseTypes = ModelUtil.calculateAllBaseTypes(elementType);
      return baseTypes.contains(this);
    }
  }

  /**
   * Returns a list of all attributes, including the attributes of all base types.
   *
   * @return the list of all attributes
   */
  public Collection<Attribute<?>> getAllAttributes() {
    List<Attribute<?>> allAttributes = new ArrayList<>();
    allAttributes.addAll(getAttributes());
    Collection<ModelElementType> baseTypes = ModelUtil.calculateAllBaseTypes(this);
    for (ModelElementType type : baseTypes) {
      allAttributes.addAll(type.getAttributes());
    }
    return allAttributes;
  }

  /**
   * Return the attribute for the attribute name
   *
   * @param attributeName the name of the attribute
   * @return the attribute or null if it not exists
   */
  @Override
  public Attribute<?> getAttribute(String attributeName) {
    for (Attribute<?> attribute : getAllAttributes()) {
      if (attribute.getAttributeName().equals(attributeName)) {
        return attribute;
      }
    }
    return null;
  }

  public ChildElementCollection<?> getChildElementCollection(ModelElementType childElementType) {
    for (ChildElementCollection<?> childElementCollection : getChildElementCollections()) {
      if (childElementType.equals(childElementCollection.getChildElementType(model))) {
        return childElementCollection;
      }
    }
    return null;
  }

  @Override
  public int hashCode() {
    int prime = 31;
    int result = 1;
    result = prime * result + (model == null ? 0 : model.hashCode());
    result = prime * result + (typeName == null ? 0 : typeName.hashCode());
    return prime * result + (typeNamespace == null ? 0 : typeNamespace.hashCode());
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    ModelElementTypeImpl other = (ModelElementTypeImpl) obj;
    if (model == null) {
      if (other.model != null) {
        return false;
      }
    } else if (!model.equals(other.model)) {
      return false;
    }
    if (typeName == null) {
      if (other.typeName != null) {
        return false;
      }
    } else if (!typeName.equals(other.typeName)) {
      return false;
    }
    if (typeNamespace == null) {
      if (other.typeNamespace != null) {
        return false;
      }
    } else if (!typeNamespace.equals(other.typeNamespace)) {
      return false;
    }
    return true;
  }

}

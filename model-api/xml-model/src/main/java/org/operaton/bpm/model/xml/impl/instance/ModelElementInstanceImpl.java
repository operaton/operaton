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
package org.operaton.bpm.model.xml.impl.instance;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.operaton.bpm.model.xml.Model;
import org.operaton.bpm.model.xml.ModelBuilder;
import org.operaton.bpm.model.xml.ModelException;
import org.operaton.bpm.model.xml.impl.ModelInstanceImpl;
import org.operaton.bpm.model.xml.impl.type.ModelElementTypeImpl;
import org.operaton.bpm.model.xml.impl.type.attribute.AttributeImpl;
import org.operaton.bpm.model.xml.impl.type.reference.ReferenceImpl;
import org.operaton.bpm.model.xml.impl.util.ModelUtil;
import org.operaton.bpm.model.xml.instance.DomElement;
import org.operaton.bpm.model.xml.instance.ModelElementInstance;
import org.operaton.bpm.model.xml.type.ModelElementType;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder;
import org.operaton.bpm.model.xml.type.attribute.Attribute;
import org.operaton.bpm.model.xml.type.reference.Reference;

/**
 * Base class for implementing Model Elements.
 *
 * @author Daniel Meyer
 *
 */
public class ModelElementInstanceImpl implements ModelElementInstance {

  /** the containing model instance */
  protected final ModelInstanceImpl modelInstance;
  /** the wrapped DOM {@link DomElement} */
  private final DomElement domElement;
  /** the implementing model element type */
  private final ModelElementTypeImpl elementType;

  public static void registerType(ModelBuilder modelBuilder) {
    ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(ModelElementInstance.class, "")
      .abstractType();

    typeBuilder.build();
  }

  public ModelElementInstanceImpl(ModelTypeInstanceContext instanceContext) {
    this.domElement = instanceContext.domElement();
    this.modelInstance = instanceContext.model();
    this.elementType = instanceContext.modelType();
  }

  @Override
  public DomElement getDomElement() {
    return domElement;
  }

  @Override
  public ModelInstanceImpl getModelInstance() {
    return modelInstance;
  }

  @Override
  public ModelElementInstance getParentElement() {
    DomElement parentElement = domElement.getParentElement();
    if (parentElement != null) {
      return ModelUtil.getModelElement(parentElement, modelInstance);
    }
    else {
      return null;
    }
  }

  @Override
  public ModelElementType getElementType() {
    return elementType;
  }

  @Override
  public String getAttributeValue(String attributeName) {
    return domElement.getAttribute(attributeName);
  }

  @Override
  public String getAttributeValueNs(String namespaceUri, String attributeName) {
    return domElement.getAttribute(namespaceUri, attributeName);
  }

  @Override
  public void setAttributeValue(String attributeName, String xmlValue) {
    setAttributeValue(attributeName, xmlValue, false, true);
  }

  @Override
  public void setAttributeValue(String attributeName, String xmlValue, boolean isIdAttribute) {
    setAttributeValue(attributeName, xmlValue, isIdAttribute, true);
  }

  @Override
  public void setAttributeValue(String attributeName, String xmlValue,
                                boolean isIdAttribute, boolean withReferenceUpdate) {
    String oldValue = getAttributeValue(attributeName);
    if (isIdAttribute) {
      domElement.setIdAttribute(attributeName, xmlValue);
    }
    else {
      domElement.setAttribute(attributeName, xmlValue);
    }
    Attribute<?> attribute = elementType.getAttribute(attributeName);
    if (attribute != null && withReferenceUpdate) {
      ((AttributeImpl<?>) attribute).updateIncomingReferences(this, xmlValue, oldValue);
    }
  }

  @Override
  public void setAttributeValueNs(String namespaceUri, String attributeName, String xmlValue) {
    setAttributeValueNs(namespaceUri, attributeName, xmlValue, false, true);
  }

  @Override
  public void setAttributeValueNs(String namespaceUri, String attributeName, String xmlValue, boolean isIdAttribute) {
    setAttributeValueNs(namespaceUri, attributeName, xmlValue, isIdAttribute, true);
  }

  @Override
  public void setAttributeValueNs(String namespaceUri, String attributeName, String xmlValue,
                                  boolean isIdAttribute, boolean withReferenceUpdate) {
    String namespaceForSetting = determineNamespace(namespaceUri, attributeName);
    String oldValue = getAttributeValueNs(namespaceForSetting, attributeName);
    if (isIdAttribute) {
      domElement.setIdAttribute(namespaceForSetting, attributeName, xmlValue);
    }
    else {
      domElement.setAttribute(namespaceForSetting, attributeName, xmlValue);
    }
    Attribute<?> attribute = elementType.getAttribute(attributeName);
    if (attribute != null && withReferenceUpdate) {
      ((AttributeImpl<?>) attribute).updateIncomingReferences(this, xmlValue, oldValue);
    }
  }

  private String determineNamespace(String intendedNamespace, String attributeName) {
    boolean isSetInIntendedNamespace = getAttributeValueNs(intendedNamespace, attributeName) != null;

    if (isSetInIntendedNamespace) {
      return intendedNamespace;
    }
    else {
      Set<String> alternativeNamespaces = modelInstance.getModel().getAlternativeNamespaces(intendedNamespace);

      if (alternativeNamespaces != null)
      {
        for (String alternativeNamespace : alternativeNamespaces) {
          if (getAttributeValueNs(alternativeNamespace, attributeName) != null) {
            return alternativeNamespace;
          }
        }
      }

      // default to intended namespace
      return intendedNamespace;
    }
  }

  @Override
  public void removeAttribute(String attributeName) {
    Attribute<?> attribute = elementType.getAttribute(attributeName);
    if (attribute != null) {
      Object identifier = attribute.getValue(this);
      if (identifier != null) {
        ((AttributeImpl<?>) attribute).unlinkReference(this, identifier);
      }
    }
    domElement.removeAttribute(attributeName);
  }

  @Override
  public void removeAttributeNs(String namespaceUri, String attributeName) {
    Attribute<?> attribute = elementType.getAttribute(attributeName);
    if (attribute != null) {
      Object identifier = attribute.getValue(this);
      if (identifier != null) {
        ((AttributeImpl<?>) attribute).unlinkReference(this, identifier);
      }
    }
    domElement.removeAttribute(namespaceUri, attributeName);
  }

  @Override
  public String getTextContent() {
    return getRawTextContent().trim();
  }

  @Override
  public void setTextContent(String textContent) {
    domElement.setTextContent(textContent);
  }

  @Override
  public String getRawTextContent() {
    return domElement.getTextContent();
  }

  @Override
  public ModelElementInstance getUniqueChildElementByNameNs(String namespaceUri, String elementName) {
    Model model = modelInstance.getModel();
    List<DomElement> childElements = domElement.getChildElementsByNameNs(asSet(namespaceUri, model.getAlternativeNamespaces(namespaceUri)), elementName);
    if(!childElements.isEmpty()) {
      return ModelUtil.getModelElement(childElements.get(0), modelInstance);
    } else {
      return null;
    }
  }


  @Override
  public ModelElementInstance getUniqueChildElementByType(Class<? extends ModelElementInstance> elementType) {
    List<DomElement> childElements = domElement.getChildElementsByType(modelInstance, elementType);

    if(!childElements.isEmpty()) {
      return ModelUtil.getModelElement(childElements.get(0), modelInstance);
    }
    else {
      return null;
    }
  }

  @Override
  public void setUniqueChildElementByNameNs(ModelElementInstance newChild) {
    ModelUtil.ensureInstanceOf(newChild, ModelElementInstanceImpl.class);
    ModelElementInstanceImpl newChildElement = (ModelElementInstanceImpl) newChild;

    DomElement childElement = newChildElement.getDomElement();
    ModelElementInstance existingChild = getUniqueChildElementByNameNs(childElement.getNamespaceURI(), childElement.getLocalName());
    if(existingChild == null) {
      addChildElement(newChild);
    } else {
      replaceChildElement(existingChild, newChildElement);
    }
  }

  @Override
  public void replaceChildElement(ModelElementInstance existingChild, ModelElementInstance newChild) {
    DomElement existingChildDomElement = existingChild.getDomElement();
    DomElement newChildDomElement = newChild.getDomElement();

    // unlink (remove all references) of child elements
    ((ModelElementInstanceImpl) existingChild).unlinkAllChildReferences();

    // update incoming references from old to new child element
    updateIncomingReferences(existingChild, newChild);

    // replace the existing child with the new child in the DOM
    domElement.replaceChild(newChildDomElement, existingChildDomElement);

    // execute after replacement updates
    newChild.updateAfterReplacement();
  }

  @SuppressWarnings("unchecked")
  private void updateIncomingReferences(ModelElementInstance oldInstance, ModelElementInstance newInstance) {
    String oldId = oldInstance.getAttributeValue("id");
    String newId = newInstance.getAttributeValue("id");

    if (oldId == null || newId == null) {
      return;
    }

    Collection<Attribute<?>> attributes = ((ModelElementTypeImpl) oldInstance.getElementType()).getAllAttributes();
    for (Attribute<?> attribute : attributes) {
      if (attribute.isIdAttribute()) {
        for (Reference<?> incomingReference : attribute.getIncomingReferences()) {
          ((ReferenceImpl<ModelElementInstance>) incomingReference).referencedElementUpdated(newInstance, oldId, newId);
        }
      }
    }

  }

  @Override
  public void replaceWithElement(ModelElementInstance newElement) {
    ModelElementInstanceImpl parentElement = (ModelElementInstanceImpl) getParentElement();
    if (parentElement != null) {
      parentElement.replaceChildElement(this, newElement);
    }
    else {
      throw new ModelException("Unable to remove replace without parent");
    }
  }

  @Override
  public void addChildElement(ModelElementInstance newChild) {
    ModelUtil.ensureInstanceOf(newChild, ModelElementInstanceImpl.class);
    ModelElementInstance elementToInsertAfter = findElementToInsertAfter(newChild);
    insertElementAfter(newChild, elementToInsertAfter);
  }

  @Override
  public boolean removeChildElement(ModelElementInstance child) {
    ModelElementInstanceImpl childImpl = (ModelElementInstanceImpl) child;
    childImpl.unlinkAllReferences();
    childImpl.unlinkAllChildReferences();
    return domElement.removeChild(child.getDomElement());
  }

  @Override
  public Collection<ModelElementInstance> getChildElementsByType(ModelElementType childElementType) {
    List<ModelElementInstance> instances = new ArrayList<>();
    for (ModelElementType extendingType : childElementType.getExtendingTypes()) {
      instances.addAll(getChildElementsByType(extendingType));
    }
    Model model = modelInstance.getModel();
    Set<String> alternativeNamespaces = model.getAlternativeNamespaces(childElementType.getTypeNamespace());
    List<DomElement> elements = domElement.getChildElementsByNameNs(asSet(childElementType.getTypeNamespace(), alternativeNamespaces), childElementType.getTypeName());
    instances.addAll(ModelUtil.getModelElementCollection(elements, modelInstance));
    return instances;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T extends ModelElementInstance> Collection<T> getChildElementsByType(Class<T> childElementClass) {
    return (Collection<T>) getChildElementsByType(getModelInstance().getModel().getType(childElementClass));
  }

  /**
   * Returns the element after which the new element should be inserted in the DOM document.
   *
   * @param elementToInsert  the new element to insert
   * @return the element to insert after or null
   */
  private ModelElementInstance findElementToInsertAfter(ModelElementInstance elementToInsert) {
    List<ModelElementType> childElementTypes = elementType.getAllChildElementTypes();
    List<DomElement> childDomElements = domElement.getChildElements();
    Collection<ModelElementInstance> childElements = ModelUtil.getModelElementCollection(childDomElements, modelInstance);

    ModelElementInstance insertAfterElement = null;
    int newElementTypeIndex = ModelUtil.getIndexOfElementType(elementToInsert, childElementTypes);
    for (ModelElementInstance childElement : childElements) {
      int childElementTypeIndex = ModelUtil.getIndexOfElementType(childElement, childElementTypes);
      if (newElementTypeIndex >= childElementTypeIndex) {
        insertAfterElement = childElement;
      }
      else {
        break;
      }
    }
    return insertAfterElement;
  }

  @Override
  public void insertElementAfter(ModelElementInstance elementToInsert, ModelElementInstance insertAfterElement) {
    if (insertAfterElement == null || insertAfterElement.getDomElement() == null) {
      domElement.insertChildElementAfter(elementToInsert.getDomElement(), null);
    }
    else {
      domElement.insertChildElementAfter(elementToInsert.getDomElement(), insertAfterElement.getDomElement());
    }
  }

  @Override
  public void updateAfterReplacement() {
    // do nothing
  }

  /**
   * Removes all reference to this.
   */
  private void unlinkAllReferences() {
    Collection<Attribute<?>> attributes = elementType.getAllAttributes();
    for (Attribute<?> attribute : attributes) {
      Object identifier = attribute.getValue(this);
      if (identifier != null) {
        ((AttributeImpl<?>) attribute).unlinkReference(this, identifier);
      }
    }
  }

  /**
   * Removes every reference to children of this.
   */
  private void unlinkAllChildReferences() {
    List<ModelElementType> childElementTypes = elementType.getAllChildElementTypes();
    for (ModelElementType type : childElementTypes) {
      Collection<ModelElementInstance> childElementsForType = getChildElementsByType(type);
      for (ModelElementInstance childElement : childElementsForType) {
        ((ModelElementInstanceImpl) childElement).unlinkAllReferences();
      }
    }
  }

  protected <T> Set<T> asSet(T element, Set<T> elements){
    Set<T> result = new HashSet<>();
    result.add(element);

    if (elements != null) {
      result.addAll(elements);
    }

    return result;
  }

  @Override
  public int hashCode() {
    return domElement.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if(obj == null) {
      return false;
    } else if(obj == this) {
      return true;
    } else if(!(obj instanceof ModelElementInstanceImpl other)) {
      return false;
    } else {
      return other.domElement.equals(domElement);
    }
  }

}

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
import java.util.List;
import java.util.Set;

import org.w3c.dom.*;

import org.operaton.bpm.model.xml.ModelException;
import org.operaton.bpm.model.xml.impl.ModelInstanceImpl;
import org.operaton.bpm.model.xml.impl.util.DomUtil;
import org.operaton.bpm.model.xml.impl.util.XmlQName;
import org.operaton.bpm.model.xml.instance.DomDocument;
import org.operaton.bpm.model.xml.instance.DomElement;
import org.operaton.bpm.model.xml.instance.ModelElementInstance;

import static javax.xml.XMLConstants.XMLNS_ATTRIBUTE;
import static javax.xml.XMLConstants.XMLNS_ATTRIBUTE_NS_URI;

/**
 * @author Sebastian Menski
 */
public class DomElementImpl implements DomElement {

  private static final String MODEL_ELEMENT_KEY = "operaton.modelElementRef";

  private final Element element;
  private final Document document;

  public DomElementImpl(Element element) {
    this.element = element;
    this.document = element.getOwnerDocument();
  }

  protected Element getElement() {
    return element;
  }

  @Override
  public String getNamespaceURI() {
    synchronized(document) {
      return element.getNamespaceURI();
    }
  }

  @Override
  public String getLocalName() {
    synchronized(document) {
      return element.getLocalName();
    }
  }

  @Override
  public String getPrefix() {
    synchronized(document) {
      return element.getPrefix();
    }
  }

  @Override
  public DomDocument getDocument() {
    synchronized(document) {
      Document ownerDocument = element.getOwnerDocument();
      if (ownerDocument != null) {
        return new DomDocumentImpl(ownerDocument);
      }
      else {
        return null;
      }
    }
  }

  @Override
  public DomElement getRootElement() {
    synchronized(document) {
      DomDocument doc = getDocument();
      if (doc != null) {
        return doc.getRootElement();
      }
      else {
        return null;
      }
    }
  }

  @Override
  public DomElement getParentElement() {
    synchronized(document) {
      Node parentNode = element.getParentNode();
      if (parentNode instanceof Element elem) {
        return new DomElementImpl(elem);
      }
      else {
        return null;
      }
    }
  }

  @Override
  public List<DomElement> getChildElements() {
    synchronized(document) {
      NodeList childNodes = element.getChildNodes();
      return DomUtil.filterNodeListForElements(childNodes);
    }
  }

  @Override
  public List<DomElement> getChildElementsByNameNs(String namespaceUri, String elementName) {
    synchronized(document) {
      NodeList childNodes = element.getChildNodes();
      return DomUtil.filterNodeListByName(childNodes, namespaceUri, elementName);
    }
  }

  @Override
  public List<DomElement> getChildElementsByNameNs(Set<String> namespaceUris, String elementName) {
    List<DomElement> result = new ArrayList<>();
    for (String namespace : namespaceUris) {
      if (namespace != null) {
        result.addAll(getChildElementsByNameNs(namespace, elementName));
      }
    }
    return result;
  }

  @Override
  public List<DomElement> getChildElementsByType(ModelInstanceImpl modelInstance, Class<? extends ModelElementInstance> elementType) {
    synchronized(document) {
      NodeList childNodes = element.getChildNodes();
      return DomUtil.filterNodeListByType(childNodes, modelInstance, elementType);
    }
  }

  @Override
  public void replaceChild(DomElement newChildDomElement, DomElement existingChildDomElement) {
    synchronized(document) {
      Element newElement = ((DomElementImpl) newChildDomElement).getElement();
      Element existingElement = ((DomElementImpl) existingChildDomElement).getElement();
      try {
        element.replaceChild(newElement, existingElement);
      }
      catch (DOMException e) {
        throw new ModelException("Unable to replace child <%s> of element <%s> with element <%s>".formatted(existingElement, element, newElement), e);
      }
    }
  }

  @Override
  public boolean removeChild(DomElement childDomElement) {
    synchronized(document) {
      Element childElement = ((DomElementImpl) childDomElement).getElement();
      try {
        element.removeChild(childElement);
        return true;
      }
      catch (DOMException e) {
        return false;
      }
    }
  }

  @Override
  public void appendChild(DomElement childDomElement) {
    synchronized(document) {
      Element childElement = ((DomElementImpl) childDomElement).getElement();
      element.appendChild(childElement);
    }
  }

  @Override
  public void insertChildElementAfter(DomElement elementToInsert, DomElement insertAfter) {
    synchronized(document) {
      Element newElement = ((DomElementImpl) elementToInsert).getElement();
      // find node to insert before
      Node insertBeforeNode;
      if (insertAfter == null) {
        insertBeforeNode = element.getFirstChild();
      }
      else {
        insertBeforeNode = ((DomElementImpl) insertAfter).getElement().getNextSibling();
      }

      // insert before node or append if no node was found
      if (insertBeforeNode != null) {
        element.insertBefore(newElement, insertBeforeNode);
      }
      else {
        element.appendChild(newElement);
      }
    }
  }

  @Override
  public boolean hasAttribute(String localName) {
    return hasAttribute(null, localName);
  }

  @Override
  public boolean hasAttribute(String namespaceUri, String localName) {
    synchronized(document) {
      return element.hasAttributeNS(namespaceUri, localName);
    }
  }

  @Override
  public String getAttribute(String attributeName) {
    return getAttribute(null, attributeName);
  }


  @Override
  public String getAttribute(String namespaceUri, String localName) {
    synchronized(document) {
      XmlQName xmlQName = new XmlQName(this, namespaceUri, localName);
      String value;
      if (xmlQName.hasLocalNamespace()) {
        value = element.getAttributeNS(null, xmlQName.getLocalName());
      }
      else {
        value = element.getAttributeNS(xmlQName.getNamespaceUri(), xmlQName.getLocalName());
      }
      if (value.isEmpty()) {
        return null;
      }
      else {
        return value;
      }
    }
  }

  @Override
  public void setAttribute(String localName, String value) {
    setAttribute(null, localName, value);
  }

  @Override
  public void setAttribute(String namespaceUri, String localName, String value) {
    setAttribute(namespaceUri, localName, value, false);
  }

  private void setAttribute(String namespaceUri, String localName, String value, boolean isIdAttribute) {
    synchronized(document) {
      XmlQName xmlQName = new XmlQName(this, namespaceUri, localName);
      if (xmlQName.hasLocalNamespace()) {
        element.setAttributeNS(null, xmlQName.getLocalName(), value);
        if (isIdAttribute) {
          element.setIdAttributeNS(null, xmlQName.getLocalName(), true);
        }
      }
      else {
        element.setAttributeNS(xmlQName.getNamespaceUri(), xmlQName.getPrefixedName(), value);
        if (isIdAttribute) {
          element.setIdAttributeNS(xmlQName.getNamespaceUri(), xmlQName.getLocalName(), true);
        }
      }
    }
  }

  @Override
  public void setIdAttribute(String localName, String value) {
    setIdAttribute(getNamespaceURI(), localName, value);
  }

  @Override
  public void setIdAttribute(String namespaceUri, String localName, String value) {
    setAttribute(namespaceUri, localName, value, true);
  }

  @Override
  public void removeAttribute(String localName) {
    removeAttribute(getNamespaceURI(), localName);
  }

  @Override
  public void removeAttribute(String namespaceUri, String localName) {
    synchronized(document) {
      XmlQName xmlQName = new XmlQName(this, namespaceUri, localName);
      if (xmlQName.hasLocalNamespace()) {
        element.removeAttributeNS(null, xmlQName.getLocalName());
      }
      else {
        element.removeAttributeNS(xmlQName.getNamespaceUri(), xmlQName.getLocalName());
      }
    }
  }

  @Override
  public String getTextContent() {
    synchronized(document) {
      return element.getTextContent();
    }
  }

  @Override
  public void setTextContent(String textContent) {
    synchronized(document) {
      element.setTextContent(textContent);
    }
  }

  @Override
  public void addCDataSection(String data) {
    synchronized (document) {
      CDATASection cdataSection = document.createCDATASection(data);
      element.appendChild(cdataSection);
    }
  }

  @Override
  public ModelElementInstance getModelElementInstance() {
    synchronized(document) {
      return (ModelElementInstance) element.getUserData(MODEL_ELEMENT_KEY);
    }
  }

  @Override
  public void setModelElementInstance(ModelElementInstance modelElementInstance) {
    synchronized(document) {
      element.setUserData(MODEL_ELEMENT_KEY, modelElementInstance, null);
    }
  }

  @Override
  public String registerNamespace(String namespaceUri) {
    synchronized(document) {
      String lookupPrefix = lookupPrefix(namespaceUri);
      if (lookupPrefix == null) {
        // check if a prefix is known
        String prefix = XmlQName.KNOWN_PREFIXES.get(namespaceUri);
        // check if prefix is not already used
        if (prefix != null && getRootElement() != null &&
          getRootElement().hasAttribute(XMLNS_ATTRIBUTE_NS_URI, prefix)) {
          prefix = null;
        }
        if (prefix == null) {
          // generate prefix
          prefix = ((DomDocumentImpl) getDocument()).getUnusedGenericNsPrefix();
        }
        registerNamespace(prefix, namespaceUri);
        return prefix;
      }
      else {
        return lookupPrefix;
      }
    }
  }

  @Override
  public void registerNamespace(String prefix, String namespaceUri) {
    synchronized(document) {
      element.setAttributeNS(XMLNS_ATTRIBUTE_NS_URI, XMLNS_ATTRIBUTE + ":" + prefix, namespaceUri);
    }
  }

  @Override
  public String lookupPrefix(String namespaceUri) {
    synchronized(document) {
      return element.lookupPrefix(namespaceUri);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    DomElementImpl that = (DomElementImpl) o;
    return element.equals(that.element);
  }

  @Override
  public int hashCode() {
    return element.hashCode();
  }

}

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
package org.operaton.bpm.model.xml.impl.util;

import java.util.Map;

import org.operaton.bpm.model.xml.instance.DomDocument;
import org.operaton.bpm.model.xml.instance.DomElement;

import static javax.xml.XMLConstants.XMLNS_ATTRIBUTE_NS_URI;

/**
 * @author Sebastian Menski
 */
public class XmlQName {

  public static final Map<String, String> KNOWN_PREFIXES = Map.of(
    "http://www.operaton.com/fox", "fox",
    "http://activiti.org/bpmn", "operaton",
    "http://operaton.org/schema/1.0/bpmn", "operaton",
    "http://www.omg.org/spec/BPMN/20100524/MODEL", "bpmn2",
    "http://www.omg.org/spec/BPMN/20100524/DI", "bpmndi",
    "http://www.omg.org/spec/DD/20100524/DI", "di",
    "http://www.omg.org/spec/DD/20100524/DC", "dc",
    XMLNS_ATTRIBUTE_NS_URI, ""
  );

  protected DomElement rootElement;
  protected DomElement element;

  protected String localName;
  protected String namespaceUri;
  protected String prefix;

  public XmlQName(DomDocument document, String namespaceUri, String localName) {
    this(document, null, namespaceUri, localName);
  }

  public XmlQName(DomElement element, String namespaceUri, String localName) {
    this(element.getDocument(), element, namespaceUri, localName);
  }

  public XmlQName(DomDocument document, DomElement element, String namespaceUri, String localName) {
    this.rootElement = document.getRootElement();
    this.element = element;
    this.localName = localName;
    this.namespaceUri = namespaceUri;
    this.prefix = null;
  }

  public String getNamespaceUri() {
    return namespaceUri;
  }

  public String getLocalName() {
    return localName;
  }

  public synchronized String getPrefixedName() {
    if (prefix == null) {
      this.prefix = determinePrefixAndNamespaceUri();
    }
    return QName.combine(prefix, localName);
  }

  public boolean hasLocalNamespace() {
    if (element != null) {
      return element.getNamespaceURI().equals(namespaceUri);
    }
    else {
      return false;
    }
  }

  private String determinePrefixAndNamespaceUri() {
    if (namespaceUri == null) {
      return null;
    }

    if (rootElement != null && namespaceUri.equals(rootElement.getNamespaceURI())) {
      // global namespaces do not have a prefix or namespace URI
      return null;
    }

    // lookup for prefix
    String lookupPrefix = lookupPrefix();
    if (lookupPrefix != null || rootElement == null) {
      return lookupPrefix;
    }

    // if no prefix is found we generate a new one
    // search for known prefixes
    String knownPrefix = KNOWN_PREFIXES.get(namespaceUri);
    if (knownPrefix == null) {
      // generate namespace
      return rootElement.registerNamespace(namespaceUri);
    } else if (knownPrefix.isEmpty()) {
      // ignored namespace
      return null;
    } else {
      // register known prefix
      rootElement.registerNamespace(knownPrefix, namespaceUri);
      return knownPrefix;
    }
  }

  private String lookupPrefix() {
    if (namespaceUri == null) {
      return null;
    }

    String lookupPrefix = null;
    if (element != null) {
      lookupPrefix = element.lookupPrefix(namespaceUri);
    } else if (rootElement != null) {
      lookupPrefix = rootElement.lookupPrefix(namespaceUri);
    }

    return lookupPrefix;
  }
}

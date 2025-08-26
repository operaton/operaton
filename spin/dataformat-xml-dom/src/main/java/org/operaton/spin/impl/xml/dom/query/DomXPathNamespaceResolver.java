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
package org.operaton.spin.impl.xml.dom.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;

import org.operaton.spin.xml.SpinXmlElement;

import static org.operaton.commons.utils.EnsureUtil.ensureNotNull;


/**
 * Resolves the namespaces of the given element.
 *
 * @author Stefan Hentschel
 */
public class DomXPathNamespaceResolver implements NamespaceContext {

  protected Map<String, String> namespaces;
  protected SpinXmlElement element;

  public DomXPathNamespaceResolver(SpinXmlElement element) {
    this.namespaces = new HashMap<>();
    this.element = element;
  }

  @Override
  public String getNamespaceURI(String prefix) {
    ensureNotNull("Prefix", prefix);

    if(XMLConstants.XML_NS_PREFIX.equals(prefix)) {
      return XMLConstants.XML_NS_URI;
    }

    if(XMLConstants.XMLNS_ATTRIBUTE.equals(prefix)) {
      return XMLConstants.XMLNS_ATTRIBUTE_NS_URI;
    }

    /**
     * TODO: This only works for the root element. Every child element with a 'xmlns'-attribute will be ignored
     * So you need to specify an own prefix for the child elements default namespace uri
     */
    if(XMLConstants.DEFAULT_NS_PREFIX.equals(prefix)) {
      return element.namespace();
    }

    if(namespaces.containsKey(prefix)) {
      return namespaces.get(prefix);
    } else {
      return XMLConstants.NULL_NS_URI;
    }
  }

  @Override
  public String getPrefix(String namespaceURI) {
    ensureNotNull("Namespace URI", namespaceURI);

    if(XMLConstants.XML_NS_URI.equals(namespaceURI)) {
      return XMLConstants.XML_NS_PREFIX;
    }

    if(XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(namespaceURI)) {
      return XMLConstants.XMLNS_ATTRIBUTE;
    }

    /**
     * TODO: This only works for the root element. Every child element with a 'xmlns'-attribute will be ignored.
     */
    if (namespaceURI.equals(element.name())) {
      return XMLConstants.DEFAULT_NS_PREFIX;
    }

    String key = null;
    if(namespaces.containsValue(namespaceURI)) {
      for(Map.Entry<String, String> entry : namespaces.entrySet()) {
        if(namespaceURI.equals(entry.getValue())) {
          key = entry.getKey();
          break;
        }
      }
    }
    return key;

  }

  @Override
  public Iterator getPrefixes(String namespaceURI) {
    ensureNotNull("Namespace URI", namespaceURI);

    List<String> list = new ArrayList<>();
    if(XMLConstants.XML_NS_URI.equals(namespaceURI)) {
      list.add(XMLConstants.XML_NS_PREFIX);
      return Collections.unmodifiableList(list).iterator();
    }


    if(XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(namespaceURI)) {
      list.add(XMLConstants.XMLNS_ATTRIBUTE);
      return Collections.unmodifiableList(list).iterator();
    }

    // default namespace
    if(namespaceURI.equals(element.namespace())) {
      list.add(XMLConstants.DEFAULT_NS_PREFIX);
    }

    if(namespaces.containsValue(namespaceURI)) {
      // all other namespaces
      for (Map.Entry<String, String> entry : namespaces.entrySet()) {
        if (namespaceURI.equals(entry.getValue())) {
          list.add(entry.getKey());
        }
      }
    }

    return Collections.unmodifiableList(list).iterator();
  }

  /**
   * Maps a single prefix, uri pair as namespace.
   *
   * @param prefix the prefix to use
   * @param namespaceURI the URI to use
   * @throws IllegalArgumentException if prefix or namespaceURI is null
   */
  public void setNamespace(String prefix, String namespaceURI) {
    ensureNotNull("Prefix", prefix);
    ensureNotNull("Namespace URI", namespaceURI);

    namespaces.put(prefix, namespaceURI);
  }

  /**
   * Maps a map of prefix, uri pairs as namespaces.
   *
   * @param namespaces the map of namespaces
   * @throws IllegalArgumentException if namespaces is null
   */
  public void setNamespaces(Map<String, String> namespaces) {
    ensureNotNull("Namespaces", namespaces);
    this.namespaces = namespaces;
  }
}

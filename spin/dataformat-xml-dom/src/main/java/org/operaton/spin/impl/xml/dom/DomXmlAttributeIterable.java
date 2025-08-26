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
package org.operaton.spin.impl.xml.dom;

import java.util.Iterator;

import org.w3c.dom.Attr;
import org.w3c.dom.NodeList;

import org.operaton.spin.impl.xml.dom.format.DomXmlDataFormat;
import org.operaton.spin.xml.SpinXmlAttribute;

/**
 * @author Sebastian Menski
 */
public class DomXmlAttributeIterable implements Iterable<SpinXmlAttribute> {

  protected final NodeList nodeList;
  protected final DomXmlDataFormat dataFormat;
  protected final String namespace;
  protected final boolean validating;

  public DomXmlAttributeIterable(NodeList nodeList, DomXmlDataFormat dataFormat) {
    this.nodeList = nodeList;
    this.dataFormat = dataFormat;
    this.namespace = null;
    validating = false;
  }

  public DomXmlAttributeIterable(NodeList nodeList, DomXmlDataFormat dataFormat, String namespace) {
    this.nodeList = nodeList;
    this.dataFormat = dataFormat;
    this.namespace = namespace;
    validating = true;
  }

  @Override
  public Iterator<SpinXmlAttribute> iterator() {
    return new DomXmlNodeIterator<>() {

      private NodeList attributes = nodeList;

      @Override
      protected int getLength() {
        return attributes.getLength();
      }

      @Override
      protected SpinXmlAttribute getCurrent() {
        if (attributes != null) {
          Attr attribute = (Attr) attributes.item(index);
          SpinXmlAttribute current = dataFormat.createAttributeWrapper(attribute);
          if (!validating || (current.hasNamespace(namespace))) {
            return current;
          }
        }
        return null;
      }
    };
  }

}

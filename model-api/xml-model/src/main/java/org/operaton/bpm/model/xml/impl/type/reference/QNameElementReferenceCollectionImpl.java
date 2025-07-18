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
package org.operaton.bpm.model.xml.impl.type.reference;

import org.operaton.bpm.model.xml.impl.util.QName;
import org.operaton.bpm.model.xml.instance.ModelElementInstance;
import org.operaton.bpm.model.xml.type.child.ChildElementCollection;

/**
 * @author Sebastian Menski
 */
public class QNameElementReferenceCollectionImpl<Target extends ModelElementInstance, Source extends ModelElementInstance> extends ElementReferenceCollectionImpl<Target, Source> {

  public QNameElementReferenceCollectionImpl(ChildElementCollection<Source> referenceSourceCollection) {
    super(referenceSourceCollection);
  }

  @Override
  public String getReferenceIdentifier(ModelElementInstance referenceSourceElement) {
    String identifier = super.getReferenceIdentifier(referenceSourceElement);
    if (identifier != null) {
      QName qName = QName.parseQName(identifier);
      return qName.getLocalName();
    }
    else {
      return null;
    }
  }

}

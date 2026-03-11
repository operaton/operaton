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

import java.io.Serial;

import org.w3c.dom.Node;

import org.operaton.bpm.model.xml.ModelException;
import org.operaton.bpm.model.xml.instance.DomDocument;

/**
 * <p>Thrown when a Model Element is added to the wrong document</p>
 *
 * @author Daniel Meyer
 *
 */
public class WrongDocumentException extends ModelException {

  @Serial private static final long serialVersionUID = 1L;

  public WrongDocumentException(Node nodeToAdd, DomDocument targetDocument) {
    super("Cannot add attribute '%s' to document '%s' not created by document.".formatted(nodeToAdd, targetDocument));
  }

}

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
package org.operaton.bpm.engine;


import org.operaton.bpm.engine.impl.util.xml.Element;

/**
 * Exception during the parsing of an BPMN model.
 */
public class BpmnParseException extends ProcessEngineException {

  private static final long serialVersionUID = 1L;
  protected final Element element;

  public BpmnParseException(String message, Element element) {
    super(message);
    this.element = element;
  }

  public BpmnParseException(String message, Element element, Throwable cause) {
    super(message, cause);
    this.element = element;
  }

  public Element getElement() {
    return element;
  }

}

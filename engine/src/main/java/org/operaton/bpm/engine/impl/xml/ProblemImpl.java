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
package org.operaton.bpm.engine.impl.xml;

import java.util.ArrayList;
import java.util.List;

import org.xml.sax.SAXParseException;

import org.operaton.bpm.engine.BpmnParseException;
import org.operaton.bpm.engine.Problem;
import org.operaton.bpm.engine.impl.util.xml.Element;

/**
 * @author Tom Baeyens
 * @author Joram Barrez
 */
public class ProblemImpl implements Problem {

  protected String message;
  protected int line;
  protected int column;
  protected String mainElementId;
  protected List<String> elementIds = new ArrayList<>();

  public ProblemImpl(SAXParseException e) {
    concatenateErrorMessages(e);
    this.line = e.getLineNumber();
    this.column = e.getColumnNumber();
  }

  public ProblemImpl(String errorMessage, Element element) {
    this.message = errorMessage;
    extractElementDetails(element);
  }

  public ProblemImpl(String errorMessage, Element element, String... elementIds) {
    this(errorMessage, element);
    this.mainElementId = elementIds[0];
    for (String elementId : elementIds) {
      if(elementId != null && !elementId.isEmpty()) {
        this.elementIds.add(elementId);
      }
    }
  }

  public ProblemImpl(BpmnParseException exception) {
    concatenateErrorMessages(exception);
    extractElementDetails(exception.getElement());
  }

  public ProblemImpl(BpmnParseException exception, String elementId) {
    this(exception);
    this.mainElementId = elementId;
    if(elementId != null && !elementId.isEmpty()) {
      this.elementIds.add(elementId);
    }
  }

  protected void concatenateErrorMessages(Throwable throwable) {
    var concatenatedMessage = new StringBuilder();
    while (throwable != null) {
      if (concatenatedMessage.isEmpty()) {
        concatenatedMessage.append(throwable.getMessage());
      } else {
        concatenatedMessage.append(": ").append(throwable.getMessage());
      }
      throwable = throwable.getCause();
    }
    message = concatenatedMessage.toString();
  }

  protected void extractElementDetails(Element element) {
    if (element != null) {
      this.line = element.getLine();
      this.column = element.getColumn();
      String id = element.attribute("id");
      if (id != null && !id.isEmpty()) {
        this.mainElementId = id;
        this.elementIds.add(id);
      }
    }
  }

  // getters

  @Override
  public String getMessage() {
    return message;
  }

  @Override
  public int getLine() {
    return line;
  }

  @Override
  public int getColumn() {
    return column;
  }

  @Override
  public String getMainElementId() {
    return mainElementId;
  }

  @Override
  public List<String> getElementIds() {
    return elementIds;
  }

  @Override
  public String toString() {
    StringBuilder string = new StringBuilder();
    if (line > 0) {
      string.append(" | line ").append(line);
    }
    if (column > 0) {
      string.append(" | column ").append(column);
    }

    return string.toString();
  }
}

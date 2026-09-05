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
package org.operaton.bpm.model.bpmn.impl;

import java.io.InputStream;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.xml.impl.ModelImpl;
import org.operaton.bpm.model.xml.impl.parser.AbstractModelParser;
import org.operaton.bpm.model.xml.instance.DomDocument;

import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.BPMN20_NS;
import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_20_SCHEMA_LOCATION;

/**
 * <p>The parser used when parsing BPMN Files</p>
 *
 * @author Daniel Meyer
 *
 */
public class BpmnParser extends AbstractModelParser {

  private static final String W3C_XML_SCHEMA = "http://www.w3.org/2001/XMLSchema";

  public BpmnParser() {
    this.schemaFactory = SchemaFactory.newInstance(W3C_XML_SCHEMA);
    // Unlike engine's Parser (which reads accessExternalSchema fresh per schema-resource cache
    // miss), this schema is compiled once for the static Bpmn.INSTANCE singleton. A
    // javax.xml.accessExternalSchema restriction only takes effect if set before this class
    // first loads.
    addSchema(BPMN20_NS, createSchema(BPMN_20_SCHEMA_LOCATION, BpmnParser.class.getClassLoader()));
  }

  @Override
  protected void configureFactory(DocumentBuilderFactory dbf) {
    // No JAXP_SCHEMA_SOURCE here: getDocumentBuilderSchema() supplies the already-compiled
    // schema, so the grammar is not recompiled on every parse.
    super.configureFactory(dbf);
  }

  @Override
  protected Schema getDocumentBuilderSchema() {
    return schemas.get(BPMN20_NS);
  }

  @Override
  protected BpmnModelInstanceImpl createModelInstance(DomDocument document) {
    return new BpmnModelInstanceImpl((ModelImpl) Bpmn.INSTANCE.getBpmnModel(), Bpmn.INSTANCE.getBpmnModelBuilder(), document);
  }

  @Override
  public BpmnModelInstanceImpl parseModelFromStream(InputStream inputStream) {
    return (BpmnModelInstanceImpl) super.parseModelFromStream(inputStream);
  }

  @Override
  public BpmnModelInstanceImpl getEmptyModel() {
    return (BpmnModelInstanceImpl) super.getEmptyModel();
  }

}

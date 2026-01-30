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
package org.operaton.bpm.engine.impl.util.xml;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.SAXParser;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import org.operaton.bpm.engine.BpmnParseException;
import org.operaton.bpm.engine.Problem;
import org.operaton.bpm.engine.impl.ProcessEngineLogger;
import org.operaton.bpm.engine.impl.util.EngineUtilLogger;
import org.operaton.bpm.engine.impl.util.ReflectUtil;
import org.operaton.bpm.engine.impl.util.io.InputStreamSource;
import org.operaton.bpm.engine.impl.util.io.ResourceStreamSource;
import org.operaton.bpm.engine.impl.util.io.StreamSource;
import org.operaton.bpm.engine.impl.util.io.StringStreamSource;
import org.operaton.bpm.engine.impl.util.io.UriStreamSource;
import org.operaton.bpm.engine.impl.xml.ProblemImpl;

/**
 * @author Tom Baeyens
 */
public abstract class Parse extends DefaultHandler {

  protected static final EngineUtilLogger LOG = ProcessEngineLogger.UTIL_LOGGER;

  protected static final String JAXP_SCHEMA_SOURCE = "http://java.sun.com/xml/jaxp/properties/schemaSource";
  protected static final String JAXP_SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";
  protected static final String W3C_XML_SCHEMA = "http://www.w3.org/2001/XMLSchema";

  protected static final String JAXP_ACCESS_EXTERNAL_SCHEMA = "http://javax.xml.XMLConstants/property/accessExternalSchema";
  protected static final String JAXP_ACCESS_EXTERNAL_SCHEMA_SYSTEM_PROPERTY = "javax.xml.accessExternalSchema";
  protected static final String JAXP_ACCESS_EXTERNAL_SCHEMA_ALL = "all";

  protected Parser parser;
  protected String name;
  protected StreamSource streamSource;
  protected Element rootElement;
  protected List<Problem> errors = new ArrayList<>();
  protected List<Problem> warnings = new ArrayList<>();
  protected String schemaResource;

  protected Parse(Parser parser) {
    this.parser = parser;
  }

  public Parse name(String name) {
    this.name = name;
    return this;
  }

  public Parse sourceInputStream(InputStream inputStream) {
    if (name == null) {
      name("inputStream");
    }
    setStreamSource(new InputStreamSource(inputStream));
    return this;
  }

  public Parse sourceResource(String resource) {
    return sourceResource(resource, null);
  }

  public Parse sourceUrl(URL url) {
    return sourceUri(ReflectUtil.urlToURI(url));
  }

  public Parse sourceUri(URI uri) {
    if (name == null) {
      name(uri.toString());
    }
    setStreamSource(new UriStreamSource(uri));
    return this;
  }

  public Parse sourceUrl(String url) {
    try {
      return sourceUri(new URI(url));
    } catch (URISyntaxException e) {
      throw LOG.malformedUriException(url, e);
    }
  }

  public Parse sourceResource(String resource, ClassLoader classLoader) {
    if (name == null) {
      name(resource);
    }
    setStreamSource(new ResourceStreamSource(resource, classLoader));
    return this;
  }

  public Parse sourceString(String string) {
    if (name == null) {
      name("string");
    }
    setStreamSource(new StringStreamSource(string));
    return this;
  }

  protected void setStreamSource(StreamSource streamSource) {
    if (this.streamSource != null) {
      throw LOG.multipleSourcesException(this.streamSource, streamSource);
    }
    this.streamSource = streamSource;
  }

  public void setSchemaResource(String schemaResource) {
    boolean schemaResourceSet = schemaResource != null;
    parser.enableSchemaValidation(schemaResourceSet);

    this.schemaResource = schemaResource;
  }

  public Parse execute() {
    try {
      InputStream inputStream = streamSource.getInputStream();

      SAXParser saxParser = parser.getSaxParser();
      trySetAccessExternalSchema(saxParser);
      if (schemaResource != null) {
        saxParser.setProperty(JAXP_SCHEMA_LANGUAGE, W3C_XML_SCHEMA);
        saxParser.setProperty(JAXP_SCHEMA_SOURCE, schemaResource);
      }
      saxParser.parse(inputStream, new ParseHandler(this));
    } catch (Exception e) {
      throw LOG.parsingFailureException(name, e);
    }

    return this;
  }

  private void trySetAccessExternalSchema(SAXParser saxParser) {
    try {
      saxParser.setProperty(JAXP_ACCESS_EXTERNAL_SCHEMA, resolveAccessExternalSchemaProperty());
    } catch (Exception e) {
      // ignore unavailable option
      LOG.logAccessExternalSchemaNotSupported(e);
    }
  }

  /*
   * JAXP allows users to override the default value via system properties and
   * a central properties file (see https://docs.oracle.com/javase/tutorial/jaxp/properties/scope.html).
   * However, both are overridden by an explicit configuration in code, as we apply it.
   * Since we want users to customize the value, we take the system property into account.
   * The properties file is not supported at the moment.
   */
  protected String resolveAccessExternalSchemaProperty() {
    String systemProperty = System.getProperty(JAXP_ACCESS_EXTERNAL_SCHEMA_SYSTEM_PROPERTY);

    if (systemProperty != null) {
      return systemProperty;
    } else {
      return JAXP_ACCESS_EXTERNAL_SCHEMA_ALL;
    }
  }

  public Element getRootElement() {
    return rootElement;
  }

  public List<Problem> getProblems() {
    return errors;
  }

  public void addError(SAXParseException e) {
    errors.add(new ProblemImpl(e));
  }

  public void addError(String errorMessage, Element element) {
    errors.add(new ProblemImpl(errorMessage, element));
  }

  public void addError(String errorMessage, Element element, String... elementIds) {
    errors.add(new ProblemImpl(errorMessage, element, elementIds));
  }

  public void addError(BpmnParseException e) {
    errors.add(new ProblemImpl(e));
  }

  public void addError(BpmnParseException e, String elementId) {
    errors.add(new ProblemImpl(e, elementId));
  }

  public boolean hasErrors() {
    return errors != null && !errors.isEmpty();
  }

  public void addWarning(SAXParseException e) {
    warnings.add(new ProblemImpl(e));
  }

  public void addWarning(String errorMessage, Element element) {
    warnings.add(new ProblemImpl(errorMessage, element));
  }

  public void addWarning(String errorMessage, Element element, String... elementIds) {
    warnings.add(new ProblemImpl(errorMessage, element, elementIds));
  }

  public boolean hasWarnings() {
    return warnings != null && !warnings.isEmpty();
  }

  public void logWarnings() {
    StringBuilder builder = new StringBuilder();
    for (Problem warning : warnings) {
      builder.append("\n* ");
      builder.append(warning.getMessage());
      builder.append(" | resource ").append(name);
      builder.append(warning);
    }
    LOG.logParseWarnings(builder.toString());
  }

  public void throwExceptionForErrors() {
    StringBuilder strb = new StringBuilder();
    for (Problem error : errors) {
      strb.append("\n* ");
      strb.append(error.getMessage());
      strb.append(" | resource ").append(name);
      strb.append(error);
    }
    throw LOG.exceptionDuringParsing(strb.toString(), name, errors, warnings);
  }
}

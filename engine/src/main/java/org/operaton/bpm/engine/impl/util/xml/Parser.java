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

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.jspecify.annotations.Nullable;
import org.xml.sax.SAXException;

import org.operaton.bpm.engine.impl.ProcessEngineLogger;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.impl.util.EngineUtilLogger;

/**
 * @author Tom Baeyens
 */
public abstract class Parser {

  protected static final EngineUtilLogger LOG = ProcessEngineLogger.UTIL_LOGGER;

  protected static final String EXTERNAL_GENERAL_ENTITIES = "http://xml.org/sax/features/external-general-entities";
  protected static final String DISALLOW_DOCTYPE_DECL = "http://apache.org/xml/features/disallow-doctype-decl";
  protected static final String LOAD_EXTERNAL_DTD = "http://apache.org/xml/features/nonvalidating/load-external-dtd";
  protected static final String EXTERNAL_PARAMETER_ENTITIES = "http://xml.org/sax/features/external-parameter-entities";
  protected static final String NAMESPACE_PREFIXES = "http://xml.org/sax/features/namespace-prefixes";

  protected static final String JAXP_ACCESS_EXTERNAL_SCHEMA_SYSTEM_PROPERTY = "javax.xml.accessExternalSchema";
  protected static final String JAXP_ACCESS_EXTERNAL_SCHEMA_ALL = "all";

  private static final String NO_SCHEMA_KEY = "<none>";

  /**
   * Compiled XSD schemas, keyed by the schema resource URL. Compiling a schema is expensive
   * (BPMN20.xsd plus its Semantic/BPMNDI/DC/DI imports) and was previously repeated on every
   * single parse. {@link Schema} instances are immutable and thread-safe, so they are shared
   * process-wide. Bounded by the number of schema resources the engine ships.
   */
  private static final ConcurrentHashMap<String, Schema> SCHEMAS = new ConcurrentHashMap<>();

  /**
   * {@link SAXParserFactory} is not thread-safe, so factories stay per-thread. Keyed by schema
   * resource and XXE processing flag so that each factory is configured exactly once; this also
   * removes the previously thread-shared, cross-parse mutation of the validating flag.
   */
  private static final ThreadLocal<Map<String, SAXParserFactory>> SAX_PARSER_FACTORIES =
      ThreadLocal.withInitial(HashMap::new);

  public abstract Parse createParse();

  protected SAXParser getSaxParser(@Nullable String schemaResource) throws ParserConfigurationException, SAXException {
    boolean xxeProcessing = Boolean.TRUE.equals(isEnableXxeProcessing());
    String key = (schemaResource == null ? NO_SCHEMA_KEY : schemaResource) + '|' + xxeProcessing;

    SAXParserFactory saxParserFactory = SAX_PARSER_FACTORIES.get()
        .computeIfAbsent(key, ignored -> createSaxParserFactory(schemaResource, xxeProcessing));

    return saxParserFactory.newSAXParser();
  }

  /**
   * Creates a factory configured once for the given schema resource. When no schema resource is
   * given the factory is neither validating nor namespace aware, which reproduces the previous
   * behaviour of {@code enableSchemaValidation(false)}.
   */
  protected SAXParserFactory createSaxParserFactory(@Nullable String schemaResource, boolean xxeProcessing) {
    SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
    saxParserFactory.setNamespaceAware(schemaResource != null);
    saxParserFactory.setValidating(false);

    try {
      saxParserFactory.setFeature(NAMESPACE_PREFIXES, true);
    } catch (Exception e) {
      LOG.unableToSetSchemaResource(e);
    }

    setXxeProcessing(saxParserFactory, xxeProcessing);

    if (schemaResource != null) {
      saxParserFactory.setSchema(getSchema(schemaResource));
    }

    return saxParserFactory;
  }

  protected Schema getSchema(String schemaResource) {
    String accessProperty = resolveAccessExternalSchemaProperty();
    String cacheKey = schemaResource + '|' + accessProperty;
    return SCHEMAS.computeIfAbsent(cacheKey, key -> compileSchema(schemaResource, accessProperty));
  }

  private static Schema compileSchema(String schemaResource, String accessExternalSchemaProperty) {
    SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

    try {
      schemaFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, accessExternalSchemaProperty);
    } catch (SAXException e) {
      // ignore unavailable option, same as the per-parse code path did
      LOG.logAccessExternalSchemaNotSupported(e);
    }

    try {
      return schemaFactory.newSchema(URI.create(schemaResource).toURL());
    } catch (Exception e) {
      throw LOG.parsingFailureException(schemaResource, e);
    }
  }

  /*
   * JAXP allows users to override the default value via system properties and a central
   * properties file (see https://docs.oracle.com/javase/tutorial/jaxp/properties/scope.html).
   * However, both are overridden by an explicit configuration in code, as we apply it. Since we
   * want users to customize the value, we take the system property into account. The properties
   * file is not supported at the moment.
   */
  protected static String resolveAccessExternalSchemaProperty() {
    String systemProperty = System.getProperty(JAXP_ACCESS_EXTERNAL_SCHEMA_SYSTEM_PROPERTY);

    if (systemProperty != null) {
      return systemProperty;
    } else {
      return JAXP_ACCESS_EXTERNAL_SCHEMA_ALL;
    }
  }

  protected void setXxeProcessing(SAXParserFactory saxParserFactory, boolean enableXxeProcessing) {
    saxParserFactory.setXIncludeAware(enableXxeProcessing);
    try {
      saxParserFactory.setFeature(EXTERNAL_GENERAL_ENTITIES, enableXxeProcessing);
      saxParserFactory.setFeature(DISALLOW_DOCTYPE_DECL, !enableXxeProcessing);
      saxParserFactory.setFeature(LOAD_EXTERNAL_DTD, enableXxeProcessing);
      saxParserFactory.setFeature(EXTERNAL_PARAMETER_ENTITIES, enableXxeProcessing);
      saxParserFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

    } catch (Exception e) {
      throw LOG.exceptionWhileSettingXxeProcessing(e);

    }
  }

  public Boolean isEnableXxeProcessing() {
    return Context.findProcessEngineConfiguration()
      .map(ProcessEngineConfigurationImpl::isEnableXxeProcessing)
      .orElse(false);
  }

}

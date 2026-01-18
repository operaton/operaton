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
package org.operaton.spin.plugin.impl.feel.integration;
import java.util.List;

import java.util.*;

import org.camunda.feel.impl.DefaultValueMapper;
import org.camunda.feel.syntaxtree.Val;
import org.camunda.feel.syntaxtree.ValContext;
import org.camunda.feel.syntaxtree.ValList;
import org.camunda.feel.syntaxtree.ValString;
import org.camunda.feel.valuemapper.CustomValueMapper;
import org.camunda.feel.valuemapper.ValueMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.dmn.feel.impl.scala.ScalaFeelLogger;
import org.operaton.bpm.dmn.feel.impl.scala.spin.SpinValueMapperFactory;
import org.operaton.bpm.engine.test.junit5.ProcessEngineLoggingExtension;
import org.operaton.bpm.engine.test.junit5.WatchLogger;
import org.operaton.spin.Spin;
import org.operaton.spin.json.SpinJsonNode;
import org.operaton.spin.xml.SpinXmlElement;

import static camundajar.impl.scala.jdk.CollectionConverters.ListHasAsScala;
import static org.assertj.core.api.Assertions.assertThat;

class SpinValueMapperTest {

  protected static ValueMapper valueMapper;

  @RegisterExtension
  ProcessEngineLoggingExtension loggingExtension = new ProcessEngineLoggingExtension();

  @BeforeAll
  static void setUp() {
    DefaultValueMapper defaultValueMapper = DefaultValueMapper.instance();
    SpinValueMapper spinValueMapper = new SpinValueMapper();
    List<CustomValueMapper> mapperList = List.of(defaultValueMapper, spinValueMapper);
    valueMapper = new ValueMapper
        .CompositeValueMapper(ListHasAsScala(mapperList).asScala().toList());
  }

  @Test
  void shouldMapOperatonSpinJSONObjectAsContext() {
    // given
    Map<String, Val> map = new HashMap<>();
    map.put("customer", new ValString("Kermit"));
    map.put("language", new ValString("en"));
    ValContext context = (ValContext) valueMapper.toVal(map);
    SpinJsonNode json = Spin.JSON("{\"customer\": \"Kermit\", \"language\": \"en\"}");

    // when
    Val value = valueMapper.toVal(json);

    // then
    assertThat(value).isEqualTo(context);
  }

  @Test
  void shouldMapOperatonSpinJSONarrayAsList() {
    // given
    List<Val> list = List.of(new ValString("Kermit"), new ValString("Waldo"));
    ValList feelList = (ValList) valueMapper.toVal(list);
    ValContext context = (ValContext) valueMapper.toVal(Collections.singletonMap("customer", feelList));
    SpinJsonNode json = Spin.JSON("{\"customer\": [\"Kermit\", \"Waldo\"]}");

    // when
    Val value = valueMapper.toVal(json);

    // then
    assertThat(value).isEqualTo(context);
  }

  @Test
  void shouldMapNestedOperatonSpinJSONObjectAsContext() {

    // given
    Map<String, Val> nestedMap = new HashMap<>();
    nestedMap.put("city", new ValString("Berlin"));
    nestedMap.put("zipCode", valueMapper.toVal(10961));

    Map<String, Val> contextMap = new HashMap<>();
    contextMap.put("customer", new ValString("Kermit"));
    contextMap.put("address", valueMapper.toVal(nestedMap));

    ValContext context = (ValContext) valueMapper.toVal(contextMap);
    SpinJsonNode json = Spin.JSON("{" +
                                      "\"customer\": \"Kermit\", " +
                                      "\"address\": {\"" +
                                        "city\": \"Berlin\", " +
                                      "\"zipCode\": 10961}}");

    // when
    Val value = valueMapper.toVal(json);

    // then
    assertThat(value).isEqualTo(context);
  }

  @Test
  void shouldMapOperatonSpinXMLObjectWithAttributes() {
    // given
    Map<String, Val> xmlInnerMap = new HashMap<>();
    xmlInnerMap.put("@name", new ValString("Kermit"));
    xmlInnerMap.put("@language", new ValString("en"));
    Map<String, Val> xmlContextMap = new HashMap<>();
    xmlContextMap.put("customer", valueMapper.toVal(xmlInnerMap));

    ValContext context = (ValContext) valueMapper.toVal(xmlContextMap);
    SpinXmlElement xml = Spin.XML(" <customer name=\"Kermit\" language=\"en\" /> ");

    // when
    Val value = valueMapper.toVal(xml);

    // then
    assertThat(value).isEqualTo(context);
  }

  @Test
  void shouldMapOperatonSpinXMLObjectWithChildObject() {
    // given
    Map<String, Val> xmlAttrMap = new HashMap<>();
    xmlAttrMap.put("@city", new ValString("Berlin"));
    xmlAttrMap.put("@zipCode", new ValString("10961"));
    Map<String, Val> xmlInnerMap = new HashMap<>();
    xmlInnerMap.put("address", valueMapper.toVal(xmlAttrMap));
    Map<String, Val> xmlContextMap = new HashMap<>();
    xmlContextMap.put("customer", valueMapper.toVal(xmlInnerMap));

    ValContext context = (ValContext) valueMapper.toVal(xmlContextMap);
    SpinXmlElement xml = Spin.XML("<customer>" +
                                      "<address city=\"Berlin\" zipCode=\"10961\" />" +
                                  "</customer>");

    // when
    Val value = valueMapper.toVal(xml);

    // then
    assertThat(value).isEqualTo(context);
  }

  @Test
  void shouldMapOperatonSpinXMLObjectWithListOfChildObjects() {
    // given
    SpinXmlElement xml = Spin.XML("<data>" +
                                          "<customer name=\"Kermit\" language=\"en\" />" +
                                          "<customer name=\"John\" language=\"de\" />" +
                                          "<provider name=\"Foobar\" />" +
                                         "</data>");

    Map<String, Val> xmlProviderAttrMap = new HashMap();
    xmlProviderAttrMap.put("@name", new ValString("Foobar"));

    Map<String, Val> xmlCustomerAttrMap1 = new HashMap<>();
    xmlCustomerAttrMap1.put("@name", new ValString("Kermit"));
    xmlCustomerAttrMap1.put("@language", new ValString("en"));

    Map<String, Val> xmlCustomerAttrMap2 = new HashMap<>();
    xmlCustomerAttrMap2.put("@name", new ValString("John"));
    xmlCustomerAttrMap2.put("@language", new ValString("de"));

    Map<String, Val> xmlInnerMap = new HashMap<>();
    xmlInnerMap.put("provider", valueMapper.toVal(xmlProviderAttrMap));
    xmlInnerMap.put("customer",
                    valueMapper.toVal(List.of(xmlCustomerAttrMap1, xmlCustomerAttrMap2)));

    Map<String, Val> xmlContextMap = new HashMap<>();
    xmlContextMap.put("data", valueMapper.toVal(xmlInnerMap));

    ValContext context = (ValContext) valueMapper.toVal(xmlContextMap);

    // when
    Val value = valueMapper.toVal(xml);

    // then
    assertThat(value).isEqualTo(context);
  }

  @Test
  void shouldMapOperatonSpinXMLObjectWithContent() {
    // given
    SpinXmlElement xml = Spin.XML("<customer>Kermit</customer>");

    Map<String, Val> xmlInnerMap = new HashMap<>();
    xmlInnerMap.put("$content", new ValString("Kermit"));

    Map<String, Val> xmlContextMap = new HashMap<>();
    xmlContextMap.put("customer", valueMapper.toVal(xmlInnerMap));

    ValContext context = (ValContext) valueMapper.toVal(xmlContextMap);

    // when
    Val value = valueMapper.toVal(xml);

    // then
    assertThat(value).isEqualTo(context);
  }

  @Test
  void shouldMapOperatonSpinXMLObjectWithoutContent() {
    // given
    SpinXmlElement xml = Spin.XML("<customer />");
    ValContext context = (ValContext) valueMapper
        .toVal(Collections.singletonMap("customer", null));

    // when
    Val value = valueMapper.toVal(xml);

    // then
    assertThat(value).isEqualTo(context);
  }

  @Test
  void shouldMapOperatonSpinXMLObjectWithPrefix() {
    // given
    SpinXmlElement xml = Spin.XML("<data xmlns:p=\"http://www.example.org\">" +
                                          "<p:customer p:name=\"Kermit\" language=\"en\" />" +
                                        "</data>");

    Map<String, Val> xmlAttrMap = new HashMap<>();
    xmlAttrMap.put("@p$name", new ValString("Kermit"));
    xmlAttrMap.put("@language", new ValString("en"));

    Map<String, Val> xmlInnerMap = new HashMap<>();
    xmlInnerMap.put("p$customer", valueMapper.toVal(xmlAttrMap));
    xmlInnerMap.put("@xmlns$p", new ValString("http://www.example.org"));

    Map<String, Val> xmlContextMap = new HashMap<>();
    xmlContextMap.put("data", valueMapper.toVal(xmlInnerMap));

    ValContext context = (ValContext) valueMapper.toVal(xmlContextMap);

    // when
    Val value = valueMapper.toVal(xml);

    // then
    assertThat(value).isEqualTo(context);
  }

  @Test
  void shouldEqualClassNameForSpinValueMapper() {
    assertThat(SpinValueMapper.class.getName())
        .isEqualTo(SpinValueMapperFactory.SPIN_VALUE_MAPPER_CLASS_NAME);
  }

  @Test
  @WatchLogger(loggerNames = {ScalaFeelLogger.PROJECT_LOGGER}, level = "INFO")
  void shouldLogValueMapperDetection() {
    // given
    SpinValueMapperFactory mapperFactory = new SpinValueMapperFactory();

    // when
    mapperFactory.createInstance();

    // then
    assertThat(loggingExtension.getFilteredLog("Spin value mapper detected").size()).isOne();
  }

}

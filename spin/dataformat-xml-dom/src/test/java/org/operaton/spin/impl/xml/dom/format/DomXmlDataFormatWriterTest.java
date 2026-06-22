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
package org.operaton.spin.impl.xml.dom.format;

import java.io.*;

import org.junit.jupiter.api.Test;

import org.operaton.commons.utils.ServiceLoaderUtil;
import org.operaton.spin.DataFormats;
import org.operaton.spin.SpinFactory;
import org.operaton.spin.spi.DataFormat;
import org.operaton.spin.xml.JdkUtil;
import org.operaton.spin.xml.SpinXmlElement;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test xml transformation in DomXmlDataFormatWriter
 */
class DomXmlDataFormatWriterTest {

  private final String newLine = System.lineSeparator();
  private final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><order><product>Milk</product><product>Coffee</product><product> </product></order>";

  private final String formattedXmlIbmJDK =
      "<?xml version=\"1.0\" encoding=\"UTF-8\"?><order>" + newLine + "  <product>Milk</product>" + newLine
          + "  <product>Coffee</product>" + newLine + "  <product/>" + newLine + "</order>";

  private final String formattedXml = formattedXmlIbmJDK + newLine;

  private final String formattedXmlWithWhitespaceInProductIbmJDK =
      "<?xml version=\"1.0\" encoding=\"UTF-8\"?><order>" + newLine + "  <product>Milk</product>" + newLine
          + "  <product>Coffee</product>" + newLine + "  <product> </product>" + newLine + "</order>";

  private final String formattedXmlWithWhitespaceInProduct = formattedXmlWithWhitespaceInProductIbmJDK + newLine;

  private final SpinFactory spinFactory = ServiceLoaderUtil.loadSingleService(SpinFactory.class);

  // this is what execution.setVariable("test", spinXml); does
  // see https://github.com/operaton/operaton/blob/main/engine-plugins/spin-plugin/src/main/java/org/operaton/spin/plugin/impl/SpinValueSerializer.java
  private byte[] serializeValue(SpinXmlElement spinXml) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    OutputStreamWriter outWriter = new OutputStreamWriter(out, UTF_8);
    BufferedWriter bufferedWriter = new BufferedWriter(outWriter);

    spinXml.writeToWriter(bufferedWriter);
    return out.toByteArray();
  }

  public SpinXmlElement deserializeValue(byte[] serialized, DataFormat<SpinXmlElement> dataFormat) {
    ByteArrayInputStream bais = new ByteArrayInputStream(serialized);
    InputStreamReader inReader = new InputStreamReader(bais, UTF_8);
    BufferedReader bufferedReader = new BufferedReader(inReader);

    Object wrapper = dataFormat.getReader().readInput(bufferedReader);
    return dataFormat.createWrapperInstance(wrapper);
  }

  /**
   * IBM JDK does not generate a new line character at the end
   * of an XSLT-transformed XML document. See CAM-14806.
   */
  private String getExpectedFormattedXML(boolean withWhitespaceInElement) {
    if (JdkUtil.runsOnIbmJDK()) {
      return withWhitespaceInElement ? formattedXmlWithWhitespaceInProductIbmJDK : formattedXmlIbmJDK;
    } else {
      return withWhitespaceInElement ? formattedXmlWithWhitespaceInProduct : formattedXml;
    }
  }

  private String getExpectedFormattedXML() {
    return getExpectedFormattedXML(false);
  }

  /**
   * standard behaviour: an unformatted XML will be formatted stored into a SPIN variable and also returned formatted.
   */
  @Test
  void standardFormatter() {
    // given
    DataFormat<SpinXmlElement> dataFormat = new DomXmlDataFormat(DataFormats.XML_DATAFORMAT_NAME);

    SpinXmlElement spinXml = spinFactory.createSpin(xml, dataFormat);

    // when
    byte[] serializedValue = serializeValue(spinXml);

    // then
    // assert that there are now new lines in the serialized value:
    assertThat(new String(serializedValue, UTF_8)).isEqualTo(getExpectedFormattedXML());

    // when
    // this is what execution.getVariable("test"); does
    SpinXmlElement spinXmlElement = deserializeValue(serializedValue, dataFormat);

    // then
    assertThat(spinXmlElement).hasToString(getExpectedFormattedXML());
  }

  /**
   * behaviour fixed by CAM-13699: an already formatted XML will be formatted stored into a SPIN variable and also
   * returned formatted but no additional blank lines are inserted into the XML.
   */
  @Test
  void alreadyFormattedXml() {
    // given
    DataFormat<SpinXmlElement> dataFormat = new DomXmlDataFormat(DataFormats.XML_DATAFORMAT_NAME);

    SpinXmlElement spinXml = spinFactory.createSpin(formattedXml, dataFormat);

    // when
    byte[] serializedValue = serializeValue(spinXml);

    // then
    // assert that there are no new lines in the serialized value:
    assertThat(new String(serializedValue, UTF_8)).isEqualTo(getExpectedFormattedXML());

    // when
    // this is what execution.getVariable("test"); does
    SpinXmlElement spinXmlElement = deserializeValue(serializedValue, dataFormat);

    // then
    assertThat(spinXmlElement).hasToString(getExpectedFormattedXML());
  }

  /**
   * new feature provided by CAM-13699 - pretty print feature disabled. The XML is stored and returned as is.
   */
  @Test
  void disabledPrettyPrintUnformatted() {
    // given
    DomXmlDataFormat dataFormat = new DomXmlDataFormat(DataFormats.XML_DATAFORMAT_NAME);
    dataFormat.setPrettyPrint(false);

    SpinXmlElement spinXml = spinFactory.createSpin(xml, dataFormat);

    // when
    byte[] serializedValue = serializeValue(spinXml);

    // then
    // assert that xml has not been formatted
    assertThat(new String(serializedValue, UTF_8)).isEqualTo(xml);

    // when
    // this is what execution.getVariable("test"); does
    SpinXmlElement spinXmlElement = deserializeValue(serializedValue, dataFormat);

    // then
    assertThat(spinXmlElement).hasToString(xml);
  }

  /**
   * new feature provided by CAM-13699 - pretty print feature disabled. The XML is stored and returned as is.
   */
  @Test
  void disabledPrettyPrintFormatted() {

    // given
    String expectedXml =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?><order>" + newLine + "  <product>Milk</product>" + newLine
            + "  <product>Coffee</product>" + newLine + "  <product> </product>" + newLine + "</order>";

    DomXmlDataFormat dataFormat = new DomXmlDataFormat(DataFormats.XML_DATAFORMAT_NAME);
    dataFormat.setPrettyPrint(false);

    SpinXmlElement spinXml = spinFactory.createSpin(formattedXmlWithWhitespaceInProduct, dataFormat);

    // when
    byte[] serializedValue = serializeValue(spinXml);

    // then
    // assert that xml has not been formatted
    assertThat(new String(serializedValue, UTF_8)).isEqualTo(expectedXml);

    // when
    // this is what execution.getVariable("test"); does
    SpinXmlElement spinXmlElement = deserializeValue(serializedValue, dataFormat);

    // then
    assertThat(spinXmlElement).hasToString(expectedXml);
  }

  /**
   * new feature provided by <a href="https://github.com/camunda/camunda-bpm-platform/issues/3633">Camunda issue#3633</a>: custom formatting
   * configuration to preserve-space.
   */
  @Test
  void customStripSpaceXSL() throws Exception {
    final DomXmlDataFormat dataFormat = new DomXmlDataFormat(DataFormats.XML_DATAFORMAT_NAME);

    try (InputStream inputStream = DomXmlDataFormatWriterTest.class.getClassLoader()
        .getResourceAsStream("org/operaton/spin/strip-space-preserve-space.xsl")) {
      dataFormat.setFormattingConfiguration(inputStream);
    }

    final SpinXmlElement spinXml = spinFactory.createSpin(this.xml, dataFormat);

    // when
    final byte[] serializedValue = serializeValue(spinXml);

    // then
    // assert that xml has not been formatted
    assertThat(new String(serializedValue, UTF_8)).isEqualTo(getExpectedFormattedXML(true));

    // when
    // this is what execution.getVariable("test"); does
    final SpinXmlElement spinXmlElement = deserializeValue(serializedValue, dataFormat);

    // then
    assertThat(spinXmlElement).hasToString(getExpectedFormattedXML(true));
  }
}

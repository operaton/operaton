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
package org.operaton.bpm.qa.performance.engine.framework.report;

import org.operaton.bpm.engine.impl.form.engine.HtmlDocumentBuilder;
import org.operaton.bpm.engine.impl.form.engine.HtmlElementWriter;
import org.operaton.bpm.qa.performance.engine.framework.aggregate.TabularResultSet;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@SuppressWarnings("java:S1192")
public class SectionedHtmlReportBuilder {

  protected final String reportName;

  protected final Map<String, Object> sections = new TreeMap<>();

  public SectionedHtmlReportBuilder(String reportName) {
    this.reportName = reportName;
  }

  public SectionedHtmlReportBuilder addSection(String title, Object section) {
    this.sections.put(title, section);
    return this;
  }

  public String execute() {
    HtmlDocumentBuilder builder = new HtmlDocumentBuilder(new HtmlElementWriter("html"));

    addHtmlHead(builder);
    addHtmlBody(builder);

    return builder
      .endElement()
      .getHtmlString();
  }

  protected void addHtmlHead(HtmlDocumentBuilder builder) {
    builder
      .startElement(new HtmlElementWriter("head"))
        .startElement(new HtmlElementWriter("title").textContent(reportName)).endElement()
        .startElement(new HtmlElementWriter("link")
          .attribute("rel", "stylesheet")
          .attribute("href", "http://netdna.bootstrapcdn.com/bootstrap/3.0.3/css/bootstrap.min.css"))
        .endElement()
        .startElement(new HtmlElementWriter("link")
          .attribute("rel", "stylesheet")
          .attribute("href", "http://netdna.bootstrapcdn.com/bootstrap/3.0.3/css/bootstrap-theme.min.css"))
        .endElement()
      .endElement();
  }

  protected void addHtmlBody(HtmlDocumentBuilder builder) {
    builder.startElement(new HtmlElementWriter("body"));
    builder.startElement(new HtmlElementWriter("div").attribute("class", "container"));
    builder.startElement(new HtmlElementWriter("div").attribute("class", "row"));
    builder.startElement(new HtmlElementWriter("div").attribute("class", "coll-md-12"));
    builder.startElement(new HtmlElementWriter("h1").textContent(reportName)).endElement();
    addHtmlSections(builder, sections, 3);
    builder.endElement();
    builder.endElement();
    builder.endElement();
    builder.endElement();
  }

  protected void addHtmlSections(HtmlDocumentBuilder builder, Map<String, Object> sections, int level) {
    for (var entry : sections.entrySet()) {
      addHtmlSection(builder, entry.getKey(), entry.getValue(), level);
    }
  }

  @SuppressWarnings({"unchecked", "java:S3740", "rawtypes"})
  protected void addHtmlSection(HtmlDocumentBuilder builder, String title, Object section, int level) {
    // add heading
    builder.startElement(new HtmlElementWriter("h" + level).textContent(title)).endElement();
    if (section instanceof Map sec) {
      addHtmlSections(builder, sec, level + 1);
    }
    else {
      TabularResultSet resultSet = (TabularResultSet) section;
      addHtmlTable(builder, resultSet);
    }
  }

  protected void addHtmlTable(HtmlDocumentBuilder builder, TabularResultSet resultSet) {
    /** <table> */
    builder.startElement(new HtmlElementWriter("table").attribute("class", "table table-condensed table-hover table-bordered"));

    /** <tr> */

    HtmlDocumentBuilder tableHeadRowBuilder = builder.startElement(new HtmlElementWriter("tr"));

    for (String columnName : resultSet.getResultColumnNames()) {
      tableHeadRowBuilder.startElement(new HtmlElementWriter("th").textContent(columnName))
        .endElement();
    }

    /** </tr> */
    tableHeadRowBuilder.endElement();

    for (List<Object> resultRow : resultSet.getResults()) {

      /** <tr> */
      HtmlDocumentBuilder tableRowBuilder = builder.startElement(new HtmlElementWriter("tr"));

      for (Object value : resultRow) {
        if (value instanceof TableCell tableCell) {
          tableRowBuilder.startElement(tableCell.toHtmlElementWriter()).endElement();
        } else {
          tableRowBuilder.startElement(new HtmlElementWriter("td").textContent(String.valueOf(value))).endElement();
        }
      }

      /** </tr> */
      tableRowBuilder.endElement();
    }

    /** </table> */
    builder.endElement();
  }


  public static class TableCell {

    private final String text;
    private int colspan = 1;
    private boolean header = false;

    public TableCell(String text) {
      this(text, false);
    }

    public TableCell(String text, boolean header) {
      this(text, 1, header);
    }

    public TableCell(String text, int colspan) {
      this(text, colspan, false);
    }

    public TableCell(String text, int colspan, boolean header) {
      this.text = text;
      this.colspan = colspan;
      this.header = header;
    }

    @Override
    public String toString() {
      return text;
    }

    public HtmlElementWriter toHtmlElementWriter() {
      HtmlElementWriter elementWriter;
      if (header) {
        elementWriter = new HtmlElementWriter("th");
      }
      else {
        elementWriter = new HtmlElementWriter("td");
      }

      if (colspan > 1) {
        elementWriter
          .attribute("colspan", String.valueOf(colspan))
          .attribute("class", "text-center");
      }

      elementWriter.textContent(text);

      return elementWriter;
    }

  }

}

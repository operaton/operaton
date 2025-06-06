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
package org.operaton.bpm.engine.test.api.form;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.operaton.bpm.engine.FormService;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.form.engine.FormEngine;
import org.operaton.bpm.engine.impl.form.engine.HtmlDocumentBuilder;
import org.operaton.bpm.engine.impl.form.engine.HtmlElementWriter;
import org.operaton.bpm.engine.impl.form.engine.HtmlFormEngine;
import org.operaton.bpm.engine.impl.util.IoUtil;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;

/**
 * @author Daniel Meyer
 *
 */
@ExtendWith(ProcessEngineExtension.class)
class HtmlFormEngineTest {

  protected ProcessEngineConfigurationImpl processEngineConfiguration;
  protected RepositoryService repositoryService;
  protected TaskService taskService;
  protected FormService formService;
  protected RuntimeService runtimeService;

  @Test
  void testIsDefaultFormEngine() {

    // make sure the html form engine is the default form engine:
    Map<String, FormEngine> formEngines = processEngineConfiguration.getFormEngines();
    assertThat(formEngines.get(null)).isInstanceOf(HtmlFormEngine.class);

  }

  @Test
  void testTransformNullFormData() {
    HtmlFormEngine formEngine = new HtmlFormEngine();
    assertThat(formEngine.renderStartForm(null)).isNull();
    assertThat(formEngine.renderTaskForm(null)).isNull();
  }

  @Test
  void testHtmlElementWriter() {

    String htmlString = new HtmlDocumentBuilder(new HtmlElementWriter("someTagName"))
      .endElement()
      .getHtmlString();
    assertHtmlEquals("<someTagName></someTagName>", htmlString);

    htmlString = new HtmlDocumentBuilder(new HtmlElementWriter("someTagName", true))
      .endElement()
      .getHtmlString();
    assertHtmlEquals("<someTagName />", htmlString);

    htmlString = new HtmlDocumentBuilder(new HtmlElementWriter("someTagName", true).attribute("someAttr", "someAttrValue"))
      .endElement()
      .getHtmlString();
    assertHtmlEquals("<someTagName someAttr=\"someAttrValue\" />", htmlString);

    htmlString = new HtmlDocumentBuilder(new HtmlElementWriter("someTagName").attribute("someAttr", "someAttrValue"))
      .endElement()
      .getHtmlString();
    assertHtmlEquals("<someTagName someAttr=\"someAttrValue\"></someTagName>", htmlString);

    htmlString = new HtmlDocumentBuilder(new HtmlElementWriter("someTagName").attribute("someAttr", null))
      .endElement()
      .getHtmlString();
    assertHtmlEquals("<someTagName someAttr></someTagName>", htmlString);

    htmlString = new HtmlDocumentBuilder(new HtmlElementWriter("someTagName").textContent("someTextContent"))
      .endElement()
      .getHtmlString();
    assertHtmlEquals("<someTagName>someTextContent</someTagName>", htmlString);

    htmlString = new HtmlDocumentBuilder(
        new HtmlElementWriter("someTagName"))
          .startElement(new HtmlElementWriter("someChildTag"))
          .endElement()
        .endElement()
    .getHtmlString();
    assertHtmlEquals("<someTagName><someChildTag></someChildTag></someTagName>", htmlString);

    htmlString = new HtmlDocumentBuilder(
        new HtmlElementWriter("someTagName"))
          .startElement(new HtmlElementWriter("someChildTag").textContent("someTextContent"))
          .endElement()
        .endElement()
    .getHtmlString();
    assertHtmlEquals("<someTagName><someChildTag>someTextContent</someChildTag></someTagName>", htmlString);

    htmlString = new HtmlDocumentBuilder(
        new HtmlElementWriter("someTagName").textContent("someTextContent"))
          .startElement(new HtmlElementWriter("someChildTag"))
          .endElement()
        .endElement()
    .getHtmlString();
    assertHtmlEquals("<someTagName><someChildTag></someChildTag>someTextContent</someTagName>", htmlString);

    // invalid usage

    try {
      new HtmlElementWriter("sometagname", true).textContent("sometextcontet");
    } catch (IllegalStateException e) {
      assertThat(e.getMessage()).contains("Self-closing element cannot have text content");
    }

  }

  @Deployment
  @Test
  void testRenderEmptyStartForm() {

    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();

    assertThat(formService.getRenderedStartForm(processDefinition.getId())).isNull();

  }

  @Deployment
  @Test
  void testRenderStartForm() {

    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();

    String renderedForm = (String) formService.getRenderedStartForm(processDefinition.getId());

    String expectedForm = IoUtil.readClasspathResourceAsString("org/operaton/bpm/engine/test/api/form/HtmlFormEngineTest.testRenderStartForm.html");

    assertHtmlEquals(expectedForm, renderedForm);

  }

  @Deployment
  @Test
  void testRenderEnumField() {

    runtimeService.startProcessInstanceByKey("HtmlFormEngineTest.testRenderEnumField");

    Task t = taskService.createTaskQuery()
      .singleResult();

    String renderedForm = (String) formService.getRenderedTaskForm(t.getId());

    String expectedForm = IoUtil.readClasspathResourceAsString("org/operaton/bpm/engine/test/api/form/HtmlFormEngineTest.testRenderEnumField.html");

    assertHtmlEquals(expectedForm, renderedForm);

  }

  @Deployment
  @Test
  void testRenderTaskForm() {

    runtimeService.startProcessInstanceByKey("HtmlFormEngineTest.testRenderTaskForm");

    Task t = taskService.createTaskQuery()
      .singleResult();

    String renderedForm = (String) formService.getRenderedTaskForm(t.getId());

    String expectedForm = IoUtil.readClasspathResourceAsString("org/operaton/bpm/engine/test/api/form/HtmlFormEngineTest.testRenderTaskForm.html");

    assertHtmlEquals(expectedForm, renderedForm);

  }

  @Deployment
  @Test
  void testRenderDateField() {

    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    String renderedForm = (String) formService.getRenderedStartForm(processDefinition.getId());

    String expectedForm = IoUtil.readClasspathResourceAsString("org/operaton/bpm/engine/test/api/form/HtmlFormEngineTest.testRenderDateField.html");

    assertHtmlEquals(expectedForm, renderedForm);

  }

  @Deployment
  @Test
  void testRenderDateFieldWithPattern() {

    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    String renderedForm = (String) formService.getRenderedStartForm(processDefinition.getId());

    String expectedForm = IoUtil.readClasspathResourceAsString("org/operaton/bpm/engine/test/api/form/HtmlFormEngineTest.testRenderDateFieldWithPattern.html");

    assertHtmlEquals(expectedForm, renderedForm);

  }

  @Deployment
  @Test
  void testLegacyFormPropertySupport() {

    runtimeService.startProcessInstanceByKey("HtmlFormEngineTest.testLegacyFormPropertySupport");

    Task t = taskService.createTaskQuery()
      .singleResult();

    String renderedForm = (String) formService.getRenderedTaskForm(t.getId());

    String expectedForm = IoUtil.readClasspathResourceAsString("org/operaton/bpm/engine/test/api/form/HtmlFormEngineTest.testLegacyFormPropertySupport.html");

    assertHtmlEquals(expectedForm, renderedForm);

  }

  @Deployment
  @Test
  void testLegacyFormPropertySupportReadOnly() {

    runtimeService.startProcessInstanceByKey("HtmlFormEngineTest.testLegacyFormPropertySupportReadOnly");

    Task t = taskService.createTaskQuery()
      .singleResult();

    String renderedForm = (String) formService.getRenderedTaskForm(t.getId());

    String expectedForm = IoUtil.readClasspathResourceAsString("org/operaton/bpm/engine/test/api/form/HtmlFormEngineTest.testLegacyFormPropertySupportReadOnly.html");

    assertHtmlEquals(expectedForm, renderedForm);

  }

  @Deployment
  @Test
  void testLegacyFormPropertySupportRequired() {

    runtimeService.startProcessInstanceByKey("HtmlFormEngineTest.testLegacyFormPropertySupportRequired");

    Task t = taskService.createTaskQuery()
      .singleResult();

    String renderedForm = (String) formService.getRenderedTaskForm(t.getId());

    String expectedForm = IoUtil.readClasspathResourceAsString("org/operaton/bpm/engine/test/api/form/HtmlFormEngineTest.testLegacyFormPropertySupportRequired.html");

    assertHtmlEquals(expectedForm, renderedForm);

  }

  @Deployment
  @Test
  void testBusinessKey() {

    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();

    String renderedForm = (String) formService.getRenderedStartForm(processDefinition.getId());

    String expectedForm = IoUtil.readClasspathResourceAsString("org/operaton/bpm/engine/test/api/form/HtmlFormEngineTest.testBusinessKey.html");

    assertHtmlEquals(expectedForm, renderedForm);

  }

  public void assertHtmlEquals(String expected, String actual) {
    assertThat(filterWhitespace(actual)).isEqualTo(filterWhitespace(expected));
  }

  protected String filterWhitespace(String tofilter) {
    return tofilter.replaceAll("\\n", "").replaceAll("\\s", "");
  }

}

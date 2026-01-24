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
package org.operaton.bpm.engine.test.bpmn.scripttask;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.ScriptCompilationException;
import org.operaton.bpm.engine.ScriptEvaluationException;
import org.operaton.bpm.engine.exception.NullValueException;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.commons.utils.CollectionUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

/**
 *
 * @author Daniel Meyer (Javascript)
 * @author Sebastian Menski (Python)
 * @author Nico Rehwaldt (Ruby)
 * @author Christian Lipphardt (Groovy)
 *
 */
class ScriptTaskTest extends AbstractScriptTaskTest {

  private static final String JAVASCRIPT = "javascript";
  private static final String PYTHON = "python";
  private static final String RUBY = "ruby";
  private static final String GROOVY = "groovy";
  private static final String JUEL = "juel";

  @Test
  void testJavascriptProcessVarVisibility() {

    deployProcess(JAVASCRIPT, """
		// GIVEN
		// an execution variable 'foo'
		execution.setVariable('foo', 'a');
		
		// THEN
		// there should be a script variable defined
		if (typeof foo !== 'undefined') {
		  throw 'Variable foo should be defined as script variable.';
		}
		
		// GIVEN
		// a script variable with the same name
		var foo = 'b';
		
		// THEN
		// it should not change the value of the execution variable
		if(execution.getVariable('foo') != 'a') {
		  throw 'Execution should contain variable foo';
		}
		
		// AND
		// it should override the visibility of the execution variable
		if(foo != 'b') {
		  throw 'Script variable must override the visibiltity of the execution variable.';
		}
      """
    );

    // GIVEN
    // that we start an instance of this process
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("testProcess");

    // THEN
    // the script task can be executed without exceptions
    // the execution variable is stored and has the correct value
    Object variableValue = runtimeService.getVariable(pi.getId(), "foo");
    assertThat(variableValue).isEqualTo("a");

  }

  @Test
  void testPythonProcessVarAssignment() {

    deployProcess(PYTHON, """
		# GIVEN
		# an execution variable 'foo'
		execution.setVariable('foo', 'a')
		
		# THEN
		# there should be a script variable defined
		if not foo:
		    raise Exception('Variable foo should be defined as script variable.')
		
		# GIVEN
		# a script variable with the same name
		foo = 'b'
		
		# THEN
		# it should not change the value of the execution variable
		if execution.getVariable('foo') != 'a':
		    raise Exception('Execution should contain variable foo')
		
		# AND
		# it should override the visibility of the execution variable
		if foo != 'b':
		    raise Exception('Script variable must override the visibiltity of the execution variable.')
      """
    );

    // GIVEN
    // that we start an instance of this process
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("testProcess");

    // THEN
    // the script task can be executed without exceptions
    // the execution variable is stored and has the correct value
    Object variableValue = runtimeService.getVariable(pi.getId(), "foo");
    assertThat(variableValue).isEqualTo("a");

  }

  @Test
  void testRubyProcessVarVisibility() {

    deployProcess(RUBY, """
	      # GIVEN
	      # an execution variable 'foo'
	      $execution.setVariable('foo', 'a')
	
	      # THEN
	      # there should NOT be a script variable defined (this is unsupported in Ruby binding)
	      raise 'Variable foo should be defined as script variable.' if !$foo.nil?
	
	      # GIVEN
	      # a script variable with the same name
	      $foo = 'b'
	
	      # THEN
	      # it should not change the value of the execution variable
	      if $execution.getVariable('foo') != 'a'
	        raise 'Execution should contain variable foo'
	      end
	
	      # AND
	      # it should override the visibility of the execution variable
	      if $foo != 'b'
	        raise 'Script variable must override the visibiltity of the execution variable.'
	      end
      """
    );

    // GIVEN
    // that we start an instance of this process
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("testProcess");

    // THEN
    // the script task can be executed without exceptions
    // the execution variable is stored and has the correct value
    Object variableValue = runtimeService.getVariable(pi.getId(), "foo");
    assertThat(variableValue).isEqualTo("a");

  }

  @Test
  void testGroovyProcessVarVisibility() {

    deployProcess(GROOVY, """
        // GIVEN
        // an execution variable 'foo'
        execution.setVariable('foo', 'a')

        // THEN
        // there should be a script variable defined
        if ( !foo ) {
          throw new Exception('Variable foo should be defined as script variable.')
        }

        // GIVEN
        // a script variable with the same name
        foo = 'b'

        // THEN
        // it should not change the value of the execution variable
        if (execution.getVariable('foo') != 'a') {
          throw new Exception('Execution should contain variable foo')
        }

        // AND
        // it should override the visibility of the execution variable
        if (foo != 'b') {
          throw new Exception('Script variable must override the visibiltity of the execution variable.')
        }
      """

    );

    // GIVEN
    // that we start an instance of this process
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("testProcess");

    // THEN
    // the script task can be executed without exceptions
    // the execution variable is stored and has the correct value
    Object variableValue = runtimeService.getVariable(pi.getId(), "foo");
    assertThat(variableValue).isEqualTo("a");

  }

  @Test
  void testJavascriptFunctionInvocation() {

    deployProcess(JAVASCRIPT, """
	      // GIVEN
	      // a function named sum
	      function sum(a,b){
	        return a+b;
	      };
	      
	      // THEN
	      // i can call the function
	      var result = sum(1,2);
	      
	      execution.setVariable('foo', result);
      """
    );

    // GIVEN
    // that we start an instance of this process
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("testProcess");

    // THEN
    // the variable is defined
    Object variable = runtimeService.getVariable(pi.getId(), "foo");
    assertThat(variable).isIn(3, 3.0);

  }

  @Test
  void testPythonFunctionInvocation() {

    deployProcess(PYTHON, """
		# GIVEN
		# a function named sum
		def sum(a, b):
		    return a + b
		
		# THEN
		# i can call the function
		result = sum(1,2)
		execution.setVariable('foo', result)
      """
    );

    // GIVEN
    // that we start an instance of this process
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("testProcess");

    // THEN
    // the variable is defined
    Object variable = runtimeService.getVariable(pi.getId(), "foo");
    assertThat(variable).isIn(3, 3.0);

  }

  @Test
  void testRubyFunctionInvocation() {

    deployProcess(RUBY, """
		# GIVEN
		# a function named sum
		def sum(a, b)
		  return a + b
		end
		
		# THEN
		# i can call the function
		result = sum(1,2)
		
		$execution.setVariable('foo', result)
      """
    );

    // GIVEN
    // that we start an instance of this process
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("testProcess");

    // THEN
    // the variable is defined
    Object variable = runtimeService.getVariable(pi.getId(), "foo");
    assertThat(variable).isEqualTo(3l);

  }

  @Test
  void testGroovyFunctionInvocation() {

    deployProcess(GROOVY, """
		// GIVEN
		// a function named sum
		def sum(a, b) {
		  return a + b
		}
		
		// THEN
		// i can call the function
		result = sum(1,2)
		
		execution.setVariable('foo', result)
      """
    );

    // GIVEN
    // that we start an instance of this process
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("testProcess");

    // THEN
    // the variable is defined
    Object variable = runtimeService.getVariable(pi.getId(), "foo");
    assertThat(variable).isEqualTo(3);

  }

  @Test
  void testJsVariable() {

    String scriptText = "var foo = 1;";

    deployProcess(JAVASCRIPT, scriptText);

    ProcessInstance pi = runtimeService.startProcessInstanceByKey("testProcess");
    Object variableValue = runtimeService.getVariable(pi.getId(), "foo");
    assertThat(variableValue).isNull();

  }

  @Test
  void testPythonVariable() {

    String scriptText = "foo = 1";

    deployProcess(PYTHON, scriptText);

    ProcessInstance pi = runtimeService.startProcessInstanceByKey("testProcess");
    Object variableValue = runtimeService.getVariable(pi.getId(), "foo");
    assertThat(variableValue).isNull();

  }

  @Test
  void testRubyVariable() {

    String scriptText = "foo = 1";

    deployProcess(RUBY, scriptText);

    ProcessInstance pi = runtimeService.startProcessInstanceByKey("testProcess");
    Object variableValue = runtimeService.getVariable(pi.getId(), "foo");
    assertThat(variableValue).isNull();

  }

  @Test
  void testGroovyVariable() {

    String scriptText = "def foo = 1";

    deployProcess(GROOVY, scriptText);

    ProcessInstance pi = runtimeService.startProcessInstanceByKey("testProcess");
    Object variableValue = runtimeService.getVariable(pi.getId(), "foo");
    assertThat(variableValue).isNull();

  }

  @Test
  void testJuelExpression() {
    deployProcess(JUEL, "${execution.setVariable('foo', 'bar')}");

    ProcessInstance pi = runtimeService.startProcessInstanceByKey("testProcess");

    String variableValue = (String) runtimeService.getVariable(pi.getId(), "foo");
    assertThat(variableValue).isEqualTo("bar");
  }

  @Test
  void testJuelCapitalizedExpression() {
    deployProcess(JUEL.toUpperCase(), "${execution.setVariable('foo', 'bar')}");

    ProcessInstance pi = runtimeService.startProcessInstanceByKey("testProcess");

    String variableValue = (String) runtimeService.getVariable(pi.getId(), "foo");
    assertThat(variableValue).isEqualTo("bar");
  }

  @Test
  void testSourceAsExpressionAsVariable() {
    deployProcess(PYTHON, "${scriptSource}");

    Map<String, Object> variables = new HashMap<>();
    variables.put("scriptSource", "execution.setVariable('foo', 'bar')");
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("testProcess", variables);

    String variableValue = (String) runtimeService.getVariable(pi.getId(), "foo");
    assertThat(variableValue).isEqualTo("bar");
  }

  @Test
  void testSourceAsExpressionAsNonExistingVariable() {
    // given
    deployProcess(PYTHON, "${scriptSource}");

    // when/then
    assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("testProcess"))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContainingIgnoringCase("Cannot resolve identifier 'scriptSource'");
  }

  @Test
  void testSourceAsExpressionAsBean() {
    deployProcess(PYTHON, "#{scriptResourceBean.getSource()}");

    Map<String, Object> variables = new HashMap<>();
    variables.put("scriptResourceBean", new ScriptResourceBean());
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("testProcess", variables);

    String variableValue = (String) runtimeService.getVariable(pi.getId(), "foo");
    assertThat(variableValue).isEqualTo("bar");
  }

  @Test
  void testSourceAsExpressionWithWhitespace() {
    deployProcess(PYTHON, "\t\n  \t \n  ${scriptSource}");

    Map<String, Object> variables = new HashMap<>();
    variables.put("scriptSource", "execution.setVariable('foo', 'bar')");
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("testProcess", variables);

    String variableValue = (String) runtimeService.getVariable(pi.getId(), "foo");
    assertThat(variableValue).isEqualTo("bar");
  }

  @Test
  void testJavascriptVariableSerialization() {
    deployProcess(JAVASCRIPT, "execution.setVariable('date', new java.util.Date(0));");

    ProcessInstance pi = runtimeService.startProcessInstanceByKey("testProcess");

    Date date = (Date) runtimeService.getVariable(pi.getId(), "date");
    assertThat(date.getTime()).isZero();

    deployProcess(JAVASCRIPT, "execution.setVariable('myVar', new org.operaton.bpm.engine.test.bpmn.scripttask.MySerializable('test'));");

    pi = runtimeService.startProcessInstanceByKey("testProcess");

    MySerializable myVar = (MySerializable) runtimeService.getVariable(pi.getId(), "myVar");
    assertThat(myVar.getName()).isEqualTo("test");
  }

  @Test
  void testPythonVariableSerialization() {
    deployProcess(PYTHON, "import java.util.Date\nexecution.setVariable('date', java.util.Date(0))");

    ProcessInstance pi = runtimeService.startProcessInstanceByKey("testProcess");

    Date date = (Date) runtimeService.getVariable(pi.getId(), "date");
    assertThat(date.getTime()).isZero();

    deployProcess(PYTHON, """
      import org.operaton.bpm.engine.test.bpmn.scripttask.MySerializable
      execution.setVariable('myVar', org.operaton.bpm.engine.test.bpmn.scripttask.MySerializable('test'));\
      """);

    pi = runtimeService.startProcessInstanceByKey("testProcess");

    MySerializable myVar = (MySerializable) runtimeService.getVariable(pi.getId(), "myVar");
    assertThat(myVar.getName()).isEqualTo("test");
  }

  @Test
  void testRubyVariableSerialization() {
    deployProcess(RUBY, "require 'java'\n$execution.setVariable('date', java.util.Date.new(0))");

    ProcessInstance pi = runtimeService.startProcessInstanceByKey("testProcess");

    Date date = (Date) runtimeService.getVariable(pi.getId(), "date");
    assertThat(date.getTime()).isZero();

    deployProcess(RUBY, "$execution.setVariable('myVar', org.operaton.bpm.engine.test.bpmn.scripttask.MySerializable.new('test'));");

    pi = runtimeService.startProcessInstanceByKey("testProcess");

    MySerializable myVar = (MySerializable) runtimeService.getVariable(pi.getId(), "myVar");
    assertThat(myVar.getName()).isEqualTo("test");
  }

  @Test
  void testGroovyVariableSerialization() {
    deployProcess(GROOVY, "execution.setVariable('date', new java.util.Date(0))");

    ProcessInstance pi = runtimeService.startProcessInstanceByKey("testProcess");

    Date date = (Date) runtimeService.getVariable(pi.getId(), "date");
    assertThat(date.getTime()).isZero();

    deployProcess(GROOVY, "execution.setVariable('myVar', new org.operaton.bpm.engine.test.bpmn.scripttask.MySerializable('test'));");

    pi = runtimeService.startProcessInstanceByKey("testProcess");

    MySerializable myVar = (MySerializable) runtimeService.getVariable(pi.getId(), "myVar");
    assertThat(myVar.getName()).isEqualTo("test");
  }

  @Test
  void testGroovyNotExistingImport() {
    // given
    deployProcess(GROOVY, "import unknown");

    // when/then
    assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("testProcess"))
      .isInstanceOf(ScriptCompilationException.class)
      .hasMessageContainingIgnoringCase("import unknown");
  }

  @Test
  void testGroovyNotExistingImportWithoutCompilation() {
    // disable script compilation
    processEngineConfiguration.setEnableScriptCompilation(false);

    try {
      // given
      deployProcess(GROOVY, "import unknown");

      // when/then
      assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("testProcess"))
        .isInstanceOf(ScriptEvaluationException.class)
        .hasMessageContainingIgnoringCase("import unknown");
    }
    finally {
      // re-enable script compilation
      processEngineConfiguration.setEnableScriptCompilation(true);
    }
  }

  @Test
  void testShouldNotDeployProcessWithMissingScriptElementAndResource() {
    var processBuilder = Bpmn.createExecutableProcess("testProcess")
        .startEvent()
        .scriptTask()
          .scriptFormat(RUBY)
        .userTask()
        .endEvent()
      .done();
    assertThatThrownBy(() -> deployProcess(processBuilder)).isInstanceOf(ProcessEngineException.class);
  }

  @Test
  void testShouldUseJuelAsDefaultScriptLanguage() {
    deployProcess(Bpmn.createExecutableProcess("testProcess")
      .startEvent()
      .scriptTask()
        .scriptText("${true}")
      .userTask()
      .endEvent()
    .done());

    runtimeService.startProcessInstanceByKey("testProcess");

    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task).isNotNull();
  }

  @Test
  void testAutoStoreScriptVarsOff() {
    assertThat(processEngineConfiguration.isAutoStoreScriptVariables()).isFalse();
  }

  @org.operaton.bpm.engine.test.Deployment
  @Test
  void testPreviousTaskShouldNotHandleException(){
    try {
      runtimeService.startProcessInstanceByKey("process");
      fail("");
    }
    // since the NVE extends the ProcessEngineException we have to handle it
    // separately
    catch (NullValueException nve) {
      fail("Shouldn't have received NullValueException");
    } catch (ProcessEngineException e) {
      assertThat(e.getMessage()).contains("Invalid format");
    }
  }

  @org.operaton.bpm.engine.test.Deployment
  @Test
  void testSetScriptResultToProcessVariable() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("echo", "hello");
    variables.put("existingProcessVariableName", "one");

    ProcessInstance pi = runtimeService.startProcessInstanceByKey("setScriptResultToProcessVariable", variables);

    assertThat(runtimeService.getVariable(pi.getId(), "existingProcessVariableName")).isEqualTo("hello");
    assertThat(runtimeService.getVariable(pi.getId(), "newProcessVariableName")).isEqualTo(pi.getId());
  }

  @org.operaton.bpm.engine.test.Deployment
  @Test
  void testGroovyScriptExecution() {
    try {

      processEngineConfiguration.setAutoStoreScriptVariables(true);
      int[] inputArray = new int[] {1, 2, 3, 4, 5};
      ProcessInstance pi = runtimeService.startProcessInstanceByKey("scriptExecution", CollectionUtil.singletonMap("inputArray", inputArray));

      Integer result = (Integer) runtimeService.getVariable(pi.getId(), "sum");
      assertThat(result.intValue()).isEqualTo(15);

    } finally {
      processEngineConfiguration.setAutoStoreScriptVariables(false);
    }
  }

  @org.operaton.bpm.engine.test.Deployment
  @Test
  void testGroovySetVariableThroughExecutionInScript() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("setScriptVariableThroughExecution");

    // Since 'def' is used, the 'scriptVar' will be script local
    // and not automatically stored as a process variable.
    assertThat(runtimeService.getVariable(pi.getId(), "scriptVar")).isNull();
    assertThat(runtimeService.getVariable(pi.getId(), "myVar")).isEqualTo("test123");
  }

  @org.operaton.bpm.engine.test.Deployment
  @Test
  void testScriptEvaluationException() {
    // given
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionKey("Process_1").singleResult();
    var expectedMessage = "Unable to evaluate script while executing activity 'Failing' in the process definition with id '%s'".formatted(processDefinition.getId());

    // when/then
    assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("Process_1"))
      .isInstanceOf(ScriptEvaluationException.class)
      .hasMessageContaining(expectedMessage);
  }

  @Test
  void shouldLoadExternalScriptJavascript() {
    try {
      // GIVEN
      // an external JS file with a function
      // and external file loading being allowed
      processEngineConfiguration.setEnableScriptEngineLoadExternalResources(true);

      deployProcess(JAVASCRIPT,
          // WHEN
          // we load a function from an external file
          "load(\"" + getNormalizedResourcePath("/org/operaton/bpm/engine/test/bpmn/scripttask/sum.js") + "\");"
          // THEN
          // we can use that function
        + "execution.setVariable('foo', sum(3, 4));"
      );

      // WHEN
      // we start an instance of this process
      ProcessInstance pi = runtimeService.startProcessInstanceByKey("testProcess");

      // THEN
      // the script task can be executed without exceptions
      // the execution variable is stored and has the correct value
      Object variableValue = runtimeService.getVariable(pi.getId(), "foo");
      assertThat(variableValue).isEqualTo(7);
    } finally {
      processEngineConfiguration.setEnableScriptEngineLoadExternalResources(false);
    }
  }

  @Test
  void shouldFailOnLoadExternalScriptJavascriptIfNotEnabled() {
    // GIVEN
    // an external JS file with a function
    deployProcess(JAVASCRIPT,
        // WHEN
        // we load a function from an external file
        "load(\"" + getNormalizedResourcePath("/org/operaton/bpm/engine/test/bpmn/scripttask/sum.js") + "\");"
    );

    // WHEN
    // we start an instance of this process
    assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("testProcess"))
    // THEN
    // this is not allowed in the JS ScriptEngine
      .isInstanceOf(ScriptEvaluationException.class)
      .hasMessageContaining("Operation is not allowed");
  }

}

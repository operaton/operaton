/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.dmn.feel.impl.juel;

import org.operaton.bpm.dmn.feel.impl.FeelException;

public class FeelEngineLogger extends FeelLogger {

    /**
   * Creates a FeelSyntaxException with the given id, feelExpression, and description.
   * 
   * @param id the id of the exception
   * @param feelExpression the feel expression causing the exception
   * @param description the description of the exception
   * @return a new FeelSyntaxException object
   */
  protected FeelSyntaxException syntaxException(String id, String feelExpression, String description) {
    return new FeelSyntaxException(syntaxExceptionMessage(id, feelExpression, description), feelExpression, description);
  }

    /**
   * Constructs a FeelSyntaxException with the provided parameters.
   * 
   * @param id the ID of the syntax exception
   * @param feelExpression the FEEL expression causing the exception
   * @param description a description of the syntax exception
   * @param cause the cause of the syntax exception
   * @return a new FeelSyntaxException object
   */
  protected FeelSyntaxException syntaxException(String id, String feelExpression, String description, Throwable cause) {
    return new FeelSyntaxException(syntaxExceptionMessage(id, feelExpression, description), feelExpression, description, cause);
  }

    /**
   * Generates a syntax error message for a given FEEL expression with or without a description.
   *
   * @param id the identifier of the syntax error
   * @param feelExpression the FEEL expression causing the syntax error
   * @param description the description of the syntax error (optional)
   * @return the generated syntax error message
   */
  protected String syntaxExceptionMessage(String id, String feelExpression, String description) {
    if (description != null) {
      return exceptionMessage(
        id,
        "Syntax error in expression '{}': {}", feelExpression, description
      );
    }
    else {
      return exceptionMessage(
        id, "Syntax error in expression '{}'", feelExpression
      );
    }
  }

    /**
   * Creates a FeelSyntaxException for an invalid 'not' expression with the given FEEL expression.
   *
   * @param feelExpression the invalid FEEL expression
   * @return the FeelSyntaxException with error code "001", the invalid expression, and a description indicating the correct format
   */
  public FeelSyntaxException invalidNotExpression(String feelExpression) {
    String description = "Expression should have format 'not(...)'";
    return syntaxException("001", feelExpression, description);
  }

    /**
   * Generates a FeelSyntaxException with a descriptive message for an invalid interval expression.
   *
   * @param feelExpression the invalid FEEL expression
   * @return the generated FeelSyntaxException
   */
  public FeelSyntaxException invalidIntervalExpression(String feelExpression) {
    String description = "Expression should have format '[|(|] endpoint .. endpoint ]|)|['";
    return syntaxException("002", feelExpression, description);
  }

    /**
   * Generates FeelSyntaxException with specified error code, feel expression, and description for invalid comparison expression
   *
   * @param feelExpression the invalid comparison expression
   * @return FeelSyntaxException with error code "003", feel expression, and description
   */
  public FeelSyntaxException invalidComparisonExpression(String feelExpression) {
    String description = "Expression should have format '<=|<|>=|> endpoint'";
    return syntaxException("003", feelExpression, description);
  }

    /**
   * Creates a new FeelException with a message indicating that the variable mapper is read only.
   * 
   * @return a FeelException with the specified message
   */
  public FeelException variableMapperIsReadOnly() {
    return new FeelException(exceptionMessage(
      "004",
      "The variable mapper is read only.")
    );
  }

    /**
   * Creates a FeelException with a specific error message if a method with the given name and parameter types cannot be found.
   * 
   * @param cause the NoSuchMethodException that caused the error
   * @param name the name of the method that could not be found
   * @param parameterTypes the parameter types of the method that could not be found
   * @return a FeelException containing the error message and the cause
   */
  public FeelException unableToFindMethod(NoSuchMethodException cause, String name, Class<?>... parameterTypes) {
    return new FeelException(exceptionMessage(
      "005",
      "Unable to find method '{}' with parameter types '{}'", name, parameterTypes),
      cause
    );
  }

    /**
   * Constructs a FeelMissingFunctionException with a message indicating that a function with the given prefix and localName could not be resolved.
   * 
   * @param prefix the prefix of the function
   * @param localName the local name of the function
   * @return a FeelMissingFunctionException with the appropriate message and function
   */
  public FeelMissingFunctionException unknownFunction(String prefix, String localName) {
    String function = localName;
    if (prefix != null && !prefix.isEmpty()) {
      function = prefix + ":" + localName;
    }
    return new FeelMissingFunctionException(exceptionMessage(
      "006",
      "Unable to resolve function '{}'", function),
      function
    );
  }

    /**
   * Creates a new FeelMissingFunctionException with a specified message and function.
   * 
   * @param feelExpression the FEEL expression where the function could not be resolved
   * @param cause the FeelMissingFunctionException that caused this exception
   * @return a new FeelMissingFunctionException
   */
  public FeelMissingFunctionException unknownFunction(String feelExpression, FeelMissingFunctionException cause) {
    String function = cause.getFunction();
    return new FeelMissingFunctionException(exceptionMessage(
      "007",
      "Unable to resolve function '{}' in expression '{}'" , function, feelExpression),
      function
    );
  }

    /**
   * Creates a FeelMissingVariableException with a specific exception message and variable name.
   * 
   * @param variable the name of the unknown variable
   * @return FeelMissingVariableException with the formatted exception message and variable name
   */
  public FeelMissingVariableException unknownVariable(String variable) {
    return new FeelMissingVariableException(exceptionMessage(
      "008",
      "Unable to resolve variable '{}'", variable),
      variable
    );
  }

    /**
   * Creates a new FeelMissingVariableException with a customized exception message 
   * based on the given feelExpression and the variable from the provided cause exception.
   * 
   * @param feelExpression the FEEL expression where the variable is missing
   * @param cause the FeelMissingVariableException that contains the missing variable
   * @return a new FeelMissingVariableException with a custom exception message
   */
  public FeelMissingVariableException unknownVariable(String feelExpression, FeelMissingVariableException cause) {
    String variable = cause.getVariable();
    return new FeelMissingVariableException(exceptionMessage(
      "009",
      "Unable to resolve variable '{}' in expression '{}'", variable, feelExpression),
      variable
    );
  }

    /**
   * Create a FeelSyntaxException with error code "010" and a given feel expression and cause.
   * 
   * @param feelExpression the feel expression causing the exception
   * @param cause the cause of the exception
   * @return a FeelSyntaxException with error code "010", feel expression, and cause
   */
  public FeelSyntaxException invalidExpression(String feelExpression, Throwable cause) {
    return syntaxException("010", feelExpression, null, cause);
  }

    /**
   * Constructs a new FeelException with a specific error code and message for inability to initialize FEEL engine
   * 
   * @param cause the cause of the exception
   * @return a FeelException with the specified error code and message
   */
  public FeelException unableToInitializeFeelEngine(Throwable cause) {
    return new FeelException(exceptionMessage(
      "011",
      "Unable to initialize FEEL engine"),
      cause
    );
  }

    /**
   * Constructs a FeelException with a message indicating that the given expression could not be evaluated.
   *
   * @param simpleUnaryTests The expression that could not be evaluated
   * @param cause The cause of the evaluation failure
   * @return A FeelException representing the evaluation failure
   */
  public FeelException unableToEvaluateExpression(String simpleUnaryTests, Throwable cause) {
    return new FeelException(exceptionMessage(
      "012",
      "Unable to evaluate expression '{}'", simpleUnaryTests),
      cause
    );
  }

    /**
   * Constructs a FeelConvertException with a specific exception message based on the provided value and types.
   * 
   * @param value the value that failed to be converted
   * @param type the target type for the conversion
   * @return a FeelConvertException with the constructed exception message
   */
  public FeelConvertException unableToConvertValue(Object value, Class<?> type) {
    return new FeelConvertException(exceptionMessage(
      "013",
      "Unable to convert value '{}' of type '{}' to type '{}'", value, value.getClass(), type),
      value, type
    );
  }

    /**
   * Constructs a FeelConvertException with a specific error message when unable to convert a value to a specified type.
   *
   * @param value the value that could not be converted
   * @param type the type the value could not be converted to
   * @param cause the cause of the exception
   * @return a FeelConvertException with the specified error message and details
   */
  public FeelConvertException unableToConvertValue(Object value, Class<?> type, Throwable cause) {
    return new FeelConvertException(exceptionMessage(
      "014",
      "Unable to convert value '{}' of type '{}' to type '{}'", value, value.getClass(), type),
      value, type, cause
    );
  }

    /**
   * Constructs a new FeelConvertException with a customized exception message
   * stating the inability to convert a given value to a specified type in a given expression.
   *
   * @param feelExpression the FEEL expression where the conversion failed
   * @param cause the original FeelConvertException that caused the inability to convert
   * @return a new FeelConvertException with the customized exception message
   */
  public FeelConvertException unableToConvertValue(String feelExpression, FeelConvertException cause) {
    Object value = cause.getValue();
    Class<?> type = cause.getType();
    return new FeelConvertException(exceptionMessage(
      "015",
      "Unable to convert value '{}' of type '{}' to type '{}' in expression '{}'", value, value.getClass(), type, feelExpression),
      cause
    );
  }

    /**
   * Constructs and returns an UnsupportedOperationException with a specific exception message.
   * 
   * @return UnsupportedOperationException with exception message indicating that simple expressions are not supported by the FEEL engine
   */
  public UnsupportedOperationException simpleExpressionNotSupported() {
    return new UnsupportedOperationException(exceptionMessage(
      "016",
      "Simple Expression not supported by FEEL engine")
    );
  }

    /**
   * Creates a FeelException with a specific message indicating that the expression cannot be evaluated due to missing input.
   *
   * @param simpleUnaryTests the unary test expression that could not be evaluated
   * @param e the FeelMissingVariableException that caused the evaluation failure
   * @return a new FeelException with the error message and the original exception
   */
  public FeelException unableToEvaluateExpressionAsNotInputIsSet(String simpleUnaryTests, FeelMissingVariableException e) {
    return new FeelException(exceptionMessage(
      "017",
      "Unable to evaluate expression '{}' as no input is set. Maybe the inputExpression is missing or empty.", simpleUnaryTests),
      e
    );
  }

    /**
   * Creates a FeelMethodInvocationException with a message indicating that the provided date and time format is invalid.
   * 
   * @param dateTimeString the string representing the invalid date and time format
   * @param cause the cause of the exception
   * @return a FeelMethodInvocationException with the appropriate message and details
   */
  public FeelMethodInvocationException invalidDateAndTimeFormat(String dateTimeString, Throwable cause) {
    return new FeelMethodInvocationException(exceptionMessage(
      "018",
      "Invalid date and time format in '{}'", dateTimeString),
      cause, "date and time", dateTimeString
    );
  }

    /**
   * Constructs a new FeelMethodInvocationException with a formatted exception message based on the given method, parameters, and simpleUnaryTests.
   * 
   * @param simpleUnaryTests The simple unary tests expression
   * @param cause The original FeelMethodInvocationException cause
   * @return a new FeelMethodInvocationException instance
   */
  public FeelMethodInvocationException unableToInvokeMethod(String simpleUnaryTests, FeelMethodInvocationException cause) {
    String method = cause.getMethod();
    String[] parameters = cause.getParameters();
    return new FeelMethodInvocationException(exceptionMessage(
      "019",
      "Unable to invoke method '{}' with parameters '{}' in expression '{}'", method, parameters, simpleUnaryTests),
      cause.getCause(), method, parameters
    );
  }

    /**
   * Creates a FeelSyntaxException with error code "020" for an invalid list expression 
   * containing empty elements.
   * 
   * @param feelExpression the invalid list expression
   * @return the FeelSyntaxException object with error code "020"
   */
  public FeelSyntaxException invalidListExpression(String feelExpression) {
    String description = "List expression can not have empty elements";
    return syntaxException("020", feelExpression, description);
  }

}

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
package org.operaton.bpm.dmn.engine.impl.transform;

import org.operaton.bpm.dmn.engine.impl.DefaultDmnEngineConfiguration;
import org.operaton.bpm.dmn.engine.impl.spi.transform.DmnElementTransformContext;
import org.operaton.bpm.dmn.engine.impl.spi.type.DmnDataTypeTransformer;
import org.operaton.bpm.dmn.engine.impl.spi.type.DmnTypeDefinition;
import org.operaton.bpm.dmn.engine.impl.type.DefaultTypeDefinition;
import org.operaton.bpm.dmn.engine.impl.type.DmnTypeDefinitionImpl;
import org.operaton.bpm.model.dmn.instance.InformationItem;
import org.operaton.bpm.model.dmn.instance.LiteralExpression;
import org.operaton.bpm.model.dmn.instance.Text;
import org.operaton.bpm.model.dmn.instance.UnaryTests;

public class DmnExpressionTransformHelper {

    /**
   * Creates a DMN type definition based on the provided context and literal expression.
   * 
   * @param context the context for transforming DMN elements
   * @param expression the literal expression to create the type definition from
   * @return the created DMN type definition
   */
  public static DmnTypeDefinition createTypeDefinition(DmnElementTransformContext context, LiteralExpression expression) {
    return createTypeDefinition(context, expression.getTypeRef());
  }

    /**
   * Creates a type definition using the given context and information item.
   * 
   * @param context the context for element transformation
   * @param informationItem the information item to create the type definition from
   * @return the created type definition
   */
  public static DmnTypeDefinition createTypeDefinition(DmnElementTransformContext context, InformationItem informationItem) {
    return createTypeDefinition(context, informationItem.getTypeRef());
  }

    /**
   * Creates a type definition based on the provided type reference.
   * 
   * @param context the context for transforming DMN elements
   * @param typeRef the type reference for the type definition
   * @return the created type definition
   */
  protected static DmnTypeDefinition createTypeDefinition(DmnElementTransformContext context, String typeRef) {
    if (typeRef != null) {
      DmnDataTypeTransformer transformer = context.getDataTypeTransformerRegistry().getTransformer(typeRef);
      return new DmnTypeDefinitionImpl(typeRef, transformer);
    }
    else {
      return new DefaultTypeDefinition();
    }
  }

    /**
   * Returns the expression language of a given literal expression using the provided context.
   * 
   * @param context the context used for transforming the DMN element
   * @param expression the literal expression to retrieve the expression language from
   * @return the expression language of the literal expression
   */
  public static String getExpressionLanguage(DmnElementTransformContext context, LiteralExpression expression) {
    return getExpressionLanguage(context, expression.getExpressionLanguage());
  }

    /**
   * Retrieves the expression language from the provided DMN element transform context and unary tests expression.
   *
   * @param context the context used for transforming DMN elements
   * @param expression the unary tests expression to retrieve expression language from
   * @return the expression language associated with the unary tests expression
   */
  public static String getExpressionLanguage(DmnElementTransformContext context, UnaryTests expression) {
    return getExpressionLanguage(context, expression.getExpressionLanguage());
  }

    /**
   * Returns the expression language to be used, either from the provided expressionLanguage string or the global expression language from the context.
   *
   * @param context the context in which the method is being called
   * @param expressionLanguage the expression language specified in the DMN element
   * @return the expression language to be used
   */
  protected static String getExpressionLanguage(DmnElementTransformContext context, String expressionLanguage) {
    if (expressionLanguage != null) {
      return expressionLanguage;
    }
    else {
      return getGlobalExpressionLanguage(context);
    }
  }

    /**
   * Retrieves the global expression language from the provided DMN element transform context.
   * If the expression language is not one of the default FEEL expression languages, returns the expression language;
   * otherwise, returns null.
   * 
   * @param context the DMN element transform context
   * @return the global expression language or null
   */
  protected static String getGlobalExpressionLanguage(DmnElementTransformContext context) {
    String expressionLanguage = context.getModelInstance().getDefinitions().getExpressionLanguage();
    if (!DefaultDmnEngineConfiguration.FEEL_EXPRESSION_LANGUAGE.equals(expressionLanguage) &&
        !DefaultDmnEngineConfiguration.FEEL_EXPRESSION_LANGUAGE_DMN12.equals(expressionLanguage) &&
        !DefaultDmnEngineConfiguration.FEEL_EXPRESSION_LANGUAGE_DMN13.equals(expressionLanguage) &&
        !DefaultDmnEngineConfiguration.FEEL_EXPRESSION_LANGUAGE_DMN14.equals(expressionLanguage) &&
        !DefaultDmnEngineConfiguration.FEEL_EXPRESSION_LANGUAGE_DMN15.equals(expressionLanguage)) {
      return expressionLanguage;
    }
    else {
      return null;
    }
  }

    /**
   * Returns the text of the given LiteralExpression.
   * 
   * @param expression the LiteralExpression object
   * @return the text of the LiteralExpression
   */
  public static String getExpression(LiteralExpression expression) {
    return getExpression(expression.getText());
  }

    /**
   * Returns the expression text from the given UnaryTests object.
   * 
   * @param expression the UnaryTests object containing the expression text
   * @return the expression text
   */
  public static String getExpression(UnaryTests expression) {
    return getExpression(expression.getText());
  }

    /**
   * Returns the text content of the given Text object.
   * 
   * @param text the Text object to extract the text content from
   * @return the text content of the Text object, or null if the Text object is null or empty
   */
  protected static String getExpression(Text text) {
    if (text != null) {
      String textContent = text.getTextContent();
      if (textContent != null && !textContent.isEmpty()) {
        return textContent;
      }
    }
    return null;
  }


}

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
package org.operaton.bpm.model.cmmn.impl.instance.operaton;

import org.operaton.bpm.model.cmmn.impl.instance.CmmnModelElementInstanceImpl;
import org.operaton.bpm.model.cmmn.instance.operaton.OperatonOut;
import org.operaton.bpm.model.xml.ModelBuilder;
import org.operaton.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder;
import org.operaton.bpm.model.xml.type.attribute.Attribute;

import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.CAMUNDA_NS;
import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.OPERATON_ATTRIBUTE_SOURCE;
import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.OPERATON_ATTRIBUTE_SOURCE_EXPRESSION;
import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.OPERATON_ATTRIBUTE_TARGET;
import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.OPERATON_ATTRIBUTE_VARIABLES;
import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.OPERATON_ELEMENT_OUT;

/**
 * @author Sebastian Menski
 * @author Roman Smirnov
 *
 */
public class OperatonOutImpl extends CmmnModelElementInstanceImpl implements OperatonOut {

  protected static Attribute<String> operatonSourceAttribute;
  protected static Attribute<String> operatonSourceExpressionAttribute;
  protected static Attribute<String> operatonVariablesAttribute;
  protected static Attribute<String> operatonTargetAttribute;

  public static void registerType(ModelBuilder modelBuilder) {
    ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(OperatonOut.class, OPERATON_ELEMENT_OUT)
      .namespaceUri(CAMUNDA_NS)
      .instanceProvider(OperatonOutImpl::new);

    operatonSourceAttribute = typeBuilder.stringAttribute(OPERATON_ATTRIBUTE_SOURCE)
      .namespace(CAMUNDA_NS)
      .build();

    operatonSourceExpressionAttribute = typeBuilder.stringAttribute(OPERATON_ATTRIBUTE_SOURCE_EXPRESSION)
      .namespace(CAMUNDA_NS)
      .build();

    operatonVariablesAttribute = typeBuilder.stringAttribute(OPERATON_ATTRIBUTE_VARIABLES)
      .namespace(CAMUNDA_NS)
      .build();

    operatonTargetAttribute = typeBuilder.stringAttribute(OPERATON_ATTRIBUTE_TARGET)
      .namespace(CAMUNDA_NS)
      .build();

    typeBuilder.build();
  }

  public OperatonOutImpl(ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  @Override
  public String getOperatonSource() {
    return operatonSourceAttribute.getValue(this);
  }

  @Override
  public void setOperatonSource(String operatonSource) {
    operatonSourceAttribute.setValue(this, operatonSource);
  }

  @Override
  public String getOperatonSourceExpression() {
    return operatonSourceExpressionAttribute.getValue(this);
  }

  @Override
  public void setOperatonSourceExpression(String operatonSourceExpression) {
    operatonSourceExpressionAttribute.setValue(this, operatonSourceExpression);
  }

  @Override
  public String getOperatonVariables() {
    return operatonVariablesAttribute.getValue(this);
  }

  @Override
  public void setOperatonVariables(String operatonVariables) {
    operatonVariablesAttribute.setValue(this, operatonVariables);
  }

  @Override
  public String getOperatonTarget() {
    return operatonTargetAttribute.getValue(this);
  }

  @Override
  public void setOperatonTarget(String operatonTarget) {
    operatonTargetAttribute.setValue(this, operatonTarget);
  }

}

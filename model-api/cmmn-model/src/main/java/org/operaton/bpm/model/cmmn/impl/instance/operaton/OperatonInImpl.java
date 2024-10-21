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
package org.operaton.bpm.model.cmmn.impl.instance.operaton;

import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.CAMUNDA_ATTRIBUTE_BUSINESS_KEY;
import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.CAMUNDA_ATTRIBUTE_LOCAL;
import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.CAMUNDA_ATTRIBUTE_SOURCE;
import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.CAMUNDA_ATTRIBUTE_SOURCE_EXPRESSION;
import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.CAMUNDA_ATTRIBUTE_TARGET;
import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.CAMUNDA_ATTRIBUTE_VARIABLES;
import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.CAMUNDA_ELEMENT_IN;
import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.CAMUNDA_NS;

import org.operaton.bpm.model.cmmn.impl.instance.CmmnModelElementInstanceImpl;
import org.operaton.bpm.model.cmmn.instance.operaton.OperatonIn;
import org.operaton.bpm.model.xml.ModelBuilder;
import org.operaton.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;
import org.operaton.bpm.model.xml.type.attribute.Attribute;

/**
 * @author Sebastian Menski
 * @author Roman Smirnov
 *
 */
public class OperatonInImpl extends CmmnModelElementInstanceImpl implements OperatonIn {

  protected static Attribute<String> operatonSourceAttribute;
  protected static Attribute<String> operatonSourceExpressionAttribute;
  protected static Attribute<String> operatonVariablesAttribute;
  protected static Attribute<String> operatonTargetAttribute;
  protected static Attribute<String> operatonBusinessKeyAttribute;
  protected static Attribute<Boolean> operatonLocalAttribute;

  public static void registerType(ModelBuilder modelBuilder) {
    ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(OperatonIn.class, CAMUNDA_ELEMENT_IN)
      .namespaceUri(CAMUNDA_NS)
      .instanceProvider(new ModelTypeInstanceProvider<OperatonIn>() {
        public OperatonIn newInstance(ModelTypeInstanceContext instanceContext) {
          return new OperatonInImpl(instanceContext);
        }
      });

    operatonSourceAttribute = typeBuilder.stringAttribute(CAMUNDA_ATTRIBUTE_SOURCE)
      .namespace(CAMUNDA_NS)
      .build();

    operatonSourceExpressionAttribute = typeBuilder.stringAttribute(CAMUNDA_ATTRIBUTE_SOURCE_EXPRESSION)
      .namespace(CAMUNDA_NS)
      .build();

    operatonVariablesAttribute = typeBuilder.stringAttribute(CAMUNDA_ATTRIBUTE_VARIABLES)
      .namespace(CAMUNDA_NS)
      .build();

    operatonTargetAttribute = typeBuilder.stringAttribute(CAMUNDA_ATTRIBUTE_TARGET)
      .namespace(CAMUNDA_NS)
      .build();

    operatonBusinessKeyAttribute = typeBuilder.stringAttribute(CAMUNDA_ATTRIBUTE_BUSINESS_KEY)
      .namespace(CAMUNDA_NS)
      .build();

    operatonLocalAttribute = typeBuilder.booleanAttribute(CAMUNDA_ATTRIBUTE_LOCAL)
      .namespace(CAMUNDA_NS)
      .build();

    typeBuilder.build();
  }

  public OperatonInImpl(ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  public String getOperatonSource() {
    return operatonSourceAttribute.getValue(this);
  }

  public void setOperatonSource(String operatonSource) {
    operatonSourceAttribute.setValue(this, operatonSource);
  }

  public String getOperatonSourceExpression() {
    return operatonSourceExpressionAttribute.getValue(this);
  }

  public void setOperatonSourceExpression(String operatonSourceExpression) {
    operatonSourceExpressionAttribute.setValue(this, operatonSourceExpression);
  }

  public String getOperatonVariables() {
    return operatonVariablesAttribute.getValue(this);
  }

  public void setOperatonVariables(String operatonVariables) {
    operatonVariablesAttribute.setValue(this, operatonVariables);
  }

  public String getOperatonTarget() {
    return operatonTargetAttribute.getValue(this);
  }

  public void setOperatonTarget(String operatonTarget) {
    operatonTargetAttribute.setValue(this, operatonTarget);
  }

  public String getOperatonBusinessKey() {
    return operatonBusinessKeyAttribute.getValue(this);
  }

  public void setOperatonBusinessKey(String operatonBusinessKey) {
    operatonBusinessKeyAttribute.setValue(this, operatonBusinessKey);
  }

  public boolean getOperatonLocal() {
    return operatonLocalAttribute.getValue(this);
  }

  public void setOperatonLocal(boolean local) {
    operatonLocalAttribute.setValue(this, local);
  }

}
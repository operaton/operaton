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
package org.operaton.bpm.model.cmmn.impl.instance;

import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.CMMN11_NS;
import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.CMMN_ELEMENT_PLAN_ITEM_CONTROL;

import org.operaton.bpm.model.cmmn.instance.CmmnElement;
import org.operaton.bpm.model.cmmn.instance.ManualActivationRule;
import org.operaton.bpm.model.cmmn.instance.PlanItemControl;
import org.operaton.bpm.model.cmmn.instance.RepetitionRule;
import org.operaton.bpm.model.cmmn.instance.RequiredRule;
import org.operaton.bpm.model.xml.ModelBuilder;
import org.operaton.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder;
import org.operaton.bpm.model.xml.type.child.ChildElement;
import org.operaton.bpm.model.xml.type.child.SequenceBuilder;

/**
 * @author Roman Smirnov
 *
 */
public class PlanItemControlImpl extends CmmnElementImpl implements PlanItemControl {

  protected static ChildElement<RepetitionRule> repetitionRuleChild;
  protected static ChildElement<RequiredRule> requiredRuleChild;
  protected static ChildElement<ManualActivationRule> manualActivationRuleChild;

  public PlanItemControlImpl(ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  @Override
  public RepetitionRule getRepetitionRule() {
    return repetitionRuleChild.getChild(this);
  }

  @Override
  public void setRepetitionRule(RepetitionRule repetitionRule) {
    repetitionRuleChild.setChild(this, repetitionRule);
  }

  @Override
  public RequiredRule getRequiredRule() {
    return requiredRuleChild.getChild(this);
  }

  @Override
  public void setRequiredRule(RequiredRule requiredRule) {
    requiredRuleChild.setChild(this, requiredRule);
  }

  @Override
  public ManualActivationRule getManualActivationRule() {
    return manualActivationRuleChild.getChild(this);
  }

  @Override
  public void setManualActivationRule(ManualActivationRule manualActivationRule) {
    manualActivationRuleChild.setChild(this, manualActivationRule);
  }

  public static void registerType(ModelBuilder modelBuilder) {
    ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(PlanItemControl.class, CMMN_ELEMENT_PLAN_ITEM_CONTROL)
        .namespaceUri(CMMN11_NS)
        .extendsType(CmmnElement.class)
        .instanceProvider(PlanItemControlImpl::new);

    SequenceBuilder sequenceBuilder = typeBuilder.sequence();

    repetitionRuleChild = sequenceBuilder.element(RepetitionRule.class)
        .build();

    requiredRuleChild = sequenceBuilder.element(RequiredRule.class)
        .build();

    manualActivationRuleChild = sequenceBuilder.element(ManualActivationRule.class)
        .build();

    typeBuilder.build();
  }

}

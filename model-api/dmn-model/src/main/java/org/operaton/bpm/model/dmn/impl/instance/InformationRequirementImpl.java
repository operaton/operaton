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
package org.operaton.bpm.model.dmn.impl.instance;

import static org.operaton.bpm.model.dmn.impl.DmnModelConstants.LATEST_DMN_NS;
import static org.operaton.bpm.model.dmn.impl.DmnModelConstants.DMN_ELEMENT_INFORMATION_REQUIREMENT;

import org.operaton.bpm.model.dmn.instance.Decision;
import org.operaton.bpm.model.dmn.instance.InformationRequirement;
import org.operaton.bpm.model.dmn.instance.InputData;
import org.operaton.bpm.model.dmn.instance.RequiredDecisionReference;
import org.operaton.bpm.model.dmn.instance.RequiredInputReference;
import org.operaton.bpm.model.xml.ModelBuilder;
import org.operaton.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder;
import org.operaton.bpm.model.xml.type.child.SequenceBuilder;
import org.operaton.bpm.model.xml.type.reference.ElementReference;

public class InformationRequirementImpl extends DmnModelElementInstanceImpl implements InformationRequirement {

  protected static ElementReference<Decision, RequiredDecisionReference> requiredDecisionRef;
  protected static ElementReference<InputData, RequiredInputReference> requiredInputRef;

  public InformationRequirementImpl(ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  @Override
  public Decision getRequiredDecision() {
    return requiredDecisionRef.getReferenceTargetElement(this);
  }

  @Override
  public void setRequiredDecision(Decision requiredDecision) {
    requiredDecisionRef.setReferenceTargetElement(this, requiredDecision);
  }

  @Override
  public InputData getRequiredInput() {
    return requiredInputRef.getReferenceTargetElement(this);
  }

  @Override
  public void setRequiredInput(InputData requiredInput) {
    requiredInputRef.setReferenceTargetElement(this, requiredInput);
  }

  public static void registerType(ModelBuilder modelBuilder) {
    ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(InformationRequirement.class, DMN_ELEMENT_INFORMATION_REQUIREMENT)
      .namespaceUri(LATEST_DMN_NS)
      .instanceProvider(InformationRequirementImpl::new);

    SequenceBuilder sequenceBuilder = typeBuilder.sequence();

    requiredDecisionRef = sequenceBuilder.element(RequiredDecisionReference.class)
      .uriElementReference(Decision.class)
      .build();

    requiredInputRef = sequenceBuilder.element(RequiredInputReference.class)
      .uriElementReference(InputData.class)
      .build();

    typeBuilder.build();
  }

}

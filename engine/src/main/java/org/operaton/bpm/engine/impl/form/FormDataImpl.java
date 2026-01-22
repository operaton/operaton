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
package org.operaton.bpm.engine.impl.form;

import java.util.ArrayList;
import java.util.List;

import org.operaton.bpm.engine.form.FormData;
import org.operaton.bpm.engine.form.FormField;
import org.operaton.bpm.engine.form.FormProperty;
import org.operaton.bpm.engine.form.OperatonFormRef;

/**
 * @author Tom Baeyens
 * @author Daniel Meyer
 */
public abstract class FormDataImpl implements FormData {

  protected String formKey;
  protected OperatonFormRef operatonFormRef;
  protected String deploymentId;
  protected List<FormProperty> formProperties = new ArrayList<>();

  protected List<FormField> formFields = new ArrayList<>();

  // getters and setters //////////////////////////////////////////////////////

  @Override
  public String getFormKey() {
    return formKey;
  }

  public void setFormKey(String formKey) {
    this.formKey = formKey;
  }

  @Override
  public OperatonFormRef getOperatonFormRef() {
    return operatonFormRef;
  }

  public void setOperatonFormRef(OperatonFormRef operatonFormRef) {
    this.operatonFormRef = operatonFormRef;
  }

  @Override
  public String getDeploymentId() {
    return deploymentId;
  }

  public void setDeploymentId(String deploymentId) {
    this.deploymentId = deploymentId;
  }

  @Override
  public List<FormProperty> getFormProperties() {
    return formProperties;
  }

  public void setFormProperties(List<FormProperty> formProperties) {
    this.formProperties = formProperties;
  }

  @Override
  public List<FormField> getFormFields() {
    return formFields;
  }

  public void setFormFields(List<FormField> formFields) {
    this.formFields = formFields;
  }
}

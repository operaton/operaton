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
package org.operaton.bpm.engine.impl.form.validator;

import org.operaton.bpm.engine.ProcessEngineException;

/**
 * @author Daniel Meyer
 *
 */
public abstract class AbstractTextValueValidator implements FormFieldValidator {

  @Override
  public boolean validate(Object submittedValue, FormFieldValidatorContext validatorContext) {

    if(submittedValue == null) {
      return isNullValid();
    }

    String configuration = validatorContext.getConfiguration();

    if(submittedValue instanceof String string) {
      return validate(string, configuration);
    }

    throw new ProcessEngineException("String validator "+getClass().getSimpleName()+" cannot be used on non-string value of type "+submittedValue.getClass());
  }

  protected abstract boolean validate(String submittedValue, String configuration);

  protected boolean isNullValid() {
    return true;
  }

}

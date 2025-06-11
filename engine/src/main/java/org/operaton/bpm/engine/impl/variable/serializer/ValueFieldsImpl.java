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
package org.operaton.bpm.engine.impl.variable.serializer;

/**
 * @author Thorben Lindhauer
 *
 */
public class ValueFieldsImpl implements ValueFields {

  protected String text;
  protected String text2;
  protected Long longValue;
  protected Double doubleValue;
  protected byte[] byteArrayValue;

  @Override
  public String getName() {
    return null;
  }

  @Override
  public String getTextValue() {
    return text;
  }

  @Override
  public void setTextValue(String textValue) {
    this.text = textValue;
  }

  @Override
  public String getTextValue2() {
    return text2;
  }

  @Override
  public void setTextValue2(String textValue2) {
    this.text2 = textValue2;
  }

  @Override
  public Long getLongValue() {
    return longValue;
  }

  @Override
  public void setLongValue(Long longValue) {
    this.longValue = longValue;
  }

  @Override
  public Double getDoubleValue() {
    return doubleValue;
  }

  @Override
  public void setDoubleValue(Double doubleValue) {
    this.doubleValue = doubleValue;
  }

  @Override
  public byte[] getByteArrayValue() {
    return byteArrayValue;
  }

  @Override
  public void setByteArrayValue(byte[] bytes) {
    this.byteArrayValue = bytes;
  }

}

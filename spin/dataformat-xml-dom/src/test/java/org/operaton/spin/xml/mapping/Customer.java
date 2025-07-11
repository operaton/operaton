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
package org.operaton.spin.xml.mapping;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * @author Stefan Hentschel.
 */
@XmlRootElement(namespace = "http://operaton.org/example")
public class Customer {

  private String id;
  private String name;
  private int contractStartDate;

  // customer id attribute
  @XmlAttribute
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  // <name>
  @XmlElement(namespace = "http://operaton.org/example")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  // <contractStartDate>
  @XmlElement(namespace = "http://operaton.org/example")
  public int getContractStartDate() {
    return contractStartDate;
  }

  public void setContractStartDate(int contractStartDate) {
    this.contractStartDate = contractStartDate;
  }
}

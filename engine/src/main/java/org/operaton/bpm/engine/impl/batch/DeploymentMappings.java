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
package org.operaton.bpm.engine.impl.batch;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Objects;

/**
 * List of aggregated information on deployment ids and the number of related resources.
 */
public class DeploymentMappings extends ArrayList<DeploymentMapping> {

  @Serial
  private static final long serialVersionUID = -868922966819588407L;

  protected int overallIdCount;

  public static DeploymentMappings of(DeploymentMapping mapping) {
    DeploymentMappings mappings = new DeploymentMappings();
    mappings.add(mapping);
    return mappings;
  }

  @Override
  public boolean add(DeploymentMapping mapping) {
    overallIdCount += mapping.getCount();
    return super.add(mapping);
  }

  @Override
  public DeploymentMapping remove(int mappingIndex) {
    overallIdCount -= get(mappingIndex).getCount();
    return super.remove(mappingIndex);
  }

  @Override
  public boolean remove(Object mapping) {
    if (super.remove(mapping)) {
      overallIdCount -= ((DeploymentMapping) mapping).getCount();
      return true;
    }
    return false;
  }

  public int getOverallIdCount() {
    return overallIdCount;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + Objects.hash(overallIdCount);
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (!super.equals(obj))
      return false;
    if (getClass() != obj.getClass())
      return false;
    DeploymentMappings other = (DeploymentMappings) obj;
    return overallIdCount == other.overallIdCount;
  }
}

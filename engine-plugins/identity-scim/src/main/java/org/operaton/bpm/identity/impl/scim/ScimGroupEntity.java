/*
 * Copyright CIB software GmbH and/or licensed to CIB software GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. CIB software licenses this file to you under the Apache License,
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
package org.operaton.bpm.identity.impl.scim;

import org.operaton.bpm.engine.impl.persistence.entity.GroupEntity;

/**
 * SCIM Group Entity.
 */
public class ScimGroupEntity extends GroupEntity {

  private static final long serialVersionUID = 1L;

  protected String scimId;
  
  public ScimGroupEntity() {
    super();
  }
	  
  public ScimGroupEntity(String groupID) {
    super(groupID);
  }

  public String getScimId() {
    return scimId;
  }

  public void setScimId(String scimId) {
    this.scimId = scimId;
  }
  

  @Override
  public String toString() {
    return this.getClass().getSimpleName() + " [scimId=" + scimId + ", id=" + id + ", name=" + name + ", type=" + type + "]";
  }
}

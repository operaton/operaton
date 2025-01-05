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
package org.operaton.bpm.model.cmmn.impl.instance;

import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.CMMN11_NS;
import static org.operaton.bpm.model.cmmn.impl.CmmnModelConstants.CMMN_ELEMENT_CASE_ROLES;

import java.util.Collection;

import org.operaton.bpm.model.cmmn.instance.CaseRoles;
import org.operaton.bpm.model.cmmn.instance.Role;
import org.operaton.bpm.model.xml.ModelBuilder;
import org.operaton.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder;
import org.operaton.bpm.model.xml.type.child.ChildElementCollection;
import org.operaton.bpm.model.xml.type.child.SequenceBuilder;

/**
 * @author Roman Smirnov
 *
 */
public class CaseRolesImpl extends CmmnElementImpl implements CaseRoles {

  protected static ChildElementCollection<Role> roleCollection;

  public CaseRolesImpl(ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  @Override
  public Collection<Role> getRoles() {
    return roleCollection.get(this);
  }

  public static void registerType(ModelBuilder modelBuilder) {
    ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(CaseRoles.class, CMMN_ELEMENT_CASE_ROLES)
      .namespaceUri(CMMN11_NS)
      .instanceProvider(instanceContext -> new CaseRolesImpl(instanceContext));

    SequenceBuilder sequenceBuilder = typeBuilder.sequence();

    roleCollection = sequenceBuilder.elementCollection(Role.class)
        .build();

    typeBuilder.build();
  }
}

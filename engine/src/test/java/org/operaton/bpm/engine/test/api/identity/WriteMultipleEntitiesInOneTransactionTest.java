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
package org.operaton.bpm.engine.test.api.identity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.identity.Group;
import org.operaton.bpm.engine.identity.User;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;

/**
 * @author Simon Jonischkeit
 *
 */
public class WriteMultipleEntitiesInOneTransactionTest {

  @RegisterExtension
  protected static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().configurationResource("org/operaton/bpm/engine/test/api/identity/WriteMultipleEntitiesInOneTransactionTest.operaton.cfg.xml").build();

  protected IdentityService identityService;

  @Test
  public void testWriteMultipleEntitiesInOneTransaction(){

    // the identity service provider registered with the engine creates a user, a group, and a membership
    // in the following call:
    assertThat(identityService.checkPassword("multipleEntities", "inOneStep")).isTrue();
    User user = identityService.createUserQuery().userId("multipleEntities").singleResult();

    assertThat(user).isNotNull();
    assertThat(user.getId()).isEqualTo("multipleEntities");
    assertThat(user.getPassword()).isEqualTo("{SHA}pfdzmt+49nwknTy7xhZd7ZW5suI=");

    // It is expected, that the User is in exactly one Group
    List<Group> groups = this.identityService.createGroupQuery().groupMember("multipleEntities").list();
    assertThat(groups).hasSize(1);

    Group group = groups.get(0);
    assertThat(group.getId()).isEqualTo("multipleEntities_group");

    // clean the Db
    identityService.deleteMembership("multipleEntities", "multipleEntities_group");
    identityService.deleteGroup("multipleEntities_group");
    identityService.deleteUser("multipleEntities");
  }
}

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
package org.operaton.bpm.engine.test.history.useroperationlog;

import java.util.Arrays;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.interceptor.CommandExecutor;
import org.operaton.bpm.engine.impl.oplog.UserOperationLogContext;
import org.operaton.bpm.engine.impl.oplog.UserOperationLogContextEntry;
import org.operaton.bpm.engine.impl.persistence.entity.PropertyChange;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class CustomUserOperationLogTest {

    public static final String USER_ID = "demo";

    @RegisterExtension
    static ProcessEngineExtension engineRule = ProcessEngineExtension.builder()
      .closeEngineAfterAllTests()
      .configurationResource("org/operaton/bpm/engine/test/history/useroperationlog/enable.legacy.user.operation.log.operaton.cfg.xml")
      .build();

    private static final String TASK_ID = UUID.randomUUID().toString();

    HistoryService historyService;

    CommandExecutor commandExecutor;

  @BeforeEach
  void setUp() {
        commandExecutor = ((ProcessEngineConfigurationImpl)engineRule.getProcessEngine().getProcessEngineConfiguration()).getCommandExecutorTxRequired();
    }

  @Test
  void testDoNotOverwriteUserId() {
        commandExecutor.execute(commandContext -> {
          final UserOperationLogContext userOperationLogContext = new UserOperationLogContext();
          userOperationLogContext.setUserId("kermit");

          final UserOperationLogContextEntry entry = new UserOperationLogContextEntry("foo", "bar");
          entry.setPropertyChanges(Arrays.asList(new PropertyChange(null, null, null)));
          entry.setTaskId(TASK_ID);
          userOperationLogContext.addEntry(entry);

          commandContext.getOperationLogManager().logUserOperations(userOperationLogContext);
          return null;
        });

        // and check its there
        assertThat(historyService.createUserOperationLogQuery().taskId(TASK_ID).singleResult().getUserId()).isEqualTo("kermit");
    }
}

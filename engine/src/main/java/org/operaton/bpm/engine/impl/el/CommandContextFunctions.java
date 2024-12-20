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
package org.operaton.bpm.engine.impl.el;

import java.util.List;

import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;

/**
 * @author Sebastian Menski
 */
public class CommandContextFunctions {
  public static final String CURRENT_USER = "currentUser";
  public static final String CURRENT_USER_GROUPS = "currentUserGroups";

  private CommandContextFunctions() {
  }

  public static String currentUser() {
    CommandContext commandContext = Context.getCommandContext();
    if (commandContext != null) {
      return commandContext.getAuthenticatedUserId();
    }
    else {
      return null;
    }
  }

  public static List<String> currentUserGroups() {
    CommandContext commandContext = Context.getCommandContext();
    if (commandContext != null) {
      return commandContext.getAuthenticatedGroupIds();
    }
    else {
      return null;
    }
  }
}

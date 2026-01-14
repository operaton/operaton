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
package org.operaton.bpm.engine.impl.util;

import org.operaton.bpm.engine.BadUserRequestException;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.impl.identity.Authentication;

public final class QueryMaxResultsLimitUtil {

  private QueryMaxResultsLimitUtil() {
  }

  public static void checkMaxResultsLimit(int resultsCount, int maxResultsLimit,
                                          boolean isUserAuthenticated) {
    if (isUserAuthenticated && maxResultsLimit < Integer.MAX_VALUE) {
      if (resultsCount == Integer.MAX_VALUE) {
        throw new BadUserRequestException("An unbound number of results is forbidden!");

      } else if (resultsCount > maxResultsLimit) {
        throw new BadUserRequestException("Max results limit of %s exceeded!".formatted(maxResultsLimit));

      }
    }
  }

  public static void checkMaxResultsLimit(int resultsCount,
                                          ProcessEngineConfigurationImpl processEngineConfig) {
    // method is used in webapps
    int maxResultsLimit = processEngineConfig.getQueryMaxResultsLimit();
    checkMaxResultsLimit(resultsCount, maxResultsLimit, isUserAuthenticated(processEngineConfig));
  }

  public static void checkMaxResultsLimit(int resultsCount) {
    ProcessEngineConfigurationImpl processEngineConfiguration =
        Context.getProcessEngineConfiguration();
    if (processEngineConfiguration == null) {
      throw new ProcessEngineException("Command context unset.");
    }

    checkMaxResultsLimit(resultsCount, getMaxResultsLimit(processEngineConfiguration),
        isUserAuthenticated(processEngineConfiguration));
  }

  protected static boolean isUserAuthenticated(ProcessEngineConfigurationImpl processEngineConfig) {
    String userId = getAuthenticatedUserId(processEngineConfig);
    return userId != null && !userId.isEmpty();
  }

  protected static String getAuthenticatedUserId(
      ProcessEngineConfigurationImpl processEngineConfig) {
    IdentityService identityService = processEngineConfig.getIdentityService();
    Authentication currentAuthentication = identityService.getCurrentAuthentication();
    if(currentAuthentication == null) {
      return null;
    } else {
      return currentAuthentication.getUserId();
    }
  }

  protected static int getMaxResultsLimit(ProcessEngineConfigurationImpl processEngineConfig) {
    return processEngineConfig.getQueryMaxResultsLimit();
  }
}

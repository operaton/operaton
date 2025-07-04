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
package org.operaton.bpm.engine.rest.util;

import java.util.List;
import org.operaton.bpm.engine.query.Query;

public class QueryUtil {

  private QueryUtil() {}

  public static <T extends Query<?,?>, U> List<U> list(Query<T, U> query, Integer firstResult, Integer maxResults) {
    List<U> results;
    if (firstResult != null || maxResults != null) {
      results = executePaginatedQuery(query, firstResult, maxResults);
    } else {
      results = query.list();
    }
    return results;
  }

  private static <T extends Query<?,?>, U> List<U> executePaginatedQuery(Query<T, U> query, Integer firstResult, Integer maxResults) {
    if (firstResult == null) {
      firstResult = 0;
    }
    if (maxResults == null) {
      maxResults = Integer.MAX_VALUE;
    }
    return query.listPage(firstResult, maxResults);
  }
}

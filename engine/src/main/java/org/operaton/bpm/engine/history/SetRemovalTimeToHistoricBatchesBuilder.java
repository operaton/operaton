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
package org.operaton.bpm.engine.history;

import org.operaton.bpm.engine.AuthorizationException;
import org.operaton.bpm.engine.BadUserRequestException;
import org.operaton.bpm.engine.authorization.BatchPermissions;
import org.operaton.bpm.engine.authorization.Permissions;
import org.operaton.bpm.engine.authorization.Resources;
import org.operaton.bpm.engine.batch.Batch;
import org.operaton.bpm.engine.batch.history.HistoricBatchQuery;

/**
 * Fluent builder to set the removal time to historic batches and
 * all associated historic entities.
 *
 * @author Tassilo Weidner
 */
public interface SetRemovalTimeToHistoricBatchesBuilder {

  /**
   * Selects historic batches by the given query.
   *
   * @param historicBatchQuery to be evaluated.
   * @return the builder.
   */
  SetRemovalTimeToHistoricBatchesBuilder byQuery(HistoricBatchQuery historicBatchQuery);

  /**
   * Selects historic batches by the given ids.
   *
   * @param historicBatchIds supposed to be affected.
   * @return the builder.
   */
  SetRemovalTimeToHistoricBatchesBuilder byIds(String... historicBatchIds);

  /**
   * Sets the removal time asynchronously as batch. The returned batch can be used to
   * track the progress of setting a removal time.
   *
   * @throws BadUserRequestException when no historic batches could be found.
   * @throws AuthorizationException
   * when no {@link BatchPermissions#CREATE_BATCH_SET_REMOVAL_TIME CREATE_BATCH_SET_REMOVAL_TIME}
   * or no permission {@link Permissions#CREATE CREATE} permission is granted on {@link Resources#BATCH}.
   *
   * @return the batch which sets the removal time asynchronously.
   */
  Batch executeAsync();

}

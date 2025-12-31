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
package org.operaton.bpm.engine.impl;

import org.operaton.bpm.engine.impl.db.ListQueryParameterObject;
import org.operaton.bpm.engine.impl.interceptor.Command;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.interceptor.CommandExecutor;
import org.operaton.bpm.engine.management.TablePage;
import org.operaton.bpm.engine.management.TablePageQuery;


/**
 *
 * @author Joram Barrez
 */
public class TablePageQueryImpl extends ListQueryParameterObject implements TablePageQuery, Command<TablePage> {

  CommandExecutor commandExecutor;

  protected String tableName;
  protected String order;

  public TablePageQueryImpl() {
  }

  public TablePageQueryImpl(CommandExecutor commandExecutor) {
    this.commandExecutor = commandExecutor;
  }

  @Override
  public TablePageQueryImpl tableName(String tableName) {
    this.tableName = tableName;
    return this;
  }

  @Override
  public TablePageQueryImpl orderAsc(String column) {
    this.orderingProperties.add(new QueryOrderingProperty(new QueryPropertyImpl(column), Direction.ASCENDING));
    return this;
  }

  @Override
  public TablePageQueryImpl orderDesc(String column) {
    this.orderingProperties.add(new QueryOrderingProperty(new QueryPropertyImpl(column), Direction.DESCENDING));
    return this;
  }

  public String getTableName() {
    return tableName;
  }

  @Override
  public TablePage listPage(int firstResult, int maxResults) {
    this.firstResult = firstResult;
    this.maxResults = maxResults;
    return commandExecutor.execute(this);
  }

  @Override
  public TablePage execute(CommandContext commandContext) {
    commandContext.getAuthorizationManager().checkOperatonAdmin();
    return commandContext
      .getTableDataManager()
      .getTablePage(this);
  }

  public String getOrder() {
    return order;
  }

}

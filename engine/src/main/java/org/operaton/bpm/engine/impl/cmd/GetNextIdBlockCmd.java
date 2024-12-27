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
package org.operaton.bpm.engine.impl.cmd;

import org.operaton.bpm.engine.impl.db.IdBlock;
import org.operaton.bpm.engine.impl.interceptor.Command;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.persistence.entity.PropertyEntity;


/**
 * @author Tom Baeyens
 */
public class GetNextIdBlockCmd implements Command<IdBlock> {
  
  protected int idBlockSize;
  
  public GetNextIdBlockCmd(int idBlockSize) {
    this.idBlockSize = idBlockSize;
  }

  @Override
  public IdBlock execute(CommandContext commandContext) {
    PropertyEntity property = commandContext
      .getPropertyManager()
      .findPropertyById("next.dbid");
    long oldValue = Long.parseLong(property.getValue());
    long newValue = oldValue+idBlockSize;
    property.setValue(Long.toString(newValue));
    return new IdBlock(oldValue, newValue-1);
  }
}

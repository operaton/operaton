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
package org.operaton.bpm.engine.impl.cmd;

import org.operaton.bpm.engine.identity.Picture;
import org.operaton.bpm.engine.impl.interceptor.Command;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.persistence.entity.ByteArrayEntity;
import org.operaton.bpm.engine.impl.persistence.entity.IdentityInfoEntity;

import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureNotNull;

/**
 * @author Daniel Meyer
 * @author Tom Baeyens
 */
public class GetUserPictureCmd implements Command<Picture> {
  protected String userId;

  public GetUserPictureCmd(String userId) {
    this.userId = userId;
  }

  @Override
  public Picture execute(CommandContext commandContext) {
    ensureNotNull("userId", userId);

    IdentityInfoEntity pictureInfo = commandContext.getIdentityInfoManager()
      .findUserInfoByUserIdAndKey(userId, "picture");

    if (pictureInfo != null) {
      String pictureByteArrayId = pictureInfo.getValue();
      if (pictureByteArrayId != null) {
        ByteArrayEntity byteArray = commandContext.getDbEntityManager()
          .selectById(ByteArrayEntity.class, pictureByteArrayId);
        return new Picture(byteArray.getBytes(), byteArray.getName());
      }
    }

    return null;
  }

}

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
package org.operaton.bpm.engine.impl.persistence.entity;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.operaton.bpm.engine.impl.persistence.AbstractManager;

/**
 * @author Tom Baeyens
 */
public class IdentityInfoManager extends AbstractManager {
  private static final String USER_ID = "userId";
  private static final String KEY = "key";
  private static final String TYPE = "type";

  public void deleteUserInfoByUserIdAndKey(String userId, String key) {
    IdentityInfoEntity identityInfoEntity = findUserInfoByUserIdAndKey(userId, key);
    if (identityInfoEntity!=null) {
      deleteIdentityInfo(identityInfoEntity);
    }
  }

  public void deleteIdentityInfo(IdentityInfoEntity identityInfo) {
    getDbEntityManager().delete(identityInfo);
    if (IdentityInfoEntity.TYPE_USERACCOUNT.equals(identityInfo.getType())) {
      for (IdentityInfoEntity identityInfoDetail: findIdentityInfoDetails(identityInfo.getId())) {
        getDbEntityManager().delete(identityInfoDetail);
      }
    }
  }

  public IdentityInfoEntity findUserAccountByUserIdAndKey(String userId, String key) {
    IdentityInfoEntity identityInfoEntity = findUserInfoByUserIdAndKey(userId, key);
    if (identityInfoEntity==null) {
      return null;
    }

    Map<String,String> details = new HashMap<>();
    String identityInfoId = identityInfoEntity.getId();
    List<IdentityInfoEntity> identityInfoDetails = findIdentityInfoDetails(identityInfoId);
    for (IdentityInfoEntity identityInfoDetail: identityInfoDetails) {
      details.put(identityInfoDetail.getKey(), identityInfoDetail.getValue());
    }
    identityInfoEntity.setDetails(details);

    if (identityInfoEntity.getPasswordBytes()!=null) {
      String password = decryptPassword(identityInfoEntity.getPasswordBytes());
      identityInfoEntity.setPassword(password);
    }

    return identityInfoEntity;
  }

  @SuppressWarnings("unchecked")
  protected List<IdentityInfoEntity> findIdentityInfoDetails(String identityInfoId) {
    return getDbEntityManager().selectList("selectIdentityInfoDetails", identityInfoId);
  }

  public void setUserInfo(String userId, String type, String key, String value, String accountPassword, Map<String, String> accountDetails) {
    byte[] storedPassword = null;
    if (accountPassword!=null) {
      storedPassword = encryptPassword(accountPassword);
    }

    IdentityInfoEntity identityInfoEntity = findUserInfoByUserIdAndKey(userId, key);
    if (identityInfoEntity!=null) {
      // update
      identityInfoEntity.setValue(value);
      identityInfoEntity.setPasswordBytes(storedPassword);

      if (accountDetails==null) {
        accountDetails = new HashMap<>();
      }

      Set<String> newKeys = new HashSet<>(accountDetails.keySet());
      List<IdentityInfoEntity> identityInfoDetails = findIdentityInfoDetails(identityInfoEntity.getId());
      for (IdentityInfoEntity identityInfoDetail: identityInfoDetails) {
        String detailKey = identityInfoDetail.getKey();
        newKeys.remove(detailKey);
        String newDetailValue = accountDetails.get(detailKey);
        if (newDetailValue==null) {
          deleteIdentityInfo(identityInfoDetail);
        } else {
          // update detail
          identityInfoDetail.setValue(newDetailValue);
        }
      }
      insertAccountDetails(identityInfoEntity, accountDetails, newKeys);


    } else {
      // insert
      identityInfoEntity = new IdentityInfoEntity();
      identityInfoEntity.setUserId(userId);
      identityInfoEntity.setType(type);
      identityInfoEntity.setKey(key);
      identityInfoEntity.setValue(value);
      identityInfoEntity.setPasswordBytes(storedPassword);
      getDbEntityManager().insert(identityInfoEntity);
      if (accountDetails!=null) {
        insertAccountDetails(identityInfoEntity, accountDetails, accountDetails.keySet());
      }
    }
  }

  private void insertAccountDetails(IdentityInfoEntity identityInfoEntity, Map<String, String> accountDetails, Set<String> keys) {
    for (String newKey: keys) {
      // insert detail
      IdentityInfoEntity identityInfoDetail = new IdentityInfoEntity();
      identityInfoDetail.setParentId(identityInfoEntity.getId());
      identityInfoDetail.setKey(newKey);
      identityInfoDetail.setValue(accountDetails.get(newKey));
      getDbEntityManager().insert(identityInfoDetail);
    }
  }

  public byte[] encryptPassword(String accountPassword) {
    return accountPassword.getBytes();
  }

  public String decryptPassword(byte[] storedPassword) {
      return new String(storedPassword);
  }

  public IdentityInfoEntity findUserInfoByUserIdAndKey(String userId, String key) {
    Map<String, String> parameters = new HashMap<>();
    parameters.put(USER_ID, userId);
    parameters.put(KEY, key);
    return (IdentityInfoEntity) getDbEntityManager().selectOne("selectIdentityInfoByUserIdAndKey", parameters);
  }

  @SuppressWarnings("unchecked")
  public List<String> findUserInfoKeysByUserIdAndType(String userId, String type) {
    Map<String, String> parameters = new HashMap<>();
    parameters.put(USER_ID, userId);
    parameters.put(TYPE, type);
    return getDbEntityManager().selectList("selectIdentityInfoKeysByUserIdAndType", parameters);
  }

  public void deleteUserInfoByUserId(String userId) {
    List<IdentityInfoEntity> identityInfos = getDbEntityManager().selectList("selectIdentityInfoByUserId", userId);
    for (IdentityInfoEntity identityInfo: identityInfos) {
      getIdentityInfoManager().deleteIdentityInfo(identityInfo);
    }
  }

  public void updateUserLock(UserEntity user, int attempts, Date lockExpirationTime) {
    user.setAttempts(attempts);
    user.setLockExpirationTime(lockExpirationTime);
    getDbEntityManager().update(UserEntity.class, "updateUserLock", user);
  }
}

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
import java.util.Map;

import org.operaton.bpm.engine.identity.PasswordPolicyResult;
import org.operaton.bpm.engine.identity.User;
import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.impl.db.DbEntity;
import org.operaton.bpm.engine.impl.db.HasDbRevision;

import static org.operaton.bpm.engine.impl.util.EncryptionUtil.saltPassword;


/**
 * @author Tom Baeyens
 */
public class UserEntity implements User, DbEntity, HasDbRevision {

  protected String id;
  protected int revision;
  protected String firstName;
  protected String lastName;
  protected String email;
  protected String password;
  protected String newPassword;
  protected String salt;
  protected Date lockExpirationTime;
  protected int attempts;

  public UserEntity() {
  }

  public UserEntity(String id) {
    this.id = id;
  }

  @Override
  public Object getPersistentState() {
    Map<String, Object> persistentState = new HashMap<>();
    persistentState.put("firstName", firstName);
    persistentState.put("lastName", lastName);
    persistentState.put("email", email);
    persistentState.put("password", password);
    persistentState.put("salt", salt);
    return persistentState;
  }

  @Override
  public int getRevisionNext() {
    return revision+1;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  @Override
  public String getFirstName() {
    return firstName;
  }

  @Override
  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  @Override
  public String getLastName() {
    return lastName;
  }

  @Override
  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  @Override
  public String getEmail() {
    return email;
  }

  @Override
  public void setEmail(String email) {
    this.email = email;
  }

  @Override
  public String getPassword() {
    return password;
  }

  @Override
  public void setPassword(String password) {
    this.newPassword = password;
  }

  public String getSalt() {
    return this.salt;
  }
  public void setSalt(String salt) {
    this.salt = salt;
  }

  /**
   * Special setter for MyBatis.
   */
  public void setDbPassword(String password) {
    this.password = password;
  }

  @Override
  public int getRevision() {
    return revision;
  }

  @Override
  public void setRevision(int revision) {
    this.revision = revision;
  }

  public Date getLockExpirationTime() {
    return lockExpirationTime;
  }

  public void setLockExpirationTime(Date lockExpirationTime) {
    this.lockExpirationTime = lockExpirationTime;
  }

  public int getAttempts() {
    return attempts;
  }

  public void setAttempts(int attempts) {
    this.attempts = attempts;
  }

  public void encryptPassword() {
    if (newPassword != null) {
      salt = generateSalt();
      setDbPassword(encryptPassword(newPassword, salt));
    }
  }

  protected String encryptPassword(String password, String salt) {
    if (password == null) {
      return null;
    } else {
      String saltedPassword = saltPassword(password, salt);
      return Context.getProcessEngineConfiguration()
        .getPasswordManager()
        .encrypt(saltedPassword);
    }
  }

  protected String generateSalt() {
    return Context.getProcessEngineConfiguration()
      .getSaltGenerator()
      .generateSalt();
  }


  public boolean checkPasswordAgainstPolicy() {
    PasswordPolicyResult result = Context.getProcessEngineConfiguration()
      .getIdentityService()
      .checkPasswordAgainstPolicy(newPassword, this);

    return result.isValid();
  }

  public boolean hasNewPassword() {
    return newPassword != null;
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName()
           + "[id=" + id
           + ", revision=" + revision
           + ", firstName=" + firstName
           + ", lastName=" + lastName
           + ", email=" + email
           + ", password=******" // sensitive for logging
           + ", salt=******" // sensitive for logging
           + ", lockExpirationTime=" + lockExpirationTime
           + ", attempts=" + attempts
           + "]";
  }
}

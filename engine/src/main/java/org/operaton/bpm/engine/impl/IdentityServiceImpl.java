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
package org.operaton.bpm.engine.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.operaton.bpm.engine.BadUserRequestException;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.identity.PasswordPolicyResult;
import org.operaton.bpm.engine.identity.Group;
import org.operaton.bpm.engine.identity.GroupQuery;
import org.operaton.bpm.engine.identity.NativeUserQuery;
import org.operaton.bpm.engine.identity.PasswordPolicy;
import org.operaton.bpm.engine.identity.PasswordPolicyRule;
import org.operaton.bpm.engine.identity.Picture;
import org.operaton.bpm.engine.identity.Tenant;
import org.operaton.bpm.engine.identity.TenantQuery;
import org.operaton.bpm.engine.identity.User;
import org.operaton.bpm.engine.identity.UserQuery;
import org.operaton.bpm.engine.impl.cmd.CheckPassword;
import org.operaton.bpm.engine.impl.cmd.GetPasswordPolicyCmd;
import org.operaton.bpm.engine.impl.cmd.CreateGroupCmd;
import org.operaton.bpm.engine.impl.cmd.CreateGroupQueryCmd;
import org.operaton.bpm.engine.impl.cmd.CreateMembershipCmd;
import org.operaton.bpm.engine.impl.cmd.CreateNativeUserQueryCmd;
import org.operaton.bpm.engine.impl.cmd.CreateTenantCmd;
import org.operaton.bpm.engine.impl.cmd.CreateTenantGroupMembershipCmd;
import org.operaton.bpm.engine.impl.cmd.CreateTenantQueryCmd;
import org.operaton.bpm.engine.impl.cmd.CreateTenantUserMembershipCmd;
import org.operaton.bpm.engine.impl.cmd.CreateUserCmd;
import org.operaton.bpm.engine.impl.cmd.CreateUserQueryCmd;
import org.operaton.bpm.engine.impl.cmd.DeleteGroupCmd;
import org.operaton.bpm.engine.impl.cmd.DeleteMembershipCmd;
import org.operaton.bpm.engine.impl.cmd.DeleteTenantCmd;
import org.operaton.bpm.engine.impl.cmd.DeleteTenantGroupMembershipCmd;
import org.operaton.bpm.engine.impl.cmd.DeleteTenantUserMembershipCmd;
import org.operaton.bpm.engine.impl.cmd.DeleteUserCmd;
import org.operaton.bpm.engine.impl.cmd.DeleteUserInfoCmd;
import org.operaton.bpm.engine.impl.cmd.DeleteUserPictureCmd;
import org.operaton.bpm.engine.impl.cmd.GetUserAccountCmd;
import org.operaton.bpm.engine.impl.cmd.GetUserInfoCmd;
import org.operaton.bpm.engine.impl.cmd.GetUserInfoKeysCmd;
import org.operaton.bpm.engine.impl.cmd.GetUserPictureCmd;
import org.operaton.bpm.engine.impl.cmd.IsIdentityServiceReadOnlyCmd;
import org.operaton.bpm.engine.impl.cmd.SaveGroupCmd;
import org.operaton.bpm.engine.impl.cmd.SaveTenantCmd;
import org.operaton.bpm.engine.impl.cmd.SaveUserCmd;
import org.operaton.bpm.engine.impl.cmd.SetUserInfoCmd;
import org.operaton.bpm.engine.impl.cmd.SetUserPictureCmd;
import org.operaton.bpm.engine.impl.cmd.UnlockUserCmd;
import org.operaton.bpm.engine.impl.identity.Account;
import org.operaton.bpm.engine.impl.identity.Authentication;
import org.operaton.bpm.engine.impl.identity.PasswordPolicyResultImpl;
import org.operaton.bpm.engine.impl.persistence.entity.IdentityInfoEntity;
import org.operaton.bpm.engine.impl.util.EnsureUtil;
import org.operaton.bpm.engine.impl.util.ExceptionUtil;


/**
 * @author Tom Baeyens
 */
public class IdentityServiceImpl extends ServiceImpl implements IdentityService {

  /** thread local holding the current authentication */
  private final ThreadLocal<Authentication> currentAuthentication = new ThreadLocal<>();

  @Override
  public boolean isReadOnly() {
    return commandExecutor.execute(new IsIdentityServiceReadOnlyCmd());
  }

  @Override
  public Group newGroup(String groupId) {
    return commandExecutor.execute(new CreateGroupCmd(groupId));
  }

  @Override
  public User newUser(String userId) {
    return commandExecutor.execute(new CreateUserCmd(userId));
  }

  @Override
  public Tenant newTenant(String tenantId) {
    return commandExecutor.execute(new CreateTenantCmd(tenantId));
  }

  @Override
  public void saveGroup(Group group) {

    try {
      commandExecutor.execute(new SaveGroupCmd(group));
    } catch (ProcessEngineException ex) {
      if (ExceptionUtil.checkConstraintViolationException(ex)) {
        throw new BadUserRequestException("The group already exists", ex);
      }
      throw ex;
    }
  }

  @Override
  public void saveUser(User user) {
    saveUser(user, false);
  }
  
  public void saveUser(User user, boolean skipPasswordPolicy) {
    try {
      commandExecutor.execute(new SaveUserCmd(user, skipPasswordPolicy));
    } catch (ProcessEngineException ex) {
      if (ExceptionUtil.checkConstraintViolationException(ex)) {
        throw new BadUserRequestException("The user already exists", ex);
      }
      throw ex;
    }
  }

  @Override
  public void saveTenant(Tenant tenant) {

    try {
      commandExecutor.execute(new SaveTenantCmd(tenant));
    } catch (ProcessEngineException ex) {
      if (ExceptionUtil.checkConstraintViolationException(ex)) {
        throw new BadUserRequestException("The tenant already exists", ex);
      }
      throw ex;
    }
  }

  @Override
  public UserQuery createUserQuery() {
    return commandExecutor.execute(new CreateUserQueryCmd());
  }

  @Override
  public NativeUserQuery createNativeUserQuery() {
    return commandExecutor.execute(new CreateNativeUserQueryCmd());
  }

  @Override
  public GroupQuery createGroupQuery() {
    return commandExecutor.execute(new CreateGroupQueryCmd());
  }

  @Override
  public TenantQuery createTenantQuery() {
    return commandExecutor.execute(new CreateTenantQueryCmd());
  }

  @Override
  public void createMembership(String userId, String groupId) {
    commandExecutor.execute(new CreateMembershipCmd(userId, groupId));
  }

  @Override
  public void deleteGroup(String groupId) {
    commandExecutor.execute(new DeleteGroupCmd(groupId));
  }

  @Override
  public void deleteMembership(String userId, String groupId) {
    commandExecutor.execute(new DeleteMembershipCmd(userId, groupId));
  }

  @Override
  public boolean checkPassword(String userId, String password) {
    return commandExecutor.execute(new CheckPassword(userId, password));
  }

  @Override
  public PasswordPolicyResult checkPasswordAgainstPolicy(String candidatePassword, User user) {
    return checkPasswordAgainstPolicy(getPasswordPolicy(), candidatePassword, user);
  }

  @Override
  public PasswordPolicyResult checkPasswordAgainstPolicy(String password) {
    return checkPasswordAgainstPolicy(getPasswordPolicy(), password, null);
  }

  @Override
  public PasswordPolicyResult checkPasswordAgainstPolicy(PasswordPolicy policy,
                                                         String candidatePassword,
                                                         User user) {
    EnsureUtil.ensureNotNull("policy", policy);
    EnsureUtil.ensureNotNull("password", candidatePassword);

    List<PasswordPolicyRule> violatedRules = new ArrayList<>();
    List<PasswordPolicyRule> fulfilledRules = new ArrayList<>();

    for (PasswordPolicyRule rule : policy.getRules()) {
      if (rule.execute(candidatePassword, user)) {
        fulfilledRules.add(rule);
      } else {
        violatedRules.add(rule);
      }
    }
    return new PasswordPolicyResultImpl(violatedRules, fulfilledRules);

  }

  @Override
  public PasswordPolicyResult checkPasswordAgainstPolicy(PasswordPolicy policy, String password) {
    return checkPasswordAgainstPolicy(policy, password, null);
  }

  @Override
  public PasswordPolicy getPasswordPolicy() {
    return commandExecutor.execute(new GetPasswordPolicyCmd());
  }

  @Override
  public void unlockUser(String userId) {
    commandExecutor.execute(new UnlockUserCmd(userId));
  }

  @Override
  public void deleteUser(String userId) {
    commandExecutor.execute(new DeleteUserCmd(userId));
  }

  @Override
  public void deleteTenant(String tenantId) {
    commandExecutor.execute(new DeleteTenantCmd(tenantId));
  }

  @Override
  public void setUserPicture(String userId, Picture picture) {
    commandExecutor.execute(new SetUserPictureCmd(userId, picture));
  }

  @Override
  public Picture getUserPicture(String userId) {
    return commandExecutor.execute(new GetUserPictureCmd(userId));
  }

  @Override
  public void deleteUserPicture(String userId) {
    commandExecutor.execute(new DeleteUserPictureCmd(userId));
  }

  @Override
  public void setAuthenticatedUserId(String authenticatedUserId) {
    setAuthentication(new Authentication(authenticatedUserId, null));
  }

  @Override
  public void setAuthentication(Authentication auth) {
    if(auth == null) {
      clearAuthentication();
    } else {
      if (auth.getUserId() != null) {
        EnsureUtil.ensureValidIndividualResourceId("Invalid user id provided", auth.getUserId());
      }
      if (auth.getGroupIds() != null) {
        EnsureUtil.ensureValidIndividualResourceIds("At least one invalid group id provided", auth.getGroupIds());
      }
      if (auth.getTenantIds() != null) {
        EnsureUtil.ensureValidIndividualResourceIds("At least one invalid tenant id provided", auth.getTenantIds());
      }

      currentAuthentication.set(auth);
    }
  }

  @Override
  public void setAuthentication(String userId, List<String> groups) {
    setAuthentication(new Authentication(userId, groups));
  }

  @Override
  public void setAuthentication(String userId, List<String> groups, List<String> tenantIds) {
    setAuthentication(new Authentication(userId, groups, tenantIds));
  }

  @Override
  public void clearAuthentication() {
    currentAuthentication.remove();
  }

  @Override
  public Authentication getCurrentAuthentication() {
    return currentAuthentication.get();
  }

  @Override
  public String getUserInfo(String userId, String key) {
    return commandExecutor.execute(new GetUserInfoCmd(userId, key));
  }

  @Override
  public List<String> getUserInfoKeys(String userId) {
    return commandExecutor.execute(new GetUserInfoKeysCmd(userId, IdentityInfoEntity.TYPE_USERINFO));
  }

  @Override
  public List<String> getUserAccountNames(String userId) {
    return commandExecutor.execute(new GetUserInfoKeysCmd(userId, IdentityInfoEntity.TYPE_USERACCOUNT));
  }

  @Override
  public void setUserInfo(String userId, String key, String value) {
    commandExecutor.execute(new SetUserInfoCmd(userId, key, value));
  }

  @Override
  public void deleteUserInfo(String userId, String key) {
    commandExecutor.execute(new DeleteUserInfoCmd(userId, key));
  }

  @Override
  public void deleteUserAccount(String userId, String accountName) {
    commandExecutor.execute(new DeleteUserInfoCmd(userId, accountName));
  }

  @Override
  public Account getUserAccount(String userId, String userPassword, String accountName) {
    return commandExecutor.execute(new GetUserAccountCmd(userId, userPassword, accountName));
  }

  @Override
  public void setUserAccount(String userId, String userPassword, String accountName, String accountUsername, String accountPassword, Map<String, String> accountDetails) {
    commandExecutor.execute(new SetUserInfoCmd(userId, userPassword, accountName, accountUsername, accountPassword, accountDetails));
  }

  @Override
  public void createTenantUserMembership(String tenantId, String userId) {
    commandExecutor.execute(new CreateTenantUserMembershipCmd(tenantId, userId));
  }

  @Override
  public void createTenantGroupMembership(String tenantId, String groupId) {
    commandExecutor.execute(new CreateTenantGroupMembershipCmd(tenantId, groupId));
  }

  @Override
  public void deleteTenantUserMembership(String tenantId, String userId) {
    commandExecutor.execute(new DeleteTenantUserMembershipCmd(tenantId, userId));
  }

  @Override
  public void deleteTenantGroupMembership(String tenantId, String groupId) {
    commandExecutor.execute(new DeleteTenantGroupMembershipCmd(tenantId, groupId));
  }

}

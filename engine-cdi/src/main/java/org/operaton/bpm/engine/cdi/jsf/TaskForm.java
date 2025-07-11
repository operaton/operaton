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
package org.operaton.bpm.engine.cdi.jsf;

import jakarta.enterprise.context.Conversation;
import jakarta.enterprise.context.ConversationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.cdi.BusinessProcess;
import org.operaton.bpm.engine.repository.ProcessDefinition;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@ConversationScoped
@Named("operatonTaskForm")
public class TaskForm implements Serializable {

  private static final String REQUEST_PARAM_TASK_ID = "taskId";
  private static final String REQUEST_PARAM_CALLBACK_URL = "callbackUrl";
  private static final String REQUEST_PARAM_PROCESS_DEFINITION_ID = "processDefinitionId";
  private static final String REQUEST_PARAM_PROCESS_DEFINITION_KEY = "processDefinitionKey";
  private static Logger log = Logger.getLogger(TaskForm.class.getName());

  private static final long serialVersionUID = 1L;

  protected String url;

  protected String processDefinitionId;
  protected String processDefinitionKey;

  @Inject
  protected BusinessProcess businessProcess;

  @Inject
  protected RepositoryService repositoryService;

  @Inject
  protected Instance<Conversation> conversationInstance;

  /**
   * @deprecated Use {@link startTaskForm()} instead.
   *
   * @param taskId
   * @param callbackUrl
   */
  @Deprecated(forRemoval = true, since = "1.0")
  public void startTask(String taskId, String callbackUrl) {
    if (taskId==null || callbackUrl == null) {
      if (FacesContext.getCurrentInstance().isPostback()) {
        // if this is an AJAX request ignore it, since we will receive multiple calls to this bean if it is added
        // as preRenderView event
        // see http://stackoverflow.com/questions/2830834/jsf-fevent-prerenderview-is-triggered-by-fajax-calls-and-partial-renders-some
        return;
      }
      // return it anyway but log an info message
      log.log(Level.INFO, () -> "Called startTask method without proper parameter (taskId='"+taskId+"'; callbackUrl='"+callbackUrl+"') even if it seems we are not called by an AJAX Postback. Are you using the operatonTaskForm bean correctly?");
      return;
    }
    // Note that we always run in a conversation
    this.url = callbackUrl;
    businessProcess.startTask(taskId, true);
  }

  /**
   * Get taskId and callBackUrl from request and start a conversation
   * to start the form
   *
   */
  public void startTaskForm() {
    Map<String, String> requestParameterMap = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
    String taskId = requestParameterMap.get(REQUEST_PARAM_TASK_ID);
    String callbackUrl = requestParameterMap.get(REQUEST_PARAM_CALLBACK_URL);

    if (taskId==null || callbackUrl == null) {
      if (FacesContext.getCurrentInstance().isPostback()) {
        // if this is an AJAX request ignore it, since we will receive multiple calls to this bean if it is added
        // as preRenderView event
        // see http://stackoverflow.com/questions/2830834/jsf-fevent-prerenderview-is-triggered-by-fajax-calls-and-partial-renders-some
        return;
      }
      // return it anyway but log an info message
      log.log(Level.INFO, () -> "Called startTask method without proper parameter (taskId='"+taskId+"'; callbackUrl='"+callbackUrl+"') even if it seems we are not called by an AJAX Postback. Are you using the operatonTaskForm bean correctly?");
      return;
    }
    // Note that we always run in a conversation
    this.url = callbackUrl;
    businessProcess.startTask(taskId, true);
  }

  public void completeTask() throws IOException {
    // the conversation is always ended on task completion (otherwise the
    // redirect will end up in an exception anyway!)
    businessProcess.completeTask(true);
    FacesContext.getCurrentInstance().getExternalContext().redirect(url);
  }

  private void beginConversation() {
    if (conversationInstance.get().isTransient()) {
      conversationInstance.get().begin();
    }
  }

  /**
   * @deprecated Use {@link #startProcessInstanceByIdForm()} instead.
   *
   * @param processDefinitionId
   * @param callbackUrl
   */
  @Deprecated(forRemoval = true, since = "1.0")
  public void startProcessInstanceByIdForm(String processDefinitionId, String callbackUrl) {
    this.url = callbackUrl;
    this.processDefinitionId = processDefinitionId;
    beginConversation();
  }

  /**
   * Get processDefinitionId and callbackUrl from request and start a conversation
   * to start the form
   *
   */
  public void startProcessInstanceByIdForm() {
    if (FacesContext.getCurrentInstance().isPostback()) {
      // if this is an AJAX request ignore it, since we will receive multiple calls to this bean if it is added
      // as preRenderView event
      // see http://stackoverflow.com/questions/2830834/jsf-fevent-prerenderview-is-triggered-by-fajax-calls-and-partial-renders-some
      return;
    }

    Map<String, String> requestParameterMap = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
    this.url = requestParameterMap.get(REQUEST_PARAM_CALLBACK_URL);
    this.processDefinitionId = requestParameterMap.get(REQUEST_PARAM_PROCESS_DEFINITION_ID);
    beginConversation();
  }

  /**
   * @deprecated Use {@link startProcessInstanceByKeyForm()} instead.
   *
   * @param processDefinitionKey
   * @param callbackUrl
   */
  @Deprecated(forRemoval = true, since = "1.0")
  public void startProcessInstanceByKeyForm(String processDefinitionKey, String callbackUrl) {
    this.url = callbackUrl;
    this.processDefinitionKey = processDefinitionKey;
    beginConversation();
  }

  /**
   * Get processDefinitionKey and callbackUrl from request and start a conversation
   * to start the form
   *
   */
  public void startProcessInstanceByKeyForm() {
    if (FacesContext.getCurrentInstance().isPostback()) {
      // if this is an AJAX request ignore it, since we will receive multiple calls to this bean if it is added
      // as preRenderView event
      // see http://stackoverflow.com/questions/2830834/jsf-fevent-prerenderview-is-triggered-by-fajax-calls-and-partial-renders-some
      return;
    }

    Map<String, String> requestParameterMap = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
    String processDefinitionKey = requestParameterMap.get(REQUEST_PARAM_PROCESS_DEFINITION_KEY);
    String callbackUrl = requestParameterMap.get(REQUEST_PARAM_CALLBACK_URL);
    this.url = callbackUrl;
    this.processDefinitionKey = processDefinitionKey;
    beginConversation();
  }

  public void completeProcessInstanceForm() throws IOException {
    // start the process instance
    if (processDefinitionId!=null) {
      businessProcess.startProcessById(processDefinitionId);
      processDefinitionId = null;
    } else {
      businessProcess.startProcessByKey(processDefinitionKey);
      processDefinitionKey = null;
    }

    // End the conversation
    conversationInstance.get().end();

    // and redirect
    FacesContext.getCurrentInstance().getExternalContext().redirect(url);
  }

  public ProcessDefinition getProcessDefinition() {
    // TODO cache result to avoid multiple queries within one page request
    if (processDefinitionId!=null) {
      return repositoryService.createProcessDefinitionQuery().processDefinitionId(processDefinitionId).singleResult();
    } else {
      return repositoryService.createProcessDefinitionQuery().processDefinitionKey(processDefinitionKey).latestVersion().singleResult();
    }
  }

  public String getUrl() {
    return url;
  }
  public void setUrl(String url) {
    this.url = url;
  }

}

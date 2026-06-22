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
package org.operaton.bpm.engine.rest.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.core.Response.Status;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import org.operaton.bpm.engine.ExternalTaskService;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.externaltask.ExternalTaskQueryTopicBuilder;
import org.operaton.bpm.engine.externaltask.FetchAndLockBuilder;
import org.operaton.bpm.engine.externaltask.LockedExternalTask;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.rest.dto.externaltask.FetchExternalTasksExtendedDto;
import org.operaton.bpm.engine.rest.exception.InvalidRequestException;
import org.operaton.bpm.engine.rest.exception.RestException;
import org.operaton.bpm.engine.rest.helper.MockProvider;

import static org.operaton.bpm.engine.rest.impl.FetchAndLockHandlerImpl.BLOCKING_QUEUE_CAPACITY_PARAM_NAME;
import static org.operaton.bpm.engine.rest.impl.FetchAndLockHandlerImpl.DEFAULT_BLOCKING_QUEUE_CAPACITY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

/**
 * @author Tassilo Weidner
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FetchAndLockHandlerTest {

  @Mock
  protected ProcessEngine processEngine;

  @Mock
  protected IdentityService identityService;

  @Mock
  protected ExternalTaskService externalTaskService;

  @Mock
  protected ExternalTaskQueryTopicBuilder externalTaskQueryTopicBuilder;

  @Mock
  protected FetchAndLockBuilder fetchAndLockBuilder;

  @Mock
  private ServletContext servletContext;

  @Mock
  private ServletContextEvent servletContextEvent;

  @Spy
  protected FetchAndLockHandlerImpl handler;

  protected LockedExternalTask lockedExternalTaskMock;

  protected static final Date START_DATE = new Date(1457326800000L);

  @BeforeEach
  void initMocks() {
    when(fetchAndLockBuilder.workerId(anyString())).thenReturn(fetchAndLockBuilder);
    when(fetchAndLockBuilder.maxTasks(anyInt())).thenReturn(fetchAndLockBuilder);
    when(fetchAndLockBuilder.usePriority(anyBoolean())).thenReturn(fetchAndLockBuilder);

    when(processEngine.getIdentityService()).thenReturn(identityService);
    when(processEngine.getExternalTaskService()).thenReturn(externalTaskService);
    when(processEngine.getName()).thenReturn("default");

    when(externalTaskService.fetchAndLock()).thenReturn(fetchAndLockBuilder);

    when(fetchAndLockBuilder.subscribe()).thenReturn(externalTaskQueryTopicBuilder);

    when(externalTaskQueryTopicBuilder.topic(any(String.class), anyLong())).thenReturn(externalTaskQueryTopicBuilder);

    doNothing().when(handler).suspend(anyLong());
    doReturn(processEngine).when(handler).getProcessEngine(any(FetchAndLockRequest.class));

    lockedExternalTaskMock = MockProvider.createMockLockedExternalTask();

    when(servletContextEvent.getServletContext()).thenReturn(servletContext);
    handler.contextInitialized(servletContextEvent);
  }

  @BeforeEach
  void setClock() {
    ClockUtil.setCurrentTime(START_DATE);
  }

  @AfterEach
  void resetClock() {
    ClockUtil.reset();
  }

  @AfterEach
  void resetUniqueWorkerRequestParam() {
    handler.parseUniqueWorkerRequestParam("false");
  }

  @Test
  void shouldResumeAsyncResponseDueToAvailableTasks() {
    // given
    List<LockedExternalTask> tasks = new ArrayList<>();
    tasks.add(lockedExternalTaskMock);

    when(fetchAndLockBuilder.subscribe()).thenReturn(externalTaskQueryTopicBuilder);
    when(externalTaskQueryTopicBuilder.execute()).thenReturn(tasks);


    AsyncResponse asyncResponse = mock(AsyncResponse.class);
    handler.addPendingRequest(createDto(5000L), asyncResponse, processEngine);

    // when
    handler.acquire();

    // then
    verify(asyncResponse).resume(argThat(Matchers.hasSize(1)));
    assertThat(handler.getPendingRequests()).isEmpty();
    verify(handler).suspend(Long.MAX_VALUE);
  }

  @Test
  void shouldNotResumeAsyncResponseDueToNoAvailableTasks() {
    // given
    when(fetchAndLockBuilder.subscribe()).thenReturn(externalTaskQueryTopicBuilder);
    when(externalTaskQueryTopicBuilder.execute()).thenReturn(Collections.emptyList());

    AsyncResponse asyncResponse = mock(AsyncResponse.class);
    handler.addPendingRequest(createDto(5000L), asyncResponse, processEngine);

    // when
    handler.acquire();

    // then
    verify(asyncResponse, never()).resume(any());
    assertThat(handler.getPendingRequests()).hasSize(1);
    verify(handler).suspend(5000L);
  }

  @Test
  void shouldResumeAsyncResponseDueToTimeoutExpired_1() {
    // given
    when(fetchAndLockBuilder.subscribe()).thenReturn(externalTaskQueryTopicBuilder);
    when(externalTaskQueryTopicBuilder.execute()).thenReturn(Collections.emptyList());

    AsyncResponse asyncResponse = mock(AsyncResponse.class);
    handler.addPendingRequest(createDto(5000L), asyncResponse, processEngine);
    handler.acquire();

    // assume
    assertThat(handler.getPendingRequests()).hasSize(1);
    verify(handler).suspend(5000L);

    List<LockedExternalTask> tasks = new ArrayList<>();
    tasks.add(lockedExternalTaskMock);

    when(externalTaskQueryTopicBuilder.execute()).thenReturn(tasks);

    addSecondsToClock(5);

    // when
    handler.acquire();

    // then
    verify(asyncResponse).resume(argThat(Matchers.hasSize(1)));
    assertThat(handler.getPendingRequests()).isEmpty();
    verify(handler).suspend(Long.MAX_VALUE);
  }

  @Test
  void shouldResumeAsyncResponseDueToTimeoutExpired_2() {
    // given
    when(fetchAndLockBuilder.subscribe()).thenReturn(externalTaskQueryTopicBuilder);
    when(externalTaskQueryTopicBuilder.execute()).thenReturn(Collections.emptyList());

    AsyncResponse asyncResponse = mock(AsyncResponse.class);
    handler.addPendingRequest(createDto(5000L), asyncResponse, processEngine);

    addSecondsToClock(1);
    handler.acquire();

    // assume
    assertThat(handler.getPendingRequests()).hasSize(1);
    verify(handler).suspend(4000L);

    addSecondsToClock(4);

    // when
    handler.acquire();

    // then
    verify(asyncResponse).resume(argThat(Matchers.hasSize(0)));
    assertThat(handler.getPendingRequests()).isEmpty();
    verify(handler).suspend(Long.MAX_VALUE);
  }

  @Test
  void shouldResumeAsyncResponseDueToTimeoutExpired_3() {
    // given
    when(fetchAndLockBuilder.subscribe()).thenReturn(externalTaskQueryTopicBuilder);
    when(externalTaskQueryTopicBuilder.execute()).thenReturn(Collections.emptyList());

    AsyncResponse asyncResponse = mock(AsyncResponse.class);
    handler.addPendingRequest(createDto(5000L), asyncResponse, processEngine);
    handler.addPendingRequest(createDto(4000L), asyncResponse, processEngine);

    addSecondsToClock(1);
    handler.acquire();

    // assume
    assertThat(handler.getPendingRequests()).hasSize(2);
    verify(handler).suspend(3000L);

    addSecondsToClock(4);

    // when
    handler.acquire();

    // then
    verify(asyncResponse, times(2)).resume(Collections.emptyList());
    assertThat(handler.getPendingRequests()).isEmpty();
    verify(handler).suspend(Long.MAX_VALUE);
  }

  @Test
  void shouldResumeAsyncResponseImmediatelyDueToProcessEngineException() {
    // given
    when(fetchAndLockBuilder.subscribe()).thenReturn(externalTaskQueryTopicBuilder);
    when(externalTaskQueryTopicBuilder.execute()).thenThrow(new ProcessEngineException());

    // when
    AsyncResponse asyncResponse = mock(AsyncResponse.class);
    handler.addPendingRequest(createDto(5000L), asyncResponse, processEngine);

    // Then
    assertThat(handler.getPendingRequests()).isEmpty();
    verify(handler, never()).suspend(anyLong());
    verify(asyncResponse).resume(any(ProcessEngineException.class));
  }

  @Test
  void shouldResumeAsyncResponseAfterBackoffDueToProcessEngineException() {
    // given
    when(fetchAndLockBuilder.subscribe()).thenReturn(externalTaskQueryTopicBuilder);
    when(externalTaskQueryTopicBuilder.execute()).thenReturn(Collections.emptyList());

    AsyncResponse asyncResponse = mock(AsyncResponse.class);
    handler.addPendingRequest(createDto(5000L), asyncResponse, processEngine);
    handler.acquire();

    // assume
    assertThat(handler.getPendingRequests()).hasSize(1);
    verify(handler).suspend(5000L);

    // when
    doThrow(new ProcessEngineException()).when(externalTaskQueryTopicBuilder).execute();
    handler.acquire();

    // then
    assertThat(handler.getPendingRequests()).isEmpty();
    verify(handler).suspend(Long.MAX_VALUE);
    verify(asyncResponse).resume(any(ProcessEngineException.class));
  }

  @Test
  void shouldResumeAsyncResponseDueToTimeoutExceeded() {
    // given - no pending requests

    // assume
    assertThat(handler.getPendingRequests()).isEmpty();

    // when
    AsyncResponse asyncResponse = mock(AsyncResponse.class);
    handler.addPendingRequest(createDto(FetchAndLockHandlerImpl.MAX_REQUEST_TIMEOUT + 1), asyncResponse, processEngine);

    // then
    verify(handler, never()).suspend(anyLong());
    assertThat(handler.getPendingRequests()).isEmpty();

    ArgumentCaptor<InvalidRequestException> argumentCaptor = ArgumentCaptor.forClass(InvalidRequestException.class);
    verify(asyncResponse).resume(argumentCaptor.capture());
    assertThat(argumentCaptor.getValue().getMessage()).isEqualTo("The asynchronous response timeout cannot " +
      "be set to a value greater than " + FetchAndLockHandlerImpl.MAX_REQUEST_TIMEOUT + " milliseconds");
  }

  @Test
  void shouldPollPeriodicallyWhenRequestPending() {
    // given
    doReturn(Collections.emptyList()).when(externalTaskQueryTopicBuilder).execute();

    // when
    AsyncResponse asyncResponse = mock(AsyncResponse.class);
    handler.addPendingRequest(createDto(FetchAndLockHandlerImpl.MAX_REQUEST_TIMEOUT), asyncResponse, processEngine);
    handler.acquire();

    // then
    verify(handler).suspend(FetchAndLockHandlerImpl.PENDING_REQUEST_FETCH_INTERVAL);
  }

  @Test
  void shouldNotPollPeriodicallyWhenNotRequestsPending() {
    // when
    handler.acquire();

    // then
    verify(handler).suspend(FetchAndLockHandlerImpl.MAX_BACK_OFF_TIME);
  }

  @Test
  void shouldCancelPreviousPendingRequestWhenWorkerIdsEqual() {
    // given
    doReturn(Collections.emptyList()).when(externalTaskQueryTopicBuilder).execute();

    handler.parseUniqueWorkerRequestParam("true");

    AsyncResponse asyncResponse = mock(AsyncResponse.class);
    handler.addPendingRequest(createDto(FetchAndLockHandlerImpl.MAX_REQUEST_TIMEOUT, "aWorkerId"), asyncResponse, processEngine);
    handler.acquire();

    handler.addPendingRequest(createDto(FetchAndLockHandlerImpl.MAX_REQUEST_TIMEOUT, "aWorkerId"), mock(AsyncResponse.class), processEngine);

    // when
    handler.acquire();

    // then
    verify(asyncResponse).cancel();
    assertThat(handler.getPendingRequests()).hasSize(1);
  }

  @Test
  void shouldNotCancelPreviousPendingRequestWhenWorkerIdsDiffer() {
    // given
    doReturn(Collections.emptyList()).when(externalTaskQueryTopicBuilder).execute();

    handler.parseUniqueWorkerRequestParam("true");

    AsyncResponse asyncResponse = mock(AsyncResponse.class);
    handler.addPendingRequest(createDto(FetchAndLockHandlerImpl.MAX_REQUEST_TIMEOUT, "aWorkerId"), asyncResponse, processEngine);
    handler.acquire();

    handler.addPendingRequest(createDto(FetchAndLockHandlerImpl.MAX_REQUEST_TIMEOUT, "anotherWorkerId"), mock(AsyncResponse.class), processEngine);

    // when
    handler.acquire();

    // then
    verify(asyncResponse, never()).cancel();
    assertThat(handler.getPendingRequests()).hasSize(2);
  }

  @Test
  void shouldResumeAsyncResponseDueToTooManyRequests() {
    // given

    // when
    AsyncResponse asyncResponse = mock(AsyncResponse.class);
    handler.errorTooManyRequests(asyncResponse);

    // then
    ArgumentCaptor<InvalidRequestException> argumentCaptor = ArgumentCaptor.forClass(InvalidRequestException.class);
    verify(asyncResponse).resume(argumentCaptor.capture());
    assertThat(argumentCaptor.getValue().getMessage()).isEqualTo("At the moment the server has to handle too " +
      "many requests at the same time. Please try again later.");
  }

  @Test
  void shouldSuspendForeverDueToNoPendingRequests() {
    // given - no pending requests

    // assume
    assertThat(handler.getPendingRequests()).isEmpty();

    // when
    handler.acquire();

    // then
    assertThat(handler.getPendingRequests()).isEmpty();
    verify(handler).suspend(Long.MAX_VALUE);
  }

  @Test
  void shouldRejectRequestDueToShutdown() {
    // given
    AsyncResponse asyncResponse = mock(AsyncResponse.class);
    handler.addPendingRequest(createDto(5000L), asyncResponse, processEngine);
    handler.acquire();

    // assume
    assertThat(handler.getPendingRequests()).hasSize(1);

    // when
    handler.rejectPendingRequests();

    // then
    ArgumentCaptor<RestException> argumentCaptor = ArgumentCaptor.forClass(RestException.class);

    verify(asyncResponse).resume(argumentCaptor.capture());
    assertThat(argumentCaptor.getValue().getStatus()).isEqualTo(Status.INTERNAL_SERVER_ERROR);
    assertThat(argumentCaptor.getValue().getMessage()).isEqualTo("Request rejected due to shutdown of application server.");
  }

  @Test
  void shouldInitialiseQueueWithSpecifiedParam() {
    // given
    String queueSizeParamValue = "5";
    when(servletContext.getInitParameter(BLOCKING_QUEUE_CAPACITY_PARAM_NAME)).then(invocation -> queueSizeParamValue);

    // when
    handler.contextInitialized(servletContextEvent);

    // then
    assertThat(handler.queue.remainingCapacity()).isEqualTo(5);
  }

  @Test
  void shouldInitialiseQueueWithDefaultCapacityWhenAbsentParam() {
    // given no parameter

    // when
    handler.contextInitialized(servletContextEvent);

    // then
    assertThat(handler.queue.remainingCapacity()).isEqualTo(DEFAULT_BLOCKING_QUEUE_CAPACITY);
  }

  @Test
  void shouldInitialiseQueueWithDefaultCapacityIfInvalidParam() {
    // given
    String queueSizeParamValue = "NaN";
    when(servletContext.getInitParameter(BLOCKING_QUEUE_CAPACITY_PARAM_NAME)).then(invocation -> queueSizeParamValue);

    // when
    handler.contextInitialized(servletContextEvent);

    // then
    assertThat(handler.queue.remainingCapacity()).isEqualTo(DEFAULT_BLOCKING_QUEUE_CAPACITY);
  }

  @Test
  void shouldInitialiseQueueWithDefaultCapacityIfNotGreaterThanZero() {
    // given
    String queueSizeParamValue = "-3";
    when(servletContext.getInitParameter(BLOCKING_QUEUE_CAPACITY_PARAM_NAME)).then(invocation -> queueSizeParamValue);

    // when
    handler.contextInitialized(servletContextEvent);

    // then
    assertThat(handler.queue.remainingCapacity()).isEqualTo(DEFAULT_BLOCKING_QUEUE_CAPACITY);
  }

  protected FetchExternalTasksExtendedDto createDto(Long responseTimeout, String workerId) {
    FetchExternalTasksExtendedDto externalTask = new FetchExternalTasksExtendedDto();

    FetchExternalTasksExtendedDto.FetchExternalTaskTopicDto topic = new FetchExternalTasksExtendedDto.FetchExternalTaskTopicDto();
    topic.setTopicName("aTopicName");
    topic.setLockDuration(12354L);

    externalTask.setMaxTasks(5);
    externalTask.setWorkerId(workerId);
    externalTask.setTopics(Collections.singletonList(topic));

    if (responseTimeout != null) {
      externalTask.setAsyncResponseTimeout(responseTimeout);
    }

    return externalTask;
  }

  protected FetchExternalTasksExtendedDto createDto(Long responseTimeout) {
    return createDto(responseTimeout, "aWorkerId");
  }

  protected Date addSeconds(Date date, int seconds) {
    return new Date(date.getTime() + seconds * 1000);
  }

  protected void addSecondsToClock(int seconds) {
    Date newDate = addSeconds(ClockUtil.getCurrentTime(), seconds);
    ClockUtil.setCurrentTime(newDate);
  }

  @Test
  void shouldLogExceptionDuringAcquireAndContinueRunning() {
    // given
    // Make acquire() throw an exception the first time, then work normally
    doThrow(new RuntimeException("Test exception"))
      .doNothing()
      .when(handler).acquire();
    
    handler.start();
    
    // when - give it time to execute the loop at least twice
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    
    // then
    // Verify acquire was called multiple times (proving it continued after exception)
    verify(handler, atLeast(2)).acquire();
    
    // Clean up
    handler.shutdown();
  }

}

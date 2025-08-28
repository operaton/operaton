/*
 * Copyright 2025 the Operaton contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.client.impl;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.operaton.bpm.client.backoff.BackoffStrategy;
import org.operaton.bpm.client.task.ExternalTask;
import org.operaton.bpm.client.task.ExternalTaskHandler;
import org.operaton.bpm.client.task.ExternalTaskService;
import org.operaton.bpm.client.task.impl.ExternalTaskImpl;
import org.operaton.bpm.client.topic.TopicSubscription;
import org.operaton.bpm.client.topic.impl.TopicSubscriptionBuilderImpl;
import org.operaton.bpm.client.topic.impl.TopicSubscriptionManager;
import org.operaton.bpm.client.variable.impl.DefaultValueMappers;
import org.operaton.bpm.client.variable.impl.TypedValueField;
import org.operaton.bpm.client.variable.impl.TypedValues;
import org.operaton.bpm.engine.variable.value.PrimitiveValue;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TopicSubscriptionManagerTest {

	private static final String T0 = "t0";
	private static final String T1 = "t1";

	TypedValues typedValues;
	EngineClient engineClient;
	TopicSubscriptionManagerForTesting topicSubscriptionManager;
	RecordingExternalTaskHandler t0Handler;
	RecordingExternalTaskHandler t1Handler;
	List<ExternalTask> taskList;
	TopicSubscription subscriptionT0;
	private TopicSubscription subscriptionT1;

	@BeforeEach
	void setUp() {
		typedValues = new TypedValues(new DefaultValueMappers<PrimitiveValue<String>>(""));
		engineClient = mock(EngineClient.class);
		topicSubscriptionManager = new TopicSubscriptionManagerForTesting(engineClient, typedValues, 0L);
		topicSubscriptionManager.setBackoffStrategy(new OneSecondBackOffStrategy());
		t0Handler = new RecordingExternalTaskHandler();
		taskList = new ArrayList<ExternalTask>();
		ExternalTaskImpl externalt0Task = new ExternalTaskImpl();
		externalt0Task.setTopicName(T0);
		externalt0Task.setVariables(new HashMap<String, TypedValueField>());
		taskList.add(externalt0Task);
		t1Handler = new RecordingExternalTaskHandler();
		ExternalTaskImpl externalt1Task = new ExternalTaskImpl();
		externalt1Task.setTopicName(T1);
		externalt1Task.setVariables(new HashMap<String, TypedValueField>());
		taskList.add(externalt1Task);
		when(engineClient.fetchAndLock(anyList())).thenReturn(taskList);
	}

	@AfterEach
	void tearDown() {
		waitForTopicSubscriptionManagerToFinish();
	}

	@Test
	void startStopFinishes() {
		assertDoesNotThrow(() -> {
			topicSubscriptionManager.start();
			topicSubscriptionManager.stop();
		});
	}

	@Test
	void isRunningAfterStart() {
		topicSubscriptionManager.start();
		assertThat(topicSubscriptionManager.isRunning()).isTrue();

		topicSubscriptionManager.stop();
	}

	@Test
	void isNotRunningAfterStop() {
		topicSubscriptionManager.start();
		topicSubscriptionManager.stop();
		assertThat(topicSubscriptionManager.isRunning()).isFalse();
	}

	@Test
	void taskExecutesOnce() {
		topicSubscriptionManager.start();
		subscribeTopicT0();
		waitMillies(500);
		topicSubscriptionManager.stop();

		assertThat(t0Handler.getExecuteCount()).isEqualTo(1);
	}

	@Test
	void taskExecutesOnceIfSubscribedBeforeStart() {
		subscribeTopicT0();
		topicSubscriptionManager.start();
		waitMillies(500);
		topicSubscriptionManager.stop();

		assertThat(t0Handler.getExecuteCount()).isEqualTo(1);
	}

	@Test
	void subscribeExecutesSecondTimeAfterBackupStrategyTimesOut() {
		subscribeTopicT0();
		topicSubscriptionManager.start();
		waitMillies(1500);
		topicSubscriptionManager.stop();

		assertThat(t0Handler.getExecuteCount()).isEqualTo(2);
	}

	@Test
	void twoTasksExecutedOnce() {
		subscribeTopicT0();
		subscribeTopicT1();
		topicSubscriptionManager.start();
		waitMillies(500);
		topicSubscriptionManager.stop();

		assertThat(t0Handler.getExecuteCount()).isEqualTo(1);
		assertThat(t1Handler.getExecuteCount()).isEqualTo(1);
	}

	@Test
	void twoTasksExecutedTwice() {
		subscribeTopicT0();
		subscribeTopicT1();
		topicSubscriptionManager.start();
		waitMillies(1500);
		topicSubscriptionManager.stop();

		assertThat(t0Handler.getExecuteCount()).isEqualTo(2);
		assertThat(t1Handler.getExecuteCount()).isEqualTo(2);
	}

	@Test
	void subscribeDuringRunning() {
		topicSubscriptionManager.start();
		waitMillies(1500);
		subscribeTopicT0();
		waitMillies(750); // 2000 ... t0 executed once
		subscribeTopicT1();
		waitMillies(500); // 3000 ... t0 executed second time, t1 executed once
		topicSubscriptionManager.stop();

		assertThat(t0Handler.getExecuteCount()).isEqualTo(2);
		assertThat(t1Handler.getExecuteCount()).isEqualTo(1);
	}

	@Test
	void unsubscribeDuringRunning() {
		subscribeTopicT0();
		subscribeTopicT1();
		topicSubscriptionManager.start();
		waitMillies(500); // t0 and t1 executed once
		unsubscribeTopicT1();
		waitMillies(1000); // t0 executed second time
		topicSubscriptionManager.stop();

		assertThat(t0Handler.getExecuteCount()).isEqualTo(2);
		assertThat(t1Handler.getExecuteCount()).isEqualTo(1);
	}

	@Test
	void unsubscribeAllBeforeRunning() {
		subscribeTopicT0();
		subscribeTopicT1();
		unsubscribeTopicT0();
		unsubscribeTopicT1();
		topicSubscriptionManager.start();
		waitMillies(500);
		topicSubscriptionManager.stop();

		assertThat(t0Handler.getExecuteCount()).isEqualTo(0);
		assertThat(t1Handler.getExecuteCount()).isEqualTo(0);
	}

	@Test
	void interruptIsWaiting() {
		subscribeTopicT0();
		subscribeTopicT1();
		topicSubscriptionManager.start();
		waitMillies(500); // t0 and t1 executed once
		topicSubscriptionManager.getThread().interrupt();
		waitMillies(250); // t0 and t1 executed second time because timer restart
		topicSubscriptionManager.stop();

		assertThat(t0Handler.getExecuteCount()).isEqualTo(2);
		assertThat(t1Handler.getExecuteCount()).isEqualTo(2);
	}

	private void waitMillies(int millies) {
		await().pollDelay(Duration.ofMillis(millies)).until(() -> true);
	}

	private void waitForTopicSubscriptionManagerToFinish() {
		try {
			topicSubscriptionManager.getThread().join();
		} catch (InterruptedException e) {
			fail(e);
		}
	}

	private void subscribeTopicT0() {
		subscriptionT0 = new TopicSubscriptionBuilderImpl(T0, topicSubscriptionManager).handler(t0Handler).open();
	}

	private void subscribeTopicT1() {
		subscriptionT1 = new TopicSubscriptionBuilderImpl(T1, topicSubscriptionManager).handler(t1Handler).open();
	}

	private void unsubscribeTopicT0() {
		subscriptionT0.close();
	}

	private void unsubscribeTopicT1() {
		subscriptionT1.close();
	}

}

// Testing class modifying visibility of fields and methods of TopicSubscriptionManager to ease testing
class TopicSubscriptionManagerForTesting extends TopicSubscriptionManager {
	public TopicSubscriptionManagerForTesting(EngineClient engineClient, TypedValues typedValues,
			long clientLockDuration) {
		super(engineClient, typedValues, clientLockDuration);
	}

	public Thread getThread() {
		return thread;
	}
}

// Testing class to count the numbers of "execute" calls for a specific task
class RecordingExternalTaskHandler implements ExternalTaskHandler {
	private int executeCount;

	@Override
	public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
		executeCount++;
	}

	public int getExecuteCount() {
		return executeCount;
	}
}

// Backup strategy that waits one second to make thread testing reliable
class OneSecondBackOffStrategy implements BackoffStrategy {
	@Override
	public void reconfigure(List<ExternalTask> externalTasks) {
		// unused
	}

	@Override
	public long calculateBackoffTime() {
		return 1000L;
	}
}

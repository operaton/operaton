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
package org.operaton.bpm.engine.impl.jobexecutor;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class BackoffJobAcquisitionStrategyTest {

  // strategy configuration
  protected static final long BASE_IDLE_WAIT_TIME = 50;
  protected static final float IDLE_INCREASE_FACTOR = 1.5F;
  protected static final long MAX_IDLE_TIME = 500;

  protected static final long BASE_BACKOFF_WAIT_TIME = 80;
  protected static final float BACKOFF_INCREASE_FACTOR = 2.0F;
  protected static final long MAX_BACKOFF_TIME = 1000;

  protected static final int DECREASE_THRESHOLD = 3;
  protected static final int NUM_JOBS_TO_ACQUIRE = 10;

  // misc
  protected static final String ENGINE_NAME = "engine";

  protected JobAcquisitionStrategy strategy;

  @BeforeEach
  void setUp() {
    strategy = new BackoffJobAcquisitionStrategy(
      BASE_IDLE_WAIT_TIME,
      IDLE_INCREASE_FACTOR,
      MAX_IDLE_TIME,
      BASE_BACKOFF_WAIT_TIME,
      BACKOFF_INCREASE_FACTOR,
      MAX_BACKOFF_TIME,
      DECREASE_THRESHOLD,
      NUM_JOBS_TO_ACQUIRE);
  }

  @Test
  void testIdleWaitTimeSequence() {

    JobAcquisitionContext context = new JobAcquisitionContext();

    // no acquired jobs -> idle level increases
    context.submitAcquiredJobs(ENGINE_NAME, buildAcquiredJobs(NUM_JOBS_TO_ACQUIRE, 0, 0));

    strategy.reconfigure(context);

    // level 1 -> base idle wait time
    assertThat(strategy.getWaitTime()).isEqualTo(BASE_IDLE_WAIT_TIME);

    // repeated idle cycles -> exponential increase until cap
    for (int idleLevel = 1; idleLevel < 6; idleLevel++) {
      context.reset();
      context.submitAcquiredJobs(ENGINE_NAME, buildAcquiredJobs(NUM_JOBS_TO_ACQUIRE, 0, 0));

      strategy.reconfigure(context);

      long expected = (long) (BASE_IDLE_WAIT_TIME * Math.pow(IDLE_INCREASE_FACTOR, idleLevel));
      assertThat(strategy.getWaitTime()).isEqualTo(expected);
    }

    // finally capped to MAX_IDLE_TIME
    context.reset();
    context.submitAcquiredJobs(ENGINE_NAME, buildAcquiredJobs(NUM_JOBS_TO_ACQUIRE, 0, 0));
    strategy.reconfigure(context);
    assertThat(strategy.getWaitTime()).isEqualTo(MAX_IDLE_TIME);
  }

  @Test
  void testAcquisitionAfterIdleWaitResetsIdle() {
    JobAcquisitionContext context = new JobAcquisitionContext();

    // first: idle
    context.submitAcquiredJobs(ENGINE_NAME, buildAcquiredJobs(NUM_JOBS_TO_ACQUIRE, 0, 0));
    strategy.reconfigure(context);
    assertThat(strategy.getWaitTime()).isEqualTo(BASE_IDLE_WAIT_TIME);

    // then: successful acquisition -> idle reset
    context.reset();
    context.submitAcquiredJobs(ENGINE_NAME, buildAcquiredJobs(NUM_JOBS_TO_ACQUIRE, NUM_JOBS_TO_ACQUIRE, 0));
    strategy.reconfigure(context);
    assertThat(strategy.getWaitTime()).isZero();
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 5, 9})
  void testAcquireLessJobsOnRejection(int numJobsRejected) {
    JobAcquisitionContext context = new JobAcquisitionContext();

    AcquiredJobs acquiredJobs = buildAcquiredJobs(NUM_JOBS_TO_ACQUIRE, NUM_JOBS_TO_ACQUIRE, 0);
    context.submitAcquiredJobs(ENGINE_NAME, acquiredJobs);

    // reject the first `numJobsRejected` batches
    for (int i = 0; i < numJobsRejected; i++) {
      context.submitRejectedBatch(ENGINE_NAME, acquiredJobs.getJobIdBatches().get(i));
    }

    strategy.reconfigure(context);

    assertThat(strategy.getNumJobsToAcquire(ENGINE_NAME)).isEqualTo(NUM_JOBS_TO_ACQUIRE - numJobsRejected);
    assertThat(strategy.getWaitTime()).isZero();
  }

  @Test
  void testWaitTimeOnFullRejectionTriggersExecutionSaturationWait() {
    JobAcquisitionContext context = new JobAcquisitionContext();

    AcquiredJobs acquiredJobs = buildAcquiredJobs(NUM_JOBS_TO_ACQUIRE, NUM_JOBS_TO_ACQUIRE, 0);
    context.submitAcquiredJobs(ENGINE_NAME, acquiredJobs);

    // reject all acquired batches
    for (int i = 0; i < NUM_JOBS_TO_ACQUIRE; i++) {
      context.submitRejectedBatch(ENGINE_NAME, acquiredJobs.getJobIdBatches().get(i));
    }

    strategy.reconfigure(context);

    assertThat(strategy.getWaitTime()).isEqualTo(BackoffJobAcquisitionStrategy.DEFAULT_EXECUTION_SATURATION_WAIT_TIME);
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 1, 2, 4, 5})
  void testCalculateBackoffTimeParameterized(int level) {
    BackoffJobAcquisitionStrategy impl = (BackoffJobAcquisitionStrategy) strategy;

    // ensure no jitter for deterministic assertion
    impl.applyJitter = false;
    impl.backoffLevel = level;

    long actual = impl.calculateBackoffTime();

    long expected;
    if (level <= 0) {
      expected = 0;
    } else if (level >= impl.maxBackoffLevel) {
      expected = MAX_BACKOFF_TIME;
    } else {
      expected = (long) (BASE_BACKOFF_WAIT_TIME * Math.pow(BACKOFF_INCREASE_FACTOR, level - 1));
    }

    assertThat(actual).isEqualTo(expected);
  }

  @Test
  void testCalculateBackoffTimeWithJitterWithinBounds() {
    BackoffJobAcquisitionStrategy impl = (BackoffJobAcquisitionStrategy) strategy;

    // pick a level that is within non-capped range
    impl.backoffLevel = 3;
    impl.applyJitter = true;

    long baseExpected = (long) (BASE_BACKOFF_WAIT_TIME * Math.pow(BACKOFF_INCREASE_FACTOR, impl.backoffLevel - 1));
    long actual = impl.calculateBackoffTime();

    // jitter adds up to backoffTime/2 -> result should be in [baseExpected, baseExpected * 1.5]
    assertThat(actual)
      .isGreaterThanOrEqualTo(baseExpected)
      .isLessThanOrEqualTo(baseExpected + (baseExpected / 2));
  }

  /**
   * numJobsToAcquire >= numJobsAcquired >= numJobsFailedToLock must hold
   */
  protected AcquiredJobs buildAcquiredJobs(int numJobsToAcquire, int numJobsAcquired, int numJobsFailedToLock) {
    AcquiredJobs acquiredJobs = new AcquiredJobs(numJobsToAcquire);
    for (int i = 0; i < numJobsAcquired; i++) {
      acquiredJobs.addJobIdBatch(List.of(Integer.toString(i)));
    }

    for (int i = 0; i < numJobsFailedToLock; i++) {
      acquiredJobs.removeJobId(Integer.toString(i));
    }

    return acquiredJobs;
  }
}

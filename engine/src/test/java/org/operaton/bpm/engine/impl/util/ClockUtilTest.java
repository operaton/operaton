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
package org.operaton.bpm.engine.impl.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Date;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ClockUtilTest {

  private static final long ONE_SECOND = 1000L;
  private static final long TWO_SECONDS = 2000L;
  private static final long FIVE_SECONDS = 5000L;
  private static final long TWO_DAYS = 172800000L;

  @BeforeEach
  void setUp() {
    ClockUtil.reset();
  }

  @AfterAll
  static void resetClock() {
    ClockUtil.reset();
  }

  @Test
  void nowShouldReturnCurrentTime() {
    assertThat(ClockUtil.now()).isCloseTo(new Date(), ONE_SECOND);
  }

  @Test
  void getCurrentTimeShouldReturnSameValueAsNow() {
    assertThat(ClockUtil.getCurrentTime()).isCloseTo(ClockUtil.now(), ONE_SECOND);
  }

  @Test
  void offsetShouldTravelInTime() throws InterruptedException {
    long duration = TWO_DAYS;
    Date target = new Date(new Date().getTime() + duration);

    ClockUtil.offset(duration);

    Thread.sleep(1100L);

    assertThat(ClockUtil.now()).isCloseTo(target, TWO_SECONDS);
  }

  @Test
  void setCurrentTimeShouldFreezeTime() throws InterruptedException {
    long duration = TWO_DAYS;
    Date target = new Date(new Date().getTime() + duration);

    ClockUtil.setCurrentTime(target);

    Thread.sleep(1100L);

    assertThat(ClockUtil.now()).isCloseTo(target, ONE_SECOND);
  }

  @Test
  void resetClockShouldResetToCurrentTime() {
    long duration = TWO_DAYS;
    Date target = new Date(new Date().getTime() + duration);

    ClockUtil.offset(duration);

    assertThat(ClockUtil.now()).isCloseTo(target, ONE_SECOND);

    assertThat(ClockUtil.resetClock()).isCloseTo(new Date(), ONE_SECOND);
    assertThat(ClockUtil.getCurrentTime()).isCloseTo(new Date(), ONE_SECOND);
  }

  @Test
  void resetShouldResetToCurrentTime() {
    long duration = TWO_DAYS;
    Date target = new Date(new Date().getTime() + duration);

    ClockUtil.offset(duration);

    assertThat(ClockUtil.now()).isCloseTo(target, ONE_SECOND);

    ClockUtil.reset();

    assertThat(ClockUtil.now()).isCloseTo(new Date(), ONE_SECOND);
  }

  @Test
  void timeShouldMoveOnAfterTravel() throws InterruptedException {
    Date now = new Date();
    long duration = TWO_DAYS;
    Date target = new Date(now.getTime() + duration);

    ClockUtil.offset(duration);

    assertThat(ClockUtil.now()).isCloseTo(target, ONE_SECOND);

    Thread.sleep(FIVE_SECONDS);

    assertThat(ClockUtil.now()).isCloseTo(new Date(target.getTime() + FIVE_SECONDS), ONE_SECOND);
  }

  @Test
  void timeShouldFreezeWithSetCurrentTime() throws InterruptedException {
    Date now = new Date();
    long duration = TWO_DAYS;
    Date target = new Date(now.getTime() + duration);
    ClockUtil.setCurrentTime(target);

    Thread.sleep(FIVE_SECONDS);

    assertThat(ClockUtil.now()).isCloseTo(target, ONE_SECOND);
  }
}

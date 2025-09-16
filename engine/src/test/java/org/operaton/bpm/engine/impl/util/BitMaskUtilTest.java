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
 * 
 */
package org.operaton.bpm.engine.impl.util;

import org.junit.jupiter.api.Test;

import org.operaton.bpm.engine.ProcessEngineException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class BitMaskUtilTest {

  @Test
  void testSetBitOn() {
    int bitOn = BitMaskUtil.setBitOn(3, 3);
    assertThat(bitOn).isEqualTo(7);
  }

  @Test
  void testSetBitOff() {
    int bitOn = BitMaskUtil.setBitOff(7, 3);
    assertThat(bitOn).isEqualTo(3);
  }

  @Test
  void testIsBitOn() {
    assertThat(BitMaskUtil.isBitOn(7, 3)).isTrue();
    assertThat(BitMaskUtil.isBitOn(3, 3)).isFalse();
  }

  @Test
  void testSetBit() {
    int setBitFalse = BitMaskUtil.setBit(1, 1, false);
    assertThat(setBitFalse).isZero();

    int setBitTrue = BitMaskUtil.setBit(0, 1, true);
    assertThat(setBitTrue).isEqualTo(1);
  }

  @Test
  void testGetMaskForBit() {
    assertThat(BitMaskUtil.getMaskForBit(1)).isEqualTo(1);
    assertThat(BitMaskUtil.getMaskForBit(2)).isEqualTo(2);
    assertThat(BitMaskUtil.getMaskForBit(3)).isEqualTo(4);
    assertThat(BitMaskUtil.getMaskForBit(4)).isEqualTo(8);
    assertThat(BitMaskUtil.getMaskForBit(5)).isEqualTo(16);
    assertThat(BitMaskUtil.getMaskForBit(6)).isEqualTo(32);
    assertThat(BitMaskUtil.getMaskForBit(7)).isEqualTo(64);
    assertThat(BitMaskUtil.getMaskForBit(8)).isEqualTo(128);
  }

  @Test
  void testEnsureBitRange() {
    assertThatExceptionOfType(ProcessEngineException.class).isThrownBy(() -> BitMaskUtil.ensureBitRange(0));
    assertThatExceptionOfType(ProcessEngineException.class).isThrownBy(() -> BitMaskUtil.ensureBitRange(9));
    assertDoesNotThrow(() -> BitMaskUtil.ensureBitRange(1));
  }

}

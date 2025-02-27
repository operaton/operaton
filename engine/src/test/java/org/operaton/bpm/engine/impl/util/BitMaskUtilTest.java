package org.operaton.bpm.engine.impl.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.operaton.bpm.engine.ProcessEngineException;

import static org.junit.jupiter.api.Assertions.*;

class BitMaskUtilTest {

  @Test
  void testSetBitOn() {
    int bitOn = BitMaskUtil.setBitOn(3, 3);
    assertEquals(7, bitOn);
  }

  @Test
  void testSetBitOff() {
    int bitOn = BitMaskUtil.setBitOff(7, 3);
    assertEquals(3, bitOn);
  }

  @Test
  void testIsBitOn() {
    assertTrue(BitMaskUtil.isBitOn(7, 3));
    assertFalse(BitMaskUtil.isBitOn(3, 3));
  }

  @Test
  void testSetBit() {
    int setBitFalse = BitMaskUtil.setBit(1, 1, false);
    assertEquals(0, setBitFalse);

    int setBitTrue = BitMaskUtil.setBit(0, 1, true);
    assertEquals(1, setBitTrue);
  }

  @Test
  void testGetMaskForBit() {
    assertEquals(1, BitMaskUtil.getMaskForBit(1));
    assertEquals(2, BitMaskUtil.getMaskForBit(2));
    assertEquals(4, BitMaskUtil.getMaskForBit(3));
    assertEquals(8, BitMaskUtil.getMaskForBit(4));
    assertEquals(16, BitMaskUtil.getMaskForBit(5));
    assertEquals(32, BitMaskUtil.getMaskForBit(6));
    assertEquals(64, BitMaskUtil.getMaskForBit(7));
    assertEquals(128, BitMaskUtil.getMaskForBit(8));
  }

  @Test
  void testEnsureBitRange() {
    assertThrows(ProcessEngineException.class, () -> BitMaskUtil.ensureBitRange(0));
    assertThrows(ProcessEngineException.class, () -> BitMaskUtil.ensureBitRange(9));
    assertDoesNotThrow(() -> BitMaskUtil.ensureBitRange(1));
  }

}

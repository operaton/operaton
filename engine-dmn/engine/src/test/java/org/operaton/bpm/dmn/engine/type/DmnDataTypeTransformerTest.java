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
package org.operaton.bpm.dmn.engine.type;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.TimeZone;
import org.camunda.feel.syntaxtree.ZonedTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.operaton.bpm.dmn.engine.DmnEngineConfiguration;
import org.operaton.bpm.dmn.engine.DmnEngineException;
import org.operaton.bpm.dmn.engine.impl.DefaultDmnEngineConfiguration;
import org.operaton.bpm.dmn.engine.impl.spi.type.DmnDataTypeTransformer;
import org.operaton.bpm.dmn.engine.impl.spi.type.DmnDataTypeTransformerRegistry;
import org.operaton.bpm.dmn.engine.test.DmnEngineTest;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.engine.variable.value.TypedValue;

/**
 * Tests the build-in {@link DmnDataTypeTransformer}s.
 *
 * @author Philipp Ossler
 */
class DmnDataTypeTransformerTest extends DmnEngineTest {

  protected DmnDataTypeTransformerRegistry registry;

  @BeforeEach
  void initRegistry() {
    DmnEngineConfiguration configuration = dmnEngine.getConfiguration();
    registry = ((DefaultDmnEngineConfiguration) configuration).getTransformer().getDataTypeTransformerRegistry();
  }

  @Test
  void customType() {
    // by default, the factory should return a transformer for unsupported type
    // that just box the value into an untyped value
    assertThat(registry.getTransformer("custom").transform("42")).isEqualTo(Variables.untypedValue("42"));
  }

  @Test
  void stringType() {
    DmnDataTypeTransformer typeTransformer = registry.getTransformer("string");

    assertThat(typeTransformer.transform("abc")).isEqualTo(Variables.stringValue("abc"));
    assertThat(typeTransformer.transform(true)).isEqualTo(Variables.stringValue("true"));
    assertThat(typeTransformer.transform(4)).isEqualTo(Variables.stringValue("4"));
    assertThat(typeTransformer.transform(2L)).isEqualTo(Variables.stringValue("2"));
    assertThat(typeTransformer.transform(4.2)).isEqualTo(Variables.stringValue("4.2"));
  }

  @Test
  void booleanType() {
    DmnDataTypeTransformer typeTransformer = registry.getTransformer("boolean");

    assertThat(typeTransformer.transform(true)).isEqualTo(Variables.booleanValue(true));
    assertThat(typeTransformer.transform(false)).isEqualTo(Variables.booleanValue(false));

    assertThat(typeTransformer.transform("true")).isEqualTo(Variables.booleanValue(true));
    assertThat(typeTransformer.transform("false")).isEqualTo(Variables.booleanValue(false));
  }

  @Test
  void invalidStringValueForBooleanType() {
    assertThrows(IllegalArgumentException.class, () -> {
      DmnDataTypeTransformer typeTransformer = registry.getTransformer("boolean");

      typeTransformer.transform("NaB");
    });
  }

  @Test
  void integerType() {
    DmnDataTypeTransformer typeTransformer = registry.getTransformer("integer");

    assertThat(typeTransformer.transform(4)).isEqualTo(Variables.integerValue(4));
    assertThat(typeTransformer.transform("4")).isEqualTo(Variables.integerValue(4));
    assertThat(typeTransformer.transform(2L)).isEqualTo(Variables.integerValue(2));
    assertThat(typeTransformer.transform(4.0)).isEqualTo(Variables.integerValue(4));

    assertThat(typeTransformer.transform(Integer.MIN_VALUE)).isEqualTo(Variables.integerValue(Integer.MIN_VALUE));
    assertThat(typeTransformer.transform(Integer.MAX_VALUE)).isEqualTo(Variables.integerValue(Integer.MAX_VALUE));
  }

  @Test
  void invalidStringValueForIntegerType() {
    assertThrows(IllegalArgumentException.class, () -> {
      DmnDataTypeTransformer typeTransformer = registry.getTransformer("integer");

      typeTransformer.transform("4.2");
    });
  }

  @Test
  void invalidDoubleValueForIntegerType() {
    assertThrows(IllegalArgumentException.class, () -> {
      DmnDataTypeTransformer typeTransformer = registry.getTransformer("integer");

      typeTransformer.transform(4.2);
    });
  }

  @Test
  void invalidLongValueForIntegerType() {
    assertThrows(IllegalArgumentException.class, () -> {
      DmnDataTypeTransformer typeTransformer = registry.getTransformer("integer");

      typeTransformer.transform(Long.MAX_VALUE);
    });
  }

  @Test
  void invalidIntegerMinValueForIntegerType() {
    assertThrows(IllegalArgumentException.class, () -> {
      DmnDataTypeTransformer typeTransformer = registry.getTransformer("integer");

      typeTransformer.transform(Integer.MIN_VALUE - 1L);
    });
  }

  @Test
  void invalidIntegerMaxValueForIntegerType() {
    assertThrows(IllegalArgumentException.class, () -> {
      DmnDataTypeTransformer typeTransformer = registry.getTransformer("integer");

      typeTransformer.transform(Integer.MAX_VALUE + 1L);
    });
  }

  @Test
  void longType() {
    DmnDataTypeTransformer typeTransformer = registry.getTransformer("long");

    assertThat(typeTransformer.transform(2L)).isEqualTo(Variables.longValue(2L));
    assertThat(typeTransformer.transform("2")).isEqualTo(Variables.longValue(2L));
    assertThat(typeTransformer.transform(4)).isEqualTo(Variables.longValue(4L));
    assertThat(typeTransformer.transform(4.0)).isEqualTo(Variables.longValue(4L));

    assertThat(typeTransformer.transform(Long.MIN_VALUE)).isEqualTo(Variables.longValue(Long.MIN_VALUE));
    assertThat(typeTransformer.transform(Long.MAX_VALUE)).isEqualTo(Variables.longValue(Long.MAX_VALUE));
  }

  @Test
  void invalidStringValueForLongType() {
    assertThrows(IllegalArgumentException.class, () -> {
      DmnDataTypeTransformer typeTransformer = registry.getTransformer("long");

      typeTransformer.transform("4.2");
    });
  }

  @Test
  void invalidDoubleValueForLongType() {
    assertThrows(IllegalArgumentException.class, () -> {
      DmnDataTypeTransformer typeTransformer = registry.getTransformer("long");

      typeTransformer.transform(4.2);
    });
  }

  @Test
  void invalidDoubleMinValueForLongType() {
    assertThrows(IllegalArgumentException.class, () -> {
      DmnDataTypeTransformer typeTransformer = registry.getTransformer("long");

      typeTransformer.transform(Double.MIN_VALUE);
    });
  }

  @Test
  void doubleType() {
    DmnDataTypeTransformer typeTransformer = registry.getTransformer("double");

    assertThat(typeTransformer.transform(4.2)).isEqualTo(Variables.doubleValue(4.2));
    assertThat(typeTransformer.transform("4.2")).isEqualTo(Variables.doubleValue(4.2));
    assertThat(typeTransformer.transform(4)).isEqualTo(Variables.doubleValue(4.0));
    assertThat(typeTransformer.transform(4L)).isEqualTo(Variables.doubleValue(4.0));

    assertThat(typeTransformer.transform(Double.MIN_VALUE)).isEqualTo(Variables.doubleValue(Double.MIN_VALUE));
    assertThat(typeTransformer.transform(Double.MAX_VALUE)).isEqualTo(Variables.doubleValue(Double.MAX_VALUE));
    assertThat(typeTransformer.transform(-Double.MAX_VALUE)).isEqualTo(Variables.doubleValue(-Double.MAX_VALUE));
    assertThat(typeTransformer.transform(Long.MAX_VALUE)).isEqualTo(Variables.doubleValue((double) Long.MAX_VALUE));
  }

  @Test
  void invalidStringValueForDoubleType() {
    assertThrows(IllegalArgumentException.class, () -> {
      DmnDataTypeTransformer typeTransformer = registry.getTransformer("double");

      typeTransformer.transform("NaD");
    });
  }

  @Test
  void dateType() {
    DmnDataTypeTransformer typeTransformer = registry.getTransformer("date");

    Date date = toDate("2015-09-18T12:00:00", null);
    TypedValue dateValue = Variables.dateValue(date);

    assertThat(typeTransformer.transform("2015-09-18T12:00:00")).isEqualTo(dateValue);
    assertThat(typeTransformer.transform(date)).isEqualTo(dateValue);
  }

  @Test
  void shouldTransformZonedDateTime() {
    // given
    DmnDataTypeTransformer typeTransformer = registry.getTransformer("date");

    Date date = toDate("2015-09-18T12:00:00", "Europe/Berlin");
    TypedValue dateValue = Variables.dateValue(date);
    ZonedDateTime zonedDateTime = ZonedDateTime.of(2015, 9, 18, 12, 0, 0, 0, ZoneId.of("Europe/Berlin"));

    // when
    TypedValue transformedFromZonedDateTime = typeTransformer.transform(zonedDateTime);

    // then
    assertThat(transformedFromZonedDateTime).isEqualTo(dateValue);
  }

  @Test
  void shouldTransformLocalDateTime() {
    // given
    DmnDataTypeTransformer typeTransformer = registry.getTransformer("date");

    Date date = toDate("2015-09-18T15:00:00", null);
    TypedValue dateValue = Variables.dateValue(date);
    LocalDateTime localDateTime = LocalDateTime.parse("2015-09-18T15:00:00");

    // when
    TypedValue transformedFromLocalDateTime = typeTransformer.transform(localDateTime);

    // then
    assertThat(transformedFromLocalDateTime).isEqualTo(dateValue);
  }

  @Test
  void shouldThrowExceptionDueToUnsupportedType_LocalTime() {
    Throwable exception = assertThrows(DmnEngineException.class, () -> {
      // given
      DmnDataTypeTransformer typeTransformer = registry.getTransformer("date");

      java.time.LocalTime localTime = java.time.LocalTime.now();

      // when
      typeTransformer.transform(localTime);
    });
    assertTrue(exception.getMessage().contains("Unsupported type: 'java.time.LocalTime' " +
      "cannot be converted to 'java.util.Date'"));
  }

  @Test
  void shouldThrowExceptionDueToUnsupportedType_LocalDate() {
    Throwable exception = assertThrows(DmnEngineException.class, () -> {
      // given
      DmnDataTypeTransformer typeTransformer = registry.getTransformer("date");

      LocalDate localDate = LocalDate.now();

      // when
      typeTransformer.transform(localDate);
    });
    assertTrue(exception.getMessage().contains("Unsupported type: 'java.time.LocalDate' " +
      "cannot be converted to 'java.util.Date'"));
  }

  @Test
  void shouldThrowExceptionDueToUnsupportedType_ZonedTime() {
    Throwable exception = assertThrows(DmnEngineException.class, () -> {
      // given
      DmnDataTypeTransformer typeTransformer = registry.getTransformer("date");

      ZonedTime zonedTime = ZonedTime.parse("22:22:22@Europe/Berlin");

      // when
      typeTransformer.transform(zonedTime);
    });
    assertTrue(exception.getMessage().contains("Unsupported type: 'org.camunda.feel.syntaxtree.ZonedTime' " +
      "cannot be converted to 'java.util.Date'"));
  }

  @Test
  void shouldThrowExceptionDueToUnsupportedType_Duration() {
    Throwable exception = assertThrows(DmnEngineException.class, () -> {
      // given
      DmnDataTypeTransformer typeTransformer = registry.getTransformer("date");

      Duration duration = Duration.ofMillis(5);

      // when
      typeTransformer.transform(duration);
    });
    assertTrue(exception.getMessage().contains("Unsupported type: 'java.time.Duration' " +
      "cannot be converted to 'java.util.Date'"));
  }

  @Test
  void shouldThrowExceptionDueToUnsupportedType_Period() {
    Throwable exception = assertThrows(DmnEngineException.class, () -> {
      // given
      DmnDataTypeTransformer typeTransformer = registry.getTransformer("date");

      Period period = Period.ofDays(5);

      // when
      typeTransformer.transform(period);
    });
    assertTrue(exception.getMessage().contains("Unsupported type: 'java.time.Period' " +
      "cannot be converted to 'java.util.Date'"));
  }

  @Test
  void invalidStringForDateType() {
    assertThrows(IllegalArgumentException.class, () -> {
      DmnDataTypeTransformer typeTransformer = registry.getTransformer("date");

      typeTransformer.transform("18.09.2015 12:00:00");
    });
  }

  protected Date toDate(String date, String timeZone) {
    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    if (timeZone != null) {
        format.setTimeZone(TimeZone.getTimeZone(timeZone));
    }

    try {
      return format.parse(date);

    } catch (ParseException e) {
      throw new RuntimeException(e);

    }
  }

}

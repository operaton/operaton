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
package org.operaton.spin.spi;
import java.util.List;

import java.util.Collections;
import java.util.ServiceLoader;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;

import org.operaton.spin.DataFormats;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * @author Thorben Lindhauer
 */
class DataFormatLoadingTest {

  protected ServiceLoader<DataFormatProvider> mockServiceLoader;

  @SuppressWarnings("rawtypes")
  protected ServiceLoader<DataFormatConfigurator> mockConfiguratorLoader;

  private MockedStatic<ServiceLoader> mockServiceLoader1;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() {
    mockServiceLoader = mock(ServiceLoader.class);
    mockConfiguratorLoader = mock(ServiceLoader.class);
    mockServiceLoader1 = mockStatic(ServiceLoader.class);
    mockServiceLoader1.when(() -> ServiceLoader.load(ArgumentMatchers.eq(DataFormatProvider.class), ArgumentMatchers.any(ClassLoader.class))).thenReturn(mockServiceLoader);
    mockServiceLoader1.when(() -> ServiceLoader.load(ArgumentMatchers.eq(DataFormatConfigurator.class), ArgumentMatchers.any(ClassLoader.class))).thenReturn(mockConfiguratorLoader);
  }

  @AfterEach
  void tearDown () {
    mockServiceLoader1.close();
  }

  @Test
  void customDataFormatProvider() {
    // given a custom data format provider that is returned by the service loader API
    mockProviders(new CustomDataFormatProvider());
    mockConfigurators();

    // when the custom data format is requested
    DataFormat<?> customDataFormat = DataFormats.getDataFormat(CustomDataFormatProvider.NAME);

    // then it should be properly returned
    assertThat(customDataFormat)
      .isNotNull()
      .isSameAs(CustomDataFormatProvider.DATA_FORMAT);
  }


  @Test
  void configureDataFormat() {
    // given a custom data format provider that is returned by the service loader API
    mockProviders(new CustomDataFormatProvider());
    mockConfigurators(new ExampleCustomDataFormatConfigurator());

    DataFormat<?> format = DataFormats.getDataFormat(CustomDataFormatProvider.NAME);
    assertThat(format).isSameAs(CustomDataFormatProvider.DATA_FORMAT);

    // then the configuration was applied
    ExampleCustomDataFormat customFormat = (ExampleCustomDataFormat) format;
    assertThat(customFormat.getProperty()).isEqualTo(ExampleCustomDataFormatConfigurator.UPDATED_PROPERTY);
  }

  @Test
  void configureDataFormatWithConfiguratorList() {
    // given a custom data format provider that is returned by the service loader API
    mockProviders(new CustomDataFormatProvider());
    mockConfigurators();
    DataFormatConfigurator configurator = new ExampleCustomDataFormatConfigurator();

    // when a list of data format configurators is passed to the "load" method
    DataFormats.loadDataFormats(DataFormats.class.getClassLoader(),
                                Collections.singletonList(configurator));

    // then the configuration was applied
    ExampleCustomDataFormat customFormat = (ExampleCustomDataFormat) DataFormats
        .getDataFormat(CustomDataFormatProvider.NAME);
    assertThat(customFormat.getProperty())
        .isEqualTo(ExampleCustomDataFormatConfigurator.UPDATED_PROPERTY);
  }

  @Test
  void registerDataFormatWithConfiguratorList() {
    // given a custom data format provider that is returned by the service loader API
    mockProviders(new CustomDataFormatProvider());
    mockConfigurators();
    DataFormatConfigurator configurator = new ExampleCustomDataFormatConfigurator();

    // when a list of data format configurators is passed to the "load" method
    DataFormats.getInstance().registerDataFormats(DataFormats.class.getClassLoader(),
                                                  Collections.singletonList(configurator));

    // then the configuration was applied
    ExampleCustomDataFormat customFormat = (ExampleCustomDataFormat) DataFormats
        .getDataFormat(CustomDataFormatProvider.NAME);
    assertThat(customFormat.getProperty())
        .isEqualTo(ExampleCustomDataFormatConfigurator.UPDATED_PROPERTY);
  }

  @Test
  void shouldPassConfiguratorPropertiesToProvider() {
    // given a custom data format provider that is returned by the service loader API
    mockProviders(new CustomDataFormatProvider());
    mockConfigurators();

    // when a map of configuration properties is passed to the "load" method
    DataFormats.getInstance().registerDataFormats(DataFormats.class.getClassLoader(),
      Collections.emptyList(),
                                                  Collections.singletonMap("conditional-prop", true));

    // then the configuration property is applied
    ExampleCustomDataFormat customFormat = (ExampleCustomDataFormat) DataFormats
        .getDataFormat(CustomDataFormatProvider.NAME);
    assertThat(customFormat.isConditionalProperty())
        .isTrue();
  }

  protected void mockProviders(final DataFormatProvider... providers) {
    when(mockServiceLoader.iterator()).thenAnswer(invocation -> List.of(providers).iterator());
  }

  protected void mockConfigurators(final DataFormatConfigurator<?>... configurators) {
    when(mockConfiguratorLoader.iterator()).thenAnswer(invocation -> List.of(configurators).iterator());
  }
}

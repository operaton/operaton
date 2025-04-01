package org.operaton.spin.impl.xml.dom.format.spi;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import java.util.Iterator;
import java.util.ServiceLoader;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.operaton.spin.xml.SpinXmlDataFormatException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class DefaultJaxBContextProviderTest {
    @Test
    void getContext_shouldCreateContext() {
        // given
        var contextProvider = new DefaultJaxBContextProvider();

        // when
        var context = contextProvider.getContext();

        // then
        assertThat(context).isNotNull();
    }

    @Test
    void getContext_throwsAndLog_whenContextCreationFails() {
        // given
        var contextProvider = new DefaultJaxBContextProvider();

        try (var mockStaticServiceLoader = mockStatic(ServiceLoader.class)) {
            var mockServiceLoader = mock(ServiceLoader.class);
            when(mockServiceLoader.iterator()).thenReturn(mock(Iterator.class));
            mockStaticServiceLoader.when(() -> ServiceLoader.load(any())).thenReturn(mockServiceLoader);
            mockStaticServiceLoader.when(() -> ServiceLoader.load(Mockito.<Class>any(), any())).thenReturn(mockServiceLoader);
            // when + then
            assertThatThrownBy(() -> contextProvider.getContext(String.class))
                    .isInstanceOf(SpinXmlDataFormatException.class)
                    .hasMessage("SPIN/DOM-XML-01030 Cannot create context")
                    .hasCauseInstanceOf(JAXBException.class);
        }
        // when + then
    }
}

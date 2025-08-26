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
package org.operaton.bpm.run.test.util;

import java.io.InputStream;
import java.security.KeyStore;

import javax.net.ssl.*;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

public class TestUtils {

  public static ClientHttpRequestFactory createClientHttpRequestFactory() throws Exception {
    SSLContext sslContext = trustSelfSignedSSL();
    DefaultClientTlsStrategy tlsStrategy = new DefaultClientTlsStrategy(sslContext, HttpsURLConnection.getDefaultHostnameVerifier());
    PoolingHttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
        .setTlsSocketStrategy(tlsStrategy)
        .build();
    CloseableHttpClient httpClient = HttpClients.custom()
        .setConnectionManager(connectionManager)
        .build();
    return new HttpComponentsClientHttpRequestFactory(httpClient);
  }

  public static SSLContext trustSelfSignedSSL() throws Exception {
    // Load the keystore from the classpath
    try (InputStream keyStoreStream = TestUtils.class.getResourceAsStream("/keystore.p12")) {
      if (keyStoreStream == null) {
        throw new RuntimeException("Keystore not found in classpath");
      }

      KeyStore keyStore = KeyStore.getInstance("PKCS12");
      keyStore.load(keyStoreStream, "operaton".toCharArray());

      // Initialize the TrustManagerFactory with the keystore
      TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
      tmf.init(keyStore);

      // Get the TrustManagers from the factory
      TrustManager[] trustManagers = tmf.getTrustManagers();

      // Initialize the SSLContext with the TrustManagers
      SSLContext sc = SSLContext.getInstance("TLS");
      sc.init(null, trustManagers, new java.security.SecureRandom());
      HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

      // Set a HostnameVerifier that bypasses hostname verification
      HostnameVerifier allHostsValid = (hostname, session) -> true;
      HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);

      return sc;
    }
  }
}

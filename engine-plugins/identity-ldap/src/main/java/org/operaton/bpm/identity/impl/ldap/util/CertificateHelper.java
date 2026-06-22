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
package org.operaton.bpm.identity.impl.ldap.util;

import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;


public final class CertificateHelper {
  private CertificateHelper() {
  }

  public static void acceptUntrusted() {
    try {
      SSLContext sslContext = SSLContext.getInstance("TLS");
      sslContext.init(new KeyManager[0], new TrustManager[] { new DefaultTrustManager() }, new SecureRandom());
      SSLContext.setDefault(sslContext);
    } catch (Exception ex) {
      throw new IllegalStateException("Could not change SSL TrustManager to accept arbitrary certificates", ex);
    }
  }

  private static class DefaultTrustManager implements X509TrustManager {


    @Override
    public void checkClientTrusted(X509Certificate[] arg0, String arg1) {
      // do nothing
    }

    @Override
    public void checkServerTrusted(X509Certificate[] arg0, String arg1) {
      // do nothing
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
      return new X509Certificate[0];
    }

  }
}

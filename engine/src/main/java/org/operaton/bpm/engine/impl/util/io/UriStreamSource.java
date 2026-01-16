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
package org.operaton.bpm.engine.impl.util.io;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.operaton.bpm.engine.ProcessEngineException;


/**
 * @author Tom Baeyens
 */
public class UriStreamSource implements StreamSource {

  URI uri;

  public UriStreamSource(URL url) {
    try {
      this.uri = url.toURI();
    } catch (URISyntaxException e) {
      throw new ProcessEngineException("couldn't convert URL to URI: '%s'".formatted(url), e);
    }
  }

  public UriStreamSource(URI uri) {
    this.uri = uri;
  }

  @Override
  public InputStream getInputStream() {
    try {
      return uri.toURL().openStream();
    } catch (MalformedURLException e) {
      throw new ProcessEngineException("couldn't convert URI to URL: '%s'".formatted(uri), e);
    } catch (IOException e) {
      throw new ProcessEngineException("couldn't open uri '%s'".formatted(uri), e);
    }
  }
}

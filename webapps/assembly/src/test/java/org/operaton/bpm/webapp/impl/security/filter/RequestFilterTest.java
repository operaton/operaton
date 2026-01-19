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
package org.operaton.bpm.webapp.impl.security.filter;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 * @author nico.rehwaldt
 */
public class RequestFilterTest {

  protected static final String EMPTY_PATH = "";
  protected static final String CUSTOM_APP_PATH = "/my-custom/application/path";

  private RequestFilter matcher;

  private Map<String, String> matchResult;

  protected String applicationPath;

  public static Collection<String> data() {
    return List.of(EMPTY_PATH, CUSTOM_APP_PATH);
  }

  public void initRequestFilterTest(String applicationPath) {
    this.applicationPath = applicationPath;
  }

  @MethodSource("data")
  @ParameterizedTest
  void shouldMatchMethod(String applicationPath) {

    initRequestFilterTest(applicationPath);

    // given
    matcher = newMatcher("/foo/bar", "POST", "PUT");

    // when
    matchResult = matcher.match("GET", applicationPath + "/foo/bar");

    // then
    assertThat(matchResult).isNull();
  }

  @MethodSource("data")
  @ParameterizedTest
  void shouldNotMatchUri(String applicationPath) {

    initRequestFilterTest(applicationPath);

    // given
    matcher = newMatcher("/foo/bar", "GET");

    // when
    matchResult = matcher.match("GET", applicationPath + "/not-matching/");

    // then
    assertThat(matchResult).isNull();
  }

  @MethodSource("data")
  @ParameterizedTest
  void shouldMatch(String applicationPath) {

    initRequestFilterTest(applicationPath);

    // given
    matcher = newMatcher("/foo/bar", "GET");

    // when
    matchResult = matcher.match("GET", applicationPath + "/foo/bar");

    // then
    assertThat(matchResult).isNotNull();
  }

  @MethodSource("data")
  @ParameterizedTest
  void shouldExtractNamedUriParts(String applicationPath) {

    initRequestFilterTest(applicationPath);

    // given
    matcher = newMatcher("/{foo}/{bar}", "GET");

    // when
    matchResult = matcher.match("GET", applicationPath + "/foo/bar");

    // then
    assertThat(matchResult)
        .isNotNull()
        .containsEntry("foo", "foo")
        .containsEntry("bar", "bar");
  }

  @MethodSource("data")
  @ParameterizedTest
  void shouldExtractNamedMatchAllUriPart(String applicationPath) {

    initRequestFilterTest(applicationPath);

    // given
    matcher = newMatcher("/{foo}/{bar:.*}", "GET");

    // when
    matchResult = matcher.match("GET", applicationPath + "/foo/bar/asdf/asd");

    // then
    assertThat(matchResult)
        .isNotNull()
        .containsEntry("bar", "bar/asdf/asd");
  }

  private RequestFilter newMatcher(String uri, String ... methods) {
    return new RequestFilter(uri, applicationPath, methods);
  }
}

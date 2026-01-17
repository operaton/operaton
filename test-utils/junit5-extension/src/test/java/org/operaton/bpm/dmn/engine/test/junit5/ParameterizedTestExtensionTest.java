/*
 * Copyright and/or licensed under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. This file is licensed to you under the Apache License,
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
package org.operaton.bpm.dmn.engine.test.junit5;
import java.util.List;

import java.util.Collection;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;

import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameterized;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameters;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * This test uses the ParameterizedTestExtension and demonstrates that the
 * passed parameters are actually available in the @BeforeEach method.
 */
@Parameterized
public class ParameterizedTestExtensionTest {

	protected String requestUrl;
	protected boolean alreadyAuthenticated;

	public ParameterizedTestExtensionTest(String requestUrl, boolean alreadyAuthenticated) {
		this.requestUrl = requestUrl;
		this.alreadyAuthenticated = alreadyAuthenticated;
	}

	@Parameters
	public static Collection<Object[]> getRequestUrls() {
		return List.of(new Object[][] { { "/app/cockpit/default/", true }, { "/app/cockpit/engine2/", false }, });
	}

	@BeforeEach
	void setUp() {
		requestUrl = requestUrl + "processed/";
	}

	@AfterEach
	void tearDown() {
		requestUrl = null;
	}

	@TestTemplate
	void ensureBeforeEachCanProcessParameters() {
		if (alreadyAuthenticated) {
      assertThat(requestUrl).isEqualTo("/app/cockpit/default/processed/");
		} else {
      assertThat(requestUrl).isEqualTo("/app/cockpit/engine2/processed/");
		}
	}

}

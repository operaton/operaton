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
package org.operaton.bpm.engine.spring.test.expression.callactivity;

/**
 * The NextProcessExecutionEvaluator class  provides the name of the sub process to be executed next. This allows
 * us to test dynamically wire in the calledElement in the callActivity task. In an actual implementation there would
 * be business logic here to determine which process to execute in the callActivity task.
 *
 * @author  Sang Venkatraman
 *
 */
@SuppressWarnings("unused")
public class NextProcessExecutionEvaluator {

	public String returnProcessDefinitionToCall() {
	  //some business logic here
		return "simpleSubProcess";
	}

}

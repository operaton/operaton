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
package org.operaton.bpm.pa.service;


import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.operaton.bpm.engine.delegate.DelegateExecution;
import org.operaton.bpm.engine.delegate.JavaDelegate;

import static org.operaton.bpm.engine.variable.Variables.objectValue;

public class AnotherChangeVariablesService implements JavaDelegate {

  @Override
  public void execute(DelegateExecution execution) {
    Date now = new Date();

    List<String> serializable = new ArrayList<>();
    serializable.add("seven");
    serializable.add("eight");
    serializable.add("nine");

    List<Date> dateList = new ArrayList<>();
    dateList.add(new Date());
    dateList.add(new Date());
    dateList.add(new Date());

    List<CockpitVariable> cockpitVariableList = new ArrayList<>();
    cockpitVariableList.add(new CockpitVariable("foo", "bar"));
    cockpitVariableList.add(new CockpitVariable("foo2", "bar"));
    cockpitVariableList.add(new CockpitVariable("foo3", "bar"));

    byte[] bytes = "someAnotherBytes".getBytes();

    FailingSerializable failingSerializable = new FailingSerializable();

    Map<String, Integer> mapVariable = new HashMap<>();

    Map<String, Object> variables = new HashMap<>();

    variables.put("shortVar", (short) 789);
    variables.put("longVar", 555555L);
    variables.put("integerVar", 963852);

    variables.put("floatVar", 55.55);
    variables.put("doubleVar", 6123.2025);

    variables.put("trueBooleanVar", true);
    variables.put("falseBooleanVar", false);

    variables.put("stringVar", "fanta");

    variables.put("dateVar", now);

    variables.put("serializableCollection", serializable);

    variables.put("bytesVar", bytes);
    variables.put("value1", "blub");

    int random = (int)(ThreadLocalRandom.current().nextDouble() * 100);
    variables.put("random", random);

    variables.put("failingSerializable", failingSerializable);

    variables.put("mapVariable", mapVariable);

    variables.put("dateList", dateList);

    variables.put("cockpitVariableList", cockpitVariableList);

    execution.setVariablesLocal(variables);

    // set JSON variable

    JsonSerialized jsonSerialized = new JsonSerialized();
    jsonSerialized.setFoo("bar");

    execution.setVariable("jsonSerializable", objectValue(jsonSerialized).serializationDataFormat("application/json"));

    // set JAXB variable

    JaxBSerialized jaxBSerialized = new JaxBSerialized();
    jaxBSerialized.setFoo("bar");

    execution.setVariable("xmlSerializable", objectValue(jaxBSerialized).serializationDataFormat("application/xml"));

  }

}

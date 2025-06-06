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
package org.operaton.bpm.model.xml;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.operaton.bpm.model.xml.impl.util.StringUtil.joinCommaSeparatedList;
import static org.operaton.bpm.model.xml.impl.util.StringUtil.splitCommaSeparatedList;

/**
 * @author Sebastian Menski
 */
class StringUtilTest {

  @Test
  void testStringListSplit() {
    assertThat(splitCommaSeparatedList("")).isEmpty();
    assertThat(splitCommaSeparatedList("  ")).isEmpty();
    assertThat(splitCommaSeparatedList("a")).containsExactly("a");
    assertThat(splitCommaSeparatedList("  a  ")).containsExactly("a");
    assertThat(splitCommaSeparatedList("a,b")).containsExactly("a", "b");
    assertThat(splitCommaSeparatedList("a , b, c ")).containsExactly("a", "b", "c");
    assertThat(splitCommaSeparatedList("${}")).containsExactly("${}");
    assertThat(splitCommaSeparatedList(" #{ } ")).containsExactly("#{ }");
    assertThat(splitCommaSeparatedList(" #{}, ${a}, #{b} ")).containsExactly("#{}", "${a}", "#{b}");
    assertThat(splitCommaSeparatedList(" a, ${b}, #{c} ")).containsExactly("a", "${b}", "#{c}");
    assertThat(splitCommaSeparatedList(" #{a}, b, ,c ,${d} ")).containsExactly("#{a}", "b", "c", "${d}");
    assertThat(splitCommaSeparatedList(" #{a(b,c)}, d, ,e ,${fg(h , i , j)} ")).containsExactly("#{a(b,c)}", "d", "e", "${fg(h , i , j)}");
    assertThat(splitCommaSeparatedList(" #{a == (b, c)}, d = e, f ,${fg(h , i , j)} ")).containsExactly("#{a == (b, c)}", "d = e", "f", "${fg(h , i , j)}");
    assertThat(splitCommaSeparatedList("accountancy, ${fakeLdapService.findManagers(execution, emp)}")).containsExactly("accountancy", "${fakeLdapService.findManagers(execution, emp)}");
  }

  @Test
  void testStringListJoin() {
    assertThat(joinCommaSeparatedList(null)).isNull();
    List<String> testList = new ArrayList<>();
    assertThat(joinCommaSeparatedList(testList)).isEmpty();
    testList.add("a");
    assertThat(joinCommaSeparatedList(testList)).isEqualTo("a");
    testList.add("b");
    assertThat(joinCommaSeparatedList(testList)).isEqualTo("a, b");
    testList.add("${a,b,c}");
    assertThat(joinCommaSeparatedList(testList)).isEqualTo("a, b, ${a,b,c}");
    testList.add("foo");
    assertThat(joinCommaSeparatedList(testList)).isEqualTo("a, b, ${a,b,c}, foo");
    testList.add("#{bar(e,f,g)}");
    assertThat(joinCommaSeparatedList(testList)).isEqualTo("a, b, ${a,b,c}, foo, #{bar(e,f,g)}");
    String testString = joinCommaSeparatedList(testList);
    assertThat(splitCommaSeparatedList(testString)).containsAll(testList);
  }

  @Test
  void testNullSplit() {
    assertThat(splitCommaSeparatedList(null)).isEmpty();
  }

  @Test
  void testNullJoin() {
    assertThat(joinCommaSeparatedList(null)).isNull();
  }

}

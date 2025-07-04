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
package org.operaton.bpm.qa.performance.engine.framework.aggregate;

import org.operaton.bpm.qa.performance.engine.framework.PerfTestException;
import org.operaton.bpm.qa.performance.engine.framework.PerfTestResults;
import org.operaton.bpm.qa.performance.engine.util.JsonUtil;

import java.io.File;
import java.util.Comparator;

/**
 * A result aggregator is used to aggregate the results of a
 * performance testsuite run as a table.
 * <p>
 * The aggregator needs to be pointed to a directory containing the
 * result files. It will read the result file by file and delegate the
 * actual processing to a subclass implementation of this class.
 * </p>
 *
 * @author Daniel Meyer
 *
 */
public abstract class TabularResultAggregator {

  protected final File resultDirectory;
  private boolean isSortingEnabled = true;

  protected TabularResultAggregator(String resultsFolderPath) {
    resultDirectory = new File(resultsFolderPath);
    if(!resultDirectory.exists()) {
      throw new PerfTestException("Folder "+resultsFolderPath+ " does not exist.");
    }
  }

  public TabularResultAggregator sortResults(boolean isSortingEnabled) {
    this.isSortingEnabled = isSortingEnabled;
    return this;
  }

  public TabularResultSet execute() {
    TabularResultSet tabularResultSet = createAggregatedResultsInstance();

    File[] resultFiles = resultDirectory.listFiles();
    for (File resultFile : resultFiles) {
      if(resultFile.getName().endsWith(".json")) {
        processFile(resultFile, tabularResultSet);
      }
    }

    if(isSortingEnabled) {
      tabularResultSet.getResults().sort(Comparator.comparing(o -> o.get(0).toString()));
    }

    postProcessResultSet(tabularResultSet);

    return tabularResultSet;
  }

  protected void postProcessResultSet(TabularResultSet tabularResultSet) {
    // do nothing
  }

  protected void processFile(File resultFile, TabularResultSet tabularResultSet) {

    PerfTestResults results = JsonUtil.readObjectFromFile(resultFile.getAbsolutePath(), PerfTestResults.class);
    processResults(results, tabularResultSet);

  }

  protected abstract TabularResultSet createAggregatedResultsInstance();

  protected abstract void processResults(PerfTestResults results, TabularResultSet tabularResultSet);

}

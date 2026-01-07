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
package org.operaton.bpm.engine.impl.pvm.runtime;

import java.io.Serial;
import java.io.Serializable;

/**
 * Contains a predefined set of states activity instances may be in
 * during the execution of a process instance.
 *
 * @author nico.rehwaldt
 */
public interface ActivityInstanceState extends Serializable {

  ActivityInstanceState DEFAULT = new ActivityInstanceStateImpl(0, "default");
  ActivityInstanceState SCOPE_COMPLETE = new ActivityInstanceStateImpl(1, "scopeComplete");
  ActivityInstanceState CANCELED = new ActivityInstanceStateImpl(2, "canceled");
  ActivityInstanceState STARTING = new ActivityInstanceStateImpl(3, "starting");
  ActivityInstanceState ENDING = new ActivityInstanceStateImpl(4, "ending");

  int getStateCode();

  // /////////////////////////////////////////////////// default implementation

  class ActivityInstanceStateImpl implements ActivityInstanceState {
    @Serial private static final long serialVersionUID = 1L;

    public final int stateCode;
    protected final String name;

    public ActivityInstanceStateImpl(int suspensionCode, String string) {
      this.stateCode = suspensionCode;
      this.name = string;
    }

    @Override
    public int getStateCode() {
      return stateCode;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      return prime * result + stateCode;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      ActivityInstanceStateImpl other = (ActivityInstanceStateImpl) obj;
      return stateCode == other.stateCode;
    }

    @Override
    public String toString() {
      return name;
    }
  }
}

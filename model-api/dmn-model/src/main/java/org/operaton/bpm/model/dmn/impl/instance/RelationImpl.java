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
package org.operaton.bpm.model.dmn.impl.instance;

import static org.operaton.bpm.model.dmn.impl.DmnModelConstants.LATEST_DMN_NS;
import static org.operaton.bpm.model.dmn.impl.DmnModelConstants.DMN_ELEMENT_RELATION;

import java.util.Collection;

import org.operaton.bpm.model.dmn.instance.Column;
import org.operaton.bpm.model.dmn.instance.Expression;
import org.operaton.bpm.model.dmn.instance.Relation;
import org.operaton.bpm.model.dmn.instance.Row;
import org.operaton.bpm.model.xml.ModelBuilder;
import org.operaton.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder;
import org.operaton.bpm.model.xml.type.child.ChildElementCollection;
import org.operaton.bpm.model.xml.type.child.SequenceBuilder;

public class RelationImpl extends ExpressionImpl implements Relation {

  protected static ChildElementCollection<Column> columnCollection;
  protected static ChildElementCollection<Row> rowCollection;

  public RelationImpl(ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  @Override
  public Collection<Column> getColumns() {
    return columnCollection.get(this);
  }

  @Override
  public Collection<Row> getRows() {
    return rowCollection.get(this);
  }

  public static void registerType(ModelBuilder modelBuilder) {
    ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(Relation.class, DMN_ELEMENT_RELATION)
      .namespaceUri(LATEST_DMN_NS)
      .extendsType(Expression.class)
      .instanceProvider(RelationImpl::new);

    SequenceBuilder sequenceBuilder = typeBuilder.sequence();

    columnCollection = sequenceBuilder.elementCollection(Column.class)
      .build();

    rowCollection = sequenceBuilder.elementCollection(Row.class)
      .build();

    typeBuilder.build();
  }

}

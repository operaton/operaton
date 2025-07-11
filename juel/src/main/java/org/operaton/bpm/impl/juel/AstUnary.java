/*
 * Based on JUEL 2.2.1 code, 2006-2009 Odysseus Software GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
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
package org.operaton.bpm.impl.juel;

import jakarta.el.ELContext;
import jakarta.el.ELException;


public class AstUnary extends AstRightValue {
	public interface Operator {
		Object eval(Bindings bindings, ELContext context, AstNode node);
	}
	public abstract static class SimpleOperator implements Operator {
    @Override
    public Object eval(Bindings bindings, ELContext context, AstNode node) {
			return apply(bindings, node.eval(bindings, context));
		}

		protected abstract Object apply(TypeConverter converter, Object o);
	}
	public static final Operator EMPTY = new SimpleOperator() {
		@Override public Object apply(TypeConverter converter, Object o) { return BooleanOperations.empty(converter, o); }
		@Override public String toString() { return "empty"; }
	};
	public static final Operator NEG = new SimpleOperator() {
		@Override public Object apply(TypeConverter converter, Object o) { return NumberOperations.neg(converter, o); }
		@Override public String toString() { return "-"; }
	};
	public static final Operator NOT = new SimpleOperator() {
		@Override public Object apply(TypeConverter converter, Object o) { return !converter.convert(o, Boolean.class); }
		@Override public String toString() { return "!"; }
	};

	private final Operator operator;
	private final AstNode child;

	public AstUnary(AstNode child, AstUnary.Operator operator) {
		this.child = child;
		this.operator = operator;
	}

	public Operator getOperator() {
		return operator;
	}

	@Override
	public Object eval(Bindings bindings, ELContext context) throws ELException {
		return operator.eval(bindings, context, child);
	}

	@Override
	public String toString() {
		return "'" + operator.toString() + "'";
	}

	@Override
	public void appendStructure(StringBuilder b, Bindings bindings) {
		b.append(operator);
		b.append(' ');
		child.appendStructure(b, bindings);
	}

  @Override
  public int getCardinality() {
		return 1;
	}

  @Override
  public AstNode getChild(int i) {
		return i == 0 ? child : null;
	}
}

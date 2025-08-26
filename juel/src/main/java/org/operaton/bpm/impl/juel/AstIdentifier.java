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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import jakarta.el.ELContext;
import jakarta.el.ELException;
import jakarta.el.MethodInfo;
import jakarta.el.MethodNotFoundException;
import jakarta.el.PropertyNotFoundException;
import jakarta.el.ValueExpression;
import jakarta.el.ValueReference;


public class AstIdentifier extends AstNode implements IdentifierNode {
	private static final String ERROR_IDENTIFIER_METHOD_ACCESS = "error.identifier.method.access";
	private static final String ERROR_IDENTIFIER_METHOD_INVOCATION = "error.identifier.method.invocation";
	private static final String ERROR_IDENTIFIER_METHOD_NOTAMETHOD = "error.identifier.method.notamethod";
	private static final String ERROR_IDENTIFIER_METHOD_NOTFOUND = "error.identifier.method.notfound";
	private static final String ERROR_IDENTIFIER_PROPERTY_NOTFOUND = "error.identifier.property.notfound";

	private final String name;
	private final int index;

	public AstIdentifier(String name, int index) {
		this.name = name;
		this.index = index;
	}

  @Override
  public Class<?> getType(Bindings bindings, ELContext context) {
		ValueExpression expression = bindings.getVariable(index);
		if (expression != null) {
			return expression.getType(context);
		}
		context.setPropertyResolved(false);
		Class<?> result = context.getELResolver().getType(context, null, name);
		if (!context.isPropertyResolved()) {
			throw new PropertyNotFoundException(LocalMessages.get(ERROR_IDENTIFIER_PROPERTY_NOTFOUND, name));
		}
		return result;
	}


  @Override
  public boolean isLeftValue() {
		return true;
	}

  @Override
  public boolean isMethodInvocation() {
		return false;
	}

  @Override
  public boolean isLiteralText() {
		return false;
	}

  @Override
  public ValueReference getValueReference(Bindings bindings, ELContext context) {
		ValueExpression expression = bindings.getVariable(index);
		if (expression != null) {
			return expression.getValueReference(context);
		}
		return new ValueReference(null, name);
	}

	@Override
	public Object eval(Bindings bindings, ELContext context) {
		ValueExpression expression = bindings.getVariable(index);
		if (expression != null) {
			return expression.getValue(context);
		}
		context.setPropertyResolved(false);
		Object result = context.getELResolver().getValue(context, null, name);
		if (!context.isPropertyResolved()) {
			throw new PropertyNotFoundException(LocalMessages.get(ERROR_IDENTIFIER_PROPERTY_NOTFOUND, name));
		}
		return result;
	}

  @Override
  public void setValue(Bindings bindings, ELContext context, Object value) {
		ValueExpression expression = bindings.getVariable(index);
		if (expression != null) {
			expression.setValue(context, value);
			return;
		}
		context.setPropertyResolved(false);
		context.getELResolver().setValue(context, null, name, value);
		if (!context.isPropertyResolved()) {
			throw new PropertyNotFoundException(LocalMessages.get(ERROR_IDENTIFIER_PROPERTY_NOTFOUND, name));
		}
	}

  @Override
  public boolean isReadOnly(Bindings bindings, ELContext context) {
		ValueExpression expression = bindings.getVariable(index);
		if (expression != null) {
			return expression.isReadOnly(context);
		}
		context.setPropertyResolved(false);
		boolean result = context.getELResolver().isReadOnly(context, null, name);
		if (!context.isPropertyResolved()) {
			throw new PropertyNotFoundException(LocalMessages.get(ERROR_IDENTIFIER_PROPERTY_NOTFOUND, name));
		}
		return result;
	}

	protected Method getMethod(Bindings bindings, ELContext context, Class<?> returnType, Class<?>[] paramTypes) {
		Object value = eval(bindings, context);
		if (value == null) {
			throw new MethodNotFoundException(LocalMessages.get(ERROR_IDENTIFIER_METHOD_NOTFOUND, name));
		}
		if (value instanceof Method method) {
			if (returnType != null && !returnType.isAssignableFrom(method.getReturnType())) {
				throw new MethodNotFoundException(LocalMessages.get(ERROR_IDENTIFIER_METHOD_NOTFOUND, name));
			}
			if (!Arrays.equals(method.getParameterTypes(), paramTypes)) {
				throw new MethodNotFoundException(LocalMessages.get(ERROR_IDENTIFIER_METHOD_NOTFOUND, name));
			}
			return method;
		}
		throw new MethodNotFoundException(LocalMessages.get(ERROR_IDENTIFIER_METHOD_NOTAMETHOD, name, value.getClass()));
	}

  @Override
  public MethodInfo getMethodInfo(Bindings bindings, ELContext context, Class<?> returnType, Class<?>[] paramTypes) {
		Method method = getMethod(bindings, context, returnType, paramTypes);
		return new MethodInfo(method.getName(), method.getReturnType(), paramTypes);
	}

  @Override
  public Object invoke(Bindings bindings, ELContext context, Class<?> returnType, Class<?>[] paramTypes, Object[] params) {
		Method method = getMethod(bindings, context, returnType, paramTypes);
		try {
			return method.invoke(null, params);
		} catch (IllegalAccessException e) {
			throw new ELException(LocalMessages.get(ERROR_IDENTIFIER_METHOD_ACCESS, name));
		} catch (IllegalArgumentException e) {
			throw new ELException(LocalMessages.get(ERROR_IDENTIFIER_METHOD_INVOCATION, name, e));
		} catch (InvocationTargetException e) {
			throw new ELException(LocalMessages.get(ERROR_IDENTIFIER_METHOD_INVOCATION, name, e.getCause()));
		}
	}

	@Override
	public String toString() {
		return name;
	}

	@Override
	public void appendStructure(StringBuilder b, Bindings bindings) {
		b.append(bindings != null && bindings.isVariableBound(index) ? "<var>" : name);
	}

  @Override
  public int getIndex() {
		return index;
	}

  @Override
  public String getName() {
		return name;
	}

  @Override
  public int getCardinality() {
		return 0;
	}

  @Override
  public AstNode getChild(int i) {
		return null;
	}
}

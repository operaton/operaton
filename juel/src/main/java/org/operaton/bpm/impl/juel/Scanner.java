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

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;


/**
 * Handcrafted scanner.
 *
 * @author Christoph Beck
 */
public class Scanner {
	/**
	 * Scan exception type
	 */
	@SuppressWarnings("serial")
	public static class ScanException extends Exception {
		final int position;
		final String encountered;
		final String expected;
		public ScanException(int position, String encountered, String expected) {
			super(LocalMessages.get("error.scan", position, encountered, expected));
			this.position = position;
			this.encountered = encountered;
			this.expected = expected;
		}
	}

	public static class Token {
		private final Symbol symbol;
		private final String image;
		private final int length;
		public Token(Symbol symbol, String image) {
			this(symbol, image, image.length());
		}
		public Token(Symbol symbol, String image, int length) {
			this.symbol = symbol;
			this.image = image;
			this.length = length;
		}
		public Symbol getSymbol() {
			return symbol;
		}
		public String getImage() {
			return image;
		}
		public int getSize() {
			return length;
		}
		@Override
		public String toString() {
			return symbol.toString();
		}
	}

	public static class ExtensionToken extends Token {
		public ExtensionToken(String image) {
			super(Scanner.Symbol.EXTENSION, image);
		}
	}

	/**
	 * Symbol type
	 */
	public enum Symbol {
		EOF,
		PLUS("'+'"), MINUS("'-'"),
		MUL("'*'"), DIV("'/'|'div'"), MOD("'%'|'mod'"),
		LPAREN("'('"), RPAREN("')'"),
		IDENTIFIER,
		NOT("'!'|'not'"), AND("'&&'|'and'"), OR("'||'|'or'"),
		EMPTY("'empty'"), INSTANCEOF("'instanceof'"),
		INTEGER, FLOAT, TRUE("'true'"), FALSE("'false'"), STRING, NULL("'null'"),
		LE("'<='|'le'"), LT("'<'|'lt'"), GE("'>='|'ge'"), GT("'>'|'gt'"),
		EQ("'=='|'eq'"), NE("'!='|'ne'"),
		QUESTION("'?'"), COLON("':'"),
		TEXT,
		DOT("'.'"), LBRACK("'['"), RBRACK("']'"),
		COMMA("','"),
		START_EVAL_DEFERRED("'#{'"), START_EVAL_DYNAMIC("'${'"), END_EVAL("'}'"),
		EXTENSION; // used in syntax extensions
		private final String string;
		Symbol() {
			this(null);
		}
		Symbol(String string) {
			this.string = string;
		}
		@Override
		public String toString() {
			return string == null ? "<" + name() + ">" : string;
		}
	}

	private static final Map<String, Token> KEYMAP = new HashMap<>();
	private static final Map<Symbol, Token> FIXMAP = new EnumMap<>(Symbol.class);

	private static void addFixToken(Token token) {
		FIXMAP.put(token.getSymbol(), token);
	}

	private static void addKeyToken(Token token) {
		KEYMAP.put(token.getImage(), token);
	}

	static {
		addFixToken(new Token(Symbol.PLUS, "+"));
		addFixToken(new Token(Symbol.MINUS, "-"));
		addFixToken(new Token(Symbol.MUL, "*"));
		addFixToken(new Token(Symbol.DIV, "/"));
		addFixToken(new Token(Symbol.MOD, "%"));
		addFixToken(new Token(Symbol.LPAREN, "("));
		addFixToken(new Token(Symbol.RPAREN, ")"));
		addFixToken(new Token(Symbol.NOT, "!"));
		addFixToken(new Token(Symbol.AND, "&&"));
		addFixToken(new Token(Symbol.OR, "||"));
		addFixToken(new Token(Symbol.EQ, "=="));
		addFixToken(new Token(Symbol.NE, "!="));
		addFixToken(new Token(Symbol.LT, "<"));
		addFixToken(new Token(Symbol.LE, "<="));
		addFixToken(new Token(Symbol.GT, ">"));
		addFixToken(new Token(Symbol.GE, ">="));
		addFixToken(new Token(Symbol.QUESTION, "?"));
		addFixToken(new Token(Symbol.COLON, ":"));
		addFixToken(new Token(Symbol.COMMA, ","));
		addFixToken(new Token(Symbol.DOT, "."));
		addFixToken(new Token(Symbol.LBRACK, "["));
		addFixToken(new Token(Symbol.RBRACK, "]"));
		addFixToken(new Token(Symbol.START_EVAL_DEFERRED, "#{"));
		addFixToken(new Token(Symbol.START_EVAL_DYNAMIC, "${"));
		addFixToken(new Token(Symbol.END_EVAL, "}"));
		addFixToken(new Token(Symbol.EOF, null, 0));

		addKeyToken(new Token(Symbol.NULL, "null"));
		addKeyToken(new Token(Symbol.TRUE, "true"));
		addKeyToken(new Token(Symbol.FALSE, "false"));
		addKeyToken(new Token(Symbol.EMPTY, "empty"));
		addKeyToken(new Token(Symbol.DIV, "div"));
		addKeyToken(new Token(Symbol.MOD, "mod"));
		addKeyToken(new Token(Symbol.NOT, "not"));
		addKeyToken(new Token(Symbol.AND, "and"));
		addKeyToken(new Token(Symbol.OR, "or"));
		addKeyToken(new Token(Symbol.LE, "le"));
		addKeyToken(new Token(Symbol.LT, "lt"));
		addKeyToken(new Token(Symbol.EQ, "eq"));
		addKeyToken(new Token(Symbol.NE, "ne"));
		addKeyToken(new Token(Symbol.GE, "ge"));
		addKeyToken(new Token(Symbol.GT, "gt"));
		addKeyToken(new Token(Symbol.INSTANCEOF, "instanceof"));
	}

	private Token token;  // current token
 	private int position; // start position of current token
	private final String input;

	protected final StringBuilder builder = new StringBuilder();

	/**
	 * Constructor.
	 * @param input expression string
	 */
	protected Scanner(String input) {
		this.input = input;
	}

	public String getInput() {
		return input;
	}

	/**
	 * @return current token
	 */
	public Token getToken() {
		return token;
	}

	/**
	 * @return current input position
	 */
	public int getPosition() {
		return position;
	}

	/**
	 * @return <code>true</code> iff the specified character is a digit
	 */
	protected boolean isDigit(char c) {
		return c >= '0' && c <= '9';
	}

	/**
	 * @param s name
	 * @return token for the given keyword or <code>null</code>
	 */
	protected Token keyword(String s) {
		return KEYMAP.get(s);
	}

	/**
	 * @param symbol
	 * @return token for the given symbol
	 */
	protected Token fixed(Symbol symbol) {
		return FIXMAP.get(symbol);
	}

	protected Token token(Symbol symbol, String value, int length) {
		return new Token(symbol, value, length);
	}

	protected boolean isEval() {
		return token != null && token.getSymbol() != Symbol.TEXT && token.getSymbol() != Symbol.END_EVAL;
	}

	/**
	 * text token
	 */
	protected Token nextText() {
		builder.setLength(0);
		int i = position;
		int l = input.length();
		boolean escaped = false;
		while (i < l) {
			char c = input.charAt(i);
			if (escaped) {
				escaped = handleEscaped(c, i);
			} else if (isEvalStart(i)) {
				return token(Symbol.TEXT, builder.toString(), i - position);
			} else if (c == '\\') {
				escaped = true;
			} else {
				builder.append(c);
			}
			i++;
		}
		if (escaped) {
			builder.append('\\');
		}
		return token(Symbol.TEXT, builder.toString(), i - position);
	}

	private boolean isEvalStart(int i) {
		char c = input.charAt(i);
		return (c == '#' || c == '$') && (i + 1 < input.length()) && (input.charAt(i + 1) == '{');
	}

	private boolean handleEscaped(char c, int i) {
		if (isEvalStart(i)) {
			builder.append(c);
			return false;
		}
		builder.append('\\');
		if (c != '\\') {
			builder.append(c);
			return false;
		}
		return true;
	}

	/**
	 * string token
	 */
	protected Token nextString() throws ScanException {
		builder.setLength(0);
		char quote = input.charAt(position);
		int i = position+1;
		int l = input.length();
		while (i < l) {
			char c = input.charAt(i++);
			if (c == '\\') {
				if (i == l) {
					throw new ScanException(position, "unterminated string", quote + " or \\");
				} else {
					c = input.charAt(i++);
					if (c == '\\' || c == quote) {
						builder.append(c);
					} else {
						throw new ScanException(position, "invalid escape sequence \\%s".formatted(c), "\\%s or \\\\".formatted(quote));
					}
				}
			} else if (c == quote) {
				return token(Symbol.STRING, builder.toString(), i - position);
			} else {
				builder.append(c);
			}
		}
		throw new ScanException(position, "unterminated string", String.valueOf(quote));
	}

	/**
	 * number token
	 */
	protected Token nextNumber() {
		int i = scanDigits(position);
		Symbol symbol = Symbol.INTEGER;

		int nextI = scanDot(i);
		if (nextI > i) {
			symbol = Symbol.FLOAT;
			i = nextI;
		}

		nextI = scanExponent(i);
		if (nextI > i) {
			symbol = Symbol.FLOAT;
			i = nextI;
		}

		return token(symbol, input.substring(position, i), i - position);
	}

	private int scanDigits(int i) {
		int l = input.length();
		while (i < l && isDigit(input.charAt(i))) {
			i++;
		}
		return i;
	}

	private int scanDot(int i) {
		if (i < input.length() && input.charAt(i) == '.') {
			return scanDigits(i + 1);
		}
		return i;
	}

	private int scanExponent(int i) {
		int l = input.length();
		if (i < l && (input.charAt(i) == 'e' || input.charAt(i) == 'E')) {
			int e = i + 1;
			if (e < l && (input.charAt(e) == '+' || input.charAt(e) == '-')) {
				e++;
			}
			if (e < l && isDigit(input.charAt(e))) {
				return scanDigits(e + 1);
			}
		}
		return i;
	}

	/**
	 * token inside an eval expression
	 */
	protected Token nextEval() throws ScanException {
		char c1 = input.charAt(position);
		char c2 = position < input.length() - 1 ? input.charAt(position + 1) : (char) 0;

		Token operator = nextOperator(c1, c2);
		if (operator != null) {
			return operator;
		}

		if (isDigit(c1) || c1 == '.') {
			return nextNumber();
		}

		if (Character.isJavaIdentifierStart(c1)) {
			return nextIdentifier();
		}

		throw new ScanException(position, "invalid character '%s'".formatted(c1), "expression token");
	}

	private Token nextOperator(char c1, char c2) throws ScanException {
		Token t = nextArithmeticOrBracketOperator(c1);
		if (t != null) {
			return t;
		}
		switch (c1) {
			case '?': return fixed(Symbol.QUESTION);
			case ':': return fixed(Symbol.COLON);
			case ',': return fixed(Symbol.COMMA);
			case '.':
				if (!isDigit(c2)) {
					return fixed(Symbol.DOT);
				}
				break;
			case '=':
				return nextEqOperator(c2);
			case '&', '|', '!':
				return nextLogicalOperator(c1, c2);
			case '<', '>':
				return nextCmpOperator(c1, c2);
			case '"', '\'':
				return nextString();
			default: break;
		}
		return null;
	}

	private Token nextArithmeticOrBracketOperator(char c) {
		switch (c) {
			case '*': return fixed(Symbol.MUL);
			case '/': return fixed(Symbol.DIV);
			case '%': return fixed(Symbol.MOD);
			case '+': return fixed(Symbol.PLUS);
			case '-': return fixed(Symbol.MINUS);
			case '[': return fixed(Symbol.LBRACK);
			case ']': return fixed(Symbol.RBRACK);
			case '(': return fixed(Symbol.LPAREN);
			case ')': return fixed(Symbol.RPAREN);
			default: return null;
		}
	}

	private Token nextLogicalOperator(char c1, char c2) {
		if (c1 == '&') {
			if (c2 == '&') {
				return fixed(Symbol.AND);
			}
		} else if (c1 == '|') {
			if (c2 == '|') {
				return fixed(Symbol.OR);
			}
		} else if (c1 == '!') {
			if (c2 == '=') {
				return fixed(Symbol.NE);
			}
			return fixed(Symbol.NOT);
		}
		return null;
	}

	private Token nextEqOperator(char c2) {
		if (c2 == '=') {
			return fixed(Symbol.EQ);
		}
		return null;
	}

	private Token nextCmpOperator(char c1, char c2) {
		if (c1 == '<') {
			if (c2 == '=') {
				return fixed(Symbol.LE);
			}
			return fixed(Symbol.LT);
		}
		if (c2 == '=') {
			return fixed(Symbol.GE);
		}
		return fixed(Symbol.GT);
	}

	private Token nextIdentifier() {
		int i = position + 1;
		int l = input.length();
		while (i < l && Character.isJavaIdentifierPart(input.charAt(i))) {
			i++;
		}
		String name = input.substring(position, i);
		Token keyword = keyword(name);
		return keyword == null ? token(Symbol.IDENTIFIER, name, i - position) : keyword;
	}

	protected Token nextToken() throws ScanException {
		char inputCharAtPosition = input.charAt(position);
		if (isEval()) {
			if (inputCharAtPosition == '}') {
				return fixed(Symbol.END_EVAL);
			}
			return nextEval();
		} else {
			if (position+1 < input.length() && input.charAt(position+1) == '{') {
        if (inputCharAtPosition == '#') {
          return fixed(Symbol.START_EVAL_DEFERRED);
        } else if (inputCharAtPosition == '$') {
          return fixed(Symbol.START_EVAL_DYNAMIC);
        }
			}
			return nextText();
		}
	}

	/**
	 * Scan next token.
	 * After calling this method, {@link #getToken()} and {@link #getPosition()}
	 * can be used to retreive the token's image and input position.
	 * @return scanned token
	 */
	public Token next() throws ScanException {
		if (token != null) {
			position += token.getSize();
		}

		int length = input.length();

		if (isEval()) {
			while (position < length && Character.isWhitespace(input.charAt(position))) {
				position++;
			}
		}

		if (position == length) {
			token = fixed(Symbol.EOF);
			return token;
		}

		token = nextToken();
		return token;
	}
}

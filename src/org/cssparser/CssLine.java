/*
 * (C) Copyright 2016 Matthieu Rejou.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Matthieu Réjou
 */
package org.cssparser;

/**
 * Line of CSS, may be either :
 * <ul>
 * <li>Block opening {@link #BLOCK_OPENING}</li>
 * <li>Property declaration {@link #PROPERTY}</li>
 * <li>Block closure {@link #BLOCK_CLOSURE}</li>
 * </ul>
 * @author Matthieu Rejou
 */
public class CssLine {
	
	/** Property type could not be determined. */
	public static final int UNKOWN = 0;
	/**
	 * Block opening type constant, {@link #getDeclaration()} returns the part of code before the <code>'{'</code>,
	 * {@link #getValue()} is always <code>null</code>.
	 */
	public static final int BLOCK_OPENING = 1;
	/**
	 * Property type constant, {@link #getDeclaration()} returns the part of code before the <code>':'</code>,
	 * {@link #getValue()} returns the part after the ':'.
	 */
	public static final int PROPERTY = 3;
	/**
	 * Block closure type constant, {@link #getDeclaration()} returns the part of code before the <code>'{'</code>,
	 * {@link #getValue()} is always <code>null</code>.
	 */
	public static final int BLOCK_CLOSURE = 2;
	
	private final CssLine parent;
	private final int type;
	private final String declaration;
	private final String value;
	
	/**
	 * Constructor necessary for block closure after an implicit ';'.
	 * @param parent line, may be <code>null</code>
	 * @param type to set always {@link #BLOCK_CLOSURE})
	 */
	protected CssLine(CssLine parent, int type) {
		this.parent = parent;
		this.type = type;
		declaration = "";
		value = "";
	}
	
	/**
	 * Builds a new CSS line.
	 * @param parent line, may be <code>null</code>
	 * @param charBuffer for character tokens
	 * @param wordBuffer for string tokens
	 * @param length number of tokens, strictly positive
	 */
	protected CssLine(CssLine parent, char[] charBuffer, String[] wordBuffer, int length) {
		this.parent = parent;
		
		switch (charBuffer[length - 1]) {
		case '{':
			declaration = join(charBuffer, wordBuffer, 0, length - 1);
			value = null;
			type = BLOCK_OPENING;
			return;
			
		case '}':
			declaration = join(charBuffer, wordBuffer, 0, length - 1);
			value = null;
			if (!declaration.isEmpty()) // Not empty if there was an implicit ';'
				type = PROPERTY;
			else
				type = BLOCK_CLOSURE;
			return;
			
		case ';':
			type = PROPERTY;
			
			for (int i = 0; i < length - 1; i++) {
				if (charBuffer[i] == ':') {
					declaration = join(charBuffer, wordBuffer, 0, i);
					value = join(charBuffer, wordBuffer, i + 1, length - 1);
					return;
				}
			}

			declaration = join(charBuffer, wordBuffer, 0, length - 1);
			value = null; // Property without value (not possible in pure CSS)
			return;
		}
		
		// Can occur with unexpected code at end of file
		type = UNKOWN;
		declaration = join(charBuffer, wordBuffer, 0, length);
		value = null;
	}

	private String join(char[] charBuffer, String[] wordBuffer, int start, int end) {
		StringBuilder sb = new StringBuilder();
		char previous = '\0';
		
		for (int i = start; i < end; i++) {

			char c = charBuffer[i];

			if (i > start) {
				switch (c) {
				case '(':
				case ')':
				case ',':
				case ';':
				case ':':
				case '{':
				case '}':
					break;
				default:
					if (previous != '(')
						sb.append(' ');
				}
			}
			
			if (c != '\0')
				sb.append(c);
			else
				sb.append(wordBuffer[i]);
			
			previous = c;
		}
		return sb.toString();
	}
	
	/**
	 * Gets the type constant of the line.
	 * @return {@link #BLOCK_OPENING}, {@link #PROPERTY} {@link #BLOCK_CLOSURE} or {@link #UNKOWN}
	 */
	public int getType() {
		return type;
	}
	
	/**
	 * Gets the parent line.
	 * @return parent, typically <code>null</code> for block openings in pure CSS,
	 * or the block opening line for properties and block closures.
	 */
	public CssLine getParent() {
		return parent;
	}

	/**
	 * Gets the declaration part of the line.
	 * @return formatted code before <code>'{'</code> or <code>':'</code>, empty string for {@link #BLOCK_CLOSURE}.<br>
	 * When <code>':'</code> is missing, contains code before <code>';'</code>.<br>
	 * For {@link #UNKOWN} lines, contains whole code.
	 */
	public String getDeclaration() {
		return declaration;
	}

	/**
	 * Gets the property value.
	 * @return formatted code after the <code>':'</code>,
	 * <code>null</code> if <code>':'</code> is missing of if line is not a {@link #PROPERTY} 
	 */
	public String getValue() {
		return value;
	}
	
	/**
	 * Converts this line to a CSS formatted code.
	 * @return CSS code
	 */
	public String toCssCode() {
		StringBuilder sb = new StringBuilder();
		
		CssLine parent = this;
		if (type == BLOCK_CLOSURE)
			parent = parent.parent;
		while ((parent = parent.parent) != null)
			sb.append('\t');
		
		switch (type) {
		case PROPERTY:
			sb.append(declaration);
			if (value != null)
				sb.append(": ").append(value);
			sb.append(';');
			break;
		case BLOCK_OPENING:
			sb.append(declaration).append(" {");
			break;
		case BLOCK_CLOSURE:
			sb.append(declaration); // Devrait être vide
			sb.append('}');
			break;
		default:
			sb.append(declaration);
			break;
		}
		return sb.toString();
	}

	@Override
	public String toString() {
		String sType;
		switch (type) {
		case PROPERTY:
			sType = "PROPERTY";
			break;
		case BLOCK_OPENING:
			sType = "BLOCK_OPENING";
			break;
		case BLOCK_CLOSURE:
			sType = "BLOCK_CLOSURE";
			break;
		default:
			sType = "UNKOWN";
		}
		return toCssCode() + " /* " + sType + "*/";
	}
	
}

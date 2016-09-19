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

import java.io.IOException;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.util.Arrays;

/**
 * <p>
 * Lenient parser for CSS syntax identifying blocs, comments and properties.<br>
 * This parser does not parse meaning of property values.<br>
 * </p>
 * </p>
 * It is compatible with <a href="http://sass-lang.com/">sass</a> and <a href="http://lesscss.org/">less</a> syntaxes.<br>
 * </p>
 * </p>
 * Comments are skipped.
 * </p>
 * @author Matthieu Rejou
 */
public class LenientCssParser {

	private StreamTokenizer tokenizer;
	private CssLine parent;
	// Implicit ';' hack
	private boolean mustCloseBlock;

	private char[] charBuffer = new char[64];
	private String[] wordBuffer = new String[64];
	
	/**
	 * Builds a new parser over the giver reader
	 * @param reader providing CSS code
	 * @throws NullPointerException if reader is <code>null</code>
	 */
	public LenientCssParser(Reader reader) {
		if (reader == null)
			throw new NullPointerException();
		
		tokenizer = new StreamTokenizer(reader);
		tokenizer.resetSyntax();
		
		tokenizer.wordChars('a', 'z');
		tokenizer.wordChars('A', 'Z');
		tokenizer.wordChars(128 + 32, 255);
		
		tokenizer.wordChars('0', '9');
		tokenizer.wordChars('-', '-');
		tokenizer.wordChars('.', '.');

		tokenizer.whitespaceChars(0, ' ');
		tokenizer.quoteChar('"');
		tokenizer.quoteChar('\'');
		
		// Coulors & units
		tokenizer.wordChars('%', '%');
		tokenizer.wordChars('#', '#');
		
		tokenizer.wordChars('$', '$'); // sass identifiers
		tokenizer.wordChars('@', '@'); // less identifiers
	}
	
	/**
	 * Reads the next line of CSS.
	 * @return line or <code>null</code> if end of stream was reached, may be either :
	 * <ul>
	 * <li>Block opening {@link CssLine#BLOCK_OPENING}</li>
	 * <li>Property declaration {@link CssLine#PROPERTY}</li>
	 * <li>Block closure {@link CssLine#BLOCK_CLOSURE}</li>
	 * <li>Unknown type, typically at end of streams {@link CssLine#UNKOWN}</li>
	 * </ul>
	 * @throws IOException if any IO error occured 
	 */
	public CssLine nextLine() throws IOException {
		if (charBuffer == null)
			return null; // End of stream was reached
			
		// Forgotten ';'
		if (mustCloseBlock) {
			CssLine cssLine = new CssLine(parent, CssLine.BLOCK_CLOSURE);
			if (parent != null)
				parent = parent.getParent();
			mustCloseBlock = false;
			return cssLine;
		}
		
		int length = 0;
		
		boolean comment = false;
		char lastCommentChar = '\0';
		int ttype;
		
		while ((ttype = tokenizer.nextToken()) != StreamTokenizer.TT_EOF) {
			char c = '\0';
			if (ttype > 0)
				c = (char)ttype;
			String token = tokenizer.sval;
			
			// In a comment...
			if (comment) {
				if (c == '/' && lastCommentChar == '*') {
					comment = false;
					lastCommentChar = '\0'; // Ensures sticked comments do not merge
				} else {
					lastCommentChar = c;
				}
				continue;
			}

			// Buffers writing
			if (length == charBuffer.length) {
				charBuffer = Arrays.copyOf(charBuffer, charBuffer.length + 64);
				wordBuffer = Arrays.copyOf(wordBuffer, wordBuffer.length + 64);
			}
			charBuffer[length] = c;
			wordBuffer[length] = token;	
			
			// End of lines and comments
			if (ttype > 0) {
				c = (char)ttype;
				
				switch (c) {
				case ';':
					return new CssLine(parent, charBuffer, wordBuffer, ++length);
				case '{':
					parent = new CssLine(parent, charBuffer, wordBuffer, ++length);
					return parent;
				case '}':
					CssLine line = new CssLine(parent, charBuffer, wordBuffer, ++length);
					if (line.getType() == CssLine.PROPERTY)
						mustCloseBlock = true;
					else if (parent != null)
						parent = parent.getParent();
					return line;
				case '*':
					if (length > 0 && charBuffer[length - 1] == '/') {
						comment = true;
						length--; // Move just before the '/'
						continue;
					}
				}
				
			}

			length++;
		}

		CssLine line = null;
		if (length > 0)
			line = new CssLine(parent, charBuffer, wordBuffer, length); // Unexpected code is ending the file
		
		charBuffer = null;
		wordBuffer = null;
		return line;
	}
	
}

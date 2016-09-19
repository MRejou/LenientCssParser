package org.cssparser.tests;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StreamTokenizer;

import org.cssparser.CssLine;
import org.cssparser.LenientCssParser;
import org.junit.Test;

public class TokenizerTests {

	@Test
	public void testTokenizer() throws IOException {
		try (InputStream is = getClass().getResourceAsStream("test1.less");
				InputStreamReader isr = new InputStreamReader(is)) {
			StreamTokenizer tokenizer = new StreamTokenizer(isr);
			tokenizer.resetSyntax();
			
			tokenizer.wordChars('a', 'z');
			tokenizer.wordChars('A', 'Z');
			tokenizer.wordChars(128 + 32, 255);
			tokenizer.whitespaceChars(0, ' ');
			tokenizer.quoteChar('"');
			tokenizer.quoteChar('\'');
			
			tokenizer.wordChars('0', '9');
			tokenizer.wordChars('-', '-');
			tokenizer.wordChars('.', '.');
			tokenizer.wordChars('%', '%');
	        
			tokenizer.wordChars('$', '$');
			tokenizer.wordChars('#', '#');
			tokenizer.wordChars('@', '@');
			
			int tType;
			while ((tType = tokenizer.nextToken()) != StreamTokenizer.TT_EOF) {
				System.out.println(tType + " : " + tokenizer);
			}
		}
	}

	@Test
	public void testLess1() throws IOException {
		try (InputStream is = getClass().getResourceAsStream("test1.less");
				InputStreamReader isr = new InputStreamReader(is)) {
			LenientCssParser parser = new LenientCssParser(isr);
			CssLine line;
			while ((line = parser.nextLine()) != null)
				System.out.println(line.toString());
		}
	}

	@Test
	public void testSass1() throws IOException {
		try (InputStream is = getClass().getResourceAsStream("test1.sass");
				InputStreamReader isr = new InputStreamReader(is)) {
			LenientCssParser parser = new LenientCssParser(isr);
			CssLine line;
			while ((line = parser.nextLine()) != null)
				System.out.println(line.toString());
		}
	}
}

/** ========================================================================= *
 * Copyright (C)  2017, 2018 Stephan Wissel                                   *
 *                            All rights reserved.                            *
 *                                                                            *
 *  @author     Stephan H. Wissel (stw) <stephan@wissel@net>                  *
 *                                       @notessensei                         *
 * @version     1.0                                                           *
 * ========================================================================== *
 *                                                                            *
 * Licensed under the  Apache License, Version 2.0  (the "License").  You may *
 * not use this file except in compliance with the License.  You may obtain a *
 * copy of the License at <http://www.apache.org/licenses/LICENSE-2.0>.       *
 *                                                                            *
 * Unless  required  by applicable  law or  agreed  to  in writing,  software *
 * distributed under the License is distributed on an  "AS IS" BASIS, WITHOUT *
 * WARRANTIES OR  CONDITIONS OF ANY KIND, either express or implied.  See the *
 * License for the  specific language  governing permissions  and limitations *
 * under the License.                                                         *
 *                                                                            *
 * ========================================================================== *
 */
package net.wissel.blogrender;

import com.vladsch.flexmark.ast.Node;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.profiles.pegdown.Extensions;
import com.vladsch.flexmark.profiles.pegdown.PegdownOptionsAdapter;
import com.vladsch.flexmark.util.options.DataHolder;

public class MarkdownConverter {
	static final DataHolder OPTIONS = PegdownOptionsAdapter.flexmarkOptions(
            Extensions.ALL
    );

    static final Parser PARSER = Parser.builder(OPTIONS).build();
    static final HtmlRenderer RENDERER = HtmlRenderer.builder(OPTIONS).build();
    
	public static String markdown2Html(String markdownText) {
		Node document = PARSER.parse(markdownText);
        String result = RENDERER.render(document);
        return result;
	}

	/**
	 * 
	 * @param mdContenCandidate markdown
	 * @return HTML from Markdown that renders nicely for code highlighter
	 */
	public static String markdown2HtmlWithCode(String mdContentCandidate) {
		String htmlContent = MarkdownConverter.markdown2Html(mdContentCandidate);
		return MarkdownConverter.fixCodeHTML(htmlContent);
	}

	/**
	 * Need to fix the way code is rendered. I'm using SyntaxHighlighter, not
	 * just pre/code. Also Flexmark converts ' into &rsquo; need to reverse that
	 * @param candidate
	 * @return the fixed html
	 */
	private static String fixCodeHTML(String candidate) {
		StringBuilder result = new StringBuilder(candidate);
		// Need to check for <pre><code class="language-
		final String searchFor = "<pre><code class=\"language-";
		final String searchForClose = "</code></pre>";
		final String replaceWith = "<pre class=\"brush: ";
		final String replaceWithClose = "</pre>";
		final String searchFor2 = "&rsquo;";
		final String replaceWith2 = "'";
		
		// Code formatting
		while (result.indexOf(searchFor) > -1) {
			int startPos = result.indexOf(searchFor);
			int secondPart = result.indexOf(searchForClose, startPos);
			result.replace(secondPart, secondPart+searchForClose.length(), replaceWithClose);
			result.replace(startPos, startPos+searchFor.length(), replaceWith);
		}
		
		// Aphostroph handling
		while (result.indexOf(searchFor2) > -1) {
			int startPos = result.indexOf(searchFor2);
			result.replace(startPos, startPos+searchFor2.length(), replaceWith2);
		}
		return result.toString();
	}
}

/**
 * ========================================================================= *
 * Copyright (C) 2017, 2018 Stephan Wissel *
 * All rights reserved. *
 * *
 *
 * @author Stephan H. Wissel (stw) <stephan@wissel@net> *
 * @notessensei *
 * @version 1.0 *
 *          ==========================================================================
 *          *
 *          *
 *          Licensed under the Apache License, Version 2.0 (the "License"). You
 *          may *
 *          not use this file except in compliance with the License. You may
 *          obtain a *
 *          copy of the License at <http://www.apache.org/licenses/LICENSE-2.0>.
 *          *
 *          *
 *          Unless required by applicable law or agreed to in writing, software
 *          *
 *          distributed under the License is distributed on an "AS IS" BASIS,
 *          WITHOUT *
 *          WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 *          the *
 *          License for the specific language governing permissions and
 *          limitations *
 *          under the License. *
 *          *
 *          ==========================================================================
 *          *
 */
package net.wissel.blogrender;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import com.vladsch.flexmark.ext.admonition.AdmonitionExtension;
import com.vladsch.flexmark.ext.anchorlink.AnchorLinkExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.parser.PegdownExtensions;
import com.vladsch.flexmark.profile.pegdown.PegdownOptionsAdapter;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.DataHolder;
import com.vladsch.flexmark.util.data.MutableDataHolder;
import com.vladsch.flexmark.util.misc.Extension;

/**
 * @author Stephan H. Wissel
 */
public class MarkdownConverter {

  private static MutableDataHolder optionHolder = null;
  private static HtmlRenderer rendererHolder = null;
  private static Parser parserHolder = null;

  public static String markdown2Html(final String markdownText) {
    final Node document = MarkdownConverter.getParser().parse(markdownText);
    final String result = MarkdownConverter.getRenderer().render(document);
    return result;
  }

  /**
   * @param mdContenCandidate markdown
   * @return HTML from Markdown that renders nicely for code highlighter
   */
  public static String markdown2HtmlWithCode(final String mdContentCandidate) {
    final String htmlContent = MarkdownConverter.markdown2Html(mdContentCandidate);
    return MarkdownConverter.fixCodeHTML(htmlContent);
  }

  /**
   * Need to fix the way code is rendered. I'm using SyntaxHighlighter, not
   * just pre/code. Also Flexmark converts ' into &rsquo; need to reverse that
   * as well as " handling
   *
   * @param candidate
   * @return the fixed html
   */
  private static String fixCodeHTML(final String candidate) {
    final StringBuilder result = new StringBuilder(candidate);
    final Map<String, String> tobeFixed = new HashMap<>();
    // Next 2 lines are for prism
    // tobeFixed.put("<pre><code class=\"language-", "<pre class=\"brush: ");
    // tobeFixed.put("</code></pre>", "</pre>");
    tobeFixed.put("&rsquo;", "'");
    tobeFixed.put("&rdquo;", "\"");
    tobeFixed.put("&ldquo;", "\"");

    tobeFixed.forEach((searchFor, replaceWith) -> {
      while (result.indexOf(searchFor) > -1) {
        final int startPos = result.indexOf(searchFor);
        result.replace(startPos, startPos + searchFor.length(), replaceWith);
      }
    });

    return result.toString();
  }

  private static DataHolder getOptions() {

    if (MarkdownConverter.optionHolder == null) {

      final ArrayList<Extension> extensions = new ArrayList<>();
      extensions.add(AdmonitionExtension.create());
      extensions.add(TablesExtension.create());
      extensions.add(AnchorLinkExtension.create());
      final MutableDataHolder options = PegdownOptionsAdapter
          .flexmarkOptions(PegdownExtensions.ALL)
          .toMutable()
          .set(com.vladsch.flexmark.parser.Parser.EXTENSIONS, extensions)
          // /why: default wrapText=true would wrap the heading's own text in the
          // generated <a href="#id">, turning every heading into a visible link.
          // We only want the id (for deep-linking), not a visual/behavioural change
          // to existing headings, so keep the anchor empty and id-only.
          .set(AnchorLinkExtension.ANCHORLINKS_WRAP_TEXT, false);
      MarkdownConverter.optionHolder = options;
    }
    return MarkdownConverter.optionHolder;
  }

  private static Parser getParser() {
    if (MarkdownConverter.parserHolder == null) {
      MarkdownConverter.parserHolder = Parser.builder(MarkdownConverter.getOptions()).build();
    }
    return MarkdownConverter.parserHolder;
  }

  private static HtmlRenderer getRenderer() {
    if (MarkdownConverter.rendererHolder == null) {
      MarkdownConverter.rendererHolder =
          HtmlRenderer.builder(MarkdownConverter.getOptions()).build();
    }
    return MarkdownConverter.rendererHolder;
  }
}

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
   * Converts markdown destined for a page body.
   *
   * /why this is now a pass-through: it used to post-process the HTML to undo
   * flexmark's smart-quote entities (&rsquo; &ldquo; &rdquo;). Those entities
   * come from the typographic extension, and the extension list this converter
   * installs -- Admonition, Tables, AnchorLink -- does not include it. The
   * explicit Parser.EXTENSIONS set below REPLACES the list the pegdown adapter
   * would otherwise supply, so nothing registers typographic processing and the
   * entities are never produced. The method walked every post body running
   * three indexOf/replace loops that could not match. Removing it leaves the
   * rendered site byte-identical (verified over a full 1833-page render).
   *
   * Kept as a named method rather than inlined at the call sites: it marks
   * "body markdown" as distinct from comment markdown, which is where any
   * future code-specific handling would belong.
   *
   * @param mdContentCandidate markdown
   * @return HTML from Markdown
   */
  public static String markdown2HtmlWithCode(final String mdContentCandidate) {
    return MarkdownConverter.markdown2Html(mdContentCandidate);
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

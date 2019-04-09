/** ========================================================================= *
 * Copyright (C)  2019, Stephan H. Wissel                                     *
 *                            All rights reserved.                            *
 *                                                                            *
 *  @author     Stephan H. Wissel (stw) <stw@linux.com>                       *
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

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

/**
 * @author swissel
 *
 */
public class HTMLUtility {
    
    public static String getTextBody(final String htmlBody, final int numOfChars) {
        
        final Document hDoc = Jsoup.parse(htmlBody);
        final String rawText = hDoc.body().text();
        return HTMLUtility.smartSubString(rawText, numOfChars);
    }

    /**
     * Returns a substring of the input, but not cut in the middle of a word
     * but at a white space or newline
     * @param rawText
     * @param numOfChars
     * @return
     */
    private static String smartSubString(String rawText, int numOfChars) {
        if (numOfChars < 1) {
            return rawText;
        }
        String candidate = rawText.substring(0,numOfChars);
        int lastSpace = candidate.lastIndexOf(" ");
        int lastNewLine = candidate.lastIndexOf("\n");
        
        if (lastSpace > 0 && lastNewLine > 0) {
            if (lastSpace > lastNewLine) {
                return candidate.substring(0, lastSpace)+" ...";
            }
            return candidate.substring(0, lastNewLine);
        }
        
        return candidate;
        
    }

}

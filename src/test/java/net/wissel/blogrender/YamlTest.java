/** ========================================================================= *
 * Copyright (C)  2017, 2018 Salesforce Inc ( http://www.salesforce.com/      *
 *                            All rights reserved.                            *
 *                                                                            *
 *  @author     Stephan H. Wissel (stw) <swissel@salesforce.com>              *
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

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Iterator;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.DumperOptions.FlowStyle;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.Tag;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

/**
 * @author swissel
 *
 */
public class YamlTest {

    /**
     * @param args
     * @throws IOException
     */
    public static void main(final String[] args) throws IOException {
        final YamlTest yt = new YamlTest();
        yt.test1();

    }

    private void test1() throws IOException {

        final Config config = Config.get(Config.CONFIG_NAME);
        final BlogRenderer br = new BlogRenderer(config);
        br.loadBlogFromDisk();
        final DumperOptions options = new DumperOptions();
        options.setPrettyFlow(true);
        options.setAllowUnicode(true);
        options.setExplicitStart(true);
        // options.setExplicitEnd(true);
        final PrintStream pw = System.out;
        final Yaml yaml = new Yaml(options);
        final Iterator<BlogEntry> iter = br.getTheBlog().iterator();
        // for (int i = 0; i < 5; i++) {
        while (iter.hasNext()) {
            final BlogEntry be = iter.next();
            final BlogEntryMeta bc = new BlogEntryMeta(be);
            pw.println(yaml.dumpAs(bc, Tag.MAP, FlowStyle.BLOCK));
            pw.println(config.MARKDOW_SEPARATOR);
            pw.println(Files.asCharSource(new File(be.sourceFileName), Charsets.UTF_8).read());
        }

    }

}

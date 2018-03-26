package net.wissel.blogrender;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Map;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.DumperOptions.FlowStyle;
import org.yaml.snakeyaml.Yaml;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

public class BlogEntryExporter {

    final static String BLOG_EXTENSION = ".blog";

    final static String BLOG_MORE = ".blog.more";

    public static void main(final String args[]) throws IOException {
        final BlogEntryExporter exporter = new BlogEntryExporter();
        exporter.runExport();
    }

    private void runExport() throws IOException {
        final Config config = Config.get(Config.CONFIG_NAME);
        final BlogRenderer br = new BlogRenderer(config);
        br.loadBlogFromDisk(false);
        final DumperOptions options = new DumperOptions();
        options.setPrettyFlow(true);
        options.setAllowUnicode(true);
        options.setExplicitStart(true);
        options.setDefaultFlowStyle(FlowStyle.BLOCK);

        final Yaml yaml = new Yaml(options);
        final Iterator<BlogEntry> iter = br.getTheBlog().descendingIterator();
     

        // while (iter.hasNext()) {
        for (int i = 0; i < 5; i++) {
            final BlogEntry be = iter.next();
            System.out.println(be.metaFileName);
            final Map<String, Object> bc = be.asMap();

            final File outFile = new File(be.metaFileName + BlogEntryExporter.BLOG_EXTENSION);
            final PrintWriter pw = new PrintWriter(outFile);
            // pw.println(yaml.dumpAs(bc, Tag.MAP, FlowStyle.BLOCK));
            yaml.dump(bc, pw);
            pw.println(config.MARKDOW_SEPARATOR);
            pw.println(Files.asCharSource(new File(be.sourceFileName), Charsets.UTF_8).read());
            pw.flush();
            pw.close();
            if (be.sourceMoreFileName != null) {
                final File sourceMore = new File(be.sourceMoreFileName);
                final File targetMore = new File(be.metaFileName + BlogEntryExporter.BLOG_MORE);
                Files.copy(sourceMore, targetMore);
            }
        }

    }
}

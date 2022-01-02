package net.wissel.blogrender;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Map;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.DumperOptions.FlowStyle;
import org.yaml.snakeyaml.Yaml;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class BlogEntryExporter {

  final static String BLOG_EXTENSION = ".blog";
  final static String BLOG_COMMENT = ".comment";
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
    final Iterator<BlogEntry> iter = br.getTheBlog().iterator();

    while (iter.hasNext()) {
      final BlogEntry be = iter.next();
      System.out.println(be.metaFileName);
      final Map<String, Object> bc = be.asMap();

      final File outFile = new File(be.metaFileName + BlogEntryExporter.BLOG_EXTENSION);
      final PrintWriter pw = new PrintWriter(outFile);
      yaml.dump(bc, pw);
      pw.println(config.MARKDOW_SEPARATOR);
      pw.println(Files.asCharSource(new File(be.sourceFileName), Charsets.UTF_8).read());
      if (be.sourceMoreFileName != null) {
        final File sourceMore = new File(be.sourceMoreFileName);
        pw.println(config.MARKDOW_SEPARATOR);
        pw.println(Files.asCharSource(sourceMore, Charsets.UTF_8).read());
      }
      pw.flush();
      pw.close();

      // Now export comments
      if (!be.getComments().isEmpty()) {
        final GsonBuilder gb = new GsonBuilder();
        gb.setPrettyPrinting();
        gb.disableHtmlEscaping();
        final Gson gson = gb.create();
        be.getComments().forEach(comment -> {
          this.exportComment(gson, this.getCommentDirectory(config, be), comment);
        });
      }
    }

  }

  private String getCommentDirectory(Config config, BlogEntry be) {
    String result = config.sourceDirectory + "/comments/" + be.getDateURL() + "/";
    // Ensure it exists
    File commentDir = new File(result);
    if (!commentDir.exists()) {
      commentDir.mkdirs();
    }
    return result;
  }

  private void exportComment(final Gson gson, final String commentDir, final BlogComments comment) {
    String cName = commentDir + comment.getUNID() + BLOG_COMMENT;
    File commentFile = new File(cName);

    try {
      PrintWriter writer = new PrintWriter(commentFile);
      gson.toJson(comment, writer);
      writer.flush();
      writer.close();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }

  }
}

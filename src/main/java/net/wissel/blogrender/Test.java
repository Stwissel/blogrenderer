package net.wissel.blogrender;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

public class Test {

  /**
   * @param args
   * @throws IOException
   */
  public static void main(String[] args) throws IOException {
    Test t = new Test();
    t.test1();

  }

  public void test1() throws IOException {
    String dataName = "/Users/stw/Blog/blogsource/src/attachments/attachments.json";
    String templateDir = "/Users/stw/Blog/blogsource/src/layouts/";
    String template = "downloads.mustache";
    String destination = "/Users/stw/temp/index.html";

    try (FileOutputStream out = new FileOutputStream(new File(destination));
        FileInputStream in = new FileInputStream(new File(dataName));
        Writer pw = new PrintWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {

      MustacheFactory mf = new DefaultMustacheFactory(new File(templateDir));
      Mustache mustache = mf.compile(template);
      EntriesWithFiles ef = EntriesWithFiles.loadDataFromJson(in);
      mustache.execute(pw, ef);
    }

    System.out.println("Done");
  }

}

package net.wissel.blogrender;

import java.io.File;

public class CommentTest {

  public static void main(String[] args) {
    CommentTest ct = new CommentTest();
    ct.test1();

  }

  private final String url =
      "/Users/swissel/Blog/blogsource/src/comments/2018/04/2f945f3e-6676-45cd-9a81-18e5f00e9ebb.json";

  private void test1() {
    File f = new File(url);
    BlogComments bc = BlogComments.loadFromJson(f);
    System.out.println(bc);

  }

}

package net.wissel.blogrender;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DateTest {

  public static void main(String[] args) {
    Date today = new Date();
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/");
    System.out.println(sdf.format(today));

  }

}

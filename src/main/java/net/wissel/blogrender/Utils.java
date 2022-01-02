package net.wissel.blogrender;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Utils {

  /**
   * How many byte to read when reading a file
   */
  private static final int FILE_READ_BYTESIZE = 1024;


  /**
   * Reads the entire content of a file into a string
   * 
   * @param inFileName
   *        the file to be read
   * @return the resulting string
   */
  public static String file2String(String inFileName) {
    File inFile = new File(inFileName);

    if (!inFile.exists()) {
      System.err.println("No such file: " + inFileName);
      return null;
    } else if (inFile.isDirectory()) {
      System.err.println(inFileName + " is a directory, but must be a file");
      return null;
    }

    long filesize = inFile.length();

    StringBuffer fileData = new StringBuffer();
    long totalRead = 0L;
    try {
      filesize = inFile.length();
      BufferedReader reader = new BufferedReader(new FileReader(inFile));
      char[] buf = new char[Utils.FILE_READ_BYTESIZE];

      int numRead = 0;
      while ((numRead = reader.read(buf)) != -1) {
        totalRead += numRead;
        String readData = String.valueOf(buf, 0, numRead);
        fileData.append(readData);
        buf = new char[Utils.FILE_READ_BYTESIZE];
      }
      // The reported size is often longer than the real one, but never
      // more than the the buffer size
      if (totalRead + Utils.FILE_READ_BYTESIZE < filesize) {
        System.err.print("File read error, reported size is ");
        System.err.print(filesize);
        System.err.print(" but only read ");
        System.err.println(totalRead);
      } else {
        System.out.print("File " + inFileName + " read:");
        System.out.println(totalRead);
      }
      reader.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
    return fileData.toString();
  }

  public static String date2ComparableString(Date inDate) {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-DDD-HH-mm-ss-S");
    return sdf.format(inDate);
  }
}

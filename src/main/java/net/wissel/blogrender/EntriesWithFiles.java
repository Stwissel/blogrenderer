package net.wissel.blogrender;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class EntriesWithFiles {

  public TreeMap<String, FileEntry> entries = new TreeMap<>();
  private final boolean isFile = true;

  public static EntriesWithFiles loadDataFromJson(InputStream in) {
    EntriesWithFiles result = null;
    Gson gson = new GsonBuilder().create();
    result = gson.fromJson(new InputStreamReader(in), EntriesWithFiles.class);
    return result;
  }

  public Collection<FileEntry> getAttachmentList() {
    return this.entries.values();
  }

  /**
   * Save the object to a JSON file for reuse
   */
  public void saveDatatoJson(OutputStream out) {
    GsonBuilder gb = new GsonBuilder();
    gb.setPrettyPrinting();
    gb.disableHtmlEscaping();
    Gson gson = gb.create();
    try (
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {
      gson.toJson(this, writer);
    }
  }

  public FileEntry add(FileEntry e) {
    this.entries.put(e.url, e);
    return e;
  }

  public FileEntry add(String subject, String url, String description, Date created) {
    FileEntry e = new FileEntry(subject, url, description, created);
    return this.add(e);
  }

  static class FileEntry {
    String url;
    String subject;
    String description;
    Date created;
    List<FileEntry> subEntries = null;

    public FileEntry(String subject, String url, String description, Date created) {
      this.subject = subject;
      this.created = created;
      this.url = url;
      this.description = description;
    }

    public FileEntry add(String incomingSubject, String incomingUrl, String incomingDescription, Date incomingCreated) {
      FileEntry e = new FileEntry(incomingSubject, incomingUrl, incomingDescription, incomingCreated);
      return this.add(e);
    }

    public FileEntry add(FileEntry e) {
      if (this.subEntries == null) {
        this.subEntries = new LinkedList<>();
      }
      this.subEntries.add(e);
      return e;
    }

  }

  /**
   * Exposes the render configuration to Mustache, so templates need not
   * hardcode site constants such as the canonical host or the copyright.
   *
   * @return the render configuration singleton
   */
  public Config getConfig() {
    return Config.get();
  }

  public boolean isFile() {
    return this.isFile;
  }
}

/** ========================================================================= *
 * Copyright (C)  2017, 2022 Stephan Wissel                                   *
 *                            All rights reserved.                            *
 *                                                                            *
 *  @author     Stephan H. Wissel (stw) <stephan@wissel@net>                  *
 *                                       @notessensei                         *
 * @version     1.1                                                           *
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiConsumer;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.DumperOptions.FlowStyle;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.Tag;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class BlogEntry implements Serializable, Comparable<BlogEntry> {

  private static final String MARKDOWN = "MARKDOWN";
  public static final String NEWLINE = System.getProperty("line.separator");

  public static final String DATE_FORMAT = "dd MMMM yyyy";
  public static final String DATE_FORMATSHORT = "MMMM yyyy";
  public static final String DATE_FORMATSORT = "yyyy-MM-dd-HH-mm-ss";

  private static final long serialVersionUID = 1L;

  private static boolean handleFirstLine(final String curLine, Config config,
      StringBuilder yaml) {
    if (!config.MARKDOW_SEPARATOR.equals(curLine)) {
      // No yaml - no luck
      return true; // Still in first line
    }
    yaml.append(config.MARKDOW_SEPARATOR);
    yaml.append(System.lineSeparator());
    return false; // got line 1
  }

  private static BlogEntry readBlogEntryFromYaml(final StringBuilder yaml, final String curLine,
      final Config config) {
    if (config.MARKDOW_SEPARATOR.equals(curLine)) {
      // Now we create the blog entry
      return BlogEntry.loadMetaFromYaml(yaml.toString());
    }

    yaml.append(curLine);
    yaml.append(System.lineSeparator());
    return null;

  }

  private static boolean addBlogEntryBody(final boolean inMoreBody, final StringBuilder rawBody,
      final StringBuilder rawMore, final String curLine, final Config config) {
    boolean separatorFound = config.MARKDOW_SEPARATOR.equals(curLine);

    if (separatorFound) {
      return true;
    }

    if (inMoreBody) {
      rawMore.append(curLine);
      rawMore.append(System.lineSeparator());
    } else {
      rawBody.append(curLine);
      rawBody.append(System.lineSeparator());
    }

    return inMoreBody || separatorFound;
  }

  public static BlogEntry loadDataFromBlog(final InputStream in, final Config config) {
    BlogEntry result = null;

    final StringBuilder yaml = new StringBuilder();
    final StringBuilder rawBody = new StringBuilder();
    final StringBuilder rawMore = new StringBuilder();

    try (Scanner scanner = new Scanner(in, "UTF-8")) {
      boolean firstLine = true;
      boolean inMoreBody = false;
      while (scanner.hasNextLine()) {
        final String curLine = scanner.nextLine();
        if (firstLine) {
          firstLine = handleFirstLine(curLine, config, yaml);
        } else {
          // Second line onwards
          if (result == null) {
            result = readBlogEntryFromYaml(yaml, curLine, config);
          } else {
            // Raw content - could be mainBody or moreBody
            inMoreBody = addBlogEntryBody(inMoreBody, rawBody, rawMore, curLine, config);
          }
        }
      }
    }

    // eventually load additional Content from extra file
    return populateResultBody(result, rawBody, rawMore);
  }

  private static BlogEntry populateResultBody(final BlogEntry result, final StringBuilder rawBody,
      final StringBuilder rawMore) {

    if (result != null) {
      if ("HTML".equals(result.getSourceType())) {
        result.setMainBody(rawBody.toString());
        if (rawMore.length() > 0) {
          result.setMoreBody(rawMore.toString());
        }
      } else {
        result.setMainBody(MarkdownConverter.markdown2HtmlWithCode(rawBody.toString()));
        if (rawMore.length() > 0) {
          result.setMoreBody(MarkdownConverter.markdown2HtmlWithCode(rawMore.toString()));
        }
      }

    }
    return result;
  }

  public static BlogEntry loadDataFromJson(final InputStream in, final String fileName,
      final Config config) {
    BlogEntry result = null;
    final Gson gson = new GsonBuilder().create();
    result = gson.fromJson(new InputStreamReader(in), BlogEntry.class);

    // eventually load Blog entry from disk
    if (fileName != null) {
      result.addBlogBodyFromFiles(fileName);
    }

    // Load new Comments
    result.loadCommentsFromDisk(config);
    result.cleanupComments();
    return result;
  }

  private static final Map<String, BiConsumer<BlogEntry, String>> yamlMapper = new HashMap<>();

  static {
    yamlMapper.put("autor", BlogEntry::setAuthor);
    yamlMapper.put("author", BlogEntry::setAuthor);
    yamlMapper.put("location", BlogEntry::setLocation);
    yamlMapper.put("status", BlogEntry::setStatus);
    yamlMapper.put("title", BlogEntry::setTitle);
    yamlMapper.put("series", BlogEntry::setSeries);
    yamlMapper.put("unid", BlogEntry::setUNID);
    yamlMapper.put("url", BlogEntry::setEntryURL);
    yamlMapper.put("oldurl", BlogEntry::setOldURL);
    yamlMapper.put("heroimage", BlogEntry::setHeroImage);
    yamlMapper.put("commentsclosed",
        (result, valueString) -> result.setCommentsclosed(Boolean.valueOf(valueString)));
    yamlMapper.put("sourcetype", (result, valueString) -> {
      final String firstLetter = valueString.substring(0, 1).toLowerCase();
      if ("h".equals(firstLetter)) {
        result.setSourceType("HTML");
      } else if ("m".equalsIgnoreCase(firstLetter)) {
        result.setSourceType(MARKDOWN);
      } else {
        // For people who can't spell
        result.setSourceType(valueString);
      }
    });
  }

  // Meta Data
  private static BlogEntry loadMetaFromYaml(final String yamlString) {
    final BlogEntry result = new BlogEntry();
    final Yaml yaml = new Yaml();
    final Map<String, Object> meta = yaml.load(yamlString);
    meta.forEach((keyCandidate, value) -> loadOneMetaFromYaml(result, keyCandidate, value));
    return result;
  }

  @SuppressWarnings("unchecked")
  private static void loadOneMetaFromYaml(final BlogEntry result, String keyCandidate,
      Object value) {

    // No processing of empty values
    if (keyCandidate == null || value == null || String.valueOf(value).equals("null")) {
      return;
    }

    final String key = String.valueOf(keyCandidate).toLowerCase();
    final String valueString = String.valueOf(value);

    if ("category".equals(key) && value instanceof List) {
      result.setCategory((List<String>) value);
      return;
    }

    if ("publishdate".equals(key)) {
      // Might have quotes or quotes
      if (value instanceof Date) {
        result.setPublishDate((Date) value);
      } else {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        try {
          result.setPublishDate(sdf.parse(String.valueOf(value)));
        } catch (ParseException e) {
          SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd");
          try {
            result.setPublishDate(
                sdf2.parse(String.valueOf(value).substring(0, 10)));
          } catch (ParseException e2) {
            System.err.printf(
                "Can't parse the date: %s%n", value);
            result.setPublishDate(new Date());
          }

        }
      }
      return;
    }

    final BiConsumer<BlogEntry, String> mapper =
        yamlMapper.computeIfAbsent(key, k -> (blogEntry, unknownKey) ->
        // We don't add anything to the blog, just log it out
        System.err.printf("Unknown key encountered (ignoring): %s-%s%n", blogEntry, unknownKey));

    mapper.accept(result, valueString);

  }

  private String author;
  private List<String> category = new ArrayList<>();
  private Date publishDate = new Date();
  private String location;
  private String status;
  private String title;
  private String series = null;
  private String UNID;
  private String entryUrl;
  private String oldURL;
  private String heroImage;
  private Boolean commentsclosed = false;
  private String sourceType;
  private int descriptionSize = 300;

  /**
   * @param descriptionSize the descriptionSize to set
   */
  public void setDescriptionSize(int descriptionSize) {
    this.descriptionSize = descriptionSize;
  }

  // The HTML representation
  private String mainBody = null;

  // If there's more to read
  private String moreBody = null;
  private final Map<String, BlogComments> comments = new HashMap<>();
  // The following strings are redundant, but it
  // makes it easier to deal with the JSON then
  private String allBody;
  private String shortDate;
  private String dateCategory;

  // The following variables are only used by the
  // templating engine and are not fed into the JSON
  private transient Collection<LinkItem> allCategories = null;
  private transient Collection<LinkItem> allDateCategories = null;
  private transient Collection<LinkItem> seriesMember = null;
  private transient LinkItem previousItem = null;

  private transient LinkItem nextItem = null;
  private final boolean isBlog = true;
  public transient String metaFileName;

  public transient String sourceFileName = null;
  public transient String sourceMoreFileName = null;

  /**
   * @param category
   *        the category to set
   */
  public void addCategory(final String cat2add) {
    this.getCategory().add(cat2add);
  }

  public BlogEntry addComment(final BlogComments bc) {
    if (bc != null) {
      this.comments.put(bc.getUNID(), bc);
    }
    return this;
  }

  public Map<String, Object> asMap() {
    final Map<String, Object> result = new HashMap<>();
    this.nonNullMapEntry(result, "Author", this.getAuthor());
    this.nonNullMapEntry(result, "Category", this.getCategory());
    this.nonNullMapEntry(result, "PublishDate", this.getPublishDate());
    this.nonNullMapEntry(result, "Location", this.getLocation());
    this.nonNullMapEntry(result, "Status", this.getStatus());
    this.nonNullMapEntry(result, "Title", this.getTitle());
    this.nonNullMapEntry(result, "Series", this.getSeries());
    this.nonNullMapEntry(result, "UNID", this.getUNID());
    this.nonNullMapEntry(result, "entryUrl", this.getEntryUrl());
    this.nonNullMapEntry(result, "oldURL", this.getOldURL());
    this.nonNullMapEntry(result, "heroImage", this.getHeroImage());
    this.nonNullMapEntry(result, "commentsclosed", this.getCommentsclosed());
    this.nonNullMapEntry(result, "SourceType", this.getSourceType());
    return result;
  }

  @Override
  public int compareTo(final BlogEntry be) {

    final String thisString = Utils.date2ComparableString(this.getPublishDate());
    final String thatString = Utils.date2ComparableString(be.getPublishDate());

    return thisString.compareTo(thatString);

  }

  /**
   * @return the allBody
   */
  public String getAllBody() {
    return this.allBody;
  }

  /**
   * @return the allCategories
   */
  public Collection<LinkItem> getAllCategories() {
    return this.allCategories;
  }

  /**
   * @return the allDateCategories
   */
  public Collection<LinkItem> getAllDateCategories() {
    return this.allDateCategories;
  }

  public String getAuthor() {
    return this.author;
  }

  public String getHeroImage() {
    return this.heroImage;
  }

  public List<String> getCategory() {
    return this.category;
  }

  public String getCommentCount() {
    if (this.comments.isEmpty()) {
      return "0";
    }
    return Integer.toHexString(this.comments.size());
  }

  /**
   * @return the comments
   */
  public Set<BlogComments> getComments() {
    final Set<BlogComments> result = new TreeSet<>();
    result.addAll(this.comments.values());
    return result;
  }

  public Boolean getCommentsclosed() {
    return this.commentsclosed;
  }

  /**
   * @return the dateCategory
   */
  public String getDateCategory() {
    return this.dateCategory;
  }

  public String getDateMonth() {
    final SimpleDateFormat sdf = new SimpleDateFormat("MMMM");
    return sdf.format(this.getPublishDate());
  }

  public String getDateMonthNumber() {
    final SimpleDateFormat sdf = new SimpleDateFormat("MM");
    return sdf.format(this.getPublishDate());
  }

  public String getDateURL() {
    final SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM");
    return sdf.format(this.getPublishDate());
  }

  public String getDateYear() {
    final SimpleDateFormat sdf = new SimpleDateFormat("yyyy");
    if (this.getPublishDate() == null) {
      return sdf.format(new Date());
    }
    return sdf.format(this.getPublishDate());
  }

  public String getDescription() {
    return (this.title + " - " +
        HTMLUtility.getTextBody(this.getMainBody(), this.descriptionSize)).replace("\"",
            "'");
  }

  public Collection<LinkItem> getDisplayCategories() {
    final TreeSet<LinkItem> result = new TreeSet<>();
    this.getCategory().forEach(cat -> result.add(new LinkItem(cat)));
    return result;
  }

  /**
   * Returns an unique key for comparison
   *
   * @return
   */
  public String getKey() {
    return this.getShortDate() + " - " + this.getEntryUrl();
  }

  public LinkItem getLinkItem(final String baseURI) {
    return new LinkItem(this.getTitle(), baseURI + this.getEntryUrl(),
        Utils.date2ComparableString(this.getPublishDate()));
  }

  public String getLocation() {
    if ((this.location == null) || this.location.trim().equals("")) {
      this.setLocation("Singapore");
    }
    return this.location;
  }

  /**
   * @return the mainBody
   */
  public String getMainBody() {
    return this.mainBody;
  }

  /**
   * @return the moreBody
   */
  public String getMoreBody() {
    return this.moreBody;
  }

  /**
   * @return the nextItem
   */
  public LinkItem getNextItem() {
    return this.nextItem;
  }

  public String getOldURL() {
    return this.oldURL;
  }

  /**
   * @return the previousItem
   */
  public LinkItem getPreviousItem() {
    return this.previousItem;
  }

  public Date getPublishDate() {
    return (this.publishDate == null) ? new Date() : this.publishDate;
  }

  public String getPublishDateString() {
    final SimpleDateFormat sdf = new SimpleDateFormat(BlogEntry.DATE_FORMAT);
    return sdf.format(this.getPublishDate());
  }

  public String getPublishDateStringShort() {
    final SimpleDateFormat sdf = new SimpleDateFormat(BlogEntry.DATE_FORMATSHORT);
    return sdf.format(this.getPublishDate());
  }

  public String getPublishDateStringSort() {
    final SimpleDateFormat sdf = new SimpleDateFormat(BlogEntry.DATE_FORMATSORT);
    return sdf.format(this.getPublishDate());
  }

  public String getSeries() {
    return this.series;
  }

  public Collection<LinkItem> getSeriesMember() {
    return this.seriesMember;
  }

  /**
   * @return the shortDate
   */
  public String getShortDate() {
    if ((this.shortDate == null) || this.shortDate.equals("")) {
      this.shortDate = this.getPublishDateString();
    }
    return this.shortDate;
  }

  /**
   * @return the blogSourceType
   */
  public String getSourceType() {
    return this.sourceType;
  }

  public String getStatus() {
    return this.status;
  }

  public String getTitle() {
    return this.title;
  }

  public String getUNID() {
    return this.UNID;
  }

  public String getEntryUrl() {
    return this.entryUrl;
  }

  public boolean isBlog() {
    return this.isBlog;
  }

  public void saveBlogEntry(final Config config, final FileOutputStream out) {
    try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {

      final DumperOptions options = new DumperOptions();
      options.setPrettyFlow(true);
      options.setAllowUnicode(true);
      options.setExplicitStart(true);
      final Yaml yaml = new Yaml(options);
      pw.println(config.MARKDOW_SEPARATOR);
      pw.println(yaml.dumpAs(this.asMap(), Tag.MAP, FlowStyle.BLOCK));
      pw.println(config.MARKDOW_SEPARATOR);
      pw.println(this.getMainBody());
      if (!this.getMainBody().isEmpty()) {
        pw.println(config.MARKDOW_SEPARATOR);
        pw.println(this.getMoreBody());
      }
    }
  }

  /**
   * Save the object to a JSON file for reuse
   */
  public void saveDatatoJson(final OutputStream out) {
    final GsonBuilder gb = new GsonBuilder();
    gb.setPrettyPrinting();
    gb.disableHtmlEscaping();
    final Gson gson = gb.create();
    try (
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {
      gson.toJson(this, writer);
    }
  }

  /**
   * @param allCategories
   *        the allCategories to set
   */
  public void setAllCategories(final Collection<LinkItem> allCategories) {
    this.allCategories = allCategories;
  }

  /**
   * @param allDateCategories
   *        the allDateCategories to set
   */
  public void setAllDateCategories(final Collection<LinkItem> allDateCategories) {
    this.allDateCategories = allDateCategories;
  }

  public void setAuthor(final String author) {
    this.author = author;
  }

  public void setHeroImage(final String heroImage) {
    this.heroImage = heroImage;
  }

  public void setCategory(final List<String> category) {
    this.category = category;
  }

  /**
   * @param comments
   *        the comments to set
   */
  public void setComments(final Set<BlogComments> comments) {
    this.comments.clear();
    comments.forEach(comment -> this.comments.put(comment.getUNID(), comment));
  }

  public void setCommentsclosed(final Boolean commentsclosed) {
    this.commentsclosed = commentsclosed;
  }

  public void setLocation(final String location) {
    this.location = location;
  }

  /**
   * @param mainBody
   *        the mainBody to set
   */
  public void setMainBody(final String mainBody) {

    this.mainBody = mainBody;

    if (this.moreBody == null) {
      this.allBody = this.mainBody;
    } else {
      this.allBody = this.mainBody + this.moreBody;
    }
  }

  /**
   * @param moreBody
   *        the moreBody to set
   */
  public void setMoreBody(final String moreBody) {
    this.moreBody = moreBody;

    if (this.mainBody == null) {
      this.allBody = this.moreBody;
    } else {
      this.allBody = this.mainBody + this.moreBody;
    }
  }

  /**
   * @param nextItem
   *        the nextItem to set
   */
  public void setNextItem(final LinkItem nextItem) {
    this.nextItem = nextItem;
  }

  public void setOldURL(final String oldURL) {
    this.oldURL = oldURL;
  }

  /**
   * @param previousItem
   *        the previousItem to set
   */
  public void setPreviousItem(final LinkItem previousItem) {
    this.previousItem = previousItem;
  }

  /**
   * @param publishDate
   *        the publishDate to set
   */
  public void setPublishDate(final Date publishDate) {
    this.publishDate = publishDate;
    this.shortDate = this.getPublishDateString();
    this.dateCategory = this.getPublishDateStringShort();
  }

  public void setSeries(final String series) {
    this.series = series;
  }

  public void setSeriesMember(final List<LinkItem> seriesMember) {
    Collections.sort(seriesMember);
    Collections.reverse(seriesMember);
    this.seriesMember = seriesMember;
  }

  /**
   * @param blogSourceType
   *        the blogSourceType to set
   */
  public void setSourceType(final String blogSourceType) {
    this.sourceType =
        ("M".equalsIgnoreCase(String.valueOf(blogSourceType).substring(0, 1))) ? MARKDOWN
            : "HTML";
  }

  public void setStatus(final String status) {
    this.status = status;
  }

  public void setTitle(final String title) {
    this.title = title;
  }

  public void setUNID(final String uNID) {
    this.UNID = uNID;
  }

  public void setEntryURL(final String newURL) {
    this.entryUrl = newURL;
  }

  @Override
  public String toString() {
    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    this.saveDatatoJson(out);
    return out.toString();
  }

  private void addBlogBodyFromFiles(final String fileName) {
    if (!fileName.endsWith(BlogRenderer.JSON_ENDING)) {
      return;
    }
    this.metaFileName = this.getMetafileName(fileName);
    final String htmlContentFile =
        fileName.substring(0, fileName.lastIndexOf(BlogRenderer.JSON_ENDING));
    final String htmlMoreFile =
        htmlContentFile.substring(0, htmlContentFile.lastIndexOf(BlogRenderer.HTML_ENDING))
            + ".more.html";
    final String mdContentFile =
        htmlContentFile.substring(0, htmlContentFile.lastIndexOf(BlogRenderer.HTML_ENDING))
            + ".md";
    final String mdMoreFile =
        htmlContentFile.substring(0, htmlContentFile.lastIndexOf(BlogRenderer.HTML_ENDING))
            + ".more.md";

    final File mdFile = new File(mdContentFile);
    final File moreMdFile = new File(mdMoreFile);
    final File htmlFile = new File(htmlContentFile);
    final File moreFile = new File(htmlMoreFile);

    // Check if we have content as markdown or HTML file. HTML takes
    // priority over md file

    // Markdown content check
    if (mdFile.exists()) {
      try {
        this.setSourceType(MARKDOWN);
        this.sourceFileName = mdFile.getAbsolutePath();
        final String mdContenCandidate =
            Files.asCharSource(mdFile, StandardCharsets.UTF_8).read();
        final String htmlContent =
            MarkdownConverter.markdown2HtmlWithCode(mdContenCandidate);
        this.setMainBody(htmlContent);
      } catch (final IOException e) {
        e.printStackTrace();
      }
    }

    if (moreMdFile.exists()) {
      try {
        this.sourceMoreFileName = moreMdFile.getAbsolutePath();
        final String moreContentCandidate =
            Files.asCharSource(moreMdFile, StandardCharsets.UTF_8).read();
        final String moreContent =
            MarkdownConverter.markdown2HtmlWithCode(moreContentCandidate);
        this.setMoreBody(moreContent);
      } catch (final IOException e) {
        e.printStackTrace();
      }
    }

    // HTML Content check
    if (htmlFile.exists()) {
      try {
        this.setSourceType("HTML");
        this.sourceFileName = htmlFile.getAbsolutePath();
        final String htmlContent =
            Files.asCharSource(htmlFile, StandardCharsets.UTF_8).read();
        this.setMainBody(htmlContent);
      } catch (final IOException e) {
        e.printStackTrace();
      }
    }

    if (moreFile.exists()) {
      try {
        this.sourceMoreFileName = moreFile.getAbsolutePath();
        final String moreContent =
            Files.asCharSource(moreFile, StandardCharsets.UTF_8).read();
        this.setMoreBody(moreContent);
      } catch (final IOException e) {
        e.printStackTrace();
      }
    }

  }

  public BlogEntry cleanupComments() {
    this.comments.forEach((key, entry) -> {
      final String candidate = entry.getComment();
      final int startpos = candidate.indexOf("<body>");
      final int endpos = candidate.indexOf("</body>");
      // Stripping out head/body
      if ((startpos > -1) && (endpos > 0)) {
        final String result = candidate.substring(startpos + 6, endpos);
        entry.setComment(result);
      }
    });
    return this;
  }

  private String getMetafileName(String fileName) {
    boolean done = false;
    while (!done) {
      final int lastDot = fileName.lastIndexOf(".");
      if (lastDot < 0) {
        done = true;
      } else {
        final String suffix = fileName.substring(lastDot);
        if (".json".equalsIgnoreCase(suffix)
            || ".html".equalsIgnoreCase(suffix)
            || ".more".equalsIgnoreCase(suffix)
            || ".md".equalsIgnoreCase(suffix)) {
          fileName = fileName.substring(0, lastDot);
        } else {
          done = true;
        }
      }
    }

    return fileName;
  }

  /**
   * Comments could be inside the the main article (from legacy) or be in a
   * separate directory (new). This function loads them from disk
   */
  private void loadCommentsFromDisk(final Config config) {
    final File commentDir =
        new File(config.sourceDirectory + config.commentDirectory + File.separator
            + this.getUNID());
    if (commentDir.exists() && commentDir.isDirectory()) {
      // We have comments (eventually)
      for (final String curFile : commentDir.list()) {
        final BlogComments curComm =
            BlogComments.loadFromJson(commentDir.getPath() + File.separator + curFile);
        if (curComm != null) {
          this.comments.put(curComm.getUNID(), curComm);
        }
      }
    }

  }

  private void nonNullMapEntry(final Map<String, Object> target, final String key,
      final Object value) {
    if ((key == null) || (value == null) || String.valueOf(value).equals("")) {
      return;
    }
    target.put(key, value);
  }
}

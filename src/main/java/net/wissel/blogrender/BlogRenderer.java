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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.TreeSet;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.google.common.base.Strings;
import com.google.common.io.Files;
import org.joda.time.Duration;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import net.wissel.blogrender.EntriesWithFiles.FileEntry;

/**
 * @author stw
 */
public class BlogRenderer {

  public static final String JSON_ENDING = ".json";
  public static final String BLOG_ENDING = ".blog";
  public static final String HTML_ENDING = ".html";
  public static final String BROTLI_ENDING = ".br";

  /** The XML prolog every sitemap document opens with. */
  private static final String XML_DECLARATION = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";

  private static final String CATEGORY = "category";
  private static final String PUBLISHED = "Published";
  private static final String ALL_CATEGORY_NAME = "allCategories";

  /**
   * @param args
   * @throws NotesException
   * @throws IOException
   */
  public static void main(final String[] args) throws IOException {
    final Date start = new Date();
    // ALL Parameters are in the config object which reads/writes
    // configuration from JSON
    final BlogRenderer bm = new BlogRenderer(Config.get(Config.CONFIG_NAME));

    System.out.println("\n\n *************** Loading JSON from disk ********************\n\n");
    // Must be called with true to get started
    final boolean useBlogYamlFormat = true;
    bm.loadBlogFromDisk(useBlogYamlFormat);
    System.out.println("\n\n ***************** Rendering to disk ***********************\n\n");
    bm.renderBlog();
    final Date end = new Date();
    System.out.println("\n\n ************************** Done! **************************\n\n");
    final Duration d = new Duration(start.getTime(), end.getTime());
    System.out.printf("Duration: %s seconds%n", d.getStandardSeconds());

  }

  private final TreeMap<String, LinkItem> allCategories = new TreeMap<>();

  private final TreeMap<String, LinkItem> allDateCategories = new TreeMap<>();
  private final TreeMap<String, DateArchive> archiveByYear = new TreeMap<>();

  private final TreeMap<String, TreeMap<String, LinkItem>> allSeries = new TreeMap<>();

  private final TreeSet<BlogEntry> theBlog = new TreeSet<>();

  private final Map<String, BlogEntry> blogById = new HashMap<>();

  private Config config = null;

  private EntriesWithFiles fileEntries = new EntriesWithFiles();

  private final TreeMap<String, RenderInstructions> overviewPages = new TreeMap<>();
  // for rendering and lookup of old/new URLs
  private final TreeMap<String, String> mapperOldNewURLs = new TreeMap<>();

  public BlogRenderer(final Config config) {
    this.config = config;
    // have 2 render instructions for the all.html and the
    // categories/index.html
    final RenderInstructions riAll = new RenderInstructions();
    riAll.type = "index";
    riAll.key = "AllDocuments";
    riAll.pageTitle = "All entries";
    riAll.outFileName = config.allIndexFileName;
    riAll.TemplateName = config.ALL_INDEX_TEMPLATE;
    riAll.pageLink = "AllDocuments";
    riAll.members = this.theBlog;
    riAll.reverse = true;

    final RenderInstructions riCat = new RenderInstructions();
    riCat.type = "index";
    riCat.key = BlogRenderer.ALL_CATEGORY_NAME;
    riCat.pageTitle = "All Categories";
    riCat.outFileName = "categories/" + config.indexFileName;
    riCat.TemplateName = config.ALL_CATEGORY_TEMPLATE;
    riCat.pageLink = BlogRenderer.ALL_CATEGORY_NAME;
    riCat.reverse = true;

    this.overviewPages.put("allEntries", riAll);
    this.overviewPages.put(BlogRenderer.ALL_CATEGORY_NAME, riCat);

  }

  /**
   * @return the config
   */
  public Config getConfig() {
    return this.config;
  }

  /**
   * @return the theBlog
   */
  public final SortedSet<BlogEntry> getTheBlog() {
    return this.theBlog;
  }

  /**
   * Laedt alle Blog entries von JSON Files auf Disk
   *
   * @param sourceFileOrDirName
   */
  public void loadBlogFromDisk(final boolean useYamlFormat) {
    final File srcDir = new File(this.config.sourceDirectory);
    if (!srcDir.exists()) {
      System.err.print(this.config.sourceDirectory + " doesn't exist!");
      return;
    } else if (!srcDir.isDirectory()) {
      System.err.print(this.config.sourceDirectory + " is not a directory!");
      return;
    }

    final String path = srcDir.getPath();
    this.loadBlogEntriesFromDisk(path + this.getConfig().documentDirectory, useYamlFormat);
    System.out.println("\n\nBlog loaded from disk");
    System.out.println("\n\nLoading comments...");
    this.loadCommentsFromDisk(path + this.getConfig().commentDirectory);
    System.out.println("\nComments loaded from disk");
    this.loadFileDefinitionsFromDisk(path);
    System.out.println("\n\nFile definitions loaded from disk");
    this.cleanupLinksAndImages();
    System.out.println("\n\nCleanup complete");

  }

  private void loadCommentsFromDisk(String sourceFileOrDirName) {
    final File srcDir = new File(sourceFileOrDirName);
    if (!srcDir.exists()) {
      System.err.print(sourceFileOrDirName + " doesn't exist");
      return;
    }

    if (srcDir.isDirectory()) {
      // Recursive call to get files in directory structure
      System.out.println("Comments from " + srcDir.getAbsolutePath());
      for (final String curFile : srcDir.list()) {
        this.loadCommentsFromDisk(srcDir.getPath() + "/" + curFile);
      }

    } else if (srcDir.getName().endsWith(".comment")
        || srcDir.getName().endsWith(JSON_ENDING)) {
      final BlogComments bc = BlogComments.loadFromJson(srcDir);
      if (bc != null && bc.isValid()) {
        final String parent = bc.getParentId();
        if (this.blogById.containsKey(parent)) {
          this.blogById.get(parent).addComment(bc);
        } else {
          System.err.println("Can't find parent:" + parent);
        }
      }

    }

  }

  public void saveBlogEntriesToDisk() throws IOException {
    final String oDir = this.config.sourceDirectory + this.config.documentDirectory;
    for (final BlogEntry be : this.theBlog) {
      this.saveBlogEntry(be, oDir);
    }

  }

  /**
   * Adds values of a blog entries to the global arrays
   *
   * @param be
   */
  private void addBlogContext(final BlogEntry be) {
    if (be == null) {
      return;
    }
    LinkItem catItem = null;
    final LinkItem dateItem =
        new LinkItem(be.getDateYear(), this.config.webBlogLocation + be.getDateYear(),
            Utils.date2ComparableString(be.getPublishDate()));
    // Store it in the big blog list for retrieval
    this.theBlog.add(be);
    // and lookup
    this.blogById.put(be.getUNID(), be);

    // Add the links to the mapping
    // this.config.plinkPrefix will be handled by the HTTP rule...
    final String key = String.valueOf(be.getOldURL()).toLowerCase();
    final String value = be.getEntryUrl();
    if (!"null".equals(key)) {
      this.mapperOldNewURLs.put(key, value);
    }

    // Populate the category list
    for (final String catName : be.getCategory()) {
      final LinkItem li = new LinkItem(catName);
      final String catKey = li.place;
      if (this.allCategories.containsKey(catKey)) {
        this.allCategories.get(catKey).count += 1;
      } else {
        catItem = new LinkItem(li.name,
            this.config.webBlogLocation + this.config.categoriesLocation);
        this.allCategories.put(catKey, catItem);
      }

      this.overviewPages.get(ALL_CATEGORY_NAME).addToCategory(be, li.name, li.place);

    }
    // Date with month and year or only year?
    // Month - year
    // Year only
    if (this.allDateCategories.containsKey(be.getDateYear())) {
      this.allDateCategories.get(be.getDateYear()).count += 1;
    } else {
      this.allDateCategories.put(be.getDateYear(), dateItem);
    }
    // Add to the lists for category, month, year
    this.addToOverviewPage("year", null, be.getDateYear(), be);
    be.getCategory().forEach((final String catName) -> {
      final LinkItem li = new LinkItem(catName);
      this.addToOverviewPage(CATEGORY, li.name, li.place, be);
    });
    this.addToOverviewPage("yearmonth", null,
        be.getDateYear() + "/" + be.getDateMonthNumber(), be);

    /*
     * /why built here and not derived from allDateCategories later: the month a
     * post belongs to comes from getDateYear()/getDateMonthNumber(), the same
     * getters addToOverviewPage files the archive pages by. Deriving it from the
     * permalink would disagree with those pages for any post whose publish
     * instant and authored URL fall on different sides of midnight.
     */
    final DateArchive year = this.archiveByYear.computeIfAbsent(be.getDateYear(),
        y -> new DateArchive(y, this.config.webBlogLocation + y));
    year.count += 1;
    final String monthPlace =
        this.config.webBlogLocation + be.getDateYear() + "/" + be.getDateMonthNumber() + "/";
    LinkItem month = null;
    for (final LinkItem m : year.months) {
      if (m.place.equals(monthPlace)) {
        month = m;
        break;
      }
    }
    if (month == null) {
      year.months.add(new LinkItem(be.getDateMonth(), monthPlace, be.getDateMonthNumber(), true));
    } else {
      month.count += 1;
    }

    // Add to a series collection if there
    if (be.getSeries() != null) {
      final String series = be.getSeries();
      final TreeMap<String, LinkItem> c =
          this.allSeries.computeIfAbsent(series, k -> new TreeMap<>());
      // We sort categories reverse
      catItem =
          new LinkItem(be.getTitle(), this.config.webBlogLocation + be.getEntryUrl(),
              Utils.date2ComparableString(be.getPublishDate()), true);
      c.put(be.getEntryUrl(), catItem);

      this.allSeries.put(series, c);
    }
  }

  private void addToOverviewPage(final String type, final String title, final String key,
      final BlogEntry be) {
    RenderInstructions ri;
    String templateName;
    String outfileName;
    String pageTitle;
    String pageLink;

    if (be == null) {
      return;
    }

    if (type.equals("year")) {
      templateName = this.config.YEAR_TEMPLATE;
      outfileName = key + File.separator + this.config.indexFileName;
      pageTitle = "Year " + key;
      pageLink = key;

    } else if (type.equals(CATEGORY)) {
      templateName = this.config.CATEGORY_TEMPLATE;
      outfileName = this.config.categoriesLocation + key + HTML_ENDING;
      pageTitle = title;
      pageLink = key;
    } else if (type.equals("yearmonth")) {
      templateName = this.config.MONTH_TEMPLATE;
      outfileName = key + File.separator + this.config.indexFileName;
      pageTitle = "By Date: " + be.getPublishDateStringShort();
      pageLink = key;
    } else {
      // We dont know the type
      return;
    }

    if (this.overviewPages.containsKey(key)) {
      ri = this.overviewPages.get(key);
    } else {
      ri = new RenderInstructions();
      ri.type = type;
      ri.key = key;
      ri.pageTitle = pageTitle;
      ri.outFileName = outfileName;
      ri.TemplateName = templateName;
      ri.pageLink = pageLink;
      this.overviewPages.put(key, ri);
    }

    // In year we want to subcategorize with month
    if (type.equals("year")) {
      ri.addToCategory(be, be.getDateMonth(), be.getDateMonthNumber());
    } else if (type.equals(CATEGORY)) {
      ri.addToCategory(be, be.getDateYear(), be.getDateYear());
    } else {
      ri.add(be);
    }
  }

  private String cleanNginxMapperString(final String inString, final boolean isKey) {
    final StringBuilder result = new StringBuilder();

    if (!inString.startsWith(this.config.webBlogLocation)) {
      if (isKey) {
        result.append(this.config.plinkPrefix);
      } else {
        result.append(this.config.webBlogLocation);
      }
    }
    for (int i = 0; i < inString.length(); i++) {
      final char x = inString.charAt(i);
      if (x == ' ') {
        result.append("%20");
      } /* more here */else {
        result.append(x);
      }
    }

    return result.toString();
  }

  private String cleanupHTMLlinksAndImages(final String source) {

    final org.jsoup.nodes.Document hDoc = Jsoup.parse(source);
    this.cleanupTagUrlAttribute(hDoc, "img", "src");
    this.cleanupTagUrlAttribute(hDoc, "a", "href");
    return hDoc.body().html();

  }

  /**
   * Goes through all Blog entries and sorts out the links to images and
   * attachments by looking at src and href attributes
   */
  private void cleanupLinksAndImages() {
    this.theBlog.forEach(this::cleanupOneBlogEntry);
    System.out.println("Completed mapping of Blog entries");
  }

  private void cleanupOneBlogEntry(final BlogEntry be) {
    be.setMainBody(this.cleanupHTMLlinksAndImages(be.getMainBody()));
    if ((be.getMoreBody() != null) && !be.getMoreBody().equals("")) {
      be.setMoreBody(this.cleanupHTMLlinksAndImages(be.getMoreBody()));
    }
  }

  private void cleanupTagUrlAttribute(final org.jsoup.nodes.Document hDoc,
      final String elementName,
      final String attName) {

    final String query = elementName + "[" + attName + "]";
    final Elements elements = hDoc.select(query);

    elements.forEach((final Element element) -> {
      final String attValue = element.attr(attName).trim();
      if (this.mapperOldNewURLs.containsKey(attValue.toLowerCase())) {
        final String replace = this.mapperOldNewURLs.get(attValue.toLowerCase());
        System.out.print("Replacing:");
        System.out.print(attValue);
        System.out.print(" with ");
        System.out.println(replace);
        element.attr(attName, replace);
      }
    });

  }

  /**
   * Loads Blog entries from JSON recursively from disk
   *
   * @param sourceFileOrDirName
   * @throws IOException
   */
  private void loadBlogEntriesFromDisk(final String sourceFileOrDirName,
      final boolean useYamlFormat) {
    final File srcDir = new File(sourceFileOrDirName);
    if (!srcDir.exists()) {
      System.err.print(sourceFileOrDirName + " doesn't exist");
      return;
    }

    final int descriptionSize = Integer.parseInt(this.config.topicLength);

    if (srcDir.isDirectory()) {
      System.out.println(srcDir.getAbsolutePath());
      // Recursive call to get files in directory structure
      for (final String curFile : srcDir.list()) {
        this.loadBlogEntriesFromDisk(srcDir.getPath() + "/" + curFile, useYamlFormat);
      }
      return;
    }

    if (isFileTypeSupported(srcDir, useYamlFormat)) {
      try (FileInputStream in = new FileInputStream(new File(sourceFileOrDirName))) {
        final BlogEntry be = useYamlFormat
            ? BlogEntry.loadDataFromBlog(in, this.config)
            : BlogEntry.loadDataFromJson(in, sourceFileOrDirName, this.config);
        if ((be != null) && (be.getTitle() != null)
            && be.getStatus().equalsIgnoreCase(PUBLISHED)) {
          be.setDescriptionSize(descriptionSize);
          this.addBlogContext(be);
        }
      } catch (final Exception e) {
        e.printStackTrace();
      }
    }

  }

  /**
   * Checks if the file ends with json or blog to and has the right indicator
   *
   * @param srcDir
   * @param useYamlFormat
   * @return true if it is OK to process
   */
  private boolean isFileTypeSupported(final File srcDir,
      final boolean useYamlFormat) {
    return (!useYamlFormat && srcDir.getName().endsWith(JSON_ENDING))
        || (useYamlFormat && srcDir.getName().endsWith(BLOG_ENDING));

  }

  private EntriesWithFiles loadFileDefFromDisk(final String sourceFileName) {

    EntriesWithFiles result = new EntriesWithFiles();
    final File source = new File(sourceFileName);

    try (final FileInputStream in = new FileInputStream(source);) {
      result = EntriesWithFiles.loadDataFromJson(in);
    } catch (final IOException e) {
      e.printStackTrace();
    }

    return result;

  }

  private void loadFileDefinitionsFromDisk(final String sourceDirectory) {

    final EntriesWithFiles imgEntriest = this
        .loadFileDefFromDisk(sourceDirectory + this.config.imageDirectory + "images.json");
    this.fileEntries = this
        .loadFileDefFromDisk(
            sourceDirectory + this.config.attachmentDirectory + "attachments.json");
    this.updateMapper(this.mapperOldNewURLs, imgEntriest);
    this.updateMapper(this.mapperOldNewURLs, this.fileEntries);
  }

  private void render404() {

    final String template = this.config.ERROR_TEMPLATE;
    final String finalDestination =
        this.config.destinationDirectory + this.config.errorFileName;

    final BlogIndex bi = new BlogIndex();
    bi.allCategories = this.allCategories.values();
    bi.allDateCategories = this.allDateCategories.values();
    bi.archiveByYear = this.archiveByYear.descendingMap().values();
    bi.topArticles = new BlogEntryCollection(true);
    bi.relevantArticles = new BlogEntryCollection(true);

    this.theBlog.stream()
        .filter(cur -> cur.getStatus().equals(PUBLISHED))
        .forEach(bi.topArticles::add);

    this.theBlog.stream()
        .filter(cur -> cur.getStatus().equals(PUBLISHED))
        .filter(cur -> !Strings.isNullOrEmpty(cur.getOldURL()))
        .forEach(cur -> {
          cur.setOldURL(("/blog/d6plinks/" + cur.getOldURL()).toLowerCase());
          cur.setTitle(cur.getTitle().replace("'", "&#39;"));
          bi.relevantArticles.add(cur);
        });

    this.renderToDisk(template, finalDestination, bi);
    System.out.println("Rendered 404");

  }

  private void renderAttachments() {

    final String template = this.config.ATTACHMENT_TEMPLATE;
    final String finalDestination =
        this.config.destinationDirectory + this.config.downloadDirectory
            + this.config.indexFileName;
    this.renderToDisk(template, finalDestination, this.fileEntries);

    System.out.println("Rendered Attachments");

  }

  /**
   * Writes all Blog entries out to disk. Since they are ordered by date in
   * the TreeMap that happens in sequence. Special challenge: We need to have
   * the next and previous entries to successfully render them completely.
   *
   * @throws IOException
   */
  private void renderBlog() throws IOException {
    final String template = this.config.ENTRY_TEMPLATE;
    final String baseDir = this.config.webBlogLocation;
    final BlogIndex seriesIndex = new BlogIndex();
    seriesIndex.topArticles = new BlogEntryCollection(true);
    // /why: nav_main tests {{#isSeries}} to mark its "Series" item active.
    // The flag was declared on BlogIndex but never assigned anywhere, so the
    // section never fired and series.html was the one page with no active nav
    // item. Every other BlogIndex leaves it null, which Mustache reads as false.
    seriesIndex.isSeries = Boolean.TRUE;
    final Set<String> completedSeries = new HashSet<>();
    final MustacheFactory mf =
        new DefaultMustacheFactory(new File(this.config.templateDirectory));
    final Mustache mustache = mf.compile(template);

    BlogEntry renderEntry = null;

    // Blog entries
    for (final BlogEntry be : this.theBlog) {
      renderEntry =
          renderLoop(baseDir, seriesIndex, completedSeries, mustache, renderEntry, be);
    }
    // The last entry wasn't rendered in the loop, so we do it here!
    this.renderOneEntry(renderEntry, mustache);

    System.out.println("\nEntries completed, now categories & dates\n");

    // Categories & Date Categories !!
    this.renderOverViewPages();
    this.renderAttachments();
    this.renderIndex();
    this.renderIndexRSS();
    this.render404();
    this.renderSeries(seriesIndex);
    this.renderImprint();
    this.renderURLMapper();
    this.renderNGinxURLMapper();
    this.renderSiteMap();

    System.out.println("...Done...");
  }

  private BlogEntry renderLoop(final String baseDir, final BlogIndex seriesIndex,
      final Set<String> completedSeries, final Mustache mustache, final BlogEntry renderEntry,
      final BlogEntry be) {
    if (PUBLISHED.equalsIgnoreCase(be.getStatus())) {
      be.cleanupComments();
      be.setAllCategories(this.allCategories.values());
      be.setAllDateCategories(this.allDateCategories.descendingMap().values());
      be.setArchiveByYear(this.archiveByYear.descendingMap().values());
      be.setRelatedItems(this.findRelated(be));

      if ((be.getSeries() != null)) {
        final String series = be.getSeries();
        if (this.allSeries.containsKey(series)) {
          final List<LinkItem> l = new ArrayList<>();
          l.addAll(this.allSeries.get(series).values());
          be.setSeriesMember(l);
          if (!completedSeries.contains(series)) {
            // First entry of series - to be captures
            seriesIndex.topArticles.add(be);
            completedSeries.add(series);
          }
        }
      }
      // We capture the previous link if we have one - only possible
      // for the second entry onwards
      // renderEntry contains the previous Blogentry which is the
      // needs its nextLink populated by be and be needs its
      // previousLink
      // populated by renderentry
      if (renderEntry != null) {
        renderEntry.setNextItem(be.getLinkItem(baseDir));
        be.setPreviousItem(renderEntry.getLinkItem(baseDir));
        this.renderOneEntry(renderEntry, mustache);
      }

      return be;
    }
    return renderEntry;
  }


  /**
   * Finds the posts most like this one, by shared categories.
   *
   * /why shared categories rather than anything cleverer: it is the only
   * relatedness signal this corpus actually carries. There is no tagging beyond
   * categories, no view data and no full-text index, so a similarity score would
   * be invented rather than measured. Ties break towards the newer post, which
   * is what a reader following "related" is usually looking for.
   *
   * @param be the entry to find neighbours for
   * @return at most five related posts, most shared categories first
   */
  private List<LinkItem> findRelated(final BlogEntry be) {
    final Set<String> mine = new HashSet<>(be.getCategory());
    if (mine.isEmpty()) {
      return Collections.emptyList();
    }
    final TreeMap<String, BlogEntry> ranked = new TreeMap<>();
    for (final BlogEntry other : this.theBlog) {
      if ((other == be) || !PUBLISHED.equalsIgnoreCase(other.getStatus())) {
        continue;
      }
      int shared = 0;
      for (final String c : other.getCategory()) {
        if (mine.contains(c)) {
          shared++;
        }
      }
      if (shared > 0) {
        // Sort key: more shared categories first, then newer first.
        ranked.put(String.format("%02d|%s|%s", 99 - shared,
            Utils.date2ComparableString(other.getPublishDate()), other.getEntryUrl()), other);
      }
    }
    final List<LinkItem> result = new ArrayList<>();
    for (final BlogEntry hit : ranked.values()) {
      if (result.size() >= 5) {
        break;
      }
      result.add(hit.getLinkItem(this.config.webBlogLocation));
    }
    return result;
  }

  private void renderImprint() {

    final String template = this.config.IMPRINT_TEMPLATE;
    final String finalDestination =
        this.config.destinationDirectory + this.config.imprintFileName;

    final BlogIndex bi = new BlogIndex();
    bi.allCategories = this.allCategories.values();
    bi.allDateCategories = this.allDateCategories.values();
    bi.archiveByYear = this.archiveByYear.descendingMap().values();
    bi.topArticles = new BlogEntryCollection(true);
    final int max = 5;
    int i = 0;
    final Iterator<BlogEntry> it = this.theBlog.descendingSet().iterator();

    while (it.hasNext() && (i < max)) {
      final BlogEntry cur = it.next();
      if (cur.getStatus().equals(PUBLISHED)) {
        bi.topArticles.add(cur);
        i++;
      }
    }

    this.renderToDisk(template, finalDestination, bi);
    System.out.println("Rendered Imprint");

  }

  private void renderIndex() {

    final String template = this.config.INDEX_TEMPLATE;
    final String finalDestination =
        this.config.destinationDirectory + this.config.indexFileName;

    final BlogIndex bi = new BlogIndex();
    bi.allCategories = this.allCategories.values();
    bi.allDateCategories = this.allDateCategories.values();
    bi.archiveByYear = this.archiveByYear.descendingMap().values();
    bi.topArticles = new BlogEntryCollection(true);
    final int max = 10;
    int i = 0;
    final Iterator<BlogEntry> it = this.theBlog.descendingIterator();

    while (it.hasNext() && (i < max)) {
      final BlogEntry cur = it.next();
      if (cur.getStatus().equals(PUBLISHED)) {
        bi.topArticles.add(cur);
        i++;
      }
    }
    this.renderToDisk(template, finalDestination, bi);

    System.out.println("Rendered Index");
  }

  private void renderIndexRSS() {
    final String finalDestination = this.config.destinationDirectory + this.config.indexRSSName;
    final String finalDestination2 =
        this.config.destinationDirectory + this.config.indexRSSName2;
    final BlogOutput out = new BlogOutput(finalDestination);
    final BlogIndex bi = new BlogIndex();
    bi.allCategories = this.allCategories.values();
    bi.allDateCategories = this.allDateCategories.values();
    bi.archiveByYear = this.archiveByYear.descendingMap().values();
    bi.topArticles = new BlogEntryCollection(true);
    final int max = 10;
    int i = 0;
    final Iterator<BlogEntry> it = this.theBlog.descendingIterator();

    while (it.hasNext() && (i < max)) {
      final BlogEntry cur = it.next();
      if (cur.getStatus().equals(PUBLISHED)) {
        bi.topArticles.add(cur);
        i++;
      }
    }

    final RSSFeedWriter rss = new RSSFeedWriter(this.getConfig(), bi);
    try {
      rss.write(out);
      out.flush();
      out.close();
      Files.copy(new File(finalDestination), new File(finalDestination2));
      System.out.println("\nRSS updated");

    } catch (final Exception e) {
      System.out.println("\nstories.rss rendering failed: " + e.getMessage());
    }

  }

  /**
   * Renders the sitemap index and its four child sitemaps.
   *
   * /why: one flat urlset of ~1800 URLs is valid but useless for diagnosis --
   * Search Console reports indexing coverage per submitted sitemap, so a single
   * file yields a single undifferentiated number. Splitting posts, archives,
   * categories and standing pages into children of a sitemapindex gives four
   * separately reported groups. sitemap.xml itself becomes the index;
   * build-blog.sh promotes it to the site root, because the sitemaps protocol
   * only lets a sitemap declare URLs at or below its own directory and the site
   * root, /mealplan.html and /code/ all sit above /blog/.
   */
  /**
   * Renders the sitemap index and every child it points at: one child per year,
   * one per category, and one for the standing pages.
   *
   * /why: Search Console reports indexing coverage per submitted sitemap, so the
   * grouping decides what can be diagnosed. Grouping by year and by topic answers
   * "is my Java writing being indexed?"; grouping by page type only answered "are
   * posts being indexed?". Each child opens with the summary page that owns it,
   * so a sitemap mirrors the navigation a reader would follow.
   *
   * A post appears in its year child and again in each of its category children.
   * The protocol permits a URL in more than one sitemap, and that repetition is
   * precisely what buys the per-topic coverage figures.
   */
  private void renderSiteMap() {
    final String base = "https://" + this.config.bloghost + this.config.webBlogLocation;
    /*
     * /why: pinned to UTC for the same reason BlogEntry pins its formatters -- a
     * <lastmod> is a claim about a date, and the same entry must not be dated
     * 2002-12-31 by one build machine and 2003-01-01 by another.
     */
    final SimpleDateFormat w3c = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
    w3c.setTimeZone(TimeZone.getTimeZone("UTC"));

    /*
     * /why: seeded with the names this method writes itself, so a category whose
     * slug is "pages" cannot overwrite the standing-pages child. Years are
     * claimed before categories, so a clash can only ever displace a category --
     * which is the one that carries a disambiguating suffix.
     */
    final Set<String> usedNames = new HashSet<>();
    usedNames.add(Config.SITEMAP_NAME);
    usedNames.add(Config.SITEMAP_PAGES);

    final List<SiteMapChild> children = new ArrayList<>();

    /*
     * /why: overviewPages is keyed by year ("2014"), year-month ("2014/03") and
     * category slug ("java"), with the two index pages under keys of their own.
     * The year-month instructions are skipped deliberately -- their archive URL
     * and their posts are emitted inside the year child, which is what keeps
     * everything about one year in a single file. Years are taken from
     * descendingMap() so the index lists the newest first.
     */
    for (final RenderInstructions ri : this.overviewPages.descendingMap().values()) {
      if ("year".equals(ri.type)) {
        children.add(this.siteMapForYear(ri, base, w3c, usedNames));
      }
    }
    final int yearCount = children.size();

    for (final RenderInstructions ri : this.overviewPages.values()) {
      if (BlogRenderer.CATEGORY.equals(ri.type)) {
        children.add(this.siteMapForCategory(ri, base, w3c, usedNames));
      }
    }
    final int categoryCount = children.size() - yearCount;

    children.add(this.siteMapForStandingPages(base, w3c));

    final StringBuilder index = new StringBuilder(BlogRenderer.XML_DECLARATION);
    index.append("<sitemapindex xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
    int urlCount = 0;

    for (final SiteMapChild child : children) {
      final StringBuilder urlset = new StringBuilder(BlogRenderer.XML_DECLARATION);
      urlset.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n")
          .append(child.urls())
          .append("</urlset>\n");
      this.writeSiteMapFile(child.fileName(), urlset.toString());
      urlCount += BlogRenderer.countUrls(child.urls());

      index.append("<sitemap><loc>")
          .append(BlogRenderer.xmlEscape(base + child.fileName()))
          .append("</loc><lastmod>")
          .append(child.lastmod())
          .append("</lastmod></sitemap>\n");
    }
    index.append("</sitemapindex>\n");

    this.writeSiteMapFile(Config.SITEMAP_NAME, index.toString());

    System.out.println("\n" + Config.SITEMAP_NAME + " index rendered: " + children.size()
        + " children (" + yearCount + " years, " + categoryCount
        + " categories, 1 standing pages), " + urlCount + " urls");
  }

  /**
   * One child sitemap, assembled in full before anything is written.
   *
   * @param fileName the child's file name inside destinationDirectory
   * @param urls the concatenated &lt;url&gt; elements
   * @param lastmod the W3C date of the newest entry the child declares
   */
  private record SiteMapChild(String fileName, String urls, String lastmod) {
  }

  /**
   * Builds the child for one year: the year archive, then each month archive
   * newest first, each followed by that month's posts newest first.
   *
   * /why: the order is borrowed from processCategorizedMembers() rather than
   * re-derived -- descendingKeySet() over the month groups and descendingSet()
   * within each group is exactly what the rendered year page shows. Sorting
   * independently here would let the sitemap and the page drift apart.
   *
   * @param ri the year's render instructions, whose categories are its months
   * @param base the absolute blog base URL
   * @param w3c the date format for lastmod
   * @param usedNames the file names already claimed, extended by this call
   * @return the finished child
   */
  private SiteMapChild siteMapForYear(final RenderInstructions ri, final String base,
      final SimpleDateFormat w3c, final Set<String> usedNames) {
    final StringBuilder urls = new StringBuilder();
    urls.append(BlogRenderer.urlElement(base + ri.key + "/", null));
    Date newest = null;

    if (ri.categories != null) {
      for (final String month : ri.categories.descendingKeySet()) {
        urls.append(BlogRenderer.urlElement(base + ri.key + "/" + month + "/", null));
        newest = BlogRenderer.appendEntries(urls, ri.categories.get(month), base, w3c, newest);
      }
    }

    return BlogRenderer.siteMapChild(ri.key, urls, newest, w3c, usedNames);
  }

  /**
   * Builds the child for one category: its summary page, then its posts newest
   * first, grouped by year descending as the category page groups them.
   *
   * @param ri the category's render instructions, whose categories are its years
   * @param base the absolute blog base URL
   * @param w3c the date format for lastmod
   * @param usedNames the file names already claimed, extended by this call
   * @return the finished child
   */
  private SiteMapChild siteMapForCategory(final RenderInstructions ri, final String base,
      final SimpleDateFormat w3c, final Set<String> usedNames) {
    final StringBuilder urls = new StringBuilder();
    urls.append(BlogRenderer.urlElement(
        base + this.config.categoriesLocation + ri.key + BlogRenderer.HTML_ENDING, null));
    Date newest = null;

    if (ri.categories != null) {
      for (final String year : ri.categories.descendingKeySet()) {
        newest = BlogRenderer.appendEntries(urls, ri.categories.get(year), base, w3c, newest);
      }
    }

    return BlogRenderer.siteMapChild(ri.key, urls, newest, w3c, usedNames);
  }

  /**
   * Builds the child for the pages that belong to no year and no category.
   *
   * /why: the standing pages are rendered on every run but nothing in the year
   * or category walks reaches them, so without this child they would be
   * undeclared. Their names come from Config, so a renamed page cannot silently
   * drop out. downloadDirectory and categoriesLocation are directories whose
   * index.html is the page, so they are declared as directory URLs -- the same
   * form the archives use.
   *
   * @param base the absolute blog base URL
   * @param w3c the date format for lastmod
   * @return the finished child
   */
  private SiteMapChild siteMapForStandingPages(final String base, final SimpleDateFormat w3c) {
    final StringBuilder urls = new StringBuilder();
    Date newest = null;

    for (final BlogEntry entry : this.theBlog) {
      if (PUBLISHED.equalsIgnoreCase(entry.getStatus())
          && ((newest == null) || entry.getPublishDate().after(newest))) {
        newest = entry.getPublishDate();
      }
    }

    urls.append(BlogRenderer.urlElement(base, (newest == null) ? null : w3c.format(newest)));
    for (final String page : new String[] {this.config.allIndexFileName,
        this.config.seriesFileName, this.config.imprintFileName, this.config.downloadDirectory,
        this.config.categoriesLocation}) {
      urls.append(BlogRenderer.urlElement(base + page, null));
    }

    return new SiteMapChild(Config.SITEMAP_PAGES, urls.toString(),
        w3c.format((newest == null) ? new Date() : newest));
  }

  /**
   * Appends one group's published entries, newest first, with their publish date
   * as lastmod.
   *
   * @param urls the buffer to append to
   * @param group the render instructions holding the group's members
   * @param base the absolute blog base URL
   * @param w3c the date format for lastmod
   * @param newestSoFar the newest publish date seen by the caller, or null
   * @return the newest publish date including this group
   */
  private static Date appendEntries(final StringBuilder urls, final RenderInstructions group,
      final String base, final SimpleDateFormat w3c, final Date newestSoFar) {
    Date newest = newestSoFar;
    if ((group == null) || (group.members == null)) {
      return newest;
    }
    for (final BlogEntry entry : group.members.descendingSet()) {
      if (!PUBLISHED.equalsIgnoreCase(entry.getStatus())) {
        continue;
      }
      urls.append(BlogRenderer.urlElement(base + entry.getEntryUrl(),
          w3c.format(entry.getPublishDate())));
      if ((newest == null) || entry.getPublishDate().after(newest)) {
        newest = entry.getPublishDate();
      }
    }
    return newest;
  }

  /**
   * Names a child after its key and records the name as taken.
   *
   * /why: a category slug is [a-z0-9]+ (LinkItem.cleanPlace) and a year key is
   * four digits, so a category named "2014" would produce the same file name as
   * the year. Years claim their names first, so the suffix only ever lands on the
   * category -- and a sitemap silently overwritten by another is the kind of
   * defect that shows up as missing coverage months later, not as a build error.
   *
   * @param key the year or category slug
   * @param urls the child's &lt;url&gt; elements
   * @param newest the newest publish date the child declares, or null
   * @param w3c the date format for lastmod
   * @param usedNames the file names already claimed, extended by this call
   * @return the finished child
   */
  private static SiteMapChild siteMapChild(final String key, final StringBuilder urls,
      final Date newest, final SimpleDateFormat w3c, final Set<String> usedNames) {
    String fileName = Config.SITEMAP_PREFIX + key + Config.SITEMAP_SUFFIX;
    if (!usedNames.add(fileName)) {
      fileName = Config.SITEMAP_PREFIX + key + Config.SITEMAP_CATEGORY_DISAMBIGUATOR
          + Config.SITEMAP_SUFFIX;
      usedNames.add(fileName);
    }
    return new SiteMapChild(fileName, urls.toString(),
        w3c.format((newest == null) ? new Date() : newest));
  }

  /**
   * Counts the &lt;url&gt; elements in a child, for the build log only.
   *
   * @param urls the child's &lt;url&gt; elements
   * @return how many there are
   */
  private static int countUrls(final String urls) {
    int count = 0;
    int at = urls.indexOf("<url>");
    while (at >= 0) {
      count++;
      at = urls.indexOf("<url>", at + 1);
    }
    return count;
  }

  /**
   * Renders one &lt;url&gt; element with its location escaped.
   *
   * @param loc the absolute URL
   * @param lastmod the W3C date, or null to omit the element
   * @return the &lt;url&gt; element, newline terminated
   */
  private static String urlElement(final String loc, final String lastmod) {
    final StringBuilder url =
        new StringBuilder("<url><loc>").append(BlogRenderer.xmlEscape(loc)).append("</loc>");
    if (lastmod != null) {
      url.append("<lastmod>").append(lastmod).append("</lastmod>");
    }
    return url.append("</url>\n").toString();
  }

  /**
   * Writes one finished sitemap document to the destination directory.
   *
   * /why: the document is assembled in full by the caller before the output is
   * opened. BlogOutput.close() is what commits the buffer to disk, so writing
   * incrementally and failing halfway would replace a good sitemap with a
   * truncated one -- and a malformed sitemap is discarded by crawlers silently.
   *
   * @param fileName the file name inside destinationDirectory
   * @param document the complete XML document
   */
  private void writeSiteMapFile(final String fileName, final String document) {
    final BlogOutput out = new BlogOutput(this.config.destinationDirectory + fileName);
    try {
      out.write(document.getBytes(StandardCharsets.UTF_8));
      out.flush();
      out.close();
    } catch (final Exception e) {
      System.out.println("\n" + fileName + " rendering failed: " + e.getMessage());
    }
  }

  /**
   * Escapes the five predefined XML entities so a URL is safe as element text.
   *
   * /why: 15 entries have a "/" in their title and land in nested directories,
   * and at least one permalink ends in a bare "&". A single unescaped ampersand
   * makes the whole document malformed, and search engines discard a malformed
   * sitemap without reporting an error.
   *
   * @param in the raw text
   * @return the text with the five predefined XML entities escaped
   */
  private static String xmlEscape(final String in) {
    return in.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;");
  }

  /**
   * Creates the map for blog redirections
   *
   * @param destination
   * @throws IOException
   */
  private void renderNGinxURLMapper() throws IOException {
    final File outFile = new File(this.config.destinationDirectory + this.config.urlmapNGinx);
    java.nio.file.Files.deleteIfExists(outFile.toPath());

    final ArrayList<String> keysWritten = new ArrayList<>();

    try (OutputStream out = new FileOutputStream(outFile);
        PrintWriter pw = new PrintWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {

      this.mapperOldNewURLs.entrySet().forEach((final Map.Entry<String, String> e) -> {
        final String key = e.getKey().toLowerCase();
        final String value = e.getValue();

        final String realKey = this.cleanNginxMapperString(key, true);
        final String realValue = this.cleanNginxMapperString(value, false);

        if (!keysWritten.contains(realKey)) {
          pw.write(realKey);
          pw.write(" ");
          pw.write(realValue);
          pw.write(";\n");
          keysWritten.add(realKey);
        }

      });
    }
    System.out.println("URL mapping written to file " + outFile.getPath());

  }

  private void renderOneEntry(final BlogEntry be, final Mustache mustache) {

    if (be == null || mustache == null) {
      return;
    }

    final String location = this.config.destinationDirectory + be.getEntryUrl();

    // Set the current context
    be.getCategory().forEach((final String catName) -> {
      final LinkItem cat = new LinkItem(catName);
      final String c = cat.place;
      this.allCategories.get(c).active = true;
    });
    this.allDateCategories.get(be.getDateYear()).active = true;

    if (be.getSeries() != null) {
      final String series = be.getSeries();
      if (this.allSeries.containsKey(series)) {
        this.allSeries.get(series).get(be.getEntryUrl()).active = true;
      }
    }

    // Prepare to write out
    this.renderToDisk(mustache, location, be);

    // Cleanup
    be.getCategory().forEach((final String catName) -> {
      final LinkItem cat = new LinkItem(catName);
      final String c = cat.place;
      this.allCategories.get(c).active = false;
    });
    this.allDateCategories.get(be.getDateYear()).active = false;

    if (be.getSeries() != null) {
      final String series = be.getSeries();
      if (this.allSeries.containsKey(series)) {
        this.allSeries.get(series).get(be.getEntryUrl()).active = false;
      }
    }
  }

  private void renderOverViewPage(final RenderInstructions ri) {
    if (ri == null) {
      return;
    }
    final String template = ri.getFinalTemplateName(this.config.templateDirectory, ri.key);
    final String finalDestination = this.config.destinationDirectory + ri.outFileName;
    boolean goodToGo = false;
    // Set of categories
    if (this.allCategories.containsKey(ri.key)) {
      this.allCategories.get(ri.key).active = true;
    }
    if (this.allDateCategories.containsKey(ri.key)) {
      this.allDateCategories.get(ri.key).active = true;
    }

    final BlogIndex bi = new BlogIndex();
    bi.allCategories = this.allCategories.values();
    bi.allDateCategories = this.allDateCategories.values();
    bi.archiveByYear = this.archiveByYear.descendingMap().values();
    bi.pageTitle = ri.pageTitle;
    bi.pageLink = ri.pageLink;
    bi.nextItem = ri.nextItem;
    bi.previousItem = ri.previousItem;

    // The renderinstructions might have entries or categorized entries
    // We check for null before we render

    if (ri.members != null) {
      goodToGo = processMembers(ri, bi);
    } else if (ri.categories != null) {
      goodToGo = processCategorizedMembers(ri, bi);
    }

    if (goodToGo) {
      this.renderToDisk(template, finalDestination, bi);
    }

    // Reset of categories
    if (this.allCategories.containsKey(ri.key)) {
      this.allCategories.get(ri.key).active = false;
    }
    if (this.allDateCategories.containsKey(ri.key)) {
      this.allDateCategories.get(ri.key).active = false;
    }

  }

  private boolean processCategorizedMembers(final RenderInstructions ri, final BlogIndex bi) {
    boolean goodToGo;
    bi.categorizedEntries = new ArrayList<>();
    final Iterator<String> it = (ri.reverse) ? ri.categories.keySet().iterator()
        : ri.categories.descendingKeySet().iterator();
    // We need to copy from the render instruction to get the
    // sequence reversed
    while (it.hasNext()) {
      final RenderInstructions curRi = ri.categories.get(it.next());
      final BlogIndex subBi = new BlogIndex();
      subBi.allCategories = this.allCategories.values();
      subBi.allDateCategories = this.allDateCategories.values();
      subBi.archiveByYear = this.archiveByYear.descendingMap().values();
      subBi.pageTitle = curRi.pageTitle;
      subBi.pageLink = curRi.pageLink;
      subBi.topArticles = new BlogEntryCollection(true);
      bi.categorizedEntries.add(subBi);
      final Iterator<BlogEntry> subIt = curRi.members.descendingSet().iterator();
      while (subIt.hasNext()) {
        final BlogEntry cur = subIt.next();
        if (cur.getStatus().equals(PUBLISHED)) {
          subBi.topArticles.add(cur);
        }
      }
    }
    goodToGo = true;
    return goodToGo;
  }

  private boolean processMembers(final RenderInstructions ri, final BlogIndex bi) {
    boolean goodToGo;
    bi.topArticles = new BlogEntryCollection(!ri.reverse);
    // We need to copy from the render instruction to get the
    // sequence reversed
    ri.members.stream().filter(cur -> cur.getStatus().equals(PUBLISHED))
        .forEach(bi.topArticles::add);
    goodToGo = true;
    return goodToGo;
  }

  /**
   * Renders all overview pages: year, month, categories
   */
  private void renderOverViewPages() {

    // Special challenge: every overview page needs
    // to have a previous and a next entry
    final String baseDir = this.config.webBlogLocation;
    RenderInstructions currentRI = null;
    LinkItem nextEntry = null;
    LinkItem previousEntry = null;

    for (final RenderInstructions ri : this.overviewPages.values()) {
      // We render one offset in the loop to be able to fetch the entry
      nextEntry = new LinkItem(ri.pageTitle, baseDir + ri.outFileName, null);
      if (currentRI != null) {
        previousEntry =
            new LinkItem(currentRI.pageTitle, baseDir + currentRI.outFileName, null);
        currentRI.nextItem = nextEntry;
        ri.previousItem = previousEntry;
        this.renderOverViewPage(currentRI);
      }
      currentRI = ri;
    }
    // The last entry wasn't rendered inside the loop
    this.renderOverViewPage(currentRI);
  }

  private void renderSeries(final BlogIndex bi) {

    final String template = this.config.SERIES_TEMPLATE;
    final String finalDestination =
        this.config.destinationDirectory + this.config.seriesFileName;

    bi.allCategories = this.allCategories.values();
    bi.allDateCategories = this.allDateCategories.values();
    bi.archiveByYear = this.archiveByYear.descendingMap().values();

    this.renderToDisk(template, finalDestination, bi);
    System.out.println("Rendered series");
  }

  /**
   * Renders one object to disk based on a template and a destination only
   * saves it to disk if it actually had changed
   *
   * @param template
   *        Name of the template to use
   * @param finalDestination
   *        file location
   * @param payload
   *        object to render
   */
  private void renderToDisk(final String template, final String finalDestination,
      final Object payload) {
    final MustacheFactory mf =
        new DefaultMustacheFactory(new File(this.config.templateDirectory));
    final Mustache mustache = mf.compile(template);
    this.renderToDisk(mustache, finalDestination, payload);
  }

  private void renderToDisk(final Mustache mustache, final String finalDestination,
      final Object payload) {
    final BlogOutput out = new BlogOutput(finalDestination);
    final Writer pw = new PrintWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
    mustache.execute(pw, payload);
    try {
      pw.flush();
      pw.close();
      out.close();
    } catch (final IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Creates the map for blog redirections
   *
   * @param destination
   * @throws IOException
   */
  private void renderURLMapper() throws IOException {
    final File outFile = new File(this.config.destinationDirectory + this.config.urlmapFile);
    java.nio.file.Files.deleteIfExists(outFile.toPath());
    try (OutputStream out = new FileOutputStream(outFile);
        PrintWriter pw = new PrintWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {
      pw.write("# Mapping of legacy blog URL into the new format\n");

      this.mapperOldNewURLs.entrySet().forEach((final Map.Entry<String, String> e) -> {
        pw.write(e.getKey().toLowerCase());
        pw.write(" ");
        pw.write(e.getValue());
        pw.write("\n");
      });
    }
    System.out.println("URL mapping written to file " + outFile.getPath());

  }

  private void saveBlogEntry(final BlogEntry be, final String wheretoSave) throws IOException {

    System.out.println(be.getEntryUrl());

    final String location = wheretoSave + be.getEntryUrl() + JSON_ENDING;
    final File outFile = new File(location);
    java.nio.file.Files.deleteIfExists(outFile.toPath());
    java.nio.file.Files.createDirectories(outFile.getParentFile().toPath());

    try (FileOutputStream out = new FileOutputStream(outFile)) {
      be.saveDatatoJson(out);
    }
  }

  private void updateMapper(final Map<String, String> mapper, final EntriesWithFiles outerList) {
    outerList.getAttachmentList().forEach((final FileEntry oneEntry) -> {
      final String oldUrlBeginning = oneEntry.url;
      oneEntry.subEntries.forEach((final FileEntry subEntry) -> {
        final String old = (oldUrlBeginning + subEntry.subject).toLowerCase();
        mapper.put(old, subEntry.url);
      });
    });
  }

}

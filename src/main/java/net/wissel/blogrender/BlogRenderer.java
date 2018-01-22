/**
 *
 */
package net.wissel.blogrender;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.google.common.io.ByteStreams;
import com.google.common.io.Closeables;
import com.google.common.io.Files;

import net.wissel.blogrender.EntriesWithFiles.FileEntry;


/**
 * @author stw
 */
public class BlogRenderer {

	/**
	 * @param args
	 * @throws NotesException
	 * @throws IOException
	 */
	public static void main(final String[] args) throws IOException {

		// ALL Parameters are in the config object which reads/writes
		// configuration from JSON
		final BlogRenderer bm = new BlogRenderer(Config.get(Config.CONFIG_NAME));

		System.out.println("\n\n *************** Loading JSON from disk ********************\n\n");
		// Must be called with true to get started
		bm.loadBlogFromDisk();
		System.out.println("\n\n ***************** Rendering to disk ***********************\n\n");
		bm.renderBlog();
		System.out.println("\n\n ************************** Done! **************************\n\n");

	}
	
	private final static String ALL_CATEGORY_NAME = "allCategories";

	private final TreeMap<String, LinkItem> allCategories = new TreeMap<String, LinkItem>();

	private final TreeMap<String, LinkItem> allDateCategories = new TreeMap<String, LinkItem>();

	private final TreeMap<String, TreeMap<String, LinkItem>> allSeries = new TreeMap<String, TreeMap<String, LinkItem>>();

	private final TreeSet<BlogEntry> theBlog = new TreeSet<BlogEntry>();

	private Config config = null;

	private EntriesWithFiles fileEntries = new EntriesWithFiles();
	private EntriesWithFiles imgEntries = new EntriesWithFiles();
	private final TreeMap<String, RenderInstructions> overviewPages = new TreeMap<String, RenderInstructions>();

	// for rendering and lookup of old/new URLs
	private final TreeMap<String, String> mapperOldNewURLs = new TreeMap<String, String>();

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
		riCat.key = ALL_CATEGORY_NAME;
		riCat.pageTitle = "All Categories";
		riCat.outFileName = "categories/" + config.indexFileName;
		riCat.TemplateName = config.ALL_CATEGORY_TEMPLATE;
		riCat.pageLink = ALL_CATEGORY_NAME;
		riCat.reverse = true;

		this.overviewPages.put("allEntries", riAll);
		this.overviewPages.put(ALL_CATEGORY_NAME, riCat);

	}

	/**
	 * @return the config
	 */
	public Config getConfig() {
		return this.config;
	}

	/**
	 * Laedt alle Blog entries von JSON Files auf Disk
	 *
	 * @param sourceFileOrDirName
	 * @throws IOException
	 */
	public void loadBlogFromDisk() throws IOException {
		final File srcDir = new File(this.config.sourceDirectory);
		if (!srcDir.exists()) {
			System.err.print(this.config.sourceDirectory + " doesn't exist!");
			return;
		} else if (!srcDir.isDirectory()) {
			System.err.print(this.config.sourceDirectory + " is not a directory!");
			return;
		}

		final String path = srcDir.getPath() ;
		this.loadBlogEntriesFromDisk(path+ this.getConfig().documentDirectory);
		System.out.println("\n\nBlog loaded from disk");
		this.loadFileDefinitionsFromDisk(path);
		System.out.println("\n\nFile definitions loaded from disk");
		this.cleanupLinksAndImages();
		System.out.println("\n\nCleanup complete");

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
		if (be != null) {
			LinkItem catItem = null;
			final LinkItem dateItem = new LinkItem(be.getDateYear(), this.config.webBlogLocation + be.getDateYear(),
					Utils.date2ComparableString(be.getPublishDate()));
			// Store it in the big blog list for retrieval
			this.theBlog.add(be);

			// Add the links to the mapping
			// this.config.plinkPrefix will be handled by the HTTP rule...
			// String key = this.config.plinkPrefix +
			// be.getOldURL().toLowerCase();
			final String key = be.getOldURL().toLowerCase();
			final String value = be.getNewURL();
			this.mapperOldNewURLs.put(key, value);

			// Populate the category list
			for (final LinkItem li : be.getCategory()) {
				final String catKey = li.place;
				if (this.allCategories.containsKey(catKey)) {
					this.allCategories.get(catKey).count += 1;
				} else {
					catItem = new LinkItem(li.name, this.config.webBlogLocation + this.config.categoriesLocation);
					this.allCategories.put(catKey, catItem);
				}
				
				this.overviewPages.get("allCategories").addToCategory(be, li.name, li.place);
				
			}
			// Date with month and year or only year?
			// Month - year
			// this.allDateCategories.put(be.getDateURL(), dateItem);
			// Year only
			if (this.allDateCategories.containsKey(be.getDateYear())) {
				this.allDateCategories.get(be.getDateYear()).count += 1;
			} else {
				this.allDateCategories.put(be.getDateYear(), dateItem);
			}
			// Add to the lists for category, month, year
			this.addToOverviewPage("year", null, be.getDateYear(), be);
			for (final LinkItem li : be.getCategory()) {
				this.addToOverviewPage("category", li.name, li.place, be);
			}
			this.addToOverviewPage("yearmonth", null, be.getDateYear() + "/" + be.getDateMonthNumber(), be);

			// Add to a series collection if there
			if (be.getSeries() != null) {
				final String series = be.getSeries();
				final TreeMap<String, LinkItem> c = this.allSeries.containsKey(series) ? this.allSeries.get(series)
						: new TreeMap<String, LinkItem>();
				// We sort categories reverse
				catItem = new LinkItem(be.getTitle(), this.config.webBlogLocation + be.getNewURL(),
						Utils.date2ComparableString(be.getPublishDate()), true);
				c.put(be.getNewURL(), catItem);

				this.allSeries.put(series, c);
			}

			System.out.println(be.getNewURL());
		}
	}

	private void addToOverviewPage(final String type, final String title, final String key, final BlogEntry be) {
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
			outfileName = key + "/" + this.config.indexFileName;
			pageTitle = "Year " + key;
			pageLink = key;

		} else if (type.equals("category")) {
			templateName = this.config.CATEGORY_TEMPLATE;
			outfileName = this.config.categoriesLocation + key + ".html";
			pageTitle = title;
			pageLink = key;
		} else if (type.equals("yearmonth")) {
			templateName = this.config.MONTH_TEMPLATE;
			outfileName = key + "/" + this.config.indexFileName;
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

	private String cleanupHTMLlinksAndImages(final String source, final String location) {

		final org.jsoup.nodes.Document hDoc = Jsoup.parse(source);
		this.cleanupTagUrlAttribute(hDoc, "img", "src", location);
		this.cleanupTagUrlAttribute(hDoc, "a", "href", location);
		return hDoc.body().html();

	}

	/**
	 * Goes through all Blog entries and sorts out the links to images and
	 * attachments by looking at src and href attributes
	 */
	private void cleanupLinksAndImages() {
		for (final BlogEntry be : this.theBlog) {
			this.cleanupOneBlogEntry(be);
		}
		System.out.println("Completed mapping of Blog entries");
	}

	private void cleanupOneBlogEntry(final BlogEntry be) {
		be.setMainBody(this.cleanupHTMLlinksAndImages(be.getMainBody(), be.getNewURL()));
		if ((be.getMoreBody() != null) && !be.getMoreBody().equals("")) {
			be.setMoreBody(this.cleanupHTMLlinksAndImages(be.getMoreBody(), be.getNewURL()));
		}
	}

	private void cleanupTagUrlAttribute(final org.jsoup.nodes.Document hDoc, final String elementName,
			final String attName, final String location) {

		final String query = elementName + "[" + attName + "]";
		final Elements elements = hDoc.select(query);

		for (final Element element : elements) {
			final String attValue = element.attr(attName).trim();
			if (this.mapperOldNewURLs.containsKey(attValue.toLowerCase())) {
				final String replace = this.mapperOldNewURLs.get(attValue.toLowerCase());
				System.out.print("Replacing:");
				System.out.print(attValue);
				System.out.print(" with ");
				System.out.println(replace);
				element.attr(attName, replace);
			}
		}

	}

	/**
	 * Makes sure there is a directory of that Name
	 */
	private void ensureDirectory(final String directoryName) {
		final File dir = new File(directoryName);
		if (dir.exists() && dir.isDirectory()) {
			return;
		}

		if (dir.exists()) {
			// It is NOT a directory, so we strip the file name
			final String outDirs = directoryName.substring(0, directoryName.lastIndexOf("/"));
			this.ensureDirectory(outDirs);
		} else {

			dir.mkdirs();
		}
	}

	/**
	 * Loads Blog entries from JSON recursively from disk
	 *
	 * @param sourceFileOrDirName
	 * @throws IOException
	 */
	private void loadBlogEntriesFromDisk(final String sourceFileOrDirName) {
		final File srcDir = new File(sourceFileOrDirName);
		if (!srcDir.exists()) {
			System.err.print(sourceFileOrDirName + " doesn't exist");
			return;
		}

		if (srcDir.isDirectory()) {
			// Recursive call to get files in directory structure
			for (final String curFile : srcDir.list()) {
				this.loadBlogEntriesFromDisk(srcDir.getPath() + "/" + curFile);
			}

		} else if (srcDir.getName().endsWith(".json")) {
			BlogEntry be = null;
			try {
				final FileInputStream in = new FileInputStream(new File(sourceFileOrDirName));
				be = BlogEntry.loadDataFromJson(in, this.config);
				in.close();
			} catch (final Exception e) {
				e.printStackTrace();
			}
			if ((be != null) && (be.getTitle() != null)) {
				this.addBlogContext(be);
			}

		}

	}

	private EntriesWithFiles loadFileDefFromDisk(final String sourceFileName) {

		EntriesWithFiles result = null;
		final File source = new File(sourceFileName);

		try {
			final FileInputStream in = new FileInputStream(source);
			result = EntriesWithFiles.loadDataFromJson(in);
			in.close();
		} catch (final IOException e) {
			e.printStackTrace();
		}

		return result;

	}

	private void loadFileDefinitionsFromDisk(final String sourceDirectory) {

		this.imgEntries = this.loadFileDefFromDisk(sourceDirectory + this.config.imageDirectory + "images.json");
		this.fileEntries = this
				.loadFileDefFromDisk(sourceDirectory + this.config.attachmentDirectory + "attachments.json");
		this.updateMapper(this.mapperOldNewURLs, this.imgEntries);
		this.updateMapper(this.mapperOldNewURLs, this.fileEntries);
	}

	private void render404() throws IOException {

		final String template = this.config.ERROR_TEMPLATE;
		final String finalDestination = this.config.destinationDirectory + this.config.errorFileName;

		final ByteArrayOutputStream out = new ByteArrayOutputStream(102400);
		final Writer pw = new PrintWriter(out);

		final MustacheFactory mf = new DefaultMustacheFactory(new File(this.config.templateDirectory));
		final Mustache mustache = mf.compile(template);

		final BlogIndex bi = new BlogIndex();
		bi.allCategories = this.allCategories.values();
		bi.allDateCategories = this.allDateCategories.values();
		bi.topArticles = new BlogEntryCollection(true);
		final int max = 5;
		int i = 0;
		final Iterator<BlogEntry> it = this.theBlog.descendingSet().iterator();

		while (it.hasNext() && (i < max)) {
			final BlogEntry cur = it.next();
			if (cur.getStatus().equals("Published")) {
				bi.topArticles.add(cur);
				i++;
			}
		}

		mustache.execute(pw, bi);
		pw.flush();

		pw.close();
		this.saveIfChanged(out.toByteArray(), finalDestination);
		System.out.println("Rendered 404");

	}

	private void renderAttachments() throws IOException {

		final String template = this.config.ATTACHMENT_TEMPLATE;
		final String finalDestination = this.config.destinationDirectory + this.config.downloadDirectory
				+ this.config.indexFileName;
		File wohin = new File(finalDestination);
		Files.createParentDirs(wohin);
		final FileOutputStream out = new FileOutputStream(wohin);
		final Writer pw = new PrintWriter(out);

		final MustacheFactory mf = new DefaultMustacheFactory(new File(this.config.templateDirectory));
		final Mustache mustache = mf.compile(template);
		mustache.execute(pw, this.fileEntries);
		pw.flush();

		pw.close();
		out.close();

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
		final Set<String> completedSeries = new HashSet<String>();
		final MustacheFactory mf = new DefaultMustacheFactory(new File(this.config.templateDirectory));
		final Mustache mustache = mf.compile(template);

		BlogEntry renderEntry = null;

		// Blog entries
		for (final BlogEntry be : this.theBlog) {
			be.setAllCategories(this.allCategories.values());
			be.setAllDateCategories(this.allDateCategories.descendingMap().values());

			if ((be.getSeries() != null) && be.getStatus().equals("Published")) {
				final String series = be.getSeries();
				if (this.allSeries.containsKey(series)) {
					final List<LinkItem> l = new ArrayList<LinkItem>();
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
			// needs its nextLink populated by be and be needs its previousLink
			// populated by renderentry
			if (renderEntry != null) {
				if (be.getStatus().equals("Published")) {
					renderEntry.setNextItem(be.getLinkItem(baseDir));
				}
				be.setPreviousItem(renderEntry.getLinkItem(baseDir));
				this.renderOneEntry(renderEntry, mustache);
			}

			renderEntry = be;
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

		System.out.println("...Done...");
	}

	private void renderImprint() throws IOException {

		final String template = this.config.IMPRINT_TEMPLATE;
		final String finalDestination = this.config.destinationDirectory + this.config.imprintFileName;

		final ByteArrayOutputStream out = new ByteArrayOutputStream(102400);
		final Writer pw = new PrintWriter(out);

		final MustacheFactory mf = new DefaultMustacheFactory(new File(this.config.templateDirectory));
		final Mustache mustache = mf.compile(template);

		final BlogIndex bi = new BlogIndex();
		bi.allCategories = this.allCategories.values();
		bi.allDateCategories = this.allDateCategories.values();
		bi.topArticles = new BlogEntryCollection(true);
		final int max = 5;
		int i = 0;
		final Iterator<BlogEntry> it = this.theBlog.descendingSet().iterator();

		while (it.hasNext() && (i < max)) {
			final BlogEntry cur = it.next();
			if (cur.getStatus().equals("Published")) {
				bi.topArticles.add(cur);
				i++;
			}
		}

		mustache.execute(pw, bi);
		pw.flush();

		pw.close();
		this.saveIfChanged(out.toByteArray(), finalDestination);
		System.out.println("Rendered Imprint");

	}

	private void renderIndex() throws IOException {

		final String template = this.config.INDEX_TEMPLATE;
		final String finalDestination = this.config.destinationDirectory + this.config.indexFileName;

		final ByteArrayOutputStream out = new ByteArrayOutputStream(102400);
		final Writer pw = new PrintWriter(out);

		final MustacheFactory mf = new DefaultMustacheFactory(new File(this.config.templateDirectory));
		final Mustache mustache = mf.compile(template);

		final BlogIndex bi = new BlogIndex();
		bi.allCategories = this.allCategories.values();
		bi.allDateCategories = this.allDateCategories.values();
		bi.topArticles = new BlogEntryCollection(true);
		final int max = 10;
		int i = 0;
		final Iterator<BlogEntry> it = this.theBlog.descendingIterator();

		while (it.hasNext() && (i < max)) {
			final BlogEntry cur = it.next();
			if (cur.getStatus().equals("Published")) {
				bi.topArticles.add(cur);
				i++;
			}
		}

		mustache.execute(pw, bi);
		pw.flush();

		pw.close();
		this.saveIfChanged(out.toByteArray(), finalDestination);
		System.out.println("Rendered Index");
	}

	private void renderIndexRSS() {
		final String finalDestination = this.config.destinationDirectory + this.config.indexRSSName;
		final String finalDestination2 = this.config.destinationDirectory + this.config.indexRSSName2;
		final ByteArrayOutputStream out = new ByteArrayOutputStream(102400);
		final BlogIndex bi = new BlogIndex();
		bi.allCategories = this.allCategories.values();
		bi.allDateCategories = this.allDateCategories.values();
		bi.topArticles = new BlogEntryCollection(true);
		final int max = 10;
		int i = 0;
		final Iterator<BlogEntry> it = this.theBlog.descendingIterator();

		while (it.hasNext() && (i < max)) {
			final BlogEntry cur = it.next();
			if (cur.getStatus().equals("Published")) {
				bi.topArticles.add(cur);
				i++;
			}
		}

		final RSSFeedWriter rss = new RSSFeedWriter(this.getConfig(), bi);
		try {
			rss.write(out);
			if (this.saveIfChanged(out.toByteArray(), finalDestination) || this.saveIfChanged(out.toByteArray(), finalDestination2)) {
				System.out.println("\nRendered stories.rss and stories.xml");
			} else {
				System.out.println("\nRSS hasn't changed!");
			}
			
		} catch (final Exception e) {
			System.out.println("\nstories.rss rendering failed: " + e.getMessage());
		}

	}

	/**
	 * Creates the map for blog redirections
	 *
	 * @param destination
	 * @throws FileNotFoundException
	 */
	private void renderNGinxURLMapper() throws FileNotFoundException {
		final File outFile = new File(this.config.destinationDirectory + this.config.urlmapNGinx);
		if (outFile.exists()) {
			outFile.delete();
		}

		final ArrayList<String> keysWritten = new ArrayList<String>();

		final PrintWriter pw = new PrintWriter(outFile);

		for (final Map.Entry<String, String> e : this.mapperOldNewURLs.entrySet()) {
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

		}
		pw.flush();
		pw.close();
		System.out.println("URL mapping written to file " + outFile.getPath());

	}

	private void renderOneEntry(final BlogEntry be, final Mustache mustache) throws IOException {

		final String location = this.config.destinationDirectory + be.getNewURL();
		final String outDirs = location.substring(0, location.lastIndexOf("/"));
		final File dirs = new File(outDirs);
		if (!dirs.exists()) {
			dirs.mkdirs();
		}
		// Set the current context
		for (final LinkItem cat : be.getCategory()) {
			final String c = cat.place;
			this.allCategories.get(c).active = true;
		}
		this.allDateCategories.get(be.getDateYear()).active = true;

		if (be.getSeries() != null) {
			final String series = be.getSeries();
			if (this.allSeries.containsKey(series)) {
				this.allSeries.get(series).get(be.getNewURL()).active = true;
			}
		}

		// Prepare to write out
		final ByteArrayOutputStream out = new ByteArrayOutputStream(102400);
		final Writer pw = new PrintWriter(out);

		// This is where the magic happens
		mustache.execute(pw, be);
		pw.flush();
		pw.close();
		this.saveIfChanged(out.toByteArray(), location);

		// Cleanup
		for (final LinkItem cat : be.getCategory()) {
			final String c = cat.place;
			this.allCategories.get(c).active = false;
		}
		this.allDateCategories.get(be.getDateYear()).active = false;

		if (be.getSeries() != null) {
			final String series = be.getSeries();
			if (this.allSeries.containsKey(series)) {
				this.allSeries.get(series).get(be.getNewURL()).active = false;
			}
		}
	}

	private void renderOverViewPage(final RenderInstructions ri) {
		final String template = ri.getFinalTemplateName(this.config.templateDirectory, ri.key);
		final String finalDestination = this.config.destinationDirectory + ri.outFileName;
		boolean goodToGo = false;
		try {

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
			bi.pageTitle = ri.pageTitle;
			bi.pageLink = ri.pageLink;
			bi.nextItem = ri.nextItem;
			bi.previousItem = ri.previousItem;

			// The renderinstructions might have entries or categorized entries
			// We check for null before we render

			if (ri.members != null) {
				bi.topArticles = new BlogEntryCollection(!ri.reverse);
				// Little confusion on sorting order
				final Iterator<BlogEntry> it = ri.members.iterator();
				// We need to copy from the render instruction to get the
				// sequence reversed
				while (it.hasNext()) {
					final BlogEntry cur = it.next();
					if (cur.getStatus().equals("Published")) {
						bi.topArticles.add(cur);
					}
				}
				goodToGo = true;
			} else if (ri.categories != null) {
				bi.categorizedEntries = new ArrayList<BlogIndex>();
				final Iterator<String> it = (ri.reverse) ? ri.categories.keySet().iterator(): ri.categories.descendingKeySet().iterator();
				// We need to copy from the render instruction to get the
				// sequence reversed
				while (it.hasNext()) {
					final RenderInstructions curRi = ri.categories.get(it.next());
					final BlogIndex subBi = new BlogIndex();
					subBi.allCategories = this.allCategories.values();
					subBi.allDateCategories = this.allDateCategories.values();
					subBi.pageTitle = curRi.pageTitle;
					subBi.pageLink = curRi.pageLink;
					subBi.topArticles = new BlogEntryCollection(true);
					bi.categorizedEntries.add(subBi);
					final Iterator<BlogEntry> subIt = curRi.members.descendingSet().iterator();
					while (subIt.hasNext()) {
						final BlogEntry cur = subIt.next();
						if (cur.getStatus().equals("Published")) {
							subBi.topArticles.add(cur);
						}
					}

				}
				goodToGo = true;
			}

			if (goodToGo) {
				final ByteArrayOutputStream out = new ByteArrayOutputStream(102400);
				final Writer pw = new PrintWriter(out);

				final MustacheFactory mf = new DefaultMustacheFactory(new File(this.config.templateDirectory));
				final Mustache mustache = mf.compile(template);

				mustache.execute(pw, bi);
				pw.flush();
				pw.close();

				this.saveIfChanged(out.toByteArray(), finalDestination);
			}

		} catch (final IOException e) {
			e.printStackTrace();
		}

		// Reset of categories
		if (this.allCategories.containsKey(ri.key)) {
			this.allCategories.get(ri.key).active = false;
		}
		if (this.allDateCategories.containsKey(ri.key)) {
			this.allDateCategories.get(ri.key).active = false;
		}

		// System.out.println("Overview: " + ri.outFileName);
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
				previousEntry = new LinkItem(currentRI.pageTitle, baseDir + currentRI.outFileName, null);
				currentRI.nextItem = nextEntry;
				ri.previousItem = previousEntry;
				this.renderOverViewPage(currentRI);
			}
			currentRI = ri;
		}
		// The last entry wasn't rendered inside the loop
		this.renderOverViewPage(currentRI);
	}

	private void renderSeries(final BlogIndex bi) throws IOException {

		final String template = this.config.SERIES_TEMPLATE;
		final String finalDestination = this.config.destinationDirectory + this.config.seriesFileName;

		final FileOutputStream out = new FileOutputStream(new File(finalDestination));
		final Writer pw = new PrintWriter(out);

		final MustacheFactory mf = new DefaultMustacheFactory(new File(this.config.templateDirectory));
		final Mustache mustache = mf.compile(template);

		bi.allCategories = this.allCategories.values();
		bi.allDateCategories = this.allDateCategories.values();

		mustache.execute(pw, bi);
		pw.flush();

		pw.close();
		out.close();
		System.out.println("Rendered series");
	}

	/**
	 * Creates the map for blog redirections
	 *
	 * @param destination
	 * @throws FileNotFoundException
	 */
	private void renderURLMapper() throws FileNotFoundException {
		final File outFile = new File(this.config.destinationDirectory + this.config.urlmapFile);
		if (outFile.exists()) {
			outFile.delete();
		}

		final PrintWriter pw = new PrintWriter(outFile);
		pw.write("# Mapping of legacy blog URL into the new format\n");

		for (final Map.Entry<String, String> e : this.mapperOldNewURLs.entrySet()) {
			pw.write(e.getKey().toLowerCase());
			pw.write(" ");
			pw.write(e.getValue());
			pw.write("\n");
		}
		pw.flush();
		pw.close();
		System.out.println("URL mapping written to file " + outFile.getPath());

	}

	private void saveBlogEntry(final BlogEntry be, final String wheretoSave) throws IOException {

		System.out.println(be.getNewURL());

		final String location = wheretoSave + be.getNewURL() + ".json";
		final File outFile = new File(location);
		if (outFile.exists()) {
			outFile.delete();
		} else {
			final String outDirs = location.substring(0, location.lastIndexOf("/"));
			this.ensureDirectory(outDirs);
		}

		final FileOutputStream out = new FileOutputStream(outFile);
		be.saveDatatoJson(out);
		out.flush();
		out.close();
	}

	private boolean saveIfChanged(final byte[] newData, final String targetName) {

		boolean saveThis = false;

		final File targetFile = new File(targetName);
		if (targetFile.isDirectory()) {
			System.out.println("Directory encountered!" + targetName);
		} else if (targetFile.exists()) {
			try {
				final InputStream existing = new FileInputStream(targetFile);
				final ByteArrayOutputStream compare = new ByteArrayOutputStream(102400); // 100k
				// for
				// images
				ByteStreams.copy(existing, compare);
				// Save if they are not equal..
				saveThis = !Arrays.equals(newData, compare.toByteArray());
				Closeables.close(existing, true);
			} catch (final FileNotFoundException e) {
				// Anything goes wrong -> we save the file
				e.printStackTrace();
				saveThis = true;
			} catch (final IOException e) {
				// Anything goes wrong -> we save the file
				e.printStackTrace();
				saveThis = true;
			}

			// Now if it is there, get rid of it.
			if (saveThis) {
				targetFile.delete();
			}

		} else {
			saveThis = true;
		}

		if (saveThis) {
			OutputStream finalOut = null;
			
			try {
				// Ensure the directory structure exists
				Files.createParentDirs(targetFile);
				finalOut = new FileOutputStream(targetFile);
				finalOut.write(newData);
				finalOut.flush();
				Closeables.close(finalOut, true);
			} catch (final FileNotFoundException e) {
				e.printStackTrace();
			} catch (final IOException e) {
				e.printStackTrace();
			}
			System.out.println("\n+" + targetFile);
		} else {
			System.out.print(".");
		}
		
		return saveThis;
	}

	private void updateMapper(final Map<String, String> mapper, final EntriesWithFiles outerList) {
		for (final FileEntry oneEntry : outerList.getAttachmentList()) {
			final String oldUrlBeginning = oneEntry.url;
			for (final FileEntry subEntry : oneEntry.subEntries) {
				final String old = (oldUrlBeginning + subEntry.subject).toLowerCase();
				mapper.put(old, subEntry.url);
			}
		}
	}

}

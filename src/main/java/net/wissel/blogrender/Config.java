/**
 * Configuration singleton - provides all configuration settings
 * for the Blogmover instead of static variables
 */
package net.wissel.blogrender;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * @author stw
 *
 */
public class Config {

	private static class ConfigHolder {
		private static Config INSTANCE = null;

		public static final Config getInstance(final String path) {
			if (ConfigHolder.INSTANCE == null) {
				final File configFile;
				if (path != null) {
					configFile = new File(path);
				} else {
					configFile = new File(Config.CONFIG_NAME);
				}
				if (configFile.exists()) {
					ConfigHolder.INSTANCE = Config.load(configFile);
				} else {
					ConfigHolder.INSTANCE = new Config();
					// We save so we have a parameter file later on
					ConfigHolder.INSTANCE.save(configFile);
				}
			}
			return ConfigHolder.INSTANCE;
		}
	}

    public static final String SITEMAP_NAME = "sitemap.xml";

	public static final String CONFIG_NAME = "BlogRenderConfig.json";

	public static Config get() {
		return ConfigHolder.getInstance(null);
	}

	public static Config get(final String path) {
		return ConfigHolder.getInstance(path);
	}

	private static Config load(final File destination) {
		Config result = null;

		try {
			System.out.println("Loading parameters from " + destination.getAbsolutePath());
			final FileInputStream in = new FileInputStream(destination);
			final Gson gson = new GsonBuilder().create();
			result = gson.fromJson(new InputStreamReader(in), Config.class);
			in.close();
		} catch (final IOException e) {
			e.printStackTrace();
		}
		return result;
	}

	// Templates
	public String INDEX_TEMPLATE = "index.mustache";
	public String ERROR_TEMPLATE = "404.mustache";
	public String CATEGORY_TEMPLATE = "category.mustache";
	public String YEAR_TEMPLATE = "blogyear.mustache";
	public String MONTH_TEMPLATE = "blogmonth.mustache";
	public String ENTRY_TEMPLATE = "blogentry.mustache";
	public String ATTACHMENT_TEMPLATE = "downloads.mustache";

	public String ALL_CATEGORY_TEMPLATE = "allcategory.mustache";
	public String ALL_INDEX_TEMPLATE = "allindex.mustache";
	public String IMPRINT_TEMPLATE = "imprint.mustache";

	public String SERIES_TEMPLATE = "series.mustache";
	public String MARKDOW_SEPARATOR = "---";
	// Disk locations
	public String sourceDirectory = "/home/stw/Documents/Projects/wisselblog/src/";
	public String templateDirectory = "/home/stw/Documents/Projects/wisselblog/src/layouts/";
	public String destinationDirectory = "/home/stw/www/blog/";
	// Relative location of Blog to root of website
	public String webBlogLocation = "/blog/";
	// Where in the source are the documents
	public String documentDirectory = "documents/";
	public String attachmentDirectory = "attachments/";
	public String commentDirectory = "comments/";
	public String imageDirectory = "images/";
	public String downloadDirectory = "downloads/";
	// Subdirectories based on both final directory and URL
	public String categoriesLocation = "categories/";
	// FileName for the directory files - should never need a change
	public String indexFileName = "index.html";
	public String indexRSSName = "stories.xml";
	public String indexRSSName2 = "stories.rss";
	public String seriesFileName = "series.html";
	public String errorFileName = "404.html";
	public String allIndexFileName = "all.html";
	public String imprintFileName = "imprint.html";
	// Where in the existing blog are the permanent links
	public String plinkPrefix = "/blog/d6plinks/";
	public String urlmapFile = "blogmap.txt";

	public String urlmapNGinx = "blognginx.map";
	public String oldImageLocation = "/blog/Images/";
	public String bloghost = "wissel.net";
	// RSS Properties
	public String copyRight = "(C) 2003 - 2018 Stephan H. Wissel, All rights reserved";

	public String language = "en,de";

	public String rssDescription = "Thoughts, Insights and Opinions of Stephan H. Wissel. Topics included: IBM Lotus Notes and Domino, IBM Websphere, other IBM Lotus stuff,  J2EE, .Net, Software Archtecture, Personcentric Development, Agile Software, SDLC, Singapore and my Twins";

	public String rssLink = "https://wissel.net/blog/stories.rss";

	public String rssTitle = "wissel.net Usability - Productivity - Business - The web - Singapore and Twins";

	private Config() {
		// Hide the constructor, so there can only be
		// one instance of the class
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#clone()
	 */
	@Override
	protected Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}

	/**
	 * We have a configuration file to create
	 */
	private void save(final File destination) {

		try {
			System.out.println("Saving parameters to " + destination.getAbsolutePath());
			final FileOutputStream out = new FileOutputStream(destination);
			final GsonBuilder gb = new GsonBuilder();
			gb.setPrettyPrinting();
			gb.disableHtmlEscaping();
			final Gson gson = gb.create();
			final PrintWriter writer = new PrintWriter(out);
			gson.toJson(this, writer);
			writer.flush();
			writer.close();
			out.close();
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

}

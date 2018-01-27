/** ========================================================================= *
 * Copyright (C)  2017, 2018 Stephan Wissel                                   *
 *                            All rights reserved.                            *
 *                                                                            *
 *  @author     Stephan H. Wissel (stw) <stephan@wissel@net>                  *
 *                                       @notessensei                         *
 * @version     1.0                                                           *
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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class BlogEntry implements Serializable, Comparable<BlogEntry> {

	public static final String NEWLINE = System.getProperty("line.separator");

	public static final String DATE_FORMAT = "yyyy-MM-dd hh:mm";

	private static final long serialVersionUID = 1L;

	public static BlogEntry loadDataFromJson(final InputStream in, final Config config) {
		BlogEntry result = null;
		final Gson gson = new GsonBuilder().create();
		result = gson.fromJson(new InputStreamReader(in), BlogEntry.class);

		// Load new Comments
		result.loadCommentsFromDisk(config);
		result.cleanupComments();
		return result;
	}

	private String author;

	private List<String> category = new ArrayList<String>();

	private Date publishDate = new Date();
	// The HTML representation
	private String mainBody = null;
	// If there's more to read
	private String moreBody = null;
	private String location;
	private String status;
	private String title;
	// Is this article in a mini series
	private String series = null;
	private String UNID;
	private String newURL;
	private String oldURL;
	private String storyImage;
	private Boolean commentsclosed = false;
	private final Map<String, BlogComments> comments = new HashMap<String, BlogComments>();
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

	/**
	 * @param category
	 *            the category to set
	 */
	public void addCategory(final String cat2add) {
		this.category.add(cat2add);
	}

	@Override
	public int compareTo(final BlogEntry be) {
		// TODO: Do we need to add the name?
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

	/**
	 * @return the author
	 */
	public String getAuthor() {
		return this.author;
	}

	/**
	 * @return the category
	 */
	public List<String> getCategory() {
		return this.category;
	}

	public String getCommentCount() {
		if ((this.comments == null) || this.comments.isEmpty()) {
			return "0";
		}
		return Integer.toHexString(this.comments.size());
	}

	/**
	 * @return the comments
	 */
	public Set<BlogComments> getComments() {
		final Set<BlogComments> result = new TreeSet<BlogComments>();
		result.addAll(this.comments.values());
		return result;
	}

	/**
	 * @return the commentsclosed
	 */
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

	/**
	 * Returns an unique key for comparison
	 *
	 * @return
	 */
	public String getKey() {
		return this.getShortDate() + " - " + this.getNewURL();
	}

	public LinkItem getLinkItem(final String baseURI) {
		return new LinkItem(this.getTitle(), baseURI + this.getNewURL(),
				Utils.date2ComparableString(this.getPublishDate()));
	}

	/**
	 * @return the location
	 */
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
	 * @return the newURL
	 */
	public String getNewURL() {
		return this.newURL;
	}

	/**
	 * @return the nextItem
	 */
	public LinkItem getNextItem() {
		return this.nextItem;
	}

	/**
	 * @return the oldURL
	 */
	public String getOldURL() {
		if (this.oldURL == null) {
			return "none";
		}
		return this.oldURL;
	}

	/**
	 * @return the previousItem
	 */
	public LinkItem getPreviousItem() {
		return this.previousItem;
	}

	/**
	 * @return the publishDate
	 */
	public Date getPublishDate() {
		return this.publishDate;
	}

	public String getPublishDateString() {
		final SimpleDateFormat sdf = new SimpleDateFormat(BlogEntry.DATE_FORMAT);
		return sdf.format(this.getPublishDate() == null ? new Date() : this.getPublishDate());
	}

	public String getPublishDateStringShort() {
		final SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy");
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
	 * @return the status
	 */
	public String getStatus() {
		return this.status;
	}

	/**
	 * @return the storyImage
	 */
	public String getStoryImage() {
		return this.storyImage;
	}

	/**
	 * @return the title
	 */
	public String getTitle() {
		return this.title;
	}

	/**
	 * @return the uNID
	 */
	public String getUNID() {
		return this.UNID;
	}

	public boolean isBlog() {
		return this.isBlog;
	}

	public void saveDatatoDocPad(final FileOutputStream out) {
		// TODO Saves content to DocPad format
		final PrintWriter w = new PrintWriter(out);
		// Parameter separator
		w.write("---");
		w.write(BlogEntry.NEWLINE);
		w.write("layout: default");
		w.write(BlogEntry.NEWLINE);
		w.write("author: \"");
		w.write(this.getAuthor());
		w.write("\"\n");
		w.write("category: [");
		for (int i = 0; i < this.getCategory().size(); i++) {
			if (i > 0) {
				w.write(", ");
			}
			w.write("\"");
			w.write(this.getCategory().get(i));
			w.write("\"");
		}
		w.write("]\n");
		w.write("publishDate: \"");
		w.write(this.getPublishDateString());
		w.write("\"\n");
		w.write("dateForArchive: \"");
		w.write(this.getPublishDateStringShort());
		w.write("\"\n");
		w.write("location: \"");
		w.write(this.getLocation());
		w.write("\"\n");
		w.write("status: \"");
		w.write(this.getStatus());
		w.write("\"\n");
		w.write("title: \"");
		w.write(this.getTitle());
		w.write("\"\n");
		w.write("UNID: \"");
		w.write(this.getUNID());
		w.write("\"\n");
		w.write("newURL: \"");
		w.write(this.getNewURL());
		w.write("\"\n");
		w.write("oldURL: \"");
		w.write(this.getOldURL());
		w.write("\"\n");
		w.write("---");
		w.write(BlogEntry.NEWLINE);
		w.write(this.getMainBody());
		if ((this.getMoreBody() != null) && !this.getMainBody().trim().equals("")) {
			w.write(this.getMainBody());
		}
		w.write(BlogEntry.NEWLINE);
		w.flush();
		w.close();
	}

	/**
	 * Save the object to a JSON file for reuse
	 */
	public void saveDatatoJson(final OutputStream out) {
		final GsonBuilder gb = new GsonBuilder();
		gb.setPrettyPrinting();
		gb.disableHtmlEscaping();
		final Gson gson = gb.create();
		final PrintWriter writer = new PrintWriter(out);
		gson.toJson(this, writer);
		writer.flush();
		writer.close();
	}

	/**
	 * @param allCategories
	 *            the allCategories to set
	 */
	public void setAllCategories(final Collection<LinkItem> allCategories) {
		this.allCategories = allCategories;
	}

	/**
	 * @param allDateCategories
	 *            the allDateCategories to set
	 */
	public void setAllDateCategories(final Collection<LinkItem> allDateCategories) {
		this.allDateCategories = allDateCategories;
	}

	/**
	 * @param author
	 *            the author to set
	 */
	public void setAuthor(final String author) {
		this.author = author;
	}

	/**
	 * @param category
	 *            the category to set
	 */
	public void setCategory(final List<String> category) {
		this.category = category;
	}

	/**
	 * @param comments
	 *            the comments to set
	 */
	public void setComments(final Set<BlogComments> comments) {
		this.comments.clear();
		comments.forEach(comment -> {
			this.comments.put(comment.getUNID(), comment);
		});
	}

	public void setCommentStatus(final String cStatus) {

		if (cStatus.equals("0") || cStatus.equals("")) {
			this.commentsclosed = false;
		} else {
			this.commentsclosed = true;
		}
	}

	/**
	 * @param location
	 *            the location to set
	 */
	public void setLocation(final String location) {
		this.location = location;
	}

	/**
	 * @param mainBody
	 *            the mainBody to set
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
	 *            the moreBody to set
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
	 * @param newURL
	 *            the newURL to set
	 */
	public void setNewURL(final String newURL) {
		this.newURL = newURL;
	}

	/**
	 * @param nextItem
	 *            the nextItem to set
	 */
	public void setNextItem(final LinkItem nextItem) {
		this.nextItem = nextItem;
	}

	/**
	 * @param oldURL
	 *            the oldURL to set
	 */
	public void setOldURL(final String oldURL) {
		this.oldURL = oldURL;
	}

	/**
	 * @param previousItem
	 *            the previousItem to set
	 */
	public void setPreviousItem(final LinkItem previousItem) {
		this.previousItem = previousItem;
	}

	/**
	 * @param publishDate
	 *            the publishDate to set
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
	 * @param status
	 *            the status to set
	 */
	public void setStatus(final String status) {
		this.status = status;
	}

	/**
	 * @param storyImage
	 *            the storyImage to set
	 */
	public void setStoryImage(final String storyImage) {
		this.storyImage = storyImage;
	}

	/**
	 * @param title
	 *            the title to set
	 */
	public void setTitle(final String title) {
		this.title = title;
	}

	/**
	 * @param uNID
	 *            the uNID to set
	 */
	public void setUNID(final String uNID) {
		this.UNID = uNID;
	}

	@Override
	public String toString() {
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		this.saveDatatoJson(out);
		return out.toString();
	}

	private void cleanupComments() {
		this.comments.forEach((key,entry) -> {
			String candidate = entry.getComment();
			int startpos = candidate.indexOf("<body>");
			int endpos = candidate.indexOf("</body>");
			// Stripping out head/body
			if (startpos > -1 && endpos > 0) {
				String result = candidate.substring(startpos+6, endpos);
				entry.setComment(result);
			}
		});

	}

	/**
	 * Comments could be inside the the main article (from legacy) or be in a
	 * separate directory (new). This function loads them from disk
	 */
	private void loadCommentsFromDisk(final Config config) {
		final File commentDir = new File(config.sourceDirectory + config.commentDirectory + "/" + this.getUNID());
		if (commentDir.exists() && commentDir.isDirectory()) {
			// We have comments (eventually)
			for (final String curFile : commentDir.list()) {
				final BlogComments curComm = BlogComments.loadFromJson(commentDir.getPath() + "/" + curFile);
				if (curComm != null) {
					this.comments.put(curComm.getUNID(), curComm);
				}
			}
		}

	}
}

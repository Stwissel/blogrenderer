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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class BlogEntry extends BlogEntryMeta implements Serializable, Comparable<BlogEntry> {

    public static final String NEWLINE = System.getProperty("line.separator");

    public static final String DATE_FORMAT = "yyyy-MM-dd hh:mm";

    private static final long serialVersionUID = 1L;

    public static BlogEntry loadDataFromJson(final InputStream in, final String fileName, final Config config) {
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

    // The HTML representation
    private String mainBody = null;
    // If there's more to read
    private String                          moreBody = null;
    private final Map<String, BlogComments> comments = new HashMap<String, BlogComments>();
    // The following strings are redundant, but it
    // makes it easier to deal with the JSON then
    private String allBody;
    private String shortDate;

    private String dateCategory;
    // The following variables are only used by the
    // templating engine and are not fed into the JSON
    private transient Collection<LinkItem> allCategories     = null;
    private transient Collection<LinkItem> allDateCategories = null;
    private transient Collection<LinkItem> seriesMember      = null;

    private transient LinkItem previousItem = null;
    private transient LinkItem nextItem     = null;
    private final boolean      isBlog       = true;

    public transient String metaFileName;
    public transient String sourceFileName = null;
    public transient String sourceMoreFileName = null;

    /**
     * @param category
     *            the category to set
     */
    public void addCategory(final String cat2add) {
        this.getCategory().add(cat2add);
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

    /**
     * @return the previousItem
     */
    public LinkItem getPreviousItem() {
        return this.previousItem;
    }

    public String getPublishDateString() {
        final SimpleDateFormat sdf = new SimpleDateFormat(BlogEntry.DATE_FORMAT);
        return sdf.format(this.getPublishDate() == null ? new Date() : this.getPublishDate());
    }

    public String getPublishDateStringShort() {
        final SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy");
        return sdf.format(this.getPublishDate());
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
     * @param comments
     *            the comments to set
     */
    public void setComments(final Set<BlogComments> comments) {
        this.comments.clear();
        comments.forEach(comment -> {
            this.comments.put(comment.getUNID(), comment);
        });
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
     * @param nextItem
     *            the nextItem to set
     */
    public void setNextItem(final LinkItem nextItem) {
        this.nextItem = nextItem;
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
    @Override
    public void setPublishDate(final Date publishDate) {
        super.setPublishDate(publishDate);
        this.shortDate = this.getPublishDateString();
        this.dateCategory = this.getPublishDateStringShort();
    }

    public void setSeriesMember(final List<LinkItem> seriesMember) {
        Collections.sort(seriesMember);
        Collections.reverse(seriesMember);
        this.seriesMember = seriesMember;
    }

    @Override
    public String toString() {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        this.saveDatatoJson(out);
        return out.toString();
    }

    private void addBlogBodyFromFiles(final String fileName) {
        if (!fileName.endsWith(".json")) {
            return;
        }
        this.metaFileName = this.getMetafileName(fileName);
        final String htmlContentFile = fileName.substring(0, fileName.lastIndexOf(".json"));
        final String htmlMoreFile = htmlContentFile.substring(0, htmlContentFile.lastIndexOf(".html")) + ".more.html";
        final String mdContentFile = htmlContentFile.substring(0, htmlContentFile.lastIndexOf(".html")) + ".md";
        final String mdMoreFile = htmlContentFile.substring(0, htmlContentFile.lastIndexOf(".html")) + ".more.md";

        final File mdFile = new File(mdContentFile);
        final File moreMdFile = new File(mdMoreFile);
        final File htmlFile = new File(htmlContentFile);
        final File moreFile = new File(htmlMoreFile);

        // Check if we have content as markdown or HTML file. HTML takes
        // priority over md file

        // Markdown content check
        if (mdFile.exists()) {
            try {
                this.setSourceType("MARKDOWN");
                this.sourceFileName = mdFile.getAbsolutePath();
                final String mdContenCandidate = Files.asCharSource(mdFile, Charsets.UTF_8).read();
                final String htmlContent = MarkdownConverter.markdown2HtmlWithCode(mdContenCandidate);
                this.setMainBody(htmlContent);
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }

        if (moreMdFile.exists()) {
            try {
                this.sourceMoreFileName = moreMdFile.getAbsolutePath();
                final String moreContentCandidate = Files.asCharSource(moreMdFile, Charsets.UTF_8).read();
                final String moreContent = MarkdownConverter.markdown2HtmlWithCode(moreContentCandidate);
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
                final String htmlContent = Files.asCharSource(htmlFile, Charsets.UTF_8).read();
                this.setMainBody(htmlContent);
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }

        if (moreFile.exists()) {
            try {
                this.sourceMoreFileName = moreFile.getAbsolutePath();
                final String moreContent = Files.asCharSource(moreFile, Charsets.UTF_8).read();
                this.setMoreBody(moreContent);
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }

    }

    private String getMetafileName(String fileName) {
        boolean done = false;
        while (!done) {
            int lastDot = fileName.lastIndexOf(".");
            if (lastDot < 0) {
                done = true;
            } else {
                String suffix = fileName.substring(lastDot);
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
        ;
        return fileName;
    }

    private void cleanupComments() {
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

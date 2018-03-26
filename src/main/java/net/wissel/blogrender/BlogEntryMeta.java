/** ========================================================================= *
 * Copyright (C)   2018 Stephan H. Wissel                                     *
 *                            All rights reserved.                            *
 *                                                                            *
 *  @author     Stephan H. Wissel (stw) <stephan@wissel.net>                  *
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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

/**
 * Core class that holds all the meta Data that get loaded and saved from/to
 * Disk entries. Could be as JSON entry, could be as YAML prefix of a blog entry
 *
 * @author swissel
 *
 */
public class BlogEntryMeta implements Serializable {

    private static final long serialVersionUID = 1L;

    public static BlogEntryMeta loadMetaFromYaml(final String yamlString) {
        BlogEntryMeta result;
        final Constructor constructor = new Constructor(BlogEntryMeta.class);
        final Yaml yaml = new Yaml(constructor);
        result = yaml.loadAs(yamlString, BlogEntryMeta.class);
        return result;
    }

    private String       author;
    private List<String> category       = new ArrayList<String>();
    private Date         publishDate    = new Date();
    private String       location;
    private String       status;
    private String       title;
    private String       series         = null;
    private String       UNID;
    private String       newURL;
    private String       oldURL;
    private Boolean      commentsclosed = false;
    private String       sourceType;

    public BlogEntryMeta() {
        // Bean constuctor
    }

    // Constructor to dump meta data to disk
    public BlogEntryMeta(final BlogEntry be) {
        this.setAuthor(be.getAuthor());
        this.category.addAll(be.getCategory());
        this.publishDate = be.getPublishDate();
        this.location = be.getLocation();
        this.status = be.getStatus();
        this.title = be.getTitle();
        this.series = be.getSeries();
        this.UNID = be.getUNID();
        this.newURL = be.getURL();
        this.oldURL = be.getOldURL();
        this.commentsclosed = be.getCommentsclosed();
        this.setSourceType(be.getSourceType());
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
        this.nonNullMapEntry(result, "URL", this.getURL());
        this.nonNullMapEntry(result, "oldURL", this.getOldURL());
        this.nonNullMapEntry(result, "commentsclosed", this.getCommentsclosed());
        this.nonNullMapEntry(result, "SourceType", this.getSourceType());
        return result;
    }

    public String getAuthor() {
        return this.author;
    }

    public List<String> getCategory() {
        return this.category;
    }

    public Boolean getCommentsclosed() {
        return this.commentsclosed;
    }

    public String getLocation() {
        if ((this.location == null) || this.location.trim().equals("")) {
            this.setLocation("Singapore");
        }
        return this.location;
    }

    public String getOldURL() {
        return this.oldURL;
    }

    public Date getPublishDate() {
        return this.publishDate;
    }

    public String getSeries() {
        return this.series;
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

    public String getURL() {
        return this.newURL;
    }

    public void setAuthor(final String author) {
        this.author = author;
    }

    public void setCategory(final List<String> category) {
        this.category = category;
    }

    public void setCommentsclosed(final Boolean commentsclosed) {
        this.commentsclosed = commentsclosed;
    }

    public void setLocation(final String location) {
        this.location = location;
    }

    public void setOldURL(final String oldURL) {
        this.oldURL = oldURL;
    }

    public void setPublishDate(final Date publishDate) {
        this.publishDate = publishDate;
    }

    public void setSeries(final String series) {
        this.series = series;
    }

    /**
     * @param blogSourceType
     *            the blogSourceType to set
     */
    public void setSourceType(final String blogSourceType) {
        this.sourceType = ("M".equalsIgnoreCase(String.valueOf(blogSourceType).substring(0, 1))) ? "MARKDOWN" : "HTML";
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

    public void setURL(final String newURL) {
        this.newURL = newURL;
    }

    private void nonNullMapEntry(final Map<String, Object> target, final String key, final Object value) {
        if ((key == null) || (value == null) || String.valueOf(value).equals("")) {
            return;
        }
        target.put(key, value);
    }

}

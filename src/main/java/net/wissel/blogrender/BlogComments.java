package net.wissel.blogrender;

import java.io.File;
import java.io.FileReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.apache.commons.codec.digest.DigestUtils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@JsonIgnoreProperties
public class BlogComments implements Comparable<BlogComments> {

    private final static String GRAVATAR_URL        = "//www.gravatar.com/avatar/";
    private final static String GRAVATAR_SIZE       = "88";                               // Pixels
    private final static String DISPLAY_DATE_FORMAT = "EEEE dd MMMM yyyy GG - HH:mm zzzz";
    private final static String IMPORT_DATE_FORMAT = "MMMM dd, yyyy HH:mm:ss a";
    private final static String COMPARE_DATE_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS";

    public static BlogComments loadFromJson(final File commentFile) {
        final BlogComments result = new BlogComments();
        if (commentFile.exists() && commentFile.isFile()) {
            try {
                result.setCreated(new Date(commentFile.lastModified()));
                final Reader in = new FileReader(commentFile);
                final JsonParser parser = new JsonParser();
                final JsonElement je = parser.parse(in);
                in.close();
                final JsonObject rawComment = je.getAsJsonObject();

                rawComment.entrySet().forEach(entry -> {

                    final String eName = entry.getKey().toLowerCase();
                    final JsonElement value = entry.getValue();
                    try {
                        if ("commentor".equals(eName) || "author".equals(eName)) {
                            result.setAuthor(value.getAsString());
                        } else if ("website".equals(eName) || "url".equals(eName)) {
                            result.setWebSite(value.getAsString());
                        } else if ("body".equals(eName) || "comment".equals(eName)) {
                            result.setComment(value.getAsString());
                        } else if ("parentid".equals(eName)) {
                            result.setParentId(value.getAsString());
                        } else if ("unid".equals(eName)) {
                            result.setUNID(value.getAsString());
                        } else if ("markdown".equals(eName)) {
                            result.setMarkdown(value.getAsBoolean());
                        } else if ("created".equals(eName)) {
                            SimpleDateFormat sdf = new SimpleDateFormat(IMPORT_DATE_FORMAT, Locale.US);
                            // "Oct 3, 2017 2:07:04 PM"
                            Date someDate = sdf.parse(value.getAsString());
                            result.setCreated(someDate);
                        }
                    } catch (Exception e) {
                        System.err.println(eName + " didn't work:" + e.getMessage());
                    }

                });

                // Cleanup the mark
                if (result.isMarkdown()) {
                    final String markdownText = result.getComment();
                    if (markdownText != null) {
                        final String htmlText = MarkdownConverter.markdown2HtmlWithCode(markdownText);
                        result.setComment(htmlText);
                    }
                }
                ;
            } catch (final Exception e) {
                e.printStackTrace();
                result.setValid(false);
            }
        } else {
            System.err.println("Comment doesn't exist:" + commentFile.getAbsolutePath());
        }

        return result;
    }

    public static BlogComments loadFromJson(final String fileName) {
        final File commentFile = new File(fileName);
        return BlogComments.loadFromJson(commentFile);
    }

    private boolean valid = true;
    private Date    created;
    private String  parentId;
    private String  comment;
    private String  referer;
    private String  userAgent;
    private String  author;
    private String  remoteAddress;
    private String  eMail;
    private String  url;
    private boolean markdown;
    private String  UNID;
    private String  webSite;
    private String  gravatarURL;

    @Override
    public int compareTo(final BlogComments externalComment) {
        final SimpleDateFormat sdf = new SimpleDateFormat(BlogComments.COMPARE_DATE_FORMAT);
        final String ownDate = sdf.format(this.getCreated());
        final String externalDate = sdf.format(externalComment.getCreated());
        final String ownCompare = ownDate + String.valueOf(this.getAuthor());
        final String externalCompare = externalDate + String.valueOf(externalComment.getAuthor());
        return ownCompare.compareTo(externalCompare);
    }

    /**
     * @return the author
     */
    public String getAuthor() {
        return this.author;
    }

    /**
     * @return the comment
     */
    public String getComment() {
        return this.comment;
    }

    /**
     * @return the created Date
     */
    public Date getCreated() {
        return this.created;
    }

    /**
     * @return the created Date as String
     */
    public String getCreatedString() {
        final SimpleDateFormat sdf = new SimpleDateFormat(BlogComments.DISPLAY_DATE_FORMAT);
        return sdf.format(this.created);
    }

    /**
     * @return the eMail
     */
    public String geteMail() {
        return this.eMail;
    }

    public String getGravatarURL() {
        if (((this.gravatarURL == null) || this.gravatarURL.trim().equals("")) && (this.eMail != null)) {
            final String emailHash = DigestUtils.md5Hex(this.eMail.toLowerCase().trim());
            this.setGravatarURL(BlogComments.GRAVATAR_URL + emailHash + ".jpg?s=" + BlogComments.GRAVATAR_SIZE);
        }

        return this.gravatarURL;
    }

    /**
     * @return the parentId
     */
    public String getParentId() {
        return this.parentId;
    }

    /**
     * @return the referer
     */
    public String getReferer() {
        return this.referer;
    }

    /**
     * @return the remoteAddress
     */
    public String getRemoteAddress() {
        return this.remoteAddress;
    }

    /**
     * @return the uNID
     */
    public String getUNID() {
        return this.UNID;
    }

    /**
     * @return the url
     */
    public String getUrl() {
        return this.url;
    }

    /**
     * @return the userAgent
     */
    public String getUserAgent() {
        return this.userAgent;
    }

    /**
     * @return the webSite
     */
    public String getWebSite() {
        return this.webSite;
    }

    /**
     * @return the markdown
     */
    public boolean isMarkdown() {
        return this.markdown;
    }

    /**
     * @return the valid
     */
    public boolean isValid() {
        return this.valid;
    }

    /**
     * Save the object to a JSON file for reuse
     */
    public void saveDatatoJson(final OutputStream out) {
        final Gson gson = new GsonBuilder().setPrettyPrinting().create();
        final PrintWriter writer = new PrintWriter(out);
        gson.toJson(this, writer);
        writer.flush();
        writer.close();
    }

    /**
     * @param author
     *            the author to set
     */
    public void setAuthor(final String author) {
        this.author = author;
    }

    /**
     * @param comment
     *            the comment to set
     */
    public void setComment(final String comment) {
        this.comment = comment;
    }

    /**
     * @param created
     *            the created to set
     */
    public void setCreated(final Date created) {
        this.created = created;
    }

    /**
     * @param eMail
     *            the eMail to set
     */
    public void seteMail(final String eMail) {
        this.eMail = eMail;
    }

    /* Dummy function to populate the gravatar */
    public void setGravatar() {
        this.getGravatarURL();
    }

    public void setGravatarURL(final String gravatarURL) {
        this.gravatarURL = gravatarURL;
    }

    /**
     * @param markdown
     *            the markdown to set
     */
    public void setMarkdown(final boolean markdown) {
        this.markdown = markdown;
    }

    /**
     * @param parentId
     *            the parentId to set
     */
    public void setParentId(final String parentId) {
        this.parentId = parentId;
    }

    /**
     * @param referer
     *            the referer to set
     */
    public void setReferer(final String referer) {
        this.referer = referer;
    }

    /**
     * @param remoteAddress
     *            the remoteAddress to set
     */
    public void setRemoteAddress(final String remoteAddress) {
        this.remoteAddress = remoteAddress;
    }

    /**
     * @param uNID
     *            the uNID to set
     */
    public void setUNID(final String uNID) {
        this.UNID = uNID;
    }

    /**
     * @param url
     *            the url to set
     */
    public void setUrl(final String url) {
        this.url = url;
    }

    /**
     * @param userAgent
     *            the userAgent to set
     */
    public void setUserAgent(final String userAgent) {
        this.userAgent = userAgent;
    }

    /**
     * @param valid
     *            the valid to set
     */
    public void setValid(final boolean valid) {
        this.valid = valid;
    }

    /**
     * @param webSite
     *            the webSite to set
     */
    public void setWebSite(final String webSite) {
        this.webSite = webSite;
    }

    @Override
    public String toString() {
        final Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
        return gson.toJson(this);
    }

}

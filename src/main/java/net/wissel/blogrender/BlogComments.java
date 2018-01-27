package net.wissel.blogrender;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.codec.digest.DigestUtils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

@JsonIgnoreProperties
public class BlogComments implements Comparable<BlogComments> {

	private final static String GRAVATAR_URL = "//www.gravatar.com/avatar/";
	private final static String GRAVATAR_SIZE = "88"; // Pixels
	private final static String DISPLAY_DATE_FORMAT = "EEEE dd MMMM yyyy GG - HH:mm zzzz";
	private final static String COMPARE_DATE_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS";

	public static BlogComments loadFromJson(final String fileName) {
		BlogComments result = null;
		final File commentFile = new File(fileName);
		if (commentFile.exists() && commentFile.isFile()) {
			try {
				final InputStream in = new FileInputStream(commentFile);
				final Gson gson = new GsonBuilder().create();
				result = gson.fromJson(new InputStreamReader(in), BlogComments.class);
				in.close();
				if (result.isMarkdown()) {
					String markdownText = result.getComment();
					String htmlText = MarkdownConverter.markdown2Html(markdownText);
					result.setComment(htmlText);
				}
			} catch (final FileNotFoundException e) {
				e.printStackTrace();
			} catch (final IOException e) {
				e.printStackTrace();
			}
		} else {
			System.err.println("Comment doesn't exist:" + fileName);
		}
		return result;
	}

	private Date created;
	private String parentId;
	private String comment;
	private String referer;
	private String userAgent;
	private String author;
	private String remoteAddress;
	private String eMail;
	private String url;
	private boolean markdown;
	private String UNID;

	private String gravatarURL;

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
	 * @return the markdown
	 */
	public boolean isMarkdown() {
		return this.markdown;
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

}

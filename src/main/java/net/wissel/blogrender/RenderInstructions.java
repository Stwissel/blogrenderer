package net.wissel.blogrender;

import java.io.File;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Contains the list of Blogentries that get summarized can be Month, Year,
 * Category
 * 
 * @author stw
 */
public class RenderInstructions {
	public String                              TemplateName;
	public String                              outFileName;
	public String                              pageTitle;
	public String                              pageLink;
	public String                              key;
	public String                              type;
	public LinkItem                            previousItem;
	public LinkItem                            nextItem;
	public TreeSet<BlogEntry>                  members    = null;
	public TreeMap<String, RenderInstructions> categories = null;
	public boolean reverse = false;

    public void add(BlogEntry be) {
        if (this.members == null) {
            this.members = new TreeSet<BlogEntry>();
        }
        this.members.add(be);
    }

    /**
     * Adds a blog entry to a (sub)category
     * @param be - the blog entry
     * @param categoryName the Name of the category
     * @param categoryValue the actual value of the category
     */
    public void addToCategory(BlogEntry be, String categoryName, String categoryValue) {
        RenderInstructions ri;
        if (this.categories == null) {
            this.categories = new TreeMap<String, RenderInstructions>();
        }
        if (this.categories.containsKey(categoryValue)) {
            ri = this.categories.get(categoryValue);
        } else {
            ri = new RenderInstructions();
            ri.outFileName = this.outFileName;
            ri.TemplateName = this.TemplateName;
            ri.pageTitle = categoryName;
            ri.pageLink = categoryValue;
            ri.key = categoryValue;
            this.categories.put(categoryValue, ri);
        }
        ri.add(be);
    }

    /**
     * We check if there is a special template with the
     * keyname in it
     * 
     * @param theKey
     * @return a template name
     */
    public String getFinalTemplateName(String dirLocation, String theKey) {
        int dotPosition = this.TemplateName.lastIndexOf(".");
        StringBuilder b = new StringBuilder();

        b.append(this.TemplateName.substring(0, dotPosition));
        b.append("-");
        b.append(theKey);
        b.append(this.TemplateName.substring(dotPosition));
        String result = b.toString();
        File specialTemplate = new File(dirLocation + result);

        if (specialTemplate.exists()) {
            System.out.println(result);
        }

        return (specialTemplate.exists() ? result : this.TemplateName);

    }
}

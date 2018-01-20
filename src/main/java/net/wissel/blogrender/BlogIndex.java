package net.wissel.blogrender;

import java.util.Collection;

public class BlogIndex {

    LinkItem              previousItem;
    LinkItem              nextItem;
    Collection<LinkItem>  allCategories;
    Collection<LinkItem>  allDateCategories;
    BlogEntryCollection   topArticles;
    String                pageTitle;
    String                pageLink;
    Collection<BlogIndex> categorizedEntries;
    Boolean               isSeries;
}

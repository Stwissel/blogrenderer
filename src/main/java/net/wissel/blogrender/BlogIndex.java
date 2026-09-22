package net.wissel.blogrender;

import java.util.Collection;

/**
 * Scope object backing every aggregate page: the front page, the year and
 * month archives, the category pages, the "all entries" page and the series
 * overview. Fields are read directly by the Mustache templates.
 */
public class BlogIndex {

  LinkItem previousItem;
  LinkItem nextItem;
  Collection<LinkItem> allCategories;
  Collection<LinkItem> allDateCategories;
  BlogEntryCollection topArticles;
  BlogEntryCollection relevantArticles;
  String pageTitle;
  String pageLink;
  Collection<BlogIndex> categorizedEntries;

  /**
   * True only on the series overview page, so nav_main can mark its "Series"
   * item active. Left null (falsy to Mustache) on every other page.
   */
  Boolean isSeries;

  /**
   * Exposes the render configuration to Mustache, so templates need not
   * hardcode site constants such as the canonical host or the copyright.
   *
   * @return the render configuration singleton
   */
  public Config getConfig() {
    return Config.get();
  }
}

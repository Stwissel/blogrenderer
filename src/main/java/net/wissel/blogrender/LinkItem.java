package net.wissel.blogrender;

import java.util.ArrayList;
import java.util.List;

public class LinkItem implements Comparable<LinkItem> {

  final public String name;
  final public String place;
  final private String sorter;
  final private boolean inverseSort;
  public int count = 1;
  public boolean active = false;


  // Standard full blown link item with specific location
  public LinkItem(String linkItemName, String linkItemUrl, String sorter) {
    this.name = linkItemName;
    this.place = linkItemUrl;
    this.sorter = (sorter == null) ? this.cleanPlace(linkItemName) : sorter;
    this.inverseSort = false;
  }

  // Standard full blown link item with specific location
  public LinkItem(String linkItemName, String linkItemUrl, String sorter, boolean reverse) {
    this.name = linkItemName;
    this.place = linkItemUrl;
    this.sorter = (sorter == null) ? this.cleanPlace(linkItemName) : sorter;
    this.inverseSort = reverse;
  }

  // Version for the category list
  public LinkItem(String linkItemName, String linkItemBaseUrl) {
    this.name = linkItemName;
    this.place = linkItemBaseUrl + this.cleanPlace(linkItemName);
    this.sorter = this.cleanPlace(linkItemName);
    this.inverseSort = false;
  }

  // Allows to reverse order e.g. for series
  @Override
  public int compareTo(LinkItem o) {
    return (this.inverseSort) ? o.sorter.compareTo(this.sorter) : this.sorter.compareTo(o.sorter);
  }

  // ShortCut for simple creation
  public LinkItem(String linkItemName) {
    this.name = linkItemName;
    this.place = this.cleanPlace(linkItemName);
    this.sorter = this.place;
    this.inverseSort = false;
  }

  // A URL and sort friendly representation
  private String cleanPlace(String linkItemName) {
    return linkItemName.trim().toLowerCase().replaceAll("[^a-z0-9\n]+", "");
  }

  // Cleaning up the LinkItem mess in old entries
  public static List<LinkItem> cleanupLinkItems(final List<LinkItem> rawItems) {
    final List<LinkItem> result = new ArrayList<>();
    rawItems.forEach(it -> {
      final LinkItem oneResult = new LinkItem(it.name);
      result.add(oneResult);
    });
    return result;
  }
}

package net.wissel.blogrender;

import java.util.ArrayList;
import java.util.List;

/**
 * One year of the archive, with the months inside it.
 *
 * /why this exists rather than another flat LinkItem collection: the sidebar
 * shows 24 years, and listing every month underneath them flat would be 275
 * links in one column. A year that can be expanded to its own months needs the
 * nesting to be present in the scope object, and LinkItem has no children.
 *
 * @author stw
 */
public class DateArchive implements Comparable<DateArchive> {

  public final String name;
  public final String place;
  public int count = 0;
  public final List<LinkItem> months = new ArrayList<>();

  /**
   * @param name the year as it is displayed, e.g. "2014"
   * @param place the year's archive URL
   */
  public DateArchive(final String name, final String place) {
    this.name = name;
    this.place = place;
  }

  /** Newest year first, which is the order the sidebar lists them in. */
  @Override
  public int compareTo(final DateArchive o) {
    return o.name.compareTo(this.name);
  }
}

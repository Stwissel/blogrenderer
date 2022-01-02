package net.wissel.blogrender;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class BlogEntryCollection extends ArrayList<BlogEntry> {

  private static final long serialVersionUID = 2L;
  private final boolean reverseSortOrder;

  public BlogEntryCollection(final boolean reverseRun) {
    this.reverseSortOrder = reverseRun;
  }

  @SuppressWarnings("unused")
  private BlogEntryCollection() {
    // Hide blank constructor
    this.reverseSortOrder = false;
  }

  @Override
  public boolean add(final BlogEntry e) {
    final boolean result = super.add(e);
    Collections.sort(this);
    if (this.reverseSortOrder) {
      Collections.reverse(this);
    }
    return result;
  }

  @Override
  public boolean addAll(final Collection<? extends BlogEntry> c) {
    final boolean result = super.addAll(c);
    Collections.sort(this);
    if (this.reverseSortOrder) {
      Collections.reverse(this);
    }
    return result;
  }
}

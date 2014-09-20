/***
  Copyright (c) 2008-2009 CommonsWare, LLC
  Portions (c) 2009 Google, Inc.
  
  Licensed under the Apache License, Version 2.0 (the "License"); you may
  not use this file except in compliance with the License. You may obtain
  a copy of the License at
    http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
*/    

package com.commonsware.cwac.sacklist;

import java.util.ArrayList;
import java.util.List;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

/**
 * Adapter that simply returns row views from a list.
 *
 * If you supply a size, you must implement newView(), to
 * create a required view. The adapter will then cache these
 * views.
 *
 * If you supply a list of views in the constructor, that
 * list will be used directly. If any elements in the list
 * are null, then newView() will be called just for those
 * slots.
 *
 * Subclasses may also wish to override areAllItemsEnabled()
 * (default: false) and isEnabled() (default: false), if some
 * of their rows should be selectable.
 *
 * It is assumed each view is unique, and therefore will not
 * get recycled.
 *
 * Note that this adapter is not designed for long lists. It
 * is more for screens that should behave like a list. This
 * is particularly useful if you combine this with other
 * adapters (e.g., SectionedAdapter) that might have an
 * arbitrary number of rows, so it all appears seamless.
 */
public class SackOfViewsAdapter extends BaseAdapter {
  private List<View> views=null;

  /**
    * Constructor creating an empty list of views, but with
    * a specified count. Subclasses must override newView().
    */
  public SackOfViewsAdapter(int count) {
    super();
    
    views=new ArrayList<View>(count);
    
    for (int i=0;i<count;i++) {
      views.add(null);
    }
  }

  /**
    * Constructor wrapping a supplied list of views.
    * Subclasses must override newView() if any of the elements
    * in the list are null.
    */
  public SackOfViewsAdapter(List<View> views) {
    super();
    
    this.views=views;
  }

  /**
    * Get the data item associated with the specified
    * position in the data set.
    * @param position Position of the item whose data we want
    */
  @Override
  public Object getItem(int position) {
    return(views.get(position));
  }

  /**
    * How many items are in the data set represented by this
    * Adapter.
    */
  @Override
  public int getCount() {
    return(views.size());
  }

  /**
    * Returns the number of types of Views that will be
    * created by getView().
    */
  @Override
  public int getViewTypeCount() {
    return(getCount());
  }

  /**
    * Get the type of View that will be created by getView()
    * for the specified item.
    * @param position Position of the item whose data we want
    */
  @Override
  public int getItemViewType(int position) {
    return(position);
  }

  /**
    * Are all items in this ListAdapter enabled? If yes it
    * means all items are selectable and clickable.
    */
  @Override
  public boolean areAllItemsEnabled() {
    return(false);
  }

  /**
    * Returns true if the item at the specified position is
    * not a separator.
    * @param position Position of the item whose data we want
    */
  @Override
  public boolean isEnabled(int position) {
    return(false);
  }

  /**
    * Get a View that displays the data at the specified
    * position in the data set.
    * @param position Position of the item whose data we want
    * @param convertView View to recycle, if not null
    * @param parent ViewGroup containing the returned View
    */
  @Override
  public View getView(int position, View convertView,
                      ViewGroup parent) {
    View result=views.get(position);
    
    if (result==null) {
      result=newView(position, parent);
      views.set(position, result);
    }
    
    return(result);
  }

  /**
    * Get the row id associated with the specified position
    * in the list.
    * @param position Position of the item whose data we want
    */
  @Override
  public long getItemId(int position) {
    return(position);
  }
  
  public boolean hasView(View v) {
    return(views.contains(v));
  }
  
  /**
    * Create a new View to go into the list at the specified
    * position.
    * @param position Position of the item whose data we want
    * @param parent ViewGroup containing the returned View
    */
  protected View newView(int position, ViewGroup parent) {
    throw new RuntimeException("You must override newView()!");
  }
}
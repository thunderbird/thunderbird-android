package com.fsck.k9.helper;

import android.content.Context;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.fsck.k9.service.PollService;

import java.util.*;

/**
 * Author: dzan
 * Date: 21/06/11
 *
 * This is a generic ListAdapter enabling the view to have subsections. To display an element the toString() is used from
 * the supplied type. When adding items new groups will be created as needed. If an item is added belonging to a preexisting group
 * it will be merged. Group titles are displayed with simple TextViews, this can't be changed ( unless in the code itself ofc ).
 *
 * Usage: Declare, initialise then just use add(string:sectionname, T:item).
 *        The constructor takes the context, a resource_id to draw headers with ( textview! ) and one to draw the list items with.
 */

public class SectionListAdapter<T> extends BaseAdapter{

    // These represent the different list item types
    private final int VIEWTYPE_HEADER = 0;
    private final int VIEWTYPE_CONTENT = 1;

    // Model holding our data
    private List<String> headers = new ArrayList<String>();
    private List<Integer> headersPos = new ArrayList<Integer>();
    private Map<String, ArrayAdapter> sections = new LinkedHashMap<String, ArrayAdapter>(); // important: we need the constant order property

    // Android-view stuff
    private Context context;
    private int headerResourceId, normalResourceId;

    public SectionListAdapter(Context context, int headerResourceId, int normalResourceId){
        super();
        this.context = context;
        this.headerResourceId = headerResourceId;
        this.normalResourceId = normalResourceId;
    }

    public int getCount() {
        int total = headers.size();
        for( Adapter a : sections.values() ) total += a.getCount();
        return total;
    }

    public void add(String section, T item){
        if( !headers.contains(section) ){
            headersPos.add(getCount());   // no +1 !  order important here
            headers.add(section);
            ArrayAdapter<T> tmp = new ArrayAdapter<T>(context,normalResourceId);
            tmp.add(item);
            sections.put(section, tmp);
        }else{
            sections.get(section).add(item);
            for(int i=headers.indexOf(section)+1; i<headersPos.size(); ++i)
                headersPos.set(i, headersPos.get(i)+1);
        }

        notifyDataSetChanged();
    }

    private boolean isHeader(int pos){ return headersPos.contains(pos); }
    private int numberOfSections(){ return headersPos.size(); }

    public Object getItem(int pos) {
        if( isHeader(pos) ){
            return headers.get(headersPos.indexOf(pos));
        }else{
            for(int i=0; i<headersPos.size(); ++i ){
                if( i != numberOfSections()-1 && pos >= headersPos.get(i+1) ) continue;
                return sections.get(headers.get(i)).getItem(pos - headersPos.get(i)-1);
            }
            return null; // should never happen, means pos > element count
        }
    }

    public long getItemId(int pos) { return pos; }

    public View getView(int pos, View view, ViewGroup viewGroup) {
        if( isHeader(pos) ){
            TextView result = (TextView) view;

            if (view == null){
                //result = (TextView) LayoutInflater.from(context).inflate(headerResourceId, viewGroup);
                LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                result = (TextView) inflater.inflate(headerResourceId, viewGroup, false);
            }

            String tmp = headers.get(headersPos.indexOf(pos));
            result.setText(tmp);
            return (result);
        }else{
            for(int i=0; i<numberOfSections(); ++i )  // do for each section
            {
                if( i != numberOfSections()-1 && pos >= headersPos.get(i+1) ) continue;
                return sections.get(headers.get(i)).getView(pos - headersPos.get(i)-1, view, viewGroup);
            }
            return null; // should never happen, means pos > element count
        }
    }

    @Override
    public int getItemViewType(int pos){
        if( headersPos.contains(pos) ) return VIEWTYPE_HEADER;
        else return VIEWTYPE_CONTENT;
    }

    @Override
    public int getViewTypeCount(){ return 2; }

    @Override
    public boolean areAllItemsEnabled() { return false; }

    @Override
    public boolean isEnabled(int pos) { return !isHeader(pos); }

    @Override
    public boolean isEmpty(){ return headers.isEmpty() && sections.isEmpty(); }
}

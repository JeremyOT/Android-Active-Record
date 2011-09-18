//   Copyright 2011 Jeremy Olmsted-Thompson
//
//   Licensed under the Apache License, Version 2.0 (the "License");
//   you may not use this file except in compliance with the License.
//   You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//   Unless required by applicable law or agreed to in writing, software
//   distributed under the License is distributed on an "AS IS" BASIS,
//   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//   See the License for the specific language governing permissions and
//   limitations under the License.
//   
package org.androidactiverecord;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

/**
 * A helpful class for displaying AREntities in a list.
 * 
 * @author Jeremy Olmsted-Thompson
 * 
 * @param <T>
 *            The type of the entities to display.
 */
public class ARListAdapter<T extends AREntity> extends ArrayAdapter<Long> {

    private final Class<T> mType;
    private String mColumn = null;
    private String mValue = null;
    private String mWhereClause = null;
    private String[] mWhereArgs = null;
    private String mOrderBy = null;
    private ARDatabase mDatabase;
    private Map<String, Integer> mTextViewMap = null;
    int mViewResourceID;

    /**
     * Create a new adapter for the specified type. Use this when extending the adapter, the loaded
     * views will not be filled so getView() will need to be overridden.
     * 
     * @param type
     *            Any AREntity class.
     * @param database
     *            The ARDatabase to load entities from.
     * @param context
     *            The application context.
     * @param viewResourceID
     *            The resource ID of a text view to use for display.
     */
    protected ARListAdapter(Class<T> type, ARDatabase database, Context context, int viewResourceID) {
        super(context, viewResourceID);
        this.mType = type;
        this.mDatabase = database;
        this.mViewResourceID = viewResourceID;
        refreshAdapter();
    }

    /**
     * Create a new adapter for the specified type.
     * 
     * @param type
     *            Any AREntity class.
     * @param context
     *            The application context.
     * @param viewResourceID
     *            The resource ID of a view to use for display.
     * @param textViewIDColumnMapping
     *            A String-Integer map of column names to field IDs to use for display.
     */
    public ARListAdapter(Class<T> type, ARDatabase database, Context context, int viewResourceID,
            Map<String, Integer> textViewIDColumnMapping) {
        super(context, viewResourceID);
        mTextViewMap = textViewIDColumnMapping;
        this.mType = type;
        this.mDatabase = database;
        this.mViewResourceID = viewResourceID;
        refreshAdapter();
    }

    /**
     * Create a new adapter for the specified type.
     * 
     * @param type
     *            Any AREntity class.
     * @param context
     *            The application context.
     * @param resourceID
     *            The resource ID of a view to use for display.
     * @param textViewResourceID
     *            The ID of the text view to use for display.
     */
    public ARListAdapter(Class<T> type, ARDatabase database, Context context, int viewResourceID,
            int textViewResourceID) {
        super(context, viewResourceID, textViewResourceID);
        this.mType = type;
        this.mDatabase = database;
        this.mViewResourceID = viewResourceID;
        refreshAdapter();
    }

    /**
     * Set a column restriction for entities to display (example: hidden,false)
     * 
     * @param column
     *            The column to filter by.
     * @param value
     *            The value required for display.
     */
    public void setColumnRestriction(String column, Object value) {
        mWhereClause = null;
        mWhereArgs = null;
        this.mColumn = column;
        this.mValue = value != null ? String.valueOf(value) : null;
        refreshAdapter();
    }

    /**
     * Set the column to order by, or null for the natural order.
     * 
     * @param orderBy
     *            The column(s) to order by.
     */
    public void setOrderBy(String orderBy) {
        this.mOrderBy = orderBy;
        refreshAdapter();
    }

    /**
     * Set a restriction for entities to display.
     * 
     * @param whereClause
     *            The condition to match (don't include "where").
     * @param whereArgs
     *            The values to replace "?" with.
     */
    public void setRestriction(String whereClause, String[] whereArgs) {
        mColumn = null;
        mValue = null;
        this.mWhereClause = whereClause;
        this.mWhereArgs = whereArgs;
        refreshAdapter();
    }

    public void refreshAdapter() {
        clear();
        if (mColumn != null && mValue != null) {
            for (Long item : AREntity.findIds(mType, false, String.format("%s = ?", mColumn),
                    new String[] { mValue }, mOrderBy, 0, mDatabase))
                add(item);
        } else if (mWhereClause != null) {
            for (Long item : AREntity.findIds(mType, false, mWhereClause, mWhereArgs, mOrderBy, 0,
                    mDatabase))
                add(item);
        } else {
            for (Long item : AREntity.findIds(mType, false, null, null, mOrderBy, 0, mDatabase))
                add(item);
        }
        notifyDataSetChanged();
    }

    /**
     * Get the item at the specified position.
     * 
     * @param position
     *            The position to grab.
     * @return The AREntity being displayed at the specified position.
     */
    public T getARItem(int position) {
        return AREntity.findById(mType, getItem(position), mDatabase);
    }

    private void populateView(View view, int position) {
        T are = getARItem(position);
        List<Field> fields = are.getColumnFields();
        for (Field field : fields) {
            if (mTextViewMap.containsKey(field.getName())) {
                try {
                    String text = String.valueOf(field.get(are));
                    if (text == null || text.equals("None"))
                        text = "";
                    ((TextView) view.findViewById(mTextViewMap.get(field.getName()).intValue()))
                            .setText(text);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View toRet = convertView != null ? convertView : View.inflate(getContext(),
                mViewResourceID, null);
        if (mTextViewMap == null)
            return toRet;
        populateView(toRet, position);
        return toRet;
    }
}

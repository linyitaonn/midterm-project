package com.example.android.notepad;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class CategoryExpandableListAdapter extends BaseExpandableListAdapter {

    private Context mContext;
    private List<String> mGroupData; // Categories
    private Map<String, List<NotesListFragment.NoteHolder>> mChildData; // Notes per category

    public CategoryExpandableListAdapter(Context context, List<String> groupData, Map<String, List<NotesListFragment.NoteHolder>> childData) {
        mContext = context;
        mGroupData = groupData;
        mChildData = childData;
    }

    @Override
    public int getGroupCount() {
        return mGroupData.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return mChildData.get(mGroupData.get(groupPosition)).size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return mGroupData.get(groupPosition);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return mChildData.get(mGroupData.get(groupPosition)).get(childPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        NotesListFragment.NoteHolder note = (NotesListFragment.NoteHolder) getChild(groupPosition, childPosition);
        return note.id;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        String headerTitle = (String) getGroup(groupPosition);
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(android.R.layout.simple_expandable_list_item_1, null);
        }

        TextView lblListHeader = (TextView) convertView.findViewById(android.R.id.text1);
        lblListHeader.setText(headerTitle);
        lblListHeader.setTextSize(18);
        lblListHeader.setTypeface(null, Typeface.BOLD);
        lblListHeader.setPadding(40, 30, 0, 30);

        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.noteslist_item, null);
        }

        TextView txtListChild = (TextView) convertView.findViewById(R.id.text1);
        TextView txtListTimestamp = (TextView) convertView.findViewById(R.id.text2);

        NotesListFragment.NoteHolder note = (NotesListFragment.NoteHolder) getChild(groupPosition, childPosition);

        txtListChild.setText(note.title);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateString = dateFormat.format(new Date(note.modificationDate));
        txtListTimestamp.setText(dateString);

        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }
}

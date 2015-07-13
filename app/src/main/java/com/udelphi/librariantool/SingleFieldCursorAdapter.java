package com.udelphi.librariantool;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

/**
 * Created by ODiomin on 25.06.2015.
 */

// CursorAdapter for controls with single field
public class SingleFieldCursorAdapter  extends CursorAdapter
{
    private int m_TextViewID;
    private String m_FieldName;

    public SingleFieldCursorAdapter(Context context, Cursor c, int singleTextView_ID, String fieldName)
    {
        super(context, c, 0);
        m_TextViewID = singleTextView_ID;
        m_FieldName = fieldName.toUpperCase();
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent)
    {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.record_singletext, parent , false);
        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor)
    {
        TextView text = (TextView) view.findViewById(m_TextViewID);
        text.setText(cursor.getString(cursor.getColumnIndexOrThrow(m_FieldName)));
    }
}

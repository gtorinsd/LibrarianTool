package com.udelphi.librariantool;

import android.app.Activity;
import android.content.ContentValues;
import android.database.Cursor;
import android.os.Bundle;
import android.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;

import DBTools.DBHelper;
import Tools.MessageBox;

// Fragment for edit of records with 2 fields: ID and Name
public class FragmentCatalogEdit extends Fragment
{
    // region Work with activity by event & interface
    public interface catalogFragmentEventListener
    {
        public void catalogDataChangedEvent(String s, int SpinnerID);
        public void startCatalogFragment(int controlID);
        public void endCatalogFragment(int controlID);
    }
    catalogFragmentEventListener m_DataChangedEventListener;

    @Override
    public void onAttach(Activity activity)
    {
        super.onAttach(activity);
        m_DataChangedEventListener = (catalogFragmentEventListener) activity;
    }
    //endregion:

    private View m_View;
    private String m_LogTag = FragmentCatalogEdit.class.getName();
    // table name
    private String m_TableName;
    // Current record_id
    private String m_RecordID;
    // Text field name
    private String m_TextFieldName;
    private DBHelper m_DBAdapter;
    private Cursor m_Cursor;
    private ToolApplication m_App;
    // Current spinner_id
    private int m_SpinnerID;

    //region Visual controls in the fragment
    private ImageButton m_BtnAdd;
    private ImageButton m_BtnModi;
    private ImageButton m_BtnDel;
    private ImageButton m_BtnCancel;
    private ImageButton m_BtnUndo;
    private EditText m_EditText;
    //endregion

    public static FragmentCatalogEdit GetInstance(String tableName, String id, String textFieldName, int spinnerID)
    {
        FragmentCatalogEdit fragment = new FragmentCatalogEdit();
        Bundle bundle = new Bundle();
        bundle.putString("TableName", tableName);
        bundle.putString("RecordId", id);
        bundle.putString("TextFieldName", textFieldName);
        bundle.putInt("SpinnerID", spinnerID);
        fragment.setArguments(bundle);

        return fragment;
    }

    @Override
    public void onCreate(Bundle bundle)
    {
        super.onCreate(bundle);
        if (getArguments() != null)
        {
            m_TableName = getArguments().getString("TableName");
            m_RecordID = getArguments().getString("RecordId");
            m_TextFieldName = getArguments().getString("TextFieldName");
            m_SpinnerID = getArguments().getInt("SpinnerID");
        }
        m_App = (ToolApplication) getActivity().getApplication();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
        m_View = inflater.inflate(R.layout.fragment_catalog_edit, container, false);
        GetVisualControls();
        SetOnEditTextListener();

        //region Set button listeners
        SetOnAddButtonClick();
        SetOnModiButtonClick();
        SetOnDelButtonClick();
        SetOnCancelButtonClick();
        SetOnUndoButtonClick();
        //endregion

        GetData();
        FillControls();
        m_DataChangedEventListener.startCatalogFragment(m_SpinnerID);

        return m_View;
    }

    private void DisableFragmentButtons()
    {
        m_BtnAdd.setEnabled(false);
        m_BtnModi.setEnabled(false);
        m_BtnUndo.setEnabled(false);
    }

    // Get visual controls from the view
    private void GetVisualControls()
    {
        m_BtnAdd = (ImageButton) m_View.findViewById(R.id.FragmentImageButtonCatalogAdd);
        m_BtnModi = (ImageButton) m_View.findViewById(R.id.FragmentImageButtonCatalogModi);
        m_BtnDel = (ImageButton) m_View.findViewById(R.id.FragmentImageButtonCatalogDel);
        m_BtnCancel = (ImageButton) m_View.findViewById(R.id.FragmentImageButtonCatalogCancel);
        m_BtnUndo = (ImageButton) m_View.findViewById(R.id.FragmentImageButtonCatalogUndo);
        m_EditText = (EditText) m_View.findViewById(R.id.FragmentEditTextCatalog);
    }

    // Execute query
    private void GetData()
    {
        m_DBAdapter = new DBHelper(m_App.getApplicationContext(), m_App.DatabaseName, null, m_App.DatabaseVerion);
        String sql = String.format("SELECT rowid as _id, Name FROM %s WHERE _id = %s", m_TableName, m_RecordID);
        Log.d(m_LogTag, sql);
        m_Cursor = m_DBAdapter.SelectSQL(sql);
        m_Cursor.moveToFirst();
    }

    //  Fill visual controls by query results
    private void FillControls()
    {
        if ((m_Cursor != null) && (m_Cursor.getCount() > 0))
        {
            String s = m_Cursor.getString(m_Cursor.getColumnIndexOrThrow(m_TextFieldName));
            m_EditText.setText(s);
            // Select text in the edit
            m_EditText.setSelectAllOnFocus(true);
            m_EditText.clearFocus();
            m_EditText.requestFocus();

            DisableFragmentButtons();
        }
    }

    // Send message to the parent activity: refresh data
    private void RefreshActivityData()
    {
        m_DataChangedEventListener.catalogDataChangedEvent(m_EditText.getText().toString().trim(), m_SpinnerID);
    }

    // Get error messages if field "Name" is empty
    private String GetFieldIsEmptyErrorMsg()
    {
        String s;
        switch (m_SpinnerID)
        {
            case (R.id.ModiBookSpinnerAuthor):
            {
                s = getString(R.string.FragmentModiCatalogBookAuthorIsEmptyMsg);
                break;
            }

            case (R.id.ModiBookSpinnerGenre):
            {
                s = getString(R.string.FragmentModiCatalogBookGenreIsEmptyMsg);
                break;
            }

            default:
            {
                s = getString(R.string.ModiBook_BookUnknownFieldIsEmptyMsg);
                break;
            }
        }
        return s;
    }

    // Checks the record before insert/update
    private boolean CheckFieldsBeforeSave()
    {
        if (m_EditText.getText().toString().trim().isEmpty())
        {
            String s = GetFieldIsEmptyErrorMsg();
            MessageBox.Show(m_App.getApplicationContext(), s);
            return false;
        }
        return true;
    }

    // Append new record
    private boolean AddNewRecord()
    {
        if (!CheckFieldsBeforeSave())
        {
            return false;
        }

        ContentValues contentValues = new ContentValues();
        contentValues.put("Name", m_EditText.getText().toString().trim());
        boolean b = m_DBAdapter.InsertValuesByContent(m_TableName, contentValues);
        try
        {
            if (!b)
            {
                String error = m_DBAdapter.getErrorMsg();
                Log.e(m_LogTag, error);
            }
        }
        finally
        {
            RefreshActivityData();
        }
        return b;
    }

    // Update the current record in the table
    private boolean UpdateRecord()
    {
        if (!CheckFieldsBeforeSave())
        {
            return false;
        }

        ContentValues contentValues = new ContentValues();
        contentValues.put("Name", m_EditText.getText().toString().trim());
        boolean b = m_DBAdapter.UpdateValuesByContent(m_TableName, contentValues, "_ID = ?", new String[]{m_RecordID});
        try
        {
            if (!b)
            {
                String error = m_DBAdapter.getErrorMsg();
                Log.e(m_LogTag, error);
            }
        }
        finally
        {
            RefreshActivityData();
        }
        return b;
    }

    // Delete current record
    private boolean DeleteRecord()
    {
        boolean b = m_DBAdapter.DeleteRecord(m_TableName, String.format("_ID = %s", m_RecordID));
        if (b)
        {
            m_EditText.setText(null);
            RefreshActivityData();
        }
        return b;
    }

    //region ButtonsListeners
    private void SetOnAddButtonClick()
    {
        // Add new record
        m_BtnAdd.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (AddNewRecord())
                {
                    Close();
                }
            }
        });
    }

    private void SetOnModiButtonClick()
    {
        m_BtnModi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                if (UpdateRecord())
                {
                    Close();
                }
            }
        });
    }

    private void SetOnDelButtonClick()
    {
        m_BtnDel.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (DeleteRecord())
                {
                    Close();
                }
            }
        });
    }

    private void SetOnCancelButtonClick()
    {
        // Just close fragment
        m_BtnCancel.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Close();
            }
        });
    }

    private void SetOnUndoButtonClick()
    {
        m_BtnUndo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                GetData();
                FillControls();
            }
        });
    }

    //endregion

    private void SetOnEditTextListener()
    {
        m_EditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

            @Override
            public void afterTextChanged(Editable editable)
            {
                // Enable or disable buttons
                boolean b = !(editable.toString().trim().isEmpty());
                m_BtnAdd.setEnabled(b);
                m_BtnModi.setEnabled(b);
                m_BtnDel.setEnabled(b);
                m_BtnUndo.setEnabled(b);
            }
        });
    }

    // Close fragment
    private void Close()
    {
        // Close DatabaseAdapter
        if (m_DBAdapter != null)
        {
            m_DBAdapter.close();
        }
        // Remove fragment from the activity
        getActivity().getFragmentManager().beginTransaction().remove(this).commit();
        m_DataChangedEventListener.endCatalogFragment(m_SpinnerID);
    }
}

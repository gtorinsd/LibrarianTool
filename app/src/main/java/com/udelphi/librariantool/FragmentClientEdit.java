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

public class FragmentClientEdit extends Fragment
{
    // region Work with activity by event & interface
    public interface clientFragmentEventListener
    {
        public void clientDataChangedEvent(String newValue);
        public void startClientFragment();
        public void endClientFragment();
    }
    clientFragmentEventListener m_DataChangedEventListener;

    @Override
    public void onAttach(Activity activity)
    {
        super.onAttach(activity);
        m_DataChangedEventListener = (clientFragmentEventListener) activity;
    }
    //endregion:

    private View m_View;
    private String m_LogTag = FragmentCatalogEdit.class.getName();
    private String m_TableName;
    private String m_RecordID;
    private DBHelper m_DBAdapter;
    private Cursor m_Cursor;
    private ToolApplication m_App;

    //region Visual control
    private ImageButton m_BtnAdd;
    private ImageButton m_BtnModi;
    private ImageButton m_BtnDel;
    private ImageButton m_BtnCancel;
    private ImageButton m_BtnRefresh;
    private EditText m_EditTextFirstName;
    private EditText m_EditTextLastName;
    private EditText m_EditTextSurname;
    private EditText m_EditTextAddress;
    private EditText m_EditTextPhone;
    private EditText m_EditTextComments;
    //endregion

    public static FragmentClientEdit GetInstance(String id)
    {
        FragmentClientEdit fragment = new FragmentClientEdit();
        Bundle bundle = new Bundle();
        bundle.putString("RecordId", id);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        if (getArguments() != null)
        {
            m_RecordID = getArguments().getString("RecordId");
        }
        m_App = (ToolApplication) getActivity().getApplication();
        m_TableName = m_App.tblClients;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
        m_View = inflater.inflate(R.layout.fragment_client_edit, container, false);
        GetVisualControls();
        SetOnEditTextListener();

        //region Set button listeners
        SetOnAddButtonClick();
        SetOnModiButtonClick();
        SetOnDelButtonClick();
        SetOnCancelButtonClick();
        SetOnRefreshButtonClick();
        //endregion

        GetData();
        FillControls();
        m_DataChangedEventListener.startClientFragment();

        m_EditTextFirstName.setFocusable(true);

        return m_View;
    }

    // Get visual controls from the view
    private void GetVisualControls()
    {
        m_EditTextFirstName = (EditText) m_View.findViewById(R.id.FragmentClientEditTextFirstName);
        m_EditTextLastName = (EditText) m_View.findViewById(R.id.FragmentClientEditTextLastName);
        m_EditTextSurname = (EditText) m_View.findViewById(R.id.FragmentClientEditTextSurname);
        m_EditTextAddress = (EditText) m_View.findViewById(R.id.FragmentClientEditTextAddress);
        m_EditTextPhone = (EditText) m_View.findViewById(R.id.FragmentClientEditTextPhone);
        m_EditTextComments = (EditText) m_View.findViewById(R.id.FragmentClientEditTextComment);

        m_BtnAdd = (ImageButton) m_View.findViewById(R.id.FragmentImageButtonClientAdd);
        m_BtnModi = (ImageButton) m_View.findViewById(R.id.FragmentImageButtonClientModi);
        m_BtnDel = (ImageButton) m_View.findViewById(R.id.FragmentImageButtonClientDel);
        m_BtnCancel = (ImageButton) m_View.findViewById(R.id.FragmentImageButtonClientCancel);
        m_BtnRefresh = (ImageButton) m_View.findViewById(R.id.FragmentImageButtonClientRefresh);
    }

    // Execute query
    private void GetData()
    {
        m_DBAdapter = new DBHelper(m_App.getApplicationContext(), m_App.DatabaseName, null, m_App.DatabaseVerion);
        String sql = String.format("SELECT rowid as _id, FirstName, LastName, Surname, Address, Phone, Comments FROM %s WHERE _id = %s", m_TableName, m_RecordID);
        Log.d(m_LogTag, sql);
        m_Cursor = m_DBAdapter.SelectSQL(sql);
        m_Cursor.moveToFirst();
    }

    //  Fill visual controls by query results
    private void FillControls()
    {
        if ((m_Cursor != null) && (m_Cursor.getCount() > 0))
        {
            String s = m_Cursor.getString(m_Cursor.getColumnIndexOrThrow("FIRSTNAME"));
            m_EditTextFirstName.setText(s);
            s = m_Cursor.getString(m_Cursor.getColumnIndexOrThrow("LASTNAME"));
            m_EditTextLastName.setText(s);
            s = m_Cursor.getString(m_Cursor.getColumnIndexOrThrow("SURNAME"));
            m_EditTextSurname.setText(s);
            s = m_Cursor.getString(m_Cursor.getColumnIndexOrThrow("ADDRESS"));
            m_EditTextAddress.setText(s);
            s = m_Cursor.getString(m_Cursor.getColumnIndexOrThrow("PHONE"));
            m_EditTextPhone.setText(s);
            s = m_Cursor.getString(m_Cursor.getColumnIndexOrThrow("COMMENTS"));
            m_EditTextComments.setText(s);

            // Select text in the edit
            m_EditTextFirstName.setSelectAllOnFocus(true);
            m_EditTextFirstName.clearFocus();
            m_EditTextFirstName.requestFocus();
        }
    }

    // Checks the record before insert/update
    private boolean CheckFieldsBeforeSave()
    {
        if (m_EditTextFirstName.getText().toString().isEmpty())
        {
            String s = getString(R.string.FragmentModiClientFieldFirstNameIsEmptyMsg);
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
        contentValues.put("FirstName", m_EditTextFirstName.getText().toString().trim());
        contentValues.put("LastName", m_EditTextLastName.getText().toString().trim());
        contentValues.put("Surname", m_EditTextSurname.getText().toString().trim());
        contentValues.put("Address", m_EditTextAddress.getText().toString().trim());
        contentValues.put("Phone", m_EditTextPhone.getText().toString().trim());
        contentValues.put("Comments", m_EditTextComments.getText().toString().trim());
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
        contentValues.put("FirstName", m_EditTextFirstName.getText().toString().trim());
        contentValues.put("LastName", m_EditTextLastName.getText().toString().trim());
        contentValues.put("Surname", m_EditTextSurname.getText().toString().trim());
        contentValues.put("Address", m_EditTextAddress.getText().toString().trim());
        contentValues.put("Phone", m_EditTextPhone.getText().toString().trim());
        contentValues.put("Comments", m_EditTextComments.getText().toString().trim());
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
            // Delete from tblTurnOver
            String s = "DELETE FROM %s WHERE Client_ID = %s";
            s = String.format(s, m_App.tblLibraryTurnover, m_RecordID);
            b = m_DBAdapter.ExecQueryNoResults(s);

            m_EditTextFirstName.setText(null);
            m_EditTextLastName.setText(null);
            m_EditTextSurname.setText(null);
            m_EditTextAddress.setText(null);
            m_EditTextPhone.setText(null);
            m_EditTextComments.setText(null);
            RefreshActivityData();
        }
        return b;
    }

    // Send message to the parent activity: refresh data
    private void RefreshActivityData()
    {
        String s = String.format("%s %s %s",
                m_EditTextFirstName.getText().toString().trim(),
                m_EditTextLastName.getText().toString().trim(),
                m_EditTextSurname.getText().toString().trim()
                );

        m_DataChangedEventListener.clientDataChangedEvent(s);
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
        m_BtnDel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (DeleteRecord()) {
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

    private void SetOnRefreshButtonClick()
    {
        m_BtnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GetData();
                FillControls();
            }
        });
    }

    //endregion

    private void SetOnEditTextListener()
    {
        m_EditTextFirstName.addTextChangedListener(new TextWatcher() {
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
        m_DataChangedEventListener.endClientFragment();
    }
}

package com.udelphi.trainingcenter.librariantool;

import android.app.Activity;
import android.os.Bundle;
import android.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;

import com.udelphi.trainingcenter.librariantool.R;

import com.udelphi.trainingcenter.librariantool.Tools.MessageBox;
import com.udelphi.trainingcenter.librariantool.Tools.RecordOperationTypeEnum;

// Fragment for edit of records with 2 fields: ID and Name
public class FragmentCatalogEdit extends Fragment
{
    // region Work with activity by event & interface
    public interface catalogFragmentEventListener
    {
        void catalogDataChangedEvent(String s, int SpinnerID, RecordOperationTypeEnum type);
        void startCatalogFragment(int controlID);
        void endCatalogFragment(int controlID);
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
    // Text field name
    private String m_TextValue;
    private ToolApplication m_App;
    // Current spinner_id
    private int m_SpinnerID;
    private RecordOperationTypeEnum m_RecordOperationType;

    //region Visual controls in the fragment
    private ImageButton m_BtnOK;
    private ImageButton m_BtnCancel;
    private EditText m_EditText;
    //endregion

    public static FragmentCatalogEdit GetInstance(String textValue, int spinnerID, RecordOperationTypeEnum type)
    {
        FragmentCatalogEdit fragment = new FragmentCatalogEdit();
        Bundle bundle = new Bundle();
        bundle.putString("TextValue", textValue);
        bundle.putInt("SpinnerID", spinnerID);
        bundle.putSerializable("RecordOperation", type);
        fragment.setArguments(bundle);

        return fragment;
    }

    @Override
    public void onCreate(Bundle bundle)
    {
        super.onCreate(bundle);
        if (getArguments() != null)
        {
            m_TextValue = getArguments().getString("TextValue");
            m_SpinnerID = getArguments().getInt("SpinnerID");
            m_RecordOperationType = (RecordOperationTypeEnum) getArguments().getSerializable("RecordOperation");
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
        SetOnOkButtonClick();
        SetOnCancelButtonClick();
        SetOnEditLostFocus();
        FillControls();
        ShowHideButtons(true);

        m_DataChangedEventListener.startCatalogFragment(m_SpinnerID);

        return m_View;
    }

    // Get visual controls from the view
    private void GetVisualControls()
    {
        m_BtnOK = (ImageButton) m_View.findViewById(R.id.FragmentImageButtonCatalogOk);
        m_BtnCancel = (ImageButton) m_View.findViewById(R.id.FragmentImageButtonCatalogCancel);
        m_EditText = (EditText) m_View.findViewById(R.id.FragmentEditTextCatalog);
    }

    //  Fill visual controls by query results
    private void FillControls()
    {
        m_EditText.setText(m_TextValue);
        // Select text in the edit
        m_EditText.setSelectAllOnFocus(true);
        m_EditText.clearFocus();
        m_EditText.requestFocus();
    }

    // Send message to the parent activity: refresh data
    private void RefreshActivityData()
    {
        m_DataChangedEventListener.catalogDataChangedEvent(m_EditText.getText().toString().trim(), m_SpinnerID, m_RecordOperationType);
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

    private void SetOnOkButtonClick()
    {
        // Add new record
        m_BtnOK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (CheckFieldsBeforeSave()) {
                    RefreshActivityData();
                    Close();
                }
            }
        });
    }

    private void SetOnCancelButtonClick()
    {
        // Add new record
        m_BtnCancel.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                    Close();
            }
        });
    }


    private void ShowHideButtons(boolean b)
    {
        if (b)
        {
            m_BtnOK.setVisibility(View.GONE);
            m_BtnCancel.setVisibility(View.VISIBLE);
        }
        else
        {
            m_BtnOK.setVisibility(View.VISIBLE);
            m_BtnCancel.setVisibility(View.GONE);
        }
    }

    private void SetOnEditTextListener()
    {
        m_EditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable)
            {
                // Enable or disable buttons
                ShowHideButtons((editable.toString().trim().isEmpty()));

            }
        });
    }

    private void SetOnEditLostFocus()
    {
        m_EditText.setOnFocusChangeListener(new View.OnFocusChangeListener()
        {
            @Override
            public void onFocusChange(View v, boolean hasFocus)
            {
                if (!hasFocus)
                {
                    // Just close the fragment
                    Close();
                }
            }
        });
    }

    // Close fragment
    private void Close()
    {
        // Remove fragment from the activity
        Activity activity = getActivity();
        if (!activity.isFinishing())
        {
            activity.getFragmentManager().beginTransaction().remove(this).commit();
        }
        m_DataChangedEventListener.endCatalogFragment(m_SpinnerID);
    }
}

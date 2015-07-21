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
import android.widget.LinearLayout;
import com.udelphi.trainingcenter.librariantool.R;
import com.udelphi.trainingcenter.librariantool.Tools.ClientInfoStruct;
import com.udelphi.trainingcenter.librariantool.Tools.MessageBox;
import com.udelphi.trainingcenter.librariantool.Tools.RecordOperationTypeEnum;

public class FragmentClientEdit extends Fragment
{
    // region Work with activity by event & interface
    public interface clientFragmentEventListener
    {
        void clientDataChangedEvent(ClientInfoStruct struct, RecordOperationTypeEnum type);
        void startClientFragment();
        void endClientFragment();
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
    private ToolApplication m_App;

    //region Visual control
    private ImageButton m_BtnOK;
    private ImageButton m_BtnCancel;
    private EditText m_EditTextFirstName;
    private EditText m_EditTextLastName;
    private EditText m_EditTextSurname;
    private EditText m_EditTextAddress;
    private EditText m_EditTextPhone;
    private EditText m_EditTextComments;
    //endregion

    private RecordOperationTypeEnum m_RecordOperationType;

    public static FragmentClientEdit GetInstance(ClientInfoStruct struct, RecordOperationTypeEnum type)
    {
        FragmentClientEdit fragment = new FragmentClientEdit();
        Bundle bundle = new Bundle();

        bundle.putString("FirstName", struct.FirstName);
        bundle.putString("LastName", struct.LastName);
        bundle.putString("SureName", struct.SureName);
        bundle.putString("Address", struct.Address);
        bundle.putString("Phone", struct.Phone);
        bundle.putString("Comments", struct.Comments);
        bundle.putSerializable("RecordOperation", type);

        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        m_App = (ToolApplication) getActivity().getApplication();
        m_RecordOperationType = (RecordOperationTypeEnum) getArguments().getSerializable("RecordOperation");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
        m_View = inflater.inflate(R.layout.fragment_client_edit, container, false);
        GetVisualControls();
        SetOnEditTextChangeListeners();
        SetOnOkButtonClick();
        SetOnCancelButtonClick();
        FillControls();
        ShowHideButtons(true);

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

        m_BtnOK = (ImageButton) m_View.findViewById(R.id.FragmentImageButtonClientOK);
        m_BtnCancel = (ImageButton) m_View.findViewById(R.id.FragmentImageButtonClientCancel);
    }

    //  Fill visual controls by query results
    private void FillControls()
    {
        if (getArguments() != null)
        {
            m_EditTextFirstName.setText(getArguments().getString("FirstName"));
            m_EditTextLastName.setText(getArguments().getString("LastName"));
            m_EditTextSurname.setText(getArguments().getString("SureName"));
            m_EditTextAddress.setText(getArguments().getString("Address"));
            m_EditTextPhone.setText(getArguments().getString("Phone"));
            m_EditTextComments.setText(getArguments().getString("Comments"));
        }

        // Select text in the edit
        m_EditTextFirstName.setSelectAllOnFocus(true);
        m_EditTextFirstName.clearFocus();
        m_EditTextFirstName.requestFocus();
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

    private ClientInfoStruct GetClientInfo()
    {
        ClientInfoStruct struct = new ClientInfoStruct();
        struct.FirstName = m_EditTextFirstName.getText().toString().trim();
        struct.LastName = m_EditTextLastName.getText().toString().trim();
        struct.SureName = m_EditTextSurname.getText().toString().trim();
        struct.Address = m_EditTextAddress.getText().toString().trim();
        struct.Phone = m_EditTextPhone.getText().toString().trim();
        struct.Comments = m_EditTextComments.getText().toString().trim();
        return struct;
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

    private void SetOnOkButtonClick()
    {
        // Add new record
        m_BtnOK.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (!CheckFieldsBeforeSave())
                {
                    return;
                }
                m_DataChangedEventListener.clientDataChangedEvent(GetClientInfo(), m_RecordOperationType);
                Close();
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

    private void SetOnEditTextChangeListeners()
    {
        // TextWatcher for required field
        TextWatcher watcherForRequiredField = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

            @Override
            public void afterTextChanged(Editable editable)
            {
                // Enable or disable buttons
                ShowHideButtons((editable.toString().trim().isEmpty()));            }
        };
        // TextWatcher for not required field
        TextWatcher watcherForNotRequiredField = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

            @Override
            public void afterTextChanged(Editable editable)
            {
                // Enable or disable buttons
                EditText text = (EditText) m_View.findViewById(R.id.FragmentClientEditTextFirstName);
                if (!text.getText().toString().trim().isEmpty())
                {
                    ShowHideButtons(false);
                }
            }
        };

        //region Set TextWatchers for all edits in the fragment
        LinearLayout layout = (LinearLayout) m_View.findViewById(R.id.FragmentEditClient);
        for( int i = 0; i < layout.getChildCount(); i++ )
        {
            if (layout.getChildAt(i) instanceof EditText)
            {
                EditText text = (EditText) (layout.getChildAt(i));
                if (text.getId() == R.id.FragmentClientEditTextFirstName)
                {
                    text.addTextChangedListener(watcherForRequiredField);
                }
                else
                {
                    text.addTextChangedListener(watcherForNotRequiredField);
                }
                SetOnEditLostFocus(text);
            }
        }
        //endregion
    }

    // Returns true if view in fragment has a focus
    private boolean IsFragmentHasAFocus()
    {
        LinearLayout layout = (LinearLayout) m_View.findViewById(R.id.FragmentEditClient);
        for( int i = 0; i < layout.getChildCount(); i++ ) {
            if ((layout.getChildAt(i) instanceof EditText) || (layout.getChildAt(i) instanceof ImageButton))
            {
                if (layout.getChildAt(i).isFocused())
                {
                    return true;
                }
            }
        }
        return false;
    }

    private void SetOnEditLostFocus(EditText text)
    {
        text.setOnFocusChangeListener(new View.OnFocusChangeListener()
        {
            @Override
            public void onFocusChange(View v, boolean hasFocus)
            {
                if (!hasFocus)
                {
                    if (!IsFragmentHasAFocus())
                    {
                        // Just close the fragment
                        Close();
                    }
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
        m_DataChangedEventListener.endClientFragment();
    }
}

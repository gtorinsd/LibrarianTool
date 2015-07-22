package com.udelphi.trainingcenter.librariantool;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.Spinner;
import android.widget.TextView;

import com.udelphi.trainingcenter.librariantool.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import com.udelphi.trainingcenter.librariantool.DBTools.DBHelper;
import com.udelphi.trainingcenter.librariantool.Tools.BookStateEnum;
import com.udelphi.trainingcenter.librariantool.Tools.ClientInfoStruct;
import com.udelphi.trainingcenter.librariantool.Tools.DatesWorker;
import com.udelphi.trainingcenter.librariantool.Tools.FileSystemManager;
import com.udelphi.trainingcenter.librariantool.DBTools.DBImagesWorker;
import com.udelphi.trainingcenter.librariantool.Tools.ImageResize;
import com.udelphi.trainingcenter.librariantool.Tools.MessageBox;
import com.udelphi.trainingcenter.librariantool.Tools.RecordOperationTypeEnum;

public class ModiBookActivity extends Activity implements FragmentCatalogEdit.catalogFragmentEventListener, FragmentClientEdit.clientFragmentEventListener
{
    //region Visual controls
    ImageView m_BookPhotoImage;
    EditText m_EditTextBookName;
    Spinner m_SpinnerAuthor;
    Spinner m_SpinnerGenre;
    EditText m_EditTextPublishing;
    EditText m_EditTextYear;
    EditText m_EditTextComments;
    private ImageButton m_ButtonEditAuthor;
    private ImageButton m_ButtonEditGenre;
    private TextView m_TextViewBookState;
    private View m_Clientslayout;

    // Select client controls
    private Spinner m_SpinnerClient;
    private ImageButton m_ButtonEditClient;

    private EditText m_EditTextCheckoutBook;
    private EditText m_EditTextReturnBook;

    // Save changes menu item
    Menu m_Menu;

    private boolean m_IsMenuVisible = true;
    //endregion

    private DBHelper m_DBAdapter;

    //region Cursors
    private Cursor m_BooksCursor;
    private Cursor m_AuthorsCursor;
    private Cursor m_GenresCursor;
    private Cursor m_ClientsCursor;
    private Cursor m_LibraryTurnoverCursor;
    //endregion

    private ToolApplication m_App;

    // Tag for logging
    private final String m_LogTag = ModiBookActivity.class.getName();

    // Current book_ID
    // null for new record
    private String m_ID;

    // Genre_ID for the book
    private String m_Genre_ID;
    // Author_ID for the book
    private String m_Author_ID;
    // Image file name for the book
    private String m_ImageFileName = null;

    // New Author info
    private String m_NewAuthorInfo;
    private RecordOperationTypeEnum m_AuthorRecordOperationType;

    // New Genre info
    private String m_NewGenreInfo;
    private RecordOperationTypeEnum m_GenreRecordOperationType;

    // New client info
    private ClientInfoStruct m_NewClientInfo;
    private RecordOperationTypeEnum m_ClientRecordOperationType;

    // Client_ID for the book
    // Null if book is free
    private String m_Client_ID;

    // Date Checkout of the book.
    // Null if book is free
    private String m_NewDateCheckoutBook;

    // Date return of the book.
    // Null if book is free
    private String m_NewDateReturnBook;

    // Flag responsible of any catalog changing
    // If it's true, activity will return RESULT_OK anyway
    private boolean m_OthersTablesAreChanged;

    //Call an external activity and get result
    private final int m_GalleryActivityResult = 1;
    private final int m_PhotoCameraActivityResult = 2;

    // Current Book State
    private BookStateEnum m_BookState;

    // _ID for new record
    final private String m_NewRecord_ID = "-1";

    private int m_FragmentsCounter = 0;

    private boolean m_SpinnerAuthorChangedFirstTime = true;
    private boolean m_SpinnerGenreChangedFirstTime = true;
    private boolean m_SpinnerClientChangedFirstTime = true;

    private int m_SaveMenuItemVisible;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_modi_book);
        // Remove activity caption
        setTitle("");
        // Remove keyboard when activity is started
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        m_OthersTablesAreChanged = false;
        m_App = (ToolApplication) this.getApplicationContext();
        GetVisualControls();
        ShowLayoutWithClients(false);
        ClearImage();

        String m_SQL = getIntent().getStringExtra("sql");
        m_ID = getIntent().getStringExtra("ID");

        m_DBAdapter = new DBHelper(this, m_App.DatabaseName, null, m_App.DatabaseVerion);
        String sql = m_SQL + " WHERE B._ID = " + m_ID;
        m_BooksCursor = m_DBAdapter.SelectSQL(sql);
        m_BooksCursor.moveToFirst();
        m_Client_ID = null;
        if (m_BooksCursor.getCount() > 0)
        {
            m_Client_ID = m_BooksCursor.getString(m_BooksCursor.getColumnIndexOrThrow("Client_ID"));
            m_BookState = (m_Client_ID == null) ? BookStateEnum.BookIsFree : BookStateEnum.BookIsInUse;
            m_Author_ID  = m_BooksCursor.getString(m_BooksCursor.getColumnIndexOrThrow("Author_ID"));
            m_Genre_ID  = m_BooksCursor.getString(m_BooksCursor.getColumnIndexOrThrow("Genre_ID"));
        }
        m_NewDateCheckoutBook = null;
        m_NewDateReturnBook = null;

        sql = "SELECT Book_ID, Client_ID, BookOutletDate, BookReturnDate FROM %s WHERE Book_ID = %s AND Client_ID = %s ";
        sql = String.format(sql, m_App.tblLibraryTurnover, m_ID, m_Client_ID);
        m_LibraryTurnoverCursor = m_DBAdapter.SelectSQL(sql);
        m_LibraryTurnoverCursor.moveToFirst();

        if (savedInstanceState == null)
        {
            // Just only on launch
            FillControls(m_BooksCursor);
            m_SaveMenuItemVisible = -1;
        }

        FillCatalogsSpinners();
        //region listeners
        SetImageViewClickListener();
        SetButtonEditAuthorClick();
        SetButtonEditGenreClick();
        SetButtonEditClientClick();
        SetEditTextDateListener(m_EditTextCheckoutBook);
        SetEditTextDateListener(m_EditTextReturnBook);
        SetEditTexDatetOnFocus(m_EditTextCheckoutBook);
        SetEditTexDatetOnFocus(m_EditTextReturnBook);
        SetEditTextChangeListener();
        m_SpinnerAuthorChangedFirstTime = SetSpinnerOnItemSelectListener(m_SpinnerAuthor, m_SpinnerAuthorChangedFirstTime);
        m_SpinnerGenreChangedFirstTime = SetSpinnerOnItemSelectListener(m_SpinnerGenre, m_SpinnerGenreChangedFirstTime);
        m_SpinnerClientChangedFirstTime = SetSpinnerOnItemSelectListener(m_SpinnerClient, m_SpinnerClientChangedFirstTime);
        //endregion
    }

    // Get visual controls from the view
    private void GetVisualControls()
    {
        m_BookPhotoImage = (ImageView) this.findViewById(R.id.ModiBookImageViewPhoto);
        m_EditTextBookName = (EditText) this.findViewById(R.id.ModiBookEditTextName);
        m_SpinnerAuthor = (Spinner) this.findViewById(R.id.ModiBookSpinnerAuthor);
        m_SpinnerGenre = (Spinner) this.findViewById(R.id.ModiBookSpinnerGenre);
        m_EditTextPublishing = (EditText) this.findViewById(R.id.ModiBookEditTextPublisher);
        m_EditTextYear = (EditText) this.findViewById(R.id.ModiBookEditTextYear);
        m_EditTextComments = (EditText) this.findViewById(R.id.ModiBookEditTextComment);
        m_ButtonEditAuthor = (ImageButton) this.findViewById(R.id.ModiBookImageButtonModiAuthor);
        m_ButtonEditGenre = (ImageButton) this.findViewById(R.id.ModiBookImageButtonModiGenre);
        m_TextViewBookState = (TextView) this.findViewById(R.id.ModiBook_TextViewBookState);
        m_Clientslayout = findViewById(R.id.ModiBookSelectClientLayout);
        m_SpinnerClient = (Spinner) findViewById(R.id.ModiBookSpinnerClient);
        m_ButtonEditClient = (ImageButton) findViewById(R.id.ModiBookImageButtonModiClient);
        m_EditTextCheckoutBook = (EditText) findViewById(R.id.ModiBookEditTextCheckoutBook);
        m_EditTextReturnBook = (EditText) findViewById(R.id.ModiBookEditTextReturnBook);
    }

    // Reread all data from the database and refresh visual controls
    private void Refresh()
    {
        FillCatalogsSpinners();
        FillControls(m_BooksCursor);
    }

    // Add new record to cursor
    private Cursor AddCursorRecord(Cursor oldCursor, String fieldName, String newFieldValue)
    {
        // Check for duplicates
        boolean b = oldCursor.moveToFirst();
        while (b)
        {
            String s = oldCursor.getString(oldCursor.getColumnIndexOrThrow(fieldName));
            if (s.equals(newFieldValue))
            {
                return oldCursor;
            }

            b = oldCursor.moveToNext();
        }

        // Add new row
        MatrixCursor extras = new MatrixCursor(new String[]{"_id", fieldName});
        extras.addRow(new String[]{m_NewRecord_ID, newFieldValue});
        Cursor[] cursors = {extras, oldCursor};
        return new MergeCursor(cursors);
    }

    // Updates record in the cursor
    private Cursor UpdateCursorRecord(Cursor oldCursor, String fieldName, String oldValue, String newValue)
    {
        MatrixCursor extras = new MatrixCursor(new String[]{"_id", fieldName});
        // Remove the old value from the cursor
        boolean b = oldCursor.moveToFirst();
        while (b)
        {
            String id = oldCursor.getString(oldCursor.getColumnIndexOrThrow("_id"));
            String name = oldCursor.getString(oldCursor.getColumnIndexOrThrow(fieldName));

            if (oldValue.equals(name))
            {
                name = newValue;
            }
            extras.addRow(new String[]{id, name});
            b = oldCursor.moveToNext();
        }
        Cursor[] cursors = {extras};
        return new MergeCursor(cursors);
    }

    //region Fill spinners
    private void FillSpinnerAuthor()
    {
        if (m_AuthorRecordOperationType == null)
        {
            // Just get data from the query
            String sql = String.format("SELECT rowid as _id, Name as NAME FROM %s ORDER BY NAME", m_App.tblAuthors);
            m_AuthorsCursor = m_DBAdapter.SelectSQL(sql);
        }
        else
        {
            String oldValue = GetSpinnerSelectedItemID(m_SpinnerAuthor, "NAME");
            if (m_AuthorRecordOperationType == RecordOperationTypeEnum.AddRecord)
            {
                m_AuthorsCursor = AddCursorRecord(m_AuthorsCursor, "NAME", m_NewAuthorInfo);
            }

            if (m_AuthorRecordOperationType == RecordOperationTypeEnum.UpdateRecord)
            {
                m_AuthorsCursor = UpdateCursorRecord(m_AuthorsCursor, "NAME", oldValue, m_NewAuthorInfo);
            }
        }

        SingleFieldCursorAdapter authorsAdapter = new SingleFieldCursorAdapter(this, m_AuthorsCursor, R.id.SelectItemTextValue, "Name");
        m_SpinnerAuthor.setAdapter(authorsAdapter);

        if (m_ID != null) {
            SetSpinnerSelectedItem(m_SpinnerAuthor, "NAME", m_BooksCursor.getString(m_BooksCursor.getColumnIndex("AuthorName")));
        }
    }

    private void FillSpinnerGenre()
    {
        if (m_GenreRecordOperationType == null)
        {
            String sql = String.format("SELECT rowid as _id, Name  as NAME FROM %s ORDER BY NAME ", m_App.tblGenres);
            m_GenresCursor = m_DBAdapter.SelectSQL(sql);
        }
        else
        {
            String oldValue = GetSpinnerSelectedItemID(m_SpinnerGenre, "NAME");
            if (m_GenreRecordOperationType == RecordOperationTypeEnum.AddRecord)
            {
                m_GenresCursor = AddCursorRecord(m_GenresCursor, "NAME", m_NewGenreInfo);
            }

            if (m_GenreRecordOperationType == RecordOperationTypeEnum.UpdateRecord)
            {
                m_GenresCursor = UpdateCursorRecord(m_GenresCursor, "NAME", oldValue, m_NewGenreInfo);
            }
        }
        SingleFieldCursorAdapter genresAdapter = new SingleFieldCursorAdapter(this, m_GenresCursor, R.id.SelectItemTextValue, "Name");
        m_SpinnerGenre.setAdapter(genresAdapter);
        if (m_ID != null)
        {
            SetSpinnerSelectedItem(m_SpinnerGenre, "NAME", m_BooksCursor.getString(m_BooksCursor.getColumnIndex("GenreName")));
        }
    }

    private String GetClientFullName(ClientInfoStruct struct)
    {
        return String.format("%s %s %s", struct.FirstName, struct.LastName, struct.SureName);
    }

    private void FillSpinnerClient()
    {
        if (m_ClientRecordOperationType == null)
        {
            String sql = String.format("SELECT rowid as _id, FirstName || ' ' || LastName || ' ' || Surname as FULLNAME FROM %s ORDER BY FULLNAME ", m_App.tblClients);
            m_ClientsCursor = m_DBAdapter.SelectSQL(sql);
        }
        else
        {
            String oldValue = GetSpinnerSelectedItemID(m_SpinnerClient, "FULLNAME");
            String newValue = GetClientFullName(m_NewClientInfo);
            if (m_ClientRecordOperationType  == RecordOperationTypeEnum.AddRecord)
            {
                m_ClientsCursor = AddCursorRecord(m_ClientsCursor, "FULLNAME", newValue);
            }

            if (m_ClientRecordOperationType  == RecordOperationTypeEnum.UpdateRecord)
            {
                m_ClientsCursor = UpdateCursorRecord(m_ClientsCursor, "FULLNAME", oldValue, newValue);
            }
        }

        m_ClientsCursor.moveToFirst();
        SingleFieldCursorAdapter genresAdapter = new SingleFieldCursorAdapter(this, m_ClientsCursor, R.id.SelectItemTextValue, "FULLNAME");
        m_SpinnerClient.setAdapter(genresAdapter);
        if (m_Client_ID != null)
        {
            String s = m_BooksCursor.getString(m_BooksCursor.getColumnIndexOrThrow("Client_ID"));
            SetSpinnerSelectedItem(m_SpinnerClient, "_id", s);
        }
    }
    //endregion

    // Fill Authors and Genres adapters
    private void FillCatalogsSpinners()
    {
        FillSpinnerAuthor();
        FillSpinnerGenre();
        FillSpinnerClient();
    }

    // Fill visual controls from the cursor
    private void FillControls(Cursor cursor)
    {
        // Fill visual controls
        if (cursor.getCount() > 0)
        {
            m_EditTextBookName.setText(cursor.getString(cursor.getColumnIndexOrThrow("BookName")));
            m_EditTextPublishing.setText(cursor.getString(cursor.getColumnIndexOrThrow("Publishing")));
            m_EditTextYear.setText(cursor.getString(cursor.getColumnIndexOrThrow("BookEditionYear")));
            m_EditTextComments.setText(cursor.getString(cursor.getColumnIndexOrThrow("Comments")));
        }

        // Show an image
        DBImagesWorker worker = new DBImagesWorker(m_App, cursor);
        Drawable d;
        try
        {
            d = worker.GetImageFromDB(m_ID, "Photo", true);
            m_BookPhotoImage.setImageDrawable(d);

            if (cursor.getCount() > 0)
            {
                m_ImageFileName = cursor.getString(cursor.getColumnIndexOrThrow("Photo"));
                ShowBookState();
            }
        }
        catch (Exception e)
        {
            Log.e(m_LogTag, e.getMessage());
        }

        // Pickers
        SetDatePickers();
    }

    // Set selected item for spinner from the current database value
    private void SetSpinnerSelectedItem(Spinner spinner, String fieldName,  String text)
    {
        // look for the index of string in the adapter
        int i;
        String s = "";
        for (i = 0; i < spinner.getCount(); i++)
        {
            Cursor cursor = (Cursor) spinner.getItemAtPosition(i);

            s = cursor.getString(cursor.getColumnIndex(fieldName));
            if (s.equals(text))
            {
                break;
            }
        }

        if (s.equals(text))
        {
            spinner.setSelection(i);
        }
    }

    // Show or hide BookCheckOut and BookReturn menu items
    private void ShowHideBookMenuItemsOnLaunchActivity(Menu menu)
    {
        ShowHideSaveMenuItem(true);

        if (m_BookState == BookStateEnum.BookIsFree)
        {
            menu.findItem(R.id.ModiBookCheckoutMenuItem).setVisible(true);
            menu.findItem(R.id.ModiBookReturnMenuItem).setVisible(false);
        }

        if (m_BookState == BookStateEnum.BookIsInUse)
        {
            menu.findItem(R.id.ModiBookCheckoutMenuItem).setVisible(false);
            menu.findItem(R.id.ModiBookReturnMenuItem).setVisible(true);
        }

        if (m_BookState == BookStateEnum.BookIsCheckingOut)
        {
            menu.findItem(R.id.ModiBookCheckoutMenuItem).setVisible(false);
            menu.findItem(R.id.ModiBookReturnMenuItem).setVisible(false);
        }

        int i = m_BooksCursor.getCount();
        if (i <= 0)
        {
            menu.findItem(R.id.ModiBookCheckoutMenuItem).setVisible(false);
            menu.findItem(R.id.ModiBookReturnMenuItem).setVisible(false);
            menu.findItem(R.id.ModiBookDeleteMenuItem).setVisible(false);
        }
    }

    // Show book sate: book is free, book is in use etc
    private void ShowBookState()
    {
        //String buttonCaption;
        String s;
        //String buttonCaption color
        int color;

        if (m_Client_ID == null)
        {
            s = getString(R.string.Modibook_BookState_isCheckingOut);
            //buttonCaption = getString(R.string.ModiBook_GiveBookMsg);
            color = this.getResources().getColor(R.color.ModiBook_ColorBookIsFree);
            ShowLayoutWithClients(false);
        }
        else
        {
            s = getString(R.string.Modibook_BookState_isInUse);
            //buttonCaption = getString(R.string.ModiBook_ReturnBookMsg);
            color = this.getResources().getColor(R.color.ModiBook_ColorBookIsInUse);
            ShowLayoutWithClients(true);
        }
        m_TextViewBookState.setText(s);
        m_TextViewBookState.setTextColor(color);
    }

    // Show an image from the file
    private void ShowImageFromTheFile(String imageFileName)
    {
        if (imageFileName == null)
        {
            Log.d(m_LogTag, "ShowImageFromTheFile: imageFileName is null");
            return;
        }

        Drawable d = DBImagesWorker.GetImageFromTheFile(m_App, imageFileName);
        if (d == null)
        {
            Log.e(m_LogTag, "ShowImageFromTheFile: Drawable is null!");
            return;
        }
        m_BookPhotoImage.setImageDrawable(d);
    }

    // Returns _id of selected item in the spinner
    private String GetSpinnerSelectedItemID(Spinner spinner, String fieldName)
    {
        Cursor cursor = (Cursor) spinner.getSelectedItem();
        String s;
        if ((cursor != null) && (cursor.getCount() > 0))
            s = cursor.getString(cursor.getColumnIndexOrThrow(fieldName));
        else
        {
            s = null;
        }

        return s;
    }

    //region Work with image control
    private void ClearImage()
    {
        DBImagesWorker worker = new DBImagesWorker(m_App, m_BooksCursor);
        Drawable d = worker.GetImageFromDB(null, "", true);
        m_BookPhotoImage.setImageDrawable(d);
        m_ImageFileName = null;
    }

    private void ShowImageFromUri(Uri uriFileName)
    {
        // Get file from Uri
        FileSystemManager manager = new FileSystemManager(m_App);
        m_ImageFileName = manager.CopyImageFromUriToImagesDirectory(uriFileName);
        if (m_ImageFileName == null) {
            String s = "Can't copy image from the galerry";
            MessageBox.Show(m_App.getApplicationContext(), s);
            Log.e(m_LogTag, s);
            return;
        }

        //region Resize image
        ImageResize tool = new ImageResize(m_App);
        String fullFileName = FileSystemManager.GetImagesDirectoryPath(m_App) + "/" + m_ImageFileName;
        tool.DoImageResize(fullFileName);
        //endregion

        // Show image in the ImageView
        ShowImageFromTheFile(m_ImageFileName);
    }

    //
    private void SetImageViewClickListener()
    {
        m_BookPhotoImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GetImageSource();
            }
        });
    }

    // Update content value by new image. It used for insert or update record
    private ContentValues UpdateImageField(ContentValues contentValues)
    {
        contentValues.put("Photo", m_ImageFileName);
        return contentValues;
    }
    //endregion

    //region Modify books table
    // Append new record into the tblBooks table
    private boolean AddNewRecordBook()
    {
        ContentValues contentValues = new ContentValues();
        contentValues.put("Name", m_EditTextBookName.getText().toString().trim());
        contentValues.put("Author_ID", m_Author_ID);
        contentValues.put("Genre_ID", m_Genre_ID);
        contentValues.put("Publishing", m_EditTextPublishing.getText().toString().trim());
        contentValues.put("BookEditionYear", m_EditTextYear.getText().toString().trim());
        contentValues.put("Comments", m_EditTextComments.getText().toString().trim());
        contentValues = UpdateImageField(contentValues);

        boolean b = (m_DBAdapter.InsertValuesByContent(m_App.tblBooks, contentValues) > 0);
        if (!b)
        {
            String s = m_DBAdapter.getErrorMsg();
            MessageBox.Show(m_App.getApplicationContext(), s);
            Log.e(m_LogTag, s);
        }
        return b;
    }

    // Update the current record in the tblBooks table
    private boolean UpdateRecordBook() {
        ContentValues contentValues = new ContentValues();
        contentValues.put("Name", m_EditTextBookName.getText().toString().trim());
        contentValues.put("Author_ID", m_Author_ID);
        contentValues.put("Genre_ID", m_Genre_ID);
        contentValues.put("Publishing", m_EditTextPublishing.getText().toString().trim());
        contentValues.put("BookEditionYear", m_EditTextYear.getText().toString().trim());
        contentValues.put("Comments", m_EditTextComments.getText().toString().trim());
        contentValues = UpdateImageField(contentValues);
        boolean b = m_DBAdapter.UpdateValuesByContent(m_App.tblBooks, contentValues, "_ID = ?", new String[]{m_ID});
        if (!b)
        {
            String s = m_DBAdapter.getErrorMsg();
            MessageBox.Show(m_App.getApplicationContext(), s);
            Log.e(m_LogTag, s);
        }
        return b;
    }

    // Delete the current record  from the tblBook table
    private boolean DeleteRecordBook(String id)
    {
        String s;
        // Delete book
        boolean b = m_DBAdapter.DeleteRecord(m_App.tblBooks, String.format("_ID = %s", id));
        if (!b)
        {
            s = getString(R.string.ModiBook_CantDeleteBook_errorMsg);
            MessageBox.Show(m_App.getApplicationContext(), s);
            return false;
        }

        // Delete LibraryTurnover record
        b = m_DBAdapter.DeleteRecord(m_App.tblLibraryTurnover, String.format("BOOK_ID = %s", id));
        if (!b) {
            s = getString(R.string.ModiBook_CantDeleteTurnoverBook_errorMsg);
            MessageBox.Show(m_App.getApplicationContext(), s);
        }
        return b;
    }
    //endregion

    //region Modify Turnover tables
    // Add new record into tblTurnOver table
    private boolean AddNewRecordLibraryTurnover()
    {
        ContentValues contentValues = new ContentValues();
        contentValues.put("Book_ID", m_ID);
        contentValues.put("Client_ID", m_Client_ID);
        contentValues.put("BookOutletDate", m_NewDateCheckoutBook);
        contentValues.put("BookReturnDate", m_NewDateReturnBook);
        boolean b = (m_DBAdapter.InsertValuesByContent(m_App.tblLibraryTurnover, contentValues) > 0);
        if (!b)
        {
            String s = m_DBAdapter.getErrorMsg();
            MessageBox.Show(m_App.getApplicationContext(), s);
            Log.e(m_LogTag, s);
        }
        return b;
    }

    // Update record in the  tblTurnOver table
    private boolean UpdateRecordLibraryTurnover()
    {
        ContentValues contentValues = new ContentValues();
        contentValues.put("Book_ID", m_ID);
        contentValues.put("Client_ID", m_Client_ID);
        contentValues.put("BookOutletDate", m_NewDateCheckoutBook);
        contentValues.put("BookReturnDate", m_NewDateReturnBook);
        boolean b = m_DBAdapter.UpdateValuesByContent(m_App.tblLibraryTurnover, contentValues, "Book_ID = ?", new String[]{m_ID});
        if (!b)
        {
            String s = m_DBAdapter.getErrorMsg();
            MessageBox.Show(m_App.getApplicationContext(), s);
            Log.e(m_LogTag, s);
        }
        return b;

    }
    //endregion


    private String ModifyCatalogTables(String tableName, String oldID, String fieldValue, RecordOperationTypeEnum type)
    {
        String fieldName = "Name";
        ContentValues contentValues = new ContentValues();
        switch (type)
        {
            case  AddRecord:
            {
                contentValues.put(fieldName, fieldValue);
                long l = m_DBAdapter.InsertValuesByContent(tableName, contentValues);
                if (l <=0)
                {
                    String s = m_DBAdapter.getErrorMsg();
                    MessageBox.Show(m_App.getApplicationContext(), s);
                    Log.e(m_LogTag, s);
                }
                return String.format("%d", l);
            }
            case UpdateRecord:
            {
                contentValues.put(fieldName, fieldValue);
                boolean b = m_DBAdapter.UpdateValuesByContent(tableName, contentValues, "_ID = ?", new String[]{oldID});
                if (!b)
                {
                    String s = m_DBAdapter.getErrorMsg();
                    MessageBox.Show(m_App.getApplicationContext(), s);
                    Log.e(m_LogTag, s);
                }
                return oldID;
            }

            default:
            {
                Log.d(m_LogTag, "Unexpected record operation type: ModifyCatalogTables");
                return  oldID;
            }
        }
    }

    private String ModifyClientTable(String Client_ID)
    {
        ContentValues contentValues = new ContentValues();
        contentValues.put("FirstName", m_NewClientInfo.FirstName);
        contentValues.put("LastName", m_NewClientInfo.LastName);
        contentValues.put("Surname", m_NewClientInfo.SureName);
        contentValues.put("Address", m_NewClientInfo.Address);
        contentValues.put("Phone", m_NewClientInfo.Phone);
        contentValues.put("Comments", m_NewClientInfo.Comments);

        switch (m_ClientRecordOperationType)
        {
            case AddRecord:
            {
                long l = m_DBAdapter.InsertValuesByContent(m_App.tblClients, contentValues);
                if (l <= 0)
                {
                    String s = m_DBAdapter.getErrorMsg();
                    MessageBox.Show(m_App.getApplicationContext(), s);
                    Log.e(m_LogTag, s);
                }
                return String.format("%d", l);
            }
            case UpdateRecord:
            {
                boolean b = m_DBAdapter.UpdateValuesByContent(m_App.tblClients, contentValues, "_ID = ?", new String[]{Client_ID});
                if (!b)
                {
                    String s = m_DBAdapter.getErrorMsg();
                    MessageBox.Show(m_App.getApplicationContext(), s);
                    Log.e(m_LogTag, s);
                }
                return Client_ID;
            }
            default:
            {
                Log.d(m_LogTag, "Unexpected record operation type: ModifyClientTable");
                return m_Client_ID;
            }
        }
    }

    // Get new IDs from spinners
    private void SaveCatalogsChanges()
    {
        //region Save Author
        if (m_AuthorsCursor.getCount() == 0)
        {
            return;
        }

        String s = m_AuthorsCursor.getString(m_AuthorsCursor.getColumnIndexOrThrow("_id"));
        if (s.equals(m_NewRecord_ID))
        {
            m_AuthorRecordOperationType = RecordOperationTypeEnum.AddRecord;
        }
        //endregion
        m_Author_ID = (m_AuthorRecordOperationType != null) ? ModifyCatalogTables(m_App.tblAuthors, s, m_NewAuthorInfo, m_AuthorRecordOperationType) : GetSpinnerSelectedItemID(m_SpinnerAuthor, "_id");

        //region Save Genre
        if (m_GenresCursor.getCount() == 0)
        {
            return;
        }

        s = m_GenresCursor.getString(m_GenresCursor.getColumnIndexOrThrow("_id"));
        if (s.equals(m_NewRecord_ID))
        {
            m_GenreRecordOperationType = RecordOperationTypeEnum.AddRecord;
        }
        //endregion
        m_Genre_ID = (m_GenreRecordOperationType != null) ? ModifyCatalogTables(m_App.tblGenres, s, m_NewGenreInfo, m_GenreRecordOperationType) :  GetSpinnerSelectedItemID(m_SpinnerGenre, "_id");

        //region Save Client
        if (m_ClientRecordOperationType != null)
        {
            s = m_ClientsCursor.getString(m_ClientsCursor.getColumnIndexOrThrow("_id"));
            if (s.equals(m_NewRecord_ID))
            {
                m_ClientRecordOperationType = RecordOperationTypeEnum.AddRecord;
            }
        }

        if (m_ClientsCursor.getCount() == 0)
        {
            // Clients table is empty
            m_Client_ID = null;
        }
        else
        {
            s = m_ClientsCursor.getString(m_ClientsCursor.getColumnIndexOrThrow("_id"));
            //endregion
            m_Client_ID = (m_ClientRecordOperationType != null) ? ModifyClientTable(s) : GetSpinnerSelectedItemID(m_SpinnerClient, "_id");
        }
    }

    // Show alert dialog in the onImageClick
    private void GetImageSource()
    {
        //region Get items for dialog from resource
        // Get strings from resource (array to list)
        List<String> list = new ArrayList<> (Arrays.asList(getResources().getStringArray(R.array.GetImageDialogItems)));
        if (list.size() <= 0)
        {
            // No items for menu? o_O
            return;
        }
        if (m_ImageFileName == null)
        {
            // Remove "Delete" menu item from the list
            int count = list.size() - 1;
            list.remove(count);
        }
        String[] data = list.toArray(new String[list.size()]);
        //endregion

        // Create dialog
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setTitle(getResources().getString(R.string.GetImageDialogCaption));

        alertDialog.setItems(data, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0: {
                        // Get image from gallery
                        Intent intent = new Intent(Intent.ACTION_PICK);
                        intent.setType("image/*");
                        startActivityForResult(intent, m_GalleryActivityResult);
                        break;
                    }
                    case 1: {
                        // Get image from the photo camera
                        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        if (intent.resolveActivity(getPackageManager()) != null) {
                            startActivityForResult(intent, m_PhotoCameraActivityResult);
                        }
                        break;
                    }
                    case 2: {
                        // Clear image
                        ClearImage();
                        break;
                    }

                    default: {
                        Log.d(m_LogTag, "GetImageSource: Unexpected image source");
                        break;
                    }
                }
            }
        });

        Dialog dialog = alertDialog.create();
        dialog.show();
    }

    // Runs Catalog Fragment on popup menu
    private void RunCatalogFragment(String value, int spinnerID, int fragmentID, RecordOperationTypeEnum type)
    {
        //region run fragment
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        Fragment f = fragmentManager.findFragmentById(fragmentID);
        if (f == null)
        {
            // We need only one fragment
            ShowHideMenuItemsOnRunFragment(false);

            FragmentCatalogEdit myFragment = FragmentCatalogEdit.GetInstance(value, spinnerID, type);
            fragmentTransaction.add(fragmentID, myFragment);
        } else {
            fragmentTransaction.remove(f);
        }
        fragmentTransaction.commit();
    }

    // Returns popupmenu with it's events for catalog
    private PopupMenu GetPopupMenuCatalog(final View v, final int spinnerID, final int fragmentID)
    {
        final Spinner spinner = (Spinner) this.findViewById(spinnerID);

        // Set the popup menu with some style
        Context wrapper = new ContextThemeWrapper(m_App.getApplicationContext(), R.style.popupMenuStyle);
        PopupMenu popupMenu = new PopupMenu(wrapper, v);

        popupMenu.inflate(R.menu.popupmenu_modirecords);
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case (R.id.ModiBook_PopupMenuItem_AddNewRecord): {
                        RunCatalogFragment("", spinnerID, fragmentID, RecordOperationTypeEnum.AddRecord);
                        break;
                    }

                    case (R.id.ModiBook_PopupMenuItem_ModiRecord): {
                        String value = GetSpinnerSelectedItemID(spinner, "NAME");
                        RunCatalogFragment(value, spinnerID, fragmentID, RecordOperationTypeEnum.UpdateRecord);
                        break;
                    }

                    default: {
                        Log.d(m_LogTag, "GetPopupMenuCatalog: Unexpected popup menu item");
                        break;
                    }
                }
                return true;
            }
        });
        return popupMenu;
    }

    // Runs Client Fragment on popup menu
    private void RunClientFragment(int fragmentID, ClientInfoStruct struct, RecordOperationTypeEnum type)
    {
        //region run fragment
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        Fragment f = fragmentManager.findFragmentById(fragmentID);
        if (f == null)
        {
            // We need only one fragment
            ShowHideMenuItemsOnRunFragment(false);

            FragmentClientEdit myFragment = FragmentClientEdit.GetInstance(struct, type);
            fragmentTransaction.add(fragmentID, myFragment);
        } else {
            fragmentTransaction.remove(f);
        }
        fragmentTransaction.commit();
    }

    // Returns popupmenu with it's events for catalog
    private PopupMenu GetPopupMenuClient(final View v, final int spinnerID, final int fragmentID)
    {
        final ClientInfoStruct struct = new ClientInfoStruct();

        Context wrapper = new ContextThemeWrapper(m_App.getApplicationContext(), R.style.popupMenuStyle);
        PopupMenu popupMenu = new PopupMenu(wrapper, v);
        popupMenu.inflate(R.menu.popupmenu_modirecords);
        final Spinner spinner = (Spinner) this.findViewById(spinnerID);
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case (R.id.ModiBook_PopupMenuItem_AddNewRecord): {
                        RunClientFragment(fragmentID, struct, RecordOperationTypeEnum.AddRecord);
                        break;
                    }

                    case (R.id.ModiBook_PopupMenuItem_ModiRecord): {
                        //region Fill client structure from the DB
                        String ClientID = GetSpinnerSelectedItemID(m_SpinnerClient, "_id");
                        String sql = String.format("SELECT rowid as _id, FirstName, LastName, Surname, Address, Phone, Comments FROM %s WHERE _id = %s", m_App.tblClients, ClientID);
                        Cursor cursor = m_DBAdapter.SelectSQL(sql);
                        cursor.moveToFirst();
                        try {
                            if (cursor.getCount() <= 0) {
                                Log.d(m_LogTag, "Clients list is empty: SetButtonEditClientClick");
                            } else {
                                struct.FirstName = cursor.getString(cursor.getColumnIndexOrThrow("FIRSTNAME"));
                                struct.LastName = cursor.getString(cursor.getColumnIndexOrThrow("LASTNAME"));
                                struct.SureName = cursor.getString(cursor.getColumnIndexOrThrow("SURNAME"));
                                struct.Address = cursor.getString(cursor.getColumnIndexOrThrow("ADDRESS"));
                                struct.Phone = cursor.getString(cursor.getColumnIndexOrThrow("PHONE"));
                                struct.Comments = cursor.getString(cursor.getColumnIndexOrThrow("COMMENTS"));
                            }
                        } finally {
                            cursor.close();
                        }
                        //endregion

                        RunClientFragment(fragmentID, struct, RecordOperationTypeEnum.UpdateRecord);
                        break;
                    }

                    default: {
                        Log.d(m_LogTag, "GetPopupMenuCatalog: Unexpected popup menu item");
                        break;
                    }
                }
                return true;
            }
        });

        return popupMenu;
    }

    // Edit tblAuthors button click
    private void SetButtonEditAuthorClick()
    {
        m_ButtonEditAuthor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //region PopupMenu: get record operation
                PopupMenu popupMenu = GetPopupMenuCatalog(v, R.id.ModiBookSpinnerAuthor, R.id.ModiBook_ModiAuthorFragment);
                popupMenu.show();
                //endregion
            }
        });
    }

    // Edit tblGenres button click
    private void SetButtonEditGenreClick()
    {
        m_ButtonEditGenre.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //region PopupMenu: get record operation
                PopupMenu popupMenu = GetPopupMenuCatalog(v, R.id.ModiBookSpinnerGenre, R.id.ModiBook_ModiGenreFragment);
                popupMenu.show();
                //endregion
            }
        });
    }

    // Edit tblClients button click
    private void SetButtonEditClientClick()
    {
        m_ButtonEditClient.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                GetPopupMenuClient(v, R.id.ModiBookSpinnerClient, R.id.ModiBook_ModiClientFragment).show();
            }
        });
    }

    // Set new dates into pickers
    // Get dates from the record or get current dates
    private void SetDatePickers()
    {
        DatesWorker worker = new DatesWorker(m_App);
        if (m_LibraryTurnoverCursor.getCount() == 0)
        {
            //region New record
            Calendar calendar = worker.InitCalendarCurrentDate();
            // Date checkout picker
            String s = worker.GetStrDateFromCalendar(calendar);
            m_EditTextCheckoutBook.setText(s);
            // Date return picker (day + 1)
            worker.AddCalendarDay(calendar, 1);
            s = worker.GetStrDateFromCalendar(calendar);
            m_EditTextReturnBook.setText(s);
            //endregion
        }
        else
        {
            //region Existing record
            String s = m_LibraryTurnoverCursor.getString(m_LibraryTurnoverCursor.getColumnIndex("BookOutletDate".toUpperCase()));
            m_EditTextCheckoutBook.setText(s);
            // Date return picker
            s = m_LibraryTurnoverCursor.getString(m_LibraryTurnoverCursor.getColumnIndex("BookReturnDate".toUpperCase()));
            m_EditTextReturnBook.setText(s);
            //endregion
        }
    }

    // Prepare new values for the tblLibraryTurnover table
    private void SetTurnoverValues()
    {
        // Dates
        m_NewDateCheckoutBook = m_EditTextCheckoutBook.getText().toString();
        m_NewDateReturnBook = m_EditTextReturnBook.getText().toString();
    }

    // Inflate the ModiBook_ClientFragment
    private void ShowLayoutWithClients(boolean b)
    {
        int visible = (b) ? View.VISIBLE : View.INVISIBLE;
        m_Clientslayout.setVisibility(visible);

        // Set layout height
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) m_Clientslayout.getLayoutParams();
        params.height = (b) ? LinearLayout.LayoutParams.WRAP_CONTENT : 0;
        m_Clientslayout.setLayoutParams(params);

        if ((b) && (m_Client_ID == null))
        {
            // Book is just checked out
            m_BookState = BookStateEnum.BookIsCheckingOut;
        }
        else
        {
            m_BookState = (b) ? BookStateEnum.BookIsInUse : BookStateEnum.BookIsFree;
        }
    }

    // Calls DatePickerDialog and sets new date to editText
    private void CallDatePickerDialog(final EditText editText, View view)
    {
        final DatesWorker worker = new DatesWorker(m_App);

        int mYear;
        int mMonth;
        int mDay;
        Calendar calendar = worker.InitCalendarCurrentDate();
        String s = editText.getText().toString().trim();
        if (!s.isEmpty()) {
            worker.SetStrDateToCalendar(calendar, s);
        }
        mYear = calendar.get(Calendar.YEAR);
        mMonth = calendar.get(Calendar.MONTH);
        mDay = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dpd = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                String s = worker.GetStrDateFromDatePicker(view);
                editText.setText(s);
                ShowHideSaveMenuItem(true);
            }
        }, mYear, mMonth, mDay);
        dpd.show();
    }

    // OnClickListener for date pickers
    private void SetEditTextDateListener(final EditText editText)
    {
        editText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CallDatePickerDialog(editText, v);
            }
        });
    }

    private void SetEditTexDatetOnFocus(final EditText editText)
    {
        editText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    CallDatePickerDialog(editText, v);
                }
            }
        });

    }


    // TextWatcher for EditTexts
    private void SetEditTextChangeListener()
    {
        // TextWatcher for required field
        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

            @Override
            public void afterTextChanged(Editable editable)
            {
                ShowHideSaveMenuItem(true);
            }
        };
        //region Set TextWatchers for all edits in the fragment
        LinearLayout layout = (LinearLayout) this.findViewById(R.id.ModiBookMainDataLayout);
        for( int i = 0; i < layout.getChildCount(); i++ )
        {
            if (layout.getChildAt(i) instanceof EditText)
            {
                EditText text = (EditText) (layout.getChildAt(i));
                text.addTextChangedListener(textWatcher);
            }
        }
        //endregion

    }

    // OnItemSelect listener for spinners
    private boolean SetSpinnerOnItemSelectListener(Spinner spinner, boolean isFirstRun)
    {
        final boolean[] result = {isFirstRun};
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!result[0]) {
                    ShowHideSaveMenuItem(true);
                } else {
                    result[0] = false;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        return result[0];
    }

    // Move record from the tblLibraryTurnover to the tblLibraryTurnoverArchive in a transaction
    private boolean ReturnBook()
    {
        String[] script = new String[2];
        String sql = "INSERT INTO %s (Book_ID, Client_ID, BookOutletDate, BookReturnDate) SELECT Book_ID, Client_ID, BookOutletDate, BookReturnDate FROM %s WHERE BOOK_ID = %s ";
        sql = String.format(sql, m_App.tblLibraryTurnoverArchive, m_App.tblLibraryTurnover, m_ID);
        script[0] = sql;
        sql = "DELETE FROM %s WHERE BOOK_ID = %s";
        sql = String.format(sql, m_App.tblLibraryTurnover, m_ID);
        script[1] = sql;

        boolean b = m_DBAdapter.ExecScriptInTransaction(script);
        if (!b)
        {
            String s = m_DBAdapter.getErrorMsg();
            MessageBox.Show(m_App.getApplicationContext(), s);
            Log.e(m_LogTag, s);
            return false;
        }

        m_Client_ID = null;
        m_NewDateCheckoutBook = null;
        m_NewDateReturnBook = null;
        return true;
    }

    // Check if all fields are filled
    private boolean CheckBookFieldsBeforeSave()
    {
        // Check book name field
        String s = m_EditTextBookName.getText().toString().trim();
        if (s.isEmpty()) {
            s = getString(R.string.ModiBook_BookNameIsEmptyMsg);
            MessageBox.Show(m_App.getApplicationContext(), s);
            return false;
        }

        // Author
        s = m_Author_ID;
        if ((s == null) || (s.isEmpty()))
        {
            MessageBox.Show(m_App.getApplicationContext(), getString(R.string.FragmentModiCatalogBookAuthorIsEmptyMsg));
            return false;
        }

        // Genre
        s = m_Genre_ID;
        if ((s == null) || (s.isEmpty()))
        {
            MessageBox.Show(m_App.getApplicationContext(), getString(R.string.FragmentModiCatalogBookGenreIsEmptyMsg));
            return false;
        }

        return true;
    }

    // Save changes into database
    private boolean SaveChanges()
    {
        // Get IDs from catalogs tables
        SaveCatalogsChanges();
        if (!CheckBookFieldsBeforeSave())
        {
            return false;
        }

        // Add or update record
        boolean b = (m_ID == null) ? AddNewRecordBook() : UpdateRecordBook();
        if (!b)
        {
            return false;
        }

        //region LibraryTurnOver table
        if (m_BookState == BookStateEnum.BookIsFree)
        {
            // No updates for tblTurnover
            return true;
        }

        if (m_BookState == BookStateEnum.BookIsInUse)
        {
            // Book just checked out
            m_Client_ID = GetSpinnerSelectedItemID(m_SpinnerClient, "_id");
        }

        // Client
        if (m_Client_ID == null)
        {
            String s = getString(R.string.ModiBook_ClientIsEmptyMsg);
            MessageBox.Show(m_App.getApplicationContext(), s);
            return false;
        }

        // Set newOutletDate, newReturnDate values
        SetTurnoverValues();

        // Dates
        DatesWorker worker = new DatesWorker(m_App);
        Date outletDate = worker.StrToDate(m_NewDateCheckoutBook);
        Date returnDate = worker.StrToDate(m_NewDateReturnBook);
        int dateCompare = outletDate.compareTo(returnDate);
        if (dateCompare >= 0)
        {
            String s = getString(R.string.ModiBook_CompareDatesWrongResultMsg);
            MessageBox.Show(m_App.getApplicationContext(), s);
            return false;
        }

        // Insert or update tblLibraryTurnover
        b = (m_BookState == BookStateEnum.BookIsCheckingOut) ? AddNewRecordLibraryTurnover() : UpdateRecordLibraryTurnover();
        return b;
    }

    private void ShowHideMenuItemsOnRunFragment(boolean b)
    {
        if (m_Menu != null)
        {
            ShowHideSaveMenuItem(b);
            m_Menu.findItem(R.id.ModiBookDeleteMenuItem).setVisible(b);
            m_Menu.findItem(R.id.ModiBookCheckoutMenuItem).setVisible(b);
            m_Menu.findItem(R.id.ModiBookReturnMenuItem).setVisible(b);
            m_IsMenuVisible = b;
        }
        else
        {
            Log.e(m_LogTag, "ShowHideMenuItemsOnRunFragment: m_Menu is null!");
        }
    }

    // Show or hide Save menu item
    private void ShowHideSaveMenuItem(boolean b)
    {
        if (m_Menu != null)
        {
            if (m_SaveMenuItemVisible != -1)
            {
                if (m_SaveMenuItemVisible == 0)
                {
                    b = false;
                }
                if (m_SaveMenuItemVisible == 1)
                {
                    b = true;
                }
                m_SaveMenuItemVisible = -1;
            }


            if (m_EditTextBookName.getText().toString().trim().isEmpty())
            {
                m_Menu.findItem(R.id.ModiBookBtnOk).setVisible(false);
            }
            else
            {
                m_Menu.findItem(R.id.ModiBookBtnOk).setVisible(b);
            }


        }
    }

    @Override
    public boolean onPrepareOptionsMenu (Menu menu)
    {
        m_Menu = menu;

        if (m_FragmentsCounter > 0)
        {
            ShowHideMenuItemsOnRunFragment(m_IsMenuVisible);
        }

        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_modi_book, menu);
        ShowHideBookMenuItemsOnLaunchActivity(menu);
        m_Menu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();
        switch (id) {
            case R.id.ModiBookBtnOk:
            {
                // OK button click
                if (SaveChanges())
                {
                    setResult(RESULT_OK, null);
                    Close();
                }
                break;
            }

            case R.id.ModiBookBtnCancel:
            {
                // Cancel button click
                // Do we have any changes in catalogs tables? Return RESULT_OK if we have.
                int result = (m_OthersTablesAreChanged) ? RESULT_OK: RESULT_CANCELED;
                setResult(result, null);
                Close();
                break;
            }

            case R.id.ModiBookDeleteMenuItem:
            {
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
                dialogBuilder.setTitle(R.string.WarningMsgCaption);
                dialogBuilder.setMessage(getString(R.string.ConfirmDeleteBookMsg));
                dialogBuilder.setCancelable(false);
                dialogBuilder.setPositiveButton(R.string.btnOK, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id)
                            {
                                // Delete record menu item click
                                if (DeleteRecordBook(m_ID))
                                {
                                    setResult(RESULT_OK, null);
                                    Close();
                                }

                                dialog.dismiss();
                            }
                        }
                );
                dialogBuilder.setNegativeButton(R.string.btnCancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        }
                );

                AlertDialog alert = dialogBuilder.create();
                alert.show();
                break;
            }

            case R.id.ModiBookCheckoutMenuItem:
            {
                // Checkout book to client
                ShowLayoutWithClients(true);
                break;
            }

            case R.id.ModiBookReturnMenuItem:
            {
                // Checkout book to client
                if (ReturnBook())
                {
                    Refresh();
                    ShowLayoutWithClients(false);
                    m_OthersTablesAreChanged = true;
                    setResult(RESULT_OK, null);
                    Close();
                }
                break;
            }

            default:
            {
                Log.d(m_LogTag, "onOptionsItemSelected: unknown MenuItem");
                return super.onOptionsItemSelected(item);
            }
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode)
        {
            case m_GalleryActivityResult:
            {
                // Get picture from gallery
                if (resultCode == RESULT_OK)
                {
                    ShowImageFromUri(data.getData());
                }
                break;
            }

            case m_PhotoCameraActivityResult:
            {
                // Get picture from the photo camera
                if (resultCode == RESULT_OK)
                {
                    Bundle extras = data.getExtras();
                    Bitmap imageBitmap = (Bitmap) extras.get("data");
                    String s = FileSystemManager.SaveImageFromBitmap(m_App, imageBitmap);
                    if (s != null)
                    {
                        m_ImageFileName = s;
                        m_BookPhotoImage.setImageBitmap(imageBitmap);
                    }
                }
                break;
            }
            default:
            {
                Log.d(m_LogTag, "Unexpected activity requestCode");
                break;
            }
        }
    }

    @Override
    protected void onDestroy()
    {
        //RemoveAllFragments();
        if (m_DBAdapter != null) {
            m_DBAdapter.close();
        }

        super.onDestroy();
    }

    //region Save and restore instance
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState)
    {
        outState.putString("ImageFileName", m_ImageFileName);
        outState.putBoolean("MenuIsVisible", m_IsMenuVisible);


        if (m_Menu.findItem(R.id.ModiBookBtnOk).isVisible())
        {
            m_SaveMenuItemVisible = 1;
        }
        else
        {
            m_SaveMenuItemVisible = 0;
        }

        outState.putInt("SaveMenuItemVisible", m_SaveMenuItemVisible);

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onRestoreInstanceState( @NonNull Bundle outState)
    {
        super.onRestoreInstanceState(outState);
        m_ImageFileName = outState.getString("ImageFileName");
        m_IsMenuVisible = outState.getBoolean("MenuIsVisible");
        m_SaveMenuItemVisible = outState.getInt("SaveMenuItemVisible");

        ShowImageFromTheFile(m_ImageFileName);
        ShowBookState();
        if (m_Menu != null)
        {
            ShowHideMenuItemsOnRunFragment(m_IsMenuVisible);
        }
    }
    //endregion

    //region Fragments edit listeners
    //region Catalogs
    @Override
    public void catalogDataChangedEvent(String s, int spinnerID, RecordOperationTypeEnum type)
    {
        Spinner spinner = (Spinner) this.findViewById(spinnerID);
        switch (spinnerID)
        {
            case R.id.ModiBookSpinnerAuthor:
            {
                m_NewAuthorInfo = s;
                if (m_AuthorsCursor.getCount() == 0)
                {
                    // 0 records? Insert!
                    m_AuthorRecordOperationType = RecordOperationTypeEnum.AddRecord;
                }
                else
                {
                    m_AuthorRecordOperationType = type;
                }

                FillSpinnerAuthor();
                break;
            }

            case R.id.ModiBookSpinnerGenre:
            {
                m_NewGenreInfo = s;
                if (m_GenresCursor.getCount() == 0)
                {
                    // 0 records? Insert!
                    m_GenreRecordOperationType = RecordOperationTypeEnum.AddRecord;
                }
                else
                {
                    m_GenreRecordOperationType = type;
                }

                FillSpinnerGenre();
                break;
            }
            default:
            {
                Log.d(m_LogTag, "catalogDataChangedEvent: Unexpected spinnerID");
                break;
            }
        }

        SetSpinnerSelectedItem(spinner, "NAME", s);
        m_OthersTablesAreChanged = true;
    }
    // Hide layout on start fragment
    @Override
    public void startCatalogFragment(int spinnerID)
    {
        int layoutID;
        switch (spinnerID)
        {
            case R.id.ModiBookSpinnerAuthor:
            {
                layoutID = R.id.ModiBookAuthorLayout;
                break;
            }
            case R.id.ModiBookSpinnerGenre:
            {
                layoutID = R.id.ModiBookGenreLayout;
                break;
            }
            default:
            {
                layoutID = -1;
                Log.d(m_LogTag, "startCatalogFragment: Unexpected SpinnerID from the fragment");
                break;
            }
        }
        if (layoutID >= 0)
        {
            LinearLayout layout = (LinearLayout) this.findViewById(layoutID);
            layout.setVisibility(View.GONE);

            m_FragmentsCounter++;
        }
    }
    @Override
    public void endCatalogFragment(int spinnerID)
    {
        int layoutID;
        switch (spinnerID)
        {
            case R.id.ModiBookSpinnerAuthor:
            {
                layoutID = R.id.ModiBookAuthorLayout;
                break;
            }
            case R.id.ModiBookSpinnerGenre:
            {
                layoutID = R.id.ModiBookGenreLayout;
                break;
            }
            default:
            {
                layoutID = -1;
                Log.d(m_LogTag, "endCatalogFragment: Unexpected SpinnerID from the fragment");
                break;
            }
        }
        if (layoutID >= 0)
        {
            LinearLayout layout = (LinearLayout) this.findViewById(layoutID);
            layout.setVisibility(View.VISIBLE);
            m_FragmentsCounter--;
        }

        if (m_FragmentsCounter <= 0) {
            ShowHideBookMenuItemsOnLaunchActivity(m_Menu);
        }
    }
    //endregion

    //region Clients
    @Override
    public void clientDataChangedEvent(ClientInfoStruct struct, RecordOperationTypeEnum type)
    {
        m_NewClientInfo = struct;
        if (m_ClientsCursor.getCount() == 0)
        {
            // 0 records? Insert!
            m_ClientRecordOperationType = RecordOperationTypeEnum.AddRecord;
        }
        else
        {
            m_ClientRecordOperationType = type;
        }

        FillSpinnerClient();
        String s = GetClientFullName(m_NewClientInfo);
        SetSpinnerSelectedItem(m_SpinnerClient, "FULLNAME", s);
        m_OthersTablesAreChanged = true;
    }
    // Hide layout on start fragment
    @Override
    public void startClientFragment()
    {
        LinearLayout layout = (LinearLayout) this.findViewById(R.id.ModiBookClientLayout);
        layout.setVisibility(View.GONE);
        layout = (LinearLayout) this.findViewById(R.id.LinearLayoutDates);
        layout.setVisibility(View.GONE);

        m_FragmentsCounter++;
    }
    @Override
    public void endClientFragment()
    {
        LinearLayout layout = (LinearLayout) this.findViewById(R.id.ModiBookClientLayout);
        layout.setVisibility(View.VISIBLE);
        layout = (LinearLayout) this.findViewById(R.id.LinearLayoutDates);
        layout.setVisibility(View.VISIBLE);

        m_FragmentsCounter--;
        if (m_FragmentsCounter <= 0)
        {
            ShowHideBookMenuItemsOnLaunchActivity(m_Menu);
        }

    }
    //endregion

    //endregion

    private void Close()
    {
        this.finish();
    }
}

package com.udelphi.librariantool;

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
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import DBTools.DBHelper;
import Tools.BookStateEnum;
import Tools.DatesWorker;
import Tools.FileSystemManager;
import DBTools.DBImagesWorker;
import Tools.ImageResize;
import Tools.MessageBox;

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
    private final String m_LogTag = BooksCursorAdapter.class.getName();

    // Current book_ID
    // null for new record
    private String m_ID;

    // Genre_ID for the book
    private String m_Genre_ID;
    // Author_ID for the book
    private String m_Author_ID;
    // Image file name for the book
    private String m_ImageFileName = null;

    // Client_ID for the book
    // Null if book is free
    private String m_Client_ID;

    // New Client_ID for the book
    // Null if book is free or if client is not changed
    private String m_NewClient_ID;

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
        }
        m_NewClient_ID = null;
        m_NewDateCheckoutBook = null;
        m_NewDateReturnBook = null;

        sql = "SELECT Book_ID, Client_ID, BookOutletDate, BookReturnDate FROM %s WHERE Book_ID = %s AND Client_ID = %s ";
        sql = String.format(sql, m_App.tblLibraryTurnover, m_ID, m_Client_ID);
        m_LibraryTurnoverCursor = m_DBAdapter.SelectSQL(sql);
        m_LibraryTurnoverCursor.moveToFirst();

        if (savedInstanceState == null)
        {
            FillControls(m_BooksCursor);
        }

        FillCatalogsSpinners();
        //region listeners
        SetImageViewClickListener();
        SetButtonEditAuthorClick();
        SetButtonEditGenreClick();
        SetButtonEditClientClick();
        SetEditTextDateListener(m_EditTextCheckoutBook);
        SetEditTextDateListener(m_EditTextReturnBook);
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
        m_Clientslayout = (View) findViewById(R.id.ModiBookSelectClientLayout);

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

    //region Fill spinners
    private void FillSpinnerAuthor()
    {
        String sql = String.format("SELECT rowid as _id, Name FROM %s ORDER BY Name", m_App.tblAuthors);
        m_AuthorsCursor = m_DBAdapter.SelectSQL(sql);
        SingleFieldCursorAdapter authorsAdapter = new SingleFieldCursorAdapter(this, m_AuthorsCursor, R.id.SelectItemTextValue, "Name");
        m_SpinnerAuthor.setAdapter(authorsAdapter);
        if (m_ID != null)
        {
            SetSpinnerSelectedItem(m_SpinnerAuthor, "NAME", m_BooksCursor.getString(m_BooksCursor.getColumnIndex("AuthorName")));
        }
    }

    private void FillSpinnerGenre()
    {
        String sql = String.format("SELECT rowid as _id, Name FROM %s ORDER BY Name", m_App.tblGenres);
        m_GenresCursor = m_DBAdapter.SelectSQL(sql);
        SingleFieldCursorAdapter genresAdapter = new SingleFieldCursorAdapter(this, m_GenresCursor, R.id.SelectItemTextValue, "Name");
        m_SpinnerGenre.setAdapter(genresAdapter);
        if (m_ID != null)
        {
            SetSpinnerSelectedItem(m_SpinnerGenre, "NAME", m_BooksCursor.getString(m_BooksCursor.getColumnIndex("GenreName")));
        }
    }

    private void FillSpinnerClient()
    {
        String sql = String.format("SELECT rowid as _id, FirstName || ' ' || LastName || ' ' || Surname as FULLNAME FROM %s ", m_App.tblClients);
        m_ClientsCursor = m_DBAdapter.SelectSQL(sql);
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
            s = cursor.getString(cursor.getColumnIndexOrThrow(fieldName));
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
    private void ShowHideBookMenuItems(Menu menu)
    {
        int i = m_BooksCursor.getCount();
        MenuItem item;
        if (i > 0)
        {
            item = menu.findItem(R.id.ModiBookCheckoutMenuItem);
            item.setVisible(m_Client_ID == null);
            item = menu.findItem(R.id.ModiBookReturnMenuItem);
            item.setVisible(m_Client_ID != null);
        }
        else
        {
            menu.findItem(R.id.ModiBookCheckoutMenuItem).setVisible(false);
            menu.findItem(R.id.ModiBookReturnMenuItem).setVisible(false);
        }
    }

    // Show book sate: book is free, book is in use etc
    private void ShowBookState()
    {
        String s = "";
        //String buttonCaption;
        int color = m_TextViewBookState.getTextColors().getDefaultColor();

        if (m_Client_ID == null)
        {
            s = getString(R.string.Modibook_BookState_isFree);
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
        Cursor cursor = (Cursor) spinner.getAdapter().getItem(spinner.getSelectedItemPosition());
        String s;
        if (cursor.getCount() > 0)
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

        boolean b = m_DBAdapter.InsertValuesByContent(m_App.tblBooks, contentValues);
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
        boolean b = m_DBAdapter.DeleteRecord(m_App.tblBooks, String.format("_ID = %s", id));
        if (!b) {
            MessageBox.Show(m_App.getApplicationContext(), "DeleteRecordBook: Can't delete record!");
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
        contentValues.put("Client_ID", m_NewClient_ID);
        contentValues.put("BookOutletDate", m_NewDateCheckoutBook);
        contentValues.put("BookReturnDate", m_NewDateReturnBook);
        boolean b = m_DBAdapter.InsertValuesByContent(m_App.tblLibraryTurnover, contentValues);
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
        contentValues.put("Client_ID", m_NewClient_ID);
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

    // Get new IDs from spinners
    private void GetCatalogIDsFromComboBoxes()
    {
        m_Author_ID = GetSpinnerSelectedItemID(m_SpinnerAuthor, "_id");
        m_Genre_ID = GetSpinnerSelectedItemID(m_SpinnerGenre, "_id");
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

    // Edit tblAuthors button click
    private void SetButtonEditAuthorClick()
    {
        m_ButtonEditAuthor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Inflate the fragment_catalog_edit
                FragmentManager fragmentManager = getFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                Fragment f = fragmentManager.findFragmentById(R.id.ModiBook_ModiAuthorFragment);
                if (f == null) {
                    // We need only one fragment
                    FragmentCatalogEdit myFragment = FragmentCatalogEdit.GetInstance(m_App.tblAuthors, GetSpinnerSelectedItemID(m_SpinnerAuthor, "_id"), "NAME", R.id.ModiBookSpinnerAuthor);
                    fragmentTransaction.add(R.id.ModiBook_ModiAuthorFragment, myFragment);
                } else {
                    fragmentTransaction.remove(f);
                }
                fragmentTransaction.commit();
            }
        });
    }

    // Edit tblGenres button click
    private void SetButtonEditGenreClick()
    {
        m_ButtonEditGenre.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Inflate the fragment_catalog_edit
                FragmentManager fragmentManager = getFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                Fragment f = fragmentManager.findFragmentById(R.id.ModiBook_ModiGenreFragment);
                if (f == null) {
                    // We need only one fragment
                    FragmentCatalogEdit myFragment = FragmentCatalogEdit.GetInstance(m_App.tblGenres, GetSpinnerSelectedItemID(m_SpinnerGenre, "_id"), "NAME", R.id.ModiBookSpinnerGenre);
                    fragmentTransaction.add(R.id.ModiBook_ModiGenreFragment, myFragment);
                } else {
                    fragmentTransaction.remove(f);
                }
                fragmentTransaction.commit();
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
                // Inflate the fragment_catalog_edit
                FragmentManager fragmentManager = getFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                Fragment f = fragmentManager.findFragmentById(R.id.ModiBook_ModiClientFragment);
                if (f == null)
                {
                    // We need only one fragment
                    FragmentClientEdit myFragment = FragmentClientEdit.GetInstance(GetSpinnerSelectedItemID(m_SpinnerClient, "_id"));
                    fragmentTransaction.add(R.id.ModiBook_ModiClientFragment, myFragment);
                }
                else
                {
                    fragmentTransaction.remove(f);
                }
                fragmentTransaction.commit();
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
        // Client
        m_NewClient_ID = GetSpinnerSelectedItemID(m_SpinnerClient, "_id");
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
        params.height = (b) ? LinearLayout.LayoutParams.WRAP_CONTENT: 0;
        m_Clientslayout.setLayoutParams(params);
        m_BookState = (b) ? BookStateEnum.BookIsInUse : BookStateEnum.BookIsFree;
    }

    // OnClickListener for date pickers
    //private void SetEditTextDateListener(final TextView editText)
    private void SetEditTextDateListener(final EditText editText)
    {
        final Context context = this;
        editText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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

                DatePickerDialog dpd = new DatePickerDialog(context, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                        String s = worker.GetStrDateFromDatePicker(view);
                        editText.setText(s);
                    }
                }, mYear, mMonth, mDay);
                dpd.show();
            }
        });
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

        m_NewClient_ID = null;
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
        boolean b = false;
        // Get IDs from catalogs tables
        GetCatalogIDsFromComboBoxes();
        if (!CheckBookFieldsBeforeSave())
        {
            return false;
        }

        // Add or update record
        b = (m_ID == null) ? AddNewRecordBook() : UpdateRecordBook();
        if (!b)
        {
            return false;
        }

        //region LibraryTurnOver table
        if (m_BookState != BookStateEnum.BookIsInUse)
        {
            // No updates for tblTurnover
            return true;
        }

        // Set NewClientID, newOutletDate, newReturnDate values
        SetTurnoverValues();


        // Client
        if (m_NewClient_ID == null)
        {
            String s = getString(R.string.ModiBook_ClientIsEmptyMsg);
            MessageBox.Show(m_App.getApplicationContext(), s);
            return false;
        }

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
        b = (m_Client_ID == null) ? AddNewRecordLibraryTurnover() : UpdateRecordLibraryTurnover();
        return b;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_modi_book, menu);
        ShowHideBookMenuItems(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
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

            case R.id.ModiBookRefreshMenuItem:
            {
                // Refresh record menu item click
                Refresh();
                break;
            }

            case R.id.ModiBookDeleteMenuItem:
            {
                // Delete record menu item click
                if (DeleteRecordBook(m_ID))
                {
                    setResult(RESULT_OK, null);
                    Close();
                }
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

    //region Save and restore instance
    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        outState.putString("ImageFileName", m_ImageFileName);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onRestoreInstanceState(Bundle outState)
    {
        super.onRestoreInstanceState(outState);
        m_ImageFileName = outState.getString("ImageFileName");
        ShowImageFromTheFile(m_ImageFileName);
    }
    //endregion

    //region Fragments edit listeners
    //region Catalogs
    @Override
    public void catalogDataChangedEvent(String s, int spinnerID) {
        Spinner spinner = (Spinner) this.findViewById(spinnerID);
        switch (spinnerID)
        {
            case R.id.ModiBookSpinnerAuthor:
            {
                FillSpinnerAuthor();
                break;
            }

            case R.id.ModiBookSpinnerGenre:
            {
                FillSpinnerGenre();
                break;
            }
            default:
            {
                Log.d(m_LogTag, "Unexpected spinnerID: catalogDataChangedEvent");
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
        int layoutID = -1;
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
        }
    }
    @Override
    public void endCatalogFragment(int spinnerID)
    {
        int layoutID = -1;
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
        }
    }
    //endregion

    //region Clients
    @Override
    public void clientDataChangedEvent(String s)
    {
        FillSpinnerClient();
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
    }
    @Override
    public void endClientFragment()
    {
        LinearLayout layout = (LinearLayout) this.findViewById(R.id.ModiBookClientLayout);
        layout.setVisibility(View.VISIBLE);
        layout = (LinearLayout) this.findViewById(R.id.LinearLayoutDates);
        layout.setVisibility(View.VISIBLE);
    }
    //endregion

    //endregion

    private void Close()
    {
        if (m_DBAdapter != null) {
            m_DBAdapter.close();
        }
        this.finish();
    }
}

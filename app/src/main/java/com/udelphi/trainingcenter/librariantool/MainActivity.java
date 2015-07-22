package com.udelphi.trainingcenter.librariantool;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AlphabetIndexer;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.SectionIndexer;
import android.widget.TextView;
import com.udelphi.trainingcenter.librariantool.DBTools.DBHelper;
import com.udelphi.trainingcenter.librariantool.DBTools.DBImagesWorker;
import com.udelphi.trainingcenter.librariantool.DBTools.DBImportDataTaskLoaderWrapper;
import com.udelphi.trainingcenter.librariantool.Tools.MessageBox;

import java.util.Arrays;

public class MainActivity extends Activity
{
    // CursorAdapter for activity
    private class BooksCursorAdapter extends CursorAdapter implements SectionIndexer
    {
        private ToolApplication m_App;
        private Context m_Context;
        private final String m_LogTag = BooksCursorAdapter.class.getName();
        private String m_Sections ="";

        AlphabetIndexer mAlphabetIndexer;

        public BooksCursorAdapter(ToolApplication app, Cursor c)
        {
            super(app.getApplicationContext(), c, 0);

            // Generate sections string for AlphabetIndexer
            m_Sections = "";
            boolean b = c.moveToFirst();
            while (b)
            {
                String s = c.getString(c.getColumnIndexOrThrow("BookName")).substring(0, 1);
                if (!m_Sections.contains(s))
                {
                    m_Sections += s;
                }
                b = c.moveToNext();
            }

            m_App = app;
            m_Context = m_App.getApplicationContext();
            mAlphabetIndexer = new AlphabetIndexer(c, c.getColumnIndexOrThrow("BookName"), m_Sections);
            //Sets a new cursor as the data set and resets the cache of indices.
            mAlphabetIndexer.setCursor(c);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent)
        {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            return inflater.inflate(R.layout.record_browse_books, parent, false);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor)
        {
            // Get visual control from the view
            TextView textView_ID = (TextView) view.findViewById(R.id.browsebook_field_id);
            TextView textView_Genre = (TextView) view.findViewById(R.id.browsebook_field_genre);
            TextView textView_Author = (TextView) view.findViewById(R.id.browsebook_field_author);
            TextView textView_BookName = (TextView) view.findViewById(R.id.browsebook_field_name);
            ImageView imageViewBookPhoto = (ImageView) view.findViewById(R.id.browsebook_field_photo);
            try
            {
                // Show text fields
                textView_ID.setText(cursor.getString(cursor.getColumnIndexOrThrow("_id")));
                textView_Genre.setText(cursor.getString(cursor.getColumnIndexOrThrow("GenreName")));
                textView_Author.setText(cursor.getString(cursor.getColumnIndexOrThrow("AuthorName")));
                textView_BookName.setText(cursor.getString(cursor.getColumnIndexOrThrow("BookName")));

                // Show the image
                DBImagesWorker image = new DBImagesWorker(m_App, cursor);
                Drawable d = image.GetImageFromDB(cursor.getString(cursor.getColumnIndexOrThrow("_id")), "Photo", false);
                imageViewBookPhoto.setImageDrawable(d);

                // Book is in use
                String s = cursor.getString(cursor.getColumnIndexOrThrow("Client_ID"));
                if (s != null)
                {
                    view.setBackgroundColor(m_Context.getResources().getColor(R.color.browseBooks_ColorBookIsInUse));
                }
            }
            catch (Exception Ex)
            {
                Log.e(m_LogTag, Ex.getMessage());
            }
        }

        // ========== SectionIndexer implementation ==========
        @Override
        public Object[] getSections()
        {
            return mAlphabetIndexer.getSections();
        }

        @Override
        public int getPositionForSection(int sectionIndex)
        {
            return mAlphabetIndexer.getPositionForSection(sectionIndex);
        }

        @Override
        public int getSectionForPosition(int position)
        {
            return mAlphabetIndexer.getSectionForPosition(position);
        }
        // ========== End of SectionIndexer implementation ==========
    }

    private View m_View;
    private ToolApplication m_App;
    private final String m_LogTag = MainActivity.class.getName();
    private DBHelper m_DBAdapter;
    private Context m_Context;
    private String m_BaseSQL;
    private String m_SQL;
    private String m_SQL_order;
    private String m_FilterCaption;
    private int m_FilterMenItemChecked = -1;
    private final int m_RequestRecordIsChanged = 1;
    private int m_ListViewSelectedItemIndex;
    Cursor m_Cursor;

    //region Visual controls
    private ListView m_ListView;
    private TextView m_TextViewRecCount;
    private TextView m_TextViewFilter;
    //endregion

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Show the EULA
        new Eula(this).show();

        m_View = getWindow().getDecorView().getRootView();
        m_Context = this;
        m_App = (ToolApplication) this.getApplicationContext();

        m_DBAdapter = new DBHelper(this, m_App.DatabaseName, null, m_App.DatabaseVerion);
        if (m_DBAdapter.IsDatabaseCreated())
        {
            // Fill created database with fake data
            if (getResources().getInteger(R.integer.debugFillDatabase) == 1)
            {
                ImportData();
            }
        }

        GetVisualControls();
        m_FilterCaption = getString(R.string.browseBooks_FilterItemCaption_ShowAllBooks);
        String sqlTemplate =
                "SELECT "+
                        "B.rowid as _id, " +
                        "B.Name as BookName, " +
                        "A.Name as AuthorName, " +
                        "G.Name as GenreName, " +
                        "B.Publishing as Publishing, " +
                        "B.BookEditionYear as BookEditionYear, " +
                        "B.Photo as Photo, " +
                        "B.Comments as Comments, " +
                        "T.Client_ID as Client_ID, " +
                        "B.Author_ID as Author_ID, " +
                        "B.Genre_ID as Genre_ID " +
                        "FROM "+
                        "%s B " +
                        "LEFT OUTER JOIN %s A ON B.Author_ID = A._ID " +
                        "LEFT OUTER JOIN %s G ON B.Genre_ID = G._ID " +
                        "LEFT OUTER JOIN %s T ON B._ID = T.Book_ID ";

        m_SQL_order =   "ORDER BY " +
                        "B.Name, " +
                        "A.Name, " +
                        "G.Name ";



                        m_BaseSQL = String.format(sqlTemplate, m_App.tblBooks, m_App.tblAuthors, m_App.tblGenres, m_App.tblLibraryTurnover);
        m_SQL = m_BaseSQL;
        m_SQL = m_SQL + m_SQL_order;
        FillListAdapter(m_SQL);

        SetListViewOnItemClick();
    }

    // Get visual controls
    private void GetVisualControls()
    {
        m_ListView = (ListView) this.findViewById(R.id.BrowseBooksListView);
        m_TextViewRecCount = (TextView) this.findViewById(R.id.BrowseBooksTextViewRecCount);
        m_TextViewFilter = (TextView) this.findViewById(R.id.BrowseBooksTextViewFilter);
    }

    // Fill ListView by CursorAdapter
    private void FillListAdapter(String sql)
    {
        m_Cursor = m_DBAdapter.SelectSQL(sql);
        BooksCursorAdapter m_CursorAdapter;
        try
        {
            m_CursorAdapter = new BooksCursorAdapter(m_App, m_Cursor);
            m_ListView.setAdapter(m_CursorAdapter);

            String s = getString(R.string.recordCountTextTemplate);
            m_TextViewRecCount.setText(String.format(s, m_Cursor.getCount()));
            m_TextViewFilter.setText(m_FilterCaption);
        }
        catch (Exception Ex)
        {
            Log.e(m_LogTag, Ex.getMessage());
        }
    }

    // Get value from the FieldID control in the current view
    // (ID from selected record)
    private String GetFieldValueByIndex(int FieldID, View view)
    {
        try
        {
            TextView text = (TextView) view.findViewById(FieldID);
            return text.getText().toString();
        }
        catch (Exception Ex)
        {
            String s = Ex.getMessage();
            Log.e(m_LogTag, s);
            MessageBox.Show(m_Context, s);
            return null;
        }
    }

    // OnItemClick LiveView
    private void SetListViewOnItemClick()
    {
        m_ListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                m_ListViewSelectedItemIndex = position;
                String fieldID = GetFieldValueByIndex(R.id.browsebook_field_id, view);
                EditBook(fieldID);
            }
        });
    }

    // Add new book or edit selected book
    private void EditBook(String id)
    {
        Intent intent = new Intent(this, ModiBookActivity.class);
        intent.putExtra("sql", m_BaseSQL);
        intent.putExtra("ID", id);
        // Get request from the activity
        startActivityForResult(intent, m_RequestRecordIsChanged);
    }

    private void SetPopupMenuItemClickListener(PopupMenu popupMenu)
    {
        final String[] strFilter = {""};
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                item.setChecked(!item.isChecked());
                m_FilterMenItemChecked = item.getItemId();
                m_FilterCaption = item.getTitle().toString();
                switch (item.getItemId()) {
                    case R.id.browseBooks_FilterItem_ShowAllBooks:
                        strFilter[0] = "";
                        break;
                    case R.id.browseBooks_FilterItem_ShowOutladBooks:
                        strFilter[0] = "Client_ID is not null";
                        break;
                    case R.id.browseBooks_FilterItem_ShowNotOutladBooks:
                        strFilter[0] = "Client_ID is null";
                        break;
                    default:
                        Log.e(m_LogTag, "PopupMenuItemClick: unexpected item");
                        break;
                }

                m_SQL = m_BaseSQL;
                if (!strFilter[0].trim().equals("")) {
                    m_SQL = m_SQL + " WHERE " + strFilter[0] + " " + m_SQL_order;
                }

                Log.d(m_LogTag, strFilter[0]);

                FillListAdapter(m_SQL);
                return true;
            }
        });
    }

    // Import tested data from csv
    // After creating of the database
    private void ImportData()
    {
        // Import data from CSV
        DBImportDataTaskLoaderWrapper loader = new DBImportDataTaskLoaderWrapper(m_Context);
        boolean b = loader.loadInBackground();
        if (b)
        {
            MessageBox.Show(getApplicationContext(), getString(R.string.DataIsImported));
        } else
        {
            String s = getString(R.string.ErrorMsg) + "\n" +  loader.getErrorMsg();
            MessageBox.Show(getApplicationContext(), s);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();
        switch (id)
        {
            case R.id.browsebook_add:
            {
                //region Add new record
                EditBook(null);
                break;
                //endregion
            }
            case R.id.browsebook_filter:
            {
                //region Filter books list
                // Inflate & launch a popup menu
                PopupMenu popupMenu = new PopupMenu(m_Context, m_View.findViewById(R.id.browsebook_filter));
                popupMenu.inflate(R.menu.browsebooks_popup_menu);

                // Set checked menu item
                if (m_FilterMenItemChecked < 0)
                {
                    // Set the 1st item as selected.
                    m_FilterMenItemChecked = popupMenu.getMenu().getItem(0).getItemId();
                }
                MenuItem mi = popupMenu.getMenu().findItem(m_FilterMenItemChecked);
                mi.setChecked(true);
                SetPopupMenuItemClickListener(popupMenu);
                popupMenu.show();
                break;
                //endregion
            }
            case R.id.action_about:
            {
                // Get app version info
                Intent intent = new Intent(this, AboutBoxActivity.class);
                // Send parameter to the AboutBoxActivity activity
                startActivity(intent);
                return true;
            }
            default:
            {
                //region default
                Log.d(m_LogTag, "Unexpected menuitem: onOptionsItemSelected");
                break;
                //endregion
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        switch (requestCode)
        {
            case m_RequestRecordIsChanged:
            {
                // We should refresh records
                if (resultCode == RESULT_OK)
                {
                    FillListAdapter(m_SQL);
                }
                m_ListView.setSelection(m_ListViewSelectedItemIndex);
                break;
            }

            default:
            {
                Log.d(m_LogTag, "Unexpected requestCode: onActivityResult");
                break;
            }
        }
    }

    @Override
    public void onDestroy()
    {
        if (m_Cursor != null)
        {
            m_Cursor.close();
        }
        if (m_DBAdapter != null)
        {
            m_DBAdapter.close();
        }
        super.onDestroy();
    }

}

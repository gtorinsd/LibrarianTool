package DBTools;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Handler;
import android.util.Log;

import com.udelphi.librariantool.R;
import com.udelphi.librariantool.ToolApplication;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import Tools.FileSystemManager;
import Tools.MessageBox;

/*
 * Created by odiomin on 12.05.2015.
 */

public class DBHelper extends SQLiteOpenHelper
{
    private ToolApplication m_App;
    private SQLiteDatabase m_Database;
    private String m_LogTag = DBHelper.class.getName();

    private String m_ErrorMsg;
    public String getErrorMsg() {
        return m_ErrorMsg;
    }

    public boolean isM_DatabaseCreated()
    {
        return m_DatabaseCreated;
    }
    private boolean m_DatabaseCreated = false;

    public DBHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version)
    {
        super(context, name, factory, version);

        m_App = (ToolApplication) context.getApplicationContext();
        m_LogTag = DBHelper.class.getName();
        m_ErrorMsg = "";
        m_Database = this.getWritableDatabase();
    }

    // Create tables
    public boolean CreateDatabase()
    {
        List<String> script = new ArrayList<String>();
        try
        {
            //region Create table scripts
            String tableName = m_App.tblGenres;
            String sql = String.format("DROP TABLE IF EXISTS %s", tableName);
            script.add(sql);
            sql = String.format("CREATE TABLE %s " +
                            "(" +
                            "_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            "NAME TEXT NOT NULL UNIQUE" +
                            ")",
                    tableName);
            script.add(sql);

            tableName = m_App.tblAuthors;
            sql = String.format("DROP TABLE IF EXISTS %s", tableName);
            script.add(sql);
            sql = String.format("CREATE TABLE %s " +
                            "(_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            "NAME TEXT NOT NULL UNIQUE" +
                            ")",
                    tableName);
            script.add(sql);

            tableName = m_App.tblBooks;
            sql = String.format("DROP TABLE IF EXISTS %s", tableName);
            script.add(sql);
            sql = String.format("CREATE TABLE %s " +
                            "(" +
                            "_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            "GENRE_ID INTEGER NOT NULL, " +
                            "AUTHOR_ID INTEGER NOT NULL, " +
                            "NAME TEXT NOT NULL, " +
                            "BOOKEDITIONYEAR INTEGER, " +
                            "PUBLISHING TEXT, " +
                            "PHOTO BLOB, " +
                            "COMMENTS BLOB, " +
                            "FOREIGN KEY(GENRE_ID) REFERENCES tblGenres (_ID) ON DELETE CASCADE, " +
                            "FOREIGN KEY(AUTHOR_ID) REFERENCES tblAuthors (_ID) ON DELETE CASCADE " +
                            ")",
                    tableName);
            script.add(sql);

            tableName = m_App.tblLibraryTurnover;
            sql = String.format("DROP TABLE IF EXISTS %s", tableName);
            script.add(sql);
            sql = String.format("CREATE TABLE %s " +
                            "(" +
                            "_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            "BOOK_ID INTEGER NOT NULL, " +
                            "CLIENT_ID INTEGER NOT NULL, " +
                            "BOOKOUTLETDATE DATE, " +
                            "BOOKRETURNDATE DATE, " +
                            "FOREIGN KEY(BOOK_ID) REFERENCES tblBooks (_ID)  ON DELETE CASCADE, " +
                            "FOREIGN KEY(Client_ID) REFERENCES tblClients (_ID)  ON DELETE CASCADE " +
                            ")",
                    tableName);
            script.add(sql);

            tableName = m_App.tblLibraryTurnoverArchive;
            sql = String.format("DROP TABLE IF EXISTS %s", tableName);
            script.add(sql);
            sql = String.format("CREATE TABLE %s " +
                            "(" +
                            "_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            "BOOK_ID INTEGER NOT NULL, " +
                            "CLIENT_ID INTEGER NOT NULL, " +
                            "BOOKOUTLETDATE DATE, " +
                            "BOOKRETURNDATE DATE " +
                            ")",
                    tableName);
            script.add(sql);

            tableName = m_App.tblClients;
            sql = String.format("DROP TABLE IF EXISTS %s", tableName);
            script.add(sql);
            sql = String.format("CREATE TABLE %s " +
                            "(" +
                            "_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            "FIRSTNAME TEXT NOT NULL, " +
                            "LASTNAME TEXT, " +
                            "SURNAME TEXT, " +
                            "ADDRESS TEXT, " +
                            "PHONE TEXT, " +
                            "COMMENTS BLOB, " +
                            "UNIQUE (FIRSTNAME, LASTNAME, SURNAME, ADDRESS, PHONE, COMMENTS) " +
                            ")",
                    tableName);
            script.add(sql);
            //endregion
            for (String s : script)
            {
                try
                {
                    m_Database.execSQL(s);
                }
                catch (Exception ex)
                {
                    m_ErrorMsg = ex.getMessage();
                    Log.e(m_LogTag, m_ErrorMsg);
                    return false;
                }
            }
        }
        catch (Exception ex)
        {
            m_ErrorMsg = ex.getMessage();
            Log.e(m_LogTag, m_ErrorMsg);
            return false;
        }
        return true;
    }

    // Execute query without results
    public boolean ExecQueryNoResults(String sql)
    {
        try
        {
            m_Database.execSQL(sql);
        }
        catch (Exception ex)
        {
            m_ErrorMsg = ex.getMessage();
            Log.e(m_LogTag, m_ErrorMsg);
            Log.e(m_LogTag, sql);
            return false;
        }
        return true;
    }

    // Execute script in transaction
    public boolean ExecScriptInTransaction(String[] sql)
    {
        m_Database.beginTransaction();
        for(int i = 0; i < sql.length; i++)
        {
            String s = sql[i];
            if (!ExecQueryNoResults(s))
            {
                return false;
            }
        }
        m_Database.setTransactionSuccessful();
        m_Database.endTransaction();
        return true;
    }

    // Insert record into the table by contentValues
    public boolean InsertValuesByContent(String tableName, ContentValues contentValues)
    {
        try
        {
            long l = m_Database.insert(tableName, null, contentValues);
            if (l < 0)
            {
                return false;
            }
        }
        catch (Exception ex)
        {
            m_ErrorMsg = ex.getMessage();
            Log.e(m_LogTag, m_ErrorMsg);
            return false;
        }
        return true;
    }

    // Update record by contentValues
    public boolean UpdateValuesByContent(String tableName, ContentValues contentValues, String idFieldName, String[] fieldValues)
    {
        try
        {
            long l = m_Database.update(tableName, contentValues, idFieldName, fieldValues);
            if (l < 0)
            {
                return false;
            }
        }
        catch (Exception Ex)
        {
            Log.e(m_LogTag, Ex.getMessage());
            return false;
        }

        return true;
    }

    // Execute query and returns a single result (like SELECT COUNT (*))
    public String ExecQueryGetSingleResult(String sql)
    {
        String result = "";
        Cursor cursor = null;
        try
        {
            cursor = m_Database.rawQuery(sql, null);
            if (cursor.moveToFirst())
            {
                int count = cursor.getCount();
                if (count > 0)
                {
                    result = cursor.getString(0);
                }
            }
        }
        catch(Exception ex)
        {
            Log.e(m_LogTag, ex.getMessage());
            Log.e(m_LogTag, sql);
        }
        finally
        {
            if (cursor != null)
            {
                cursor.close();

            }
        }
        return result;
    }

    // Execute query and return cursor with results
    public Cursor SelectSQL(String sql)
    {
        Cursor cursor = null;
        try
        {
            cursor = m_Database.rawQuery(sql, null);
        }
        catch (Exception Ex)
        {
            Log.e(m_LogTag, Ex.getMessage());
            cursor = null;
        }

        return cursor;
    }

    // Delete record from the table by expression
    public boolean DeleteRecord(String tableName, String expression)
    {
        try {
            Log.d(m_LogTag, expression);
            m_Database.delete(tableName, expression, null);
        }
        catch (Exception Ex)
        {
            Log.e(m_LogTag, Ex.getMessage());
            return false;
        }
        return true;
    }

    // Return quoted string
    public String StringQuotas(String str) {
        if (str != null)
        {
            return '"' + str + '"';
        }
        else
        {
            return str;
        }
    }

    // Create new database
    public void onCreate(SQLiteDatabase sqLiteDatabase)
    {
        Log.d(m_LogTag, "CreateDatabase");
        Context context = m_App.getApplicationContext();
        //region Create tables
        m_Database = sqLiteDatabase;
        boolean b = CreateDatabase();
        if (b)
        {
            MessageBox.Show(context, context.getString(R.string.DatabaseIsCreatedMsg));
        }
        else
        {
            // Show exception text message
            MessageBox.Show(context, getErrorMsg());
        }
        //endregion
        //region Create working directories
        FileSystemManager directoriesManager = new FileSystemManager(m_App);
        b = directoriesManager.CreateWorkingDirectories();
        if (b)
        {
            try
            {
                b = directoriesManager.CopyImagesFromAssetsToImagesDirectory();
            } catch (Exception Ex)
            {
                Ex.printStackTrace();
            }
            if (!b)
            {
                MessageBox.Show(context, context.getString(R.string.CantCreateWorkingDirectoriesMsg));
            }
        }
        //endregion

        m_DatabaseCreated = true;
    }

    // Update an existing database
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1)
    {
        Log.d(m_LogTag, "UpgradeDatabase");
    }
}

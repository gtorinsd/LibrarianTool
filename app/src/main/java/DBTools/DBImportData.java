package DBTools;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import com.udelphi.librariantool.ToolApplication;
import com.udelphi.librariantool.R;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/*
 * Created by ODiomin on 12.05.2015.
 */

// Import data from the csv
public class DBImportData
{
    private ToolApplication m_App;
    private Context m_Context;
    private String m_LogTag = DBImportData.class.getName();
    private String m_StrErrorMsg;
    public String getErrorMsg() {
        return m_StrErrorMsg;
    }

    public DBImportData(Context context)
    {
        m_App = (ToolApplication) context.getApplicationContext();
        m_Context = context;
        m_StrErrorMsg = "";
    }

    // Import data for all DBtables
    public boolean ImportAll()
    {
        m_StrErrorMsg = "";
        boolean b = ImportGenres();
        if (b) {
            b = ImportAuthors();
        }
        if (b) {
            b = ImportBooks();
        }
        if (b) {
            b = ImportClients();
        }

        if (b) {
            b = ImportLibraryTurnOver();
        }

        return b;
    }

    // Genres
    public boolean ImportGenres()
    {
        String fileSource = m_App.FileSourceGenres;
        String tableName = m_App.tblGenres;
        DBHelper adapter = new DBHelper(m_Context, m_App.DatabaseName, null, m_App.DatabaseVerion);
        // Get file from the asset resource
        AssetManager manager = m_Context.getAssets();

        try
        {
            int importedCount = 0;
            int allCount = 0;
            BufferedReader bufferReader = null;
            try
            {
                bufferReader = new BufferedReader(new InputStreamReader(manager.open(fileSource)));
                String line;
                while ((line = bufferReader.readLine()) != null)
                {
                    allCount++;
                    String s = line.replace(";","");

                    //region Get count of records with name = "s"
                    String sql= String.format("SELECT COUNT(*) FROM %s WHERE NAME = \"%s\";", tableName, s);
                    String strCount = adapter.ExecQueryGetSingleResult(sql);
                    if (!strCount.trim().isEmpty())
                    {
                        int i = Integer.parseInt(strCount);
                        if (i > 0)
                        {
                            // This record should be skipped
                            //Log.d(ImportData.class.getName(), s + " is exists. Skipped.");
                            continue;
                        }
                    }
                    //endregion

                    ContentValues contentValues = new ContentValues();
                    contentValues.put("Name", s);
                    if (!adapter.InsertValuesByContent(tableName, contentValues))
                    {
                        m_StrErrorMsg = adapter.getErrorMsg();
                        Log.e(m_LogTag, m_StrErrorMsg);
                        return false;
                    }

                    importedCount++;
                }
            }
            finally
            {
                adapter.close();
                if (bufferReader!= null)
                {
                    bufferReader.close();
                }
                String s = String.format(m_Context.getString(R.string.ImportDataDisplayFormat), tableName, importedCount, allCount);
                Log.d(DBImportData.class.getName(), s);
            }
        }
        catch (Exception Ex) {
            m_StrErrorMsg = Ex.getMessage();
            return false;
        }

        return true;
    }

    // Authors
    public boolean ImportAuthors()
    {
        String fileSource = m_App.FileSourceAuthors;
        String tableName = m_App.tblAuthors;
        DBHelper adapter = new DBHelper(m_Context, m_App.DatabaseName, null, m_App.DatabaseVerion);
        // Get file from the asset resource
        AssetManager manager = m_Context.getAssets();
        try
        {
            int importedCount = 0;
            int allCount = 0;
            BufferedReader bufferReader = null;
            try
            {
                bufferReader = new BufferedReader(new InputStreamReader(manager.open(fileSource)));
                String line;
                while ((line = bufferReader.readLine()) != null)
                {
                    allCount++;
                    String s = adapter.StringQuotas(line.replace(";", "").trim());

                    //region Get count of records with name = "s"
                    String sql = String.format("SELECT COUNT(*) FROM %s WHERE NAME = %s;", tableName, s);
                    String strCount = adapter.ExecQueryGetSingleResult(sql);
                    if (!strCount.trim().isEmpty())
                    {
                        int i = Integer.parseInt(strCount);
                        if (i > 0)
                        {
                            // This reccord should be skipped
                            //Log.d(ImportData.class.getName(), s + " is exists. Skipped.");
                            continue;
                        }
                    }
                    //endregion

                    sql = String.format("INSERT INTO %s (Name) VALUES (%s);", tableName, s);
                    if (!adapter.ExecQueryNoResults(sql))
                    {
                        m_StrErrorMsg = adapter.getErrorMsg();
                        return false;
                    }
                    importedCount++;
                }
            }
            finally
            {
                adapter.close();
                if (bufferReader!= null)
                {
                    bufferReader.close();
                }

                String s = String.format(m_Context.getString(R.string.ImportDataDisplayFormat), tableName, importedCount, allCount);
                Log.d(DBImportData.class.getName(), s);
            }
        }
        catch (Exception Ex)
        {
            m_StrErrorMsg = Ex.getMessage();
            return false;
        }

        return true;
    }

    // Books
    public boolean ImportBooks()
    {
        String fileSource = m_App.FileSourceBooks;
        String tableName = m_App.tblBooks;
        DBHelper adapter = new DBHelper(m_Context, m_App.DatabaseName, null, m_App.DatabaseVerion);
        // Get file from the asset resource
        AssetManager manager = m_Context.getAssets();
        try
        {
            int importedCount = 0;
            int allCount = 0;
            BufferedReader bufferReader = null;
            try
            {
                bufferReader = new BufferedReader(new InputStreamReader(manager.open(fileSource)));
                String line;
                while ((line = bufferReader.readLine()) != null)
                {
                    allCount++;
                    String s = line.trim();
                    // We have aaa;bbb;ccc;ddd;eee;fff;
                    // Remove last ";"
                    if (s.endsWith(";"))
                    {
                        s = s.substring(0, s.length() - 1);
                    }
                    String[] fields = s.split(";");
                    String sql;

                    //region Get count of records with name = "s"
                    String template = "SELECT COUNT(*) FROM " + tableName + " WHERE (NAME = \"%s\") AND (BookEditionYear = %s) AND (Publishing = \"%s\") AND (Comments = \"%s\")";
                    sql = String.format(template, fields[2], fields[3], fields[4], fields[5]);
                    String strCount = adapter.ExecQueryGetSingleResult(sql);
                    if (!strCount.trim().isEmpty())
                    {
                        int i = Integer.parseInt(strCount);
                        if (i > 0)
                        {
                            // This record should be skipped
                            //Log.d(ImportData.class.getName(), fields[2] + " is exists. Skipped.");
                            continue;
                        }
                    }
                    //endregion

                    if (fields.length > 6)
                    {

                        Log.d("=====", fields[6]);
                        // Insert with a picture
                        template = "INSERT INTO " + tableName + "(Genre_ID, Author_ID, Name, BookEditionYear, Publishing, Comments, Photo) VALUES (%s, %s, \"%s\", %s, \"%s\", \"%s\", \"%s\");";
                        sql = String.format(template, fields[0], fields[1], fields[2], fields[3], fields[4], fields[5], fields[6]);
                    }
                    else
                    {
                        Log.d("=====", "no picture");
                        // Insert a null instead a photo
                        template = "INSERT INTO " + tableName + "(Genre_ID, Author_ID, Name, BookEditionYear, Publishing, Comments) VALUES (%s, %s, \"%s\", %s, \"%s\", \"%s\");";
                        sql = String.format(template, fields[0], fields[1], fields[2], fields[3], fields[4], fields[5]);
                    }

                    boolean b = adapter.ExecQueryNoResults(sql);
                    if (!b)
                    {
                        m_StrErrorMsg = adapter.getErrorMsg();
                        return false;
                    }
                    importedCount++;
                }
            }
            finally
            {
                adapter.close();
                if (bufferReader!= null)
                {
                    bufferReader.close();
                }

                String s = String.format(m_Context.getString(R.string.ImportDataDisplayFormat), tableName, importedCount, allCount);
                Log.d(DBImportData.class.getName(), s);
            }
        }
        catch (Exception Ex)
        {
            m_StrErrorMsg = Ex.getMessage();
            return false;
        }

        return true;
    }

    // Clients
    public boolean ImportClients()
    {
        String fileSource = m_App.FileSourceClients;
        String tableName = m_App.tblClients;
        DBHelper adapter = new DBHelper(m_Context, m_App.DatabaseName, null, m_App.DatabaseVerion);
        // Get file from the asset resource
        AssetManager manager = m_Context.getAssets();
        try
        {
            int importedCount = 0;
            int allCount = 0;
            BufferedReader bufferReader = null;
            try
            {
                bufferReader = new BufferedReader(new InputStreamReader(manager.open(fileSource)));
                String line;
                while ((line = bufferReader.readLine()) != null)
                {
                    allCount++;
                    String s = line.trim();
                    // We have aaa;bbb;ccc;ddd;eee;fff;
                    // Remove last ";"
                    if (s.endsWith(";"))
                    {
                        s = s.substring(0, s.length() - 1);
                    }
                    String[] fields = s.split(";");
                    String sql;
                    String template;

                    //region Get count of records with name = "s"
                    template = "SELECT COUNT(*) FROM " + tableName + " WHERE (FirstName = \"%s\") AND (LastName = \"%s\") AND (Surname = \"%s\") AND (Address = \"%s\") AND (Phone = \"%s\")  AND (Comments = \"%s\")";
                    sql = String.format(template, fields[0], fields[1], fields[2], fields[3], fields[4], fields[5]);
                    String strCount = adapter.ExecQueryGetSingleResult(sql);
                    if (!strCount.trim().isEmpty())
                    {
                        int i = Integer.parseInt(strCount);
                        if (i > 0)
                        {
                            // This record should be skipped
                            //Log.d(ImportData.class.getName(), fields[0] + " is exists. Skipped.");
                            continue;
                        }
                    }
                    //endregion

                    template = "INSERT INTO " + tableName + "(FirstName, LastName, Surname, Address, Phone, Comments) VALUES (\"%s\", \"%s\", \"%s\", \"%s\", \"%s\", \"%s\");" ;
                    sql = String.format(template, fields[0], fields[1], fields[2], fields[3], fields[4], fields[5]);
                    if (!adapter.ExecQueryNoResults(sql))
                    {
                        m_StrErrorMsg = adapter.getErrorMsg();
                        return false;
                    }
                    importedCount++;
                }
            }
            finally
            {
                adapter.close();
                if (bufferReader!= null)
                {
                    bufferReader.close();
                }

                String s = String.format(m_Context.getString(R.string.ImportDataDisplayFormat), tableName, importedCount, allCount);
                Log.d(DBImportData.class.getName(), s);
            }
        }
        catch (Exception Ex) {
            m_StrErrorMsg = Ex.getMessage();
            return false;
        }

        return true;
    }

    // LibraryTurnOver
    public boolean ImportLibraryTurnOver()
    {
        String fileSource = m_App.FileSourceLibraryTurnover;
        String tableName = m_App.tblLibraryTurnover;
        DBHelper adapter = new DBHelper(m_Context, m_App.DatabaseName, null, m_App.DatabaseVerion);
        // Get file from the asset resource
        AssetManager manager = m_Context.getAssets();
        try
        {
            int importedCount = 0;
            int allCount = 0;
            BufferedReader bufferReader = null;
            try
            {
                bufferReader = new BufferedReader(new InputStreamReader(manager.open(fileSource)));
                String line;
                while ((line = bufferReader.readLine()) != null)
                {
                    allCount++;
                    String s = line.trim();
                    // We have aaa;bbb;ccc;ddd;eee;fff;
                    // Remove last ";"
                    if (s.endsWith(";"))
                    {
                        s = s.substring(0, s.length() - 1);
                    }
                    String[] fields = s.split(";");
                    String sql;
                    String template;

                    //region Get count of records with name = "s"
                    template = "SELECT COUNT(*) FROM " + tableName + " WHERE (Book_ID = \"%s\") AND (Client_ID = \"%s\") AND (BookOutletDate = \"%s\") AND (BookReturnDate = \"%s\")";
                    sql = String.format(template, fields[0], fields[1], fields[2], fields[3]);
                    String strCount = adapter.ExecQueryGetSingleResult(sql);
                    if (!strCount.trim().isEmpty())
                    {
                        int i = Integer.parseInt(strCount);
                        if (i > 0)
                        {
                            // This record should be skipped
                            //Log.d(ImportData.class.getName(), fields[0] + " is exists. Skipped.");
                            continue;
                        }
                    }
                    //endregion

                    template = "INSERT INTO " + tableName + "(Book_ID, Client_ID, BookOutletDate, BookReturnDate) VALUES (\"%s\", \"%s\", \"%s\", \"%s\");" ;
                    sql = String.format(template, fields[0], fields[1], fields[2], fields[3]);
                    if (!adapter.ExecQueryNoResults(sql))
                    {
                        m_StrErrorMsg = adapter.getErrorMsg();
                        return false;
                    }
                    importedCount++;
                }
            }
            finally
            {
                adapter.close();
                if (bufferReader!= null)
                {
                    bufferReader.close();
                }

                String s = String.format(m_Context.getString(R.string.ImportDataDisplayFormat), tableName, importedCount, allCount);
                Log.d(DBImportData.class.getName(), s);
            }
        }
        catch (Exception Ex) {
            m_StrErrorMsg = Ex.getMessage();
            return false;
        }

        return true;
    }
}

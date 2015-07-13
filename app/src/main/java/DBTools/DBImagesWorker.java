package DBTools;

import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import com.udelphi.librariantool.R;
import com.udelphi.librariantool.ToolApplication;
import java.io.FileInputStream;
import java.io.InputStream;

import Tools.FileSystemManager;

/*
 * Created by ODiomin on 15.06.2015.
 */

// Working with images
public class DBImagesWorker
{
    private ToolApplication m_App;
    private Cursor m_Cursor;
    private String m_LogTag = DBImagesWorker.class.getName();

    public DBImagesWorker(ToolApplication app, Cursor cursor)
    {
        m_App = app;
        m_Cursor = cursor;
    }

    // Return an image from the DBTable
    public Drawable GetImageFromDB(String id, String imageFieldName, boolean returnPlus)
    {
        Drawable d = null;

        if (id == null)
        {
            // New record
            d = ContextCompat.getDrawable(m_App.getApplicationContext(), R.drawable.add_empty);
        }
        else
        {
            String imageFileName = m_Cursor.getString(m_Cursor.getColumnIndexOrThrow(imageFieldName));
            if (imageFileName == null)
            {
                // Empty image field
                int i = 0;
                if (returnPlus)
                {
                    i = R.drawable.add_empty;
                }
                else
                {
                    i = R.drawable.empty;
                }

                d = ContextCompat.getDrawable(m_App.getApplicationContext(), i);
            }
            else
            {
                try
                {
                    d = DBImagesWorker.GetImageFromTheFile(m_App, imageFileName);
                }
                catch (Exception Ex)
                {
                    Log.e(m_LogTag, Ex.getMessage());
                }
            }
        }
        return d;
    }

    // Return an image from the file
    public static Drawable GetImageFromTheFile(ToolApplication app,  String fileName)
    {
        String fullImageFileName = String.format("/%s/%s", FileSystemManager.GetImagesDirectoryPath(app), fileName);
        Drawable d = null;
        InputStream ims = null;
        try
        {
            try
            {
                ims = new FileInputStream(fullImageFileName);
                d = Drawable.createFromStream(ims, null);
            }
            finally
            {
                if (ims != null)
                {
                    ims.close();
                }
            }
        }
        catch (Exception e)
        {

            Log.e(DBImagesWorker.class.getName(), e.getMessage());
        }
        return d;
    }
}

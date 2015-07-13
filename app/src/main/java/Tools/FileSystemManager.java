package Tools;

import android.content.ContentResolver;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;
import com.udelphi.librariantool.R;
import com.udelphi.librariantool.ToolApplication;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/*
 * Created by ODiomin on 04.06.2015.
 */

// Work with file system
public class FileSystemManager
{
    private final String m_LogTag = FileSystemManager.class.getName();
    private ToolApplication m_App;
    // Directory for photos
    private String m_BookPhotosDirectory;

    // Copying buffer size
    private final int m_CopyBufferSize = 1024;

    public FileSystemManager(ToolApplication app)
    {
        m_App = app;
        m_BookPhotosDirectory = app.getResources().getString(R.string.BookPhotosDirectory);
    }

    // Create working directories
    public boolean CreateWorkingDirectories()
    {
        return CreateBookPhotosDirectory();
    }

    // Remove Photos directory and create it.
    // All files from the photo directory will be lost
    private boolean CreateBookPhotosDirectory()
    {
        try
        {
            File bookImagesDir = new File(GetImagesDirectoryPath(m_App));
            if (bookImagesDir.exists())
            {
                // Clear directory
                Log.d(m_LogTag, "Start to remove " + m_BookPhotosDirectory);
                try
                {
                    for (File file : bookImagesDir.listFiles())
                    {
                        if (!file.delete())
                        {
                            Log.e(m_LogTag, "Failed to delete " + file.getName());
                        }
                    }
                }
                finally
                {
                    Log.d(m_LogTag, "Finish");
                }
            }
            else
            {
                // Create directory
                Log.d(m_LogTag, "Create " + m_BookPhotosDirectory);
                if (!bookImagesDir.mkdir())
                {
                    Log.e(m_LogTag, "Failed to create " + bookImagesDir.getName());
                }
            }

        }
        catch (Exception Ex)
        {
            Log.e(m_LogTag, Ex.getMessage());
            return false;
        }

        return true;
    }

    // Copy file
    private boolean CopyFile(String source, String target) throws IOException
    {
        InputStream in = null;
        OutputStream out = null;
        try
        {
            in = m_App.getAssets().open(source);
            out = new FileOutputStream(target);
            // Copy the bits from instream to outstream
            byte[] buf = new byte[m_CopyBufferSize];
            int len;
            while ((len = in.read(buf)) > 0)
            {
                out.write(buf, 0, len);
            }
        }
        catch (IOException e)
        {
            Log.e("", e.getMessage());
            return false;
        }
        finally
        {
            if (in != null)
            {
                in.close();
            }
            if (out != null)
            {
                out.close();
            }
        }

        return true;
    }

    // Copy images from the assets to the images directory
    // Files masks are hardcoded
    public boolean CopyImagesFromAssetsToImagesDirectory() throws IOException
    {
        //region Get images from the Assets directory
        // Get files list from the Assets directory
        AssetManager assetManager = m_App.getAssets();
        String[] list = assetManager.list("");

        //region Images masks
        List<String> imagesMaskList = new ArrayList<String>();
        imagesMaskList.add(".PNG");
        imagesMaskList.add(".JPG");
        imagesMaskList.add(".JPEG");
        imagesMaskList.add(".BMP");
        imagesMaskList.add(".ICO");
        //endregion

        //region Final images list to copy
        List<String> imagesList = new ArrayList<String>();
        // Get images my files extensions
        for (String s: list)
        {
            // Get file extension
            String[] fileNameArray =  s.split("\\.");
            if (fileNameArray.length > 1)
            {
                String fileExt = "." + fileNameArray[1];
                if (imagesMaskList.indexOf(fileExt.toUpperCase()) >= 0)
                {
                    imagesList.add(s);
                }
            }
        }
        //endregion

        //region Copy files from the Assets directory into the images directory
        for(String s : imagesList)
        {
            String targetFile = GetImagesDirectoryPath(m_App) + "/" + s;
            if (!CopyFile(s, targetFile))
            {
                return  false;
            }
        }
        //endregion

        return true;
    }

    // Copy image from the URI to images directory
    public String CopyImageFromUriToImagesDirectory(Uri sourceUri)
    {
        InputStream inputStream;
        OutputStream outputStream;

        // Generate new file name
        String fullPath = sourceUri.toString();
        int index = fullPath.lastIndexOf("/");
        String fileName = fullPath.substring(index + 1) + ".png";
        String targetFile = GetImagesDirectoryPath(m_App) +  "/" + fileName;
        try
        {
            ContentResolver content = m_App.getApplicationContext().getContentResolver();
            inputStream = content.openInputStream(sourceUri);
            outputStream = new FileOutputStream(targetFile);

            byte[] buf = new byte[m_CopyBufferSize];
            while ((inputStream.read(buf)) >= 0)
            {
                outputStream.write(buf, 0, buf.length);
            }
        }
        catch ( Exception Ex)
        {
            Log.e(m_LogTag, Ex.getMessage());
            return null;
        }

        return fileName;
    }

    // Returns an images directory name
    public static String GetImagesDirectoryPath(ToolApplication app)
    {
        return app.getApplicationInfo().dataDir + "/" + app.getResources().getString(R.string.BookPhotosDirectory);
    }

    // Save bitmap into the imsges directory
    public static String SaveImageFromBitmap(ToolApplication app,  Bitmap bitmap)
    {
        // Generate new file name
        String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
        String fileName = "image_" + timeStamp + ".png";
        if (bitmap != null)
        {
            OutputStream outStream;
            File file = new File(GetImagesDirectoryPath(app) + "/" + fileName);
            try
            {
                outStream = new FileOutputStream(file);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream);
                outStream.flush();
                outStream.close();
            }
            catch (Exception Ex)
            {
                Log.e(FileSystemManager.class.getName(), Ex.getMessage());
            }
        }

        return fileName;
    }
}
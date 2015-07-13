package Tools;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import com.udelphi.librariantool.R;
import com.udelphi.librariantool.ToolApplication;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

/**
 * Created by ODiomin on 16.06.2015.
 */

// Resize image
public class ImageResize
{
    private ToolApplication m_App;
    private final String m_LogTag = ImageResize.class.getName();
    public ImageResize(ToolApplication app)
    {
        m_App = app;
    }

    // Resize image
    public boolean DoImageResize(String fileName)
    {
        // Get current screen size in pixels
        Display display = ((WindowManager) m_App.getApplicationContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        Point size = new Point();
        display.getSize(size);
        int screenWidth = size.x;
        int screenHeight = size.y;

        // Image size limitation is screenWidth / 3, screenHeight / 3
        Bitmap bitmap = ImageResizeInternal(fileName, screenWidth / 3, screenHeight / 3);
        if (bitmap != null)
        {
            OutputStream outStream;
            File file = new File(fileName);
            try
            {
                outStream = new FileOutputStream(file);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream);
                outStream.flush();
                outStream.close();
            }
            catch (Exception Ex)
            {
                Log.e(m_LogTag, Ex.getMessage());
                return false;
            }
        }
        return true;
    }

    private int CalculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight)
    {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth)
        {
            // Calculate ratios of height and width to requested height and width
            final int heightRatio = Math.round((float) height / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);

            // Choose the smallest ratio as inSampleSize value, this will guarantee
            // a final image with both dimensions larger than or equal to the
            // requested height and width.
            if (heightRatio < widthRatio)
            {
                inSampleSize = heightRatio;
            }
            else
            {
                inSampleSize = widthRatio;
            }
        }
        return inSampleSize;
    }

    private Bitmap ImageResizeInternal(String pathToFile, int width, int height)
    {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(pathToFile, options);

        // Calculate inSampleSize
        options.inSampleSize = CalculateInSampleSize(options, width, height);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(pathToFile, options);
    }
}

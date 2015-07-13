package Tools;

import android.content.Context;
import android.widget.Toast;

/*
 * Created by ODiomin on 12.05.2015.
 */

public class MessageBox
{
    public static void Show(Context context,  String message)
    {
        Toast toast = Toast.makeText(context, message, Toast.LENGTH_LONG);
        toast.show();
    }
}
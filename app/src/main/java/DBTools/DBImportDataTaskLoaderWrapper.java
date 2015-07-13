package DBTools;

import android.content.AsyncTaskLoader;
import android.content.Context;

/*
 * Created by ODiomin on 19.05.2015.
 */


// AsyncTaskWrapper for DBImportData
public class DBImportDataTaskLoaderWrapper extends AsyncTaskLoader<Boolean>
{
    private Context m_Context;

    private String m_StrErrorMsg;
    public String getErrorMsg()
    {
        return m_StrErrorMsg;
    }

    public DBImportDataTaskLoaderWrapper(Context context)
    {
        super(context);
        m_Context = context;
    }

    @Override
    public Boolean loadInBackground()
    {
        m_StrErrorMsg = "";
        DBImportData importData = new DBImportData(m_Context);
        boolean result = importData.ImportAll();
        if (!result)
        {
            m_StrErrorMsg = importData.getErrorMsg();
        }
        return result;
    }
}

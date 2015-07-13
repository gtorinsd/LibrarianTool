package Tools;

import android.util.Log;
import android.widget.DatePicker;

import com.udelphi.librariantool.ToolApplication;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

/*
 * Created by ODiomin on 26.06.2015.
 */

// Worker with dates
public class DatesWorker
{
    final private String m_LogTag = DatesWorker.class.getName();
    // Current date format
    private DateFormat m_dateFormat;
    private int year;
    private int month;
    private int day;

    public DatesWorker(ToolApplication app)
    {
        m_dateFormat = android.text.format.DateFormat.getDateFormat(app.getApplicationContext());
    }

    // Inits calendar by current date
    public Calendar InitCalendarCurrentDate()
    {
        return Calendar.getInstance();
    }

    // Adds days to calendar
    public void AddCalendarDay(Calendar calendar, int daysCount)
    {
        year = calendar.get(Calendar.YEAR);
        month = calendar.get(Calendar.MONTH);
        day = calendar.get(Calendar.DAY_OF_MONTH);
        calendar.set(year, month, day + daysCount);
    }

    // Gets date from the DatePicker and returns it as String
    public String GetStrDateFromDatePicker(DatePicker picker)
    {
        Calendar calendar = InitCalendarCurrentDate();
        day = picker.getDayOfMonth();
        month = picker.getMonth();
        year = picker.getYear();
        calendar.set(year, month, day);
        Date date = calendar.getTime();
        return m_dateFormat.format(date);
    }

    // Sets Date from the String into the Calendar
    public void SetStrDateToCalendar(Calendar calendar, String strDate)
    {
        Date date = StrToDate(strDate);
        if (date != null)
        {
            calendar.setTime(date);
        }
    }

    // Gets date from the Calendar and returns it as String
    public String GetStrDateFromCalendar(Calendar calendar)
    {
        Date date = calendar.getTime();
        return m_dateFormat.format(date);
    }

    // Returns Date from Str
    public Date StrToDate(String strDate)
    {
        Date date = null;
        try
        {
            date = m_dateFormat.parse(strDate);
        }
        catch (Exception Ex)
        {
            Log.e(m_LogTag, Ex.getMessage());
        }
        return date;
    }
}

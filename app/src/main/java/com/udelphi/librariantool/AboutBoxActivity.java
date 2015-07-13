package com.udelphi.librariantool;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

public class AboutBoxActivity extends Activity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        //Remove title bar
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_about_box);
    }

    private PackageInfo GetPackageInfo()
    {
        PackageInfo packageInfo = null;
        try
        {
            packageInfo = this.getPackageManager().getPackageInfo(this.getPackageName(), PackageManager.GET_ACTIVITIES);
        }
        catch (PackageManager.NameNotFoundException e)
        {
            e.printStackTrace();
        }
        return packageInfo;
    }

    @Override
    protected void onStart()
    {
        super.onStart();

        PackageInfo packageInfo = GetPackageInfo();
        String s = this.getString(R.string.app_name) + " " + packageInfo.versionName;
        TextView text = (TextView) findViewById(R.id.textViewAppName);
        text.setText(s);
    }
}

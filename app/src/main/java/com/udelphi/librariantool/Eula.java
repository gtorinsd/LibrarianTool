package com.udelphi.librariantool;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.view.View;

/*
 * Created by ODiomin on 28.04.2015.
 */

public class Eula {

    private final String EULA_PREFIX = "eula_";
    private Activity m_Activity;

    public Eula(Activity context)
    {
        m_Activity = context;
    }

    private PackageInfo getPackageInfo()
    {
        PackageInfo packageInfo = null;
        try
        {
            packageInfo = m_Activity.getPackageManager().getPackageInfo(m_Activity.getPackageName(), PackageManager.GET_ACTIVITIES);
        }
        catch (PackageManager.NameNotFoundException e)
        {
            e.printStackTrace();
        }
        return packageInfo;
    }

    public void show()
    {
        PackageInfo versionInfo = getPackageInfo();

        // the eulaKey changes every time you increment the version number in the AndroidManifest.xml
        final String eulaKey = EULA_PREFIX + versionInfo.versionCode;
        final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(m_Activity);
        boolean flShowed = sp.getBoolean(eulaKey, false);
        if(!flShowed)
        {
            // Show the Eula
            String title = m_Activity.getString(R.string.app_name) + " " + versionInfo.versionName + " EULA";
            // Join "updates" and "eula", if "updates" resource are not empty
            String message = m_Activity.getString(R.string.eula);
            String updates = m_Activity.getString(R.string.updates).trim();
            if (!updates.equals(""))
            {
                message = updates + "\n\n" + message;
            }

            // Show the dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(m_Activity)
                    .setTitle(title)
                    .setMessage(message)
                    .setCancelable(false)
                    .setPositiveButton(android.R.string.ok, new Dialog.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i)
                        {
                            // Mark this version as read.
                            SharedPreferences.Editor referenceEditor = sp.edit();
                            referenceEditor.putBoolean(eulaKey, true);
                            referenceEditor.commit();
                            dialogInterface.dismiss();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, new Dialog.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {
                            // Close the activity as they have declined the EULA
                            m_Activity.finish();
                        }

                    });

            builder.create().show();
        }
    }
}
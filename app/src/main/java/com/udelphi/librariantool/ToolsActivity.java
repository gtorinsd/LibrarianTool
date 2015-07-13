package com.udelphi.librariantool;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import java.io.IOException;

import DBTools.DBImportDataTaskLoaderWrapper;
import Tools.FileSystemManager;
import Tools.MessageBox;
import DBTools.DBHelper;

public class ToolsActivity extends Activity
{
    private Button m_btnCreateDB;
    private Button m_btnImportDB;
    private DBHelper m_DBAdapter;
    private View m_View;
    private Context m_Context;
    private ToolApplication m_App;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tools);

        // Get activity & application context
        m_App = (ToolApplication) this.getApplicationContext();

        m_btnCreateDB = (Button) findViewById(R.id.btnCreateDB);
        m_btnImportDB = (Button) findViewById(R.id.btnImportDB);
        m_View = getWindow().getDecorView().getRootView();
        m_DBAdapter = new DBHelper(this, m_App.DatabaseName, null, m_App.DatabaseVerion);
        m_Context = this;

        SetCreateDBButtonClickListener();
        SetImportDataButtonClickListener();

        m_btnCreateDB.setClickable(true);
        m_btnImportDB.setClickable(true);
    }

    private void SetButtonsEnabled(Button[] buttons, final boolean enabled)
    {
        for (Button button : buttons)
        {
            button.setEnabled(enabled);
        }
    }

    private void SetCreateDBButtonClickListener()
    {
        m_btnCreateDB.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(m_Context);
                builder.setMessage(getString(R.string.ConfirmCreateDatabaseMsg));
                builder.setTitle(R.string.WarningMsgCaption);
                builder.setCancelable(false);
                builder.setPositiveButton(R.string.btnOK, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                try {
                                    boolean b = m_DBAdapter.CreateDatabase();
                                    if (b)
                                    {
                                        MessageBox.Show(getApplicationContext(), getString(R.string.DatabaseIsCreatedMsg));
                                    }
                                    else
                                    {
                                        // Show exception text message
                                        MessageBox.Show(getApplicationContext(), m_DBAdapter.getErrorMsg());
                                    }

                                    // Create working directories
                                    FileSystemManager directoriesManager = new FileSystemManager(m_App);
                                    b = directoriesManager.CreateWorkingDirectories();
                                    if (b)
                                    {
                                        b = directoriesManager.CopyImagesFromAssetsToImagesDirectory();
                                        if (!b)
                                        {
                                            MessageBox.Show(m_Context, "");
                                        }
                                    }

                                }
                                catch (IOException e)
                                {
                                    e.printStackTrace();
                                }
                                finally
                                {
                                    dialog.dismiss();
                                }
                            }
                        }
                );
                builder.setNegativeButton(R.string.btnCancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        }
                );

                AlertDialog alert = builder.create();
                alert.show();

            }
        });
    }

    private void SetImportDataButtonClickListener()
    {
        m_btnImportDB.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(m_Context);
                builder.setMessage(getString(R.string.ConfirmImportDataMsg));
                builder.setTitle(R.string.WarningMsgCaption);
                builder.setCancelable(false);
                builder.setPositiveButton(R.string.btnOK, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                boolean b = false;
                                try
                                {
                                    //===============================================================================================
                                    DBImportDataTaskLoaderWrapper loader = new DBImportDataTaskLoaderWrapper(m_Context);
                                    b = loader.loadInBackground();
                                    if (b) {
                                        MessageBox.Show(getApplicationContext(), getString(R.string.DataIsImported));
                                    } else {
                                        String s = getString(R.string.ErrorMsg) + "\n" +  loader.getErrorMsg();
                                        MessageBox.Show(getApplicationContext(), s);
                                    }
                                    //===============================================================================================
                                }
                                finally {
                                    dialog.dismiss();
                                }
                            }
                        }
                );
                builder.setNegativeButton(R.string.btnCancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        }
                );

                AlertDialog alert = builder.create();
                alert.show();
            }
        });
    }

    @Override
    protected void onStart()
    {
        super.onStart();
    }
}

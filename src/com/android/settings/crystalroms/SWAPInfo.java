/*
 * Copyright (C) 2012 nobodyAtall @ xda
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.crystalroms;

import com.android.settings.R;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Config;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.File;
import android.os.Environment;

import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;
import com.android.settings.Utils;

import java.io.DataOutputStream;
import java.io.DataInputStream;

public class SWAPInfo extends AlertActivity {

    private static final String TAG = "SWAPSettings";

    private static final String SWAPINFO_PATH = "/data/swapinfo.log";

    private static final String SWAP_FILE = "mnt/external_sd/.CrystalPA.swp";

    private static final String SWAP_ENABLED_PROP = "persist.sys.swap.enabled";

    private static final String SWAP_SIZE_PROP = "persist.sys.swap.size";

    private Context myContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        myContext = getApplicationContext();
        getSwapInfo();

        InputStreamReader inputReader = null;
        StringBuilder data = null;
        try {
            data = new StringBuilder(2048);
            char tmp[] = new char[2048];
            int numRead;
            mrunShellCommand.join();
            inputReader = new FileReader(SWAPINFO_PATH);
            while ((numRead = inputReader.read(tmp)) >= 0) {
                data.append(tmp, 0, numRead);
            }
        } catch (IOException e) {
            showErrorAndFinish();
            return;
        } catch (InterruptedException e) {
            showErrorAndFinish();
            return;  
        } finally {
            try {
                if (inputReader != null) {
                    inputReader.close();
                }
            } catch (IOException e) {
                 showErrorAndFinish();
            }
        }

        if (TextUtils.isEmpty(data)) {
            showErrorAndFinish();
            return;
        }

        WebView webView = new WebView(this);

        // Begin the loading.  This will be done in a separate thread in WebView.
        webView.loadDataWithBaseURL(null, data.toString(), "text/plain", "utf-8", null);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                // Change from 'Loading...' to the real title
                mAlert.setTitle(getString(R.string.swap_dialog));
            }
        });

        final AlertController.AlertParams p = mAlertParams;
        p.mTitle = getString(R.string.swap_loading);
        p.mView = webView;
        p.mForceInverseBackground = true;
        setupAlert();
    }

    private void showErrorAndFinish() {
        Toast.makeText(this, R.string.swap_error, Toast.LENGTH_LONG)
                .show();
        finish();
    }

    public static int swapEnabled() {
        String swap_enabled = SystemProperties.get(SWAP_ENABLED_PROP);
        if (swap_enabled.equals("") || swap_enabled.equals("0"))
            return 0;
        else
            return 1;
    }

    private class runShellCommand extends Thread {
        private String command = "";
        private String toastMessage = "";

        public runShellCommand(String command, String toastMessage) {
            this.command = command;
            this.toastMessage = toastMessage;
        }

        @Override
        public void run() {
        try {
            if (!command.equals("")) {
                Process process = Runtime.getRuntime().exec("su");
                Log.d(TAG, "Executing: " + command);
                DataOutputStream outputStream = new DataOutputStream(process.getOutputStream());
                DataInputStream inputStream = new DataInputStream(process.getInputStream());
                outputStream.writeBytes(command + "\n");
                outputStream.flush();
                outputStream.writeBytes("exit\n");
                outputStream.flush();
                process.waitFor();
            }
            } catch (IOException e) {
                Log.e(TAG, "Thread IOException");
            }
            catch (InterruptedException e) {
                Log.e(TAG, "Thread InterruptedException");
            }
            Message messageToThread = new Message();
            Bundle messageData = new Bundle();
            messageToThread.what = 0;
            messageData.putString("toastMessage", toastMessage);
            messageToThread.setData(messageData);
            mrunShellCommandHandler.sendMessage(messageToThread);
        }
    };

    private runShellCommand mrunShellCommand;

    private Handler mrunShellCommandHandler = new Handler() {
        public void handleMessage(Message msg) {
            CharSequence text="";
            Bundle messageData = msg.getData();
            text = messageData.getString("toastMessage", "");
            int duration = Toast.LENGTH_SHORT;
            Toast toast = Toast.makeText(myContext, text, duration);
            toast.show();
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            if (mrunShellCommand != null)
                mrunShellCommand.join();
        } 
        catch (InterruptedException e) {
        }
    }

    public void getSwapInfo() {
        String command = "";
        String toastText = "";
        if (swapEnabled() == 1)
            command = "echo -e \"Swap enabled. Size: " + SystemProperties.get(SWAP_SIZE_PROP) + "MB\n\" > " + SWAPINFO_PATH;
        else
            command = "echo -e \"Swap disabled.\n\" > " + SWAPINFO_PATH;
        command += "; cat /proc/meminfo |grep -i swap >> " + SWAPINFO_PATH;
        toastText = getString(R.string.swap_loading);
        mrunShellCommand = new runShellCommand(command, toastText);
        mrunShellCommand.start();
    }
}


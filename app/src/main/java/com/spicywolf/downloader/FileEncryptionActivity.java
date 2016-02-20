package com.spicywolf.downloader;


import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.WorkerThread;
import android.view.View;
import android.widget.Button;

public class FileEncryptionActivity extends Activity {

    private Button mEncryption;
    private Button mDecryption;

    private FileTask mTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_file_encryption);
        mEncryption = (Button) findViewById(R.id.button_encryption);

        mEncryption.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                executeTask(true);
            }
        });

        mDecryption = (Button) findViewById(R.id.button_decryption);
        mDecryption.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                executeTask(false);
            }
        });
    }

    private void executeTask(boolean encryption) {
        mTask = new FileTask();
        mTask.execute(encryption);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


    @WorkerThread
    private boolean encryption() {

        return true;
    }

    @WorkerThread
    private boolean decryption() {


        return true;
    }

    private class FileTask extends AsyncTask<Boolean, Boolean, Boolean> {

        ProgressDialog mProgressDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgressDialog = ProgressDialog.show(FileEncryptionActivity.this, null, "In progress ...", true, false);
            mEncryption.setEnabled(false);
            mDecryption.setEnabled(false);
        }

        @Override
        protected Boolean doInBackground(Boolean... params) {
            boolean encrypt = params[0];

            return encrypt ? encryption() : decryption();
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);

            mProgressDialog.hide();

            mEncryption.setEnabled(true);
            mDecryption.setEnabled(true);

        }
    }


}

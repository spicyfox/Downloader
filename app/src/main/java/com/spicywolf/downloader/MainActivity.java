package com.spicywolf.downloader;

import android.app.Activity;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.WorkerThread;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class MainActivity extends Activity {
    private final static String TAG = "MainActivity";
    private final static String DATA_FILE_FORMAT = "data_%d.json";
    private final static String TMP_EXT = ".tmp";
    private final static boolean ENABLE_RENAME_AFTER_DOWNLOAD = false;
    private ListView mListView;
    private EditText mEditUrl;
    private BaseAdapter mAdapter;
    private Spinner mSpinner;
    private Button mRefreshButton;
    private Button mDownload;
    private Button mCancel;
    private ArrayList<CharSequence> mFeedUrlList;
    private List<Item> mItemList;
    private Gson mGson = new Gson();
    private int mCurrentUrlFeedIndex = 0;
    private int mSelectedIndex = 0;
    private DownloadManager mDownloadManager;
    private ArrayAdapter<CharSequence> mCategoryAdapter;
    private BroadcastReceiver mReceiver;
    private HashMap<Long, String> mDownloadList = new HashMap<>();
    private DownloadXmlTask mDownloadXmlTask;

    public static String removeSpecialChars(String str) {
        String match1 = "[^\uAC00-\uD7A3xfe0-9a-zA-Z\\s]", match2 = "\\s{2,}";
        str = str.replaceAll(match1, "");
        str = str.replaceAll(match2, " ");

        return str;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        mEditUrl = (EditText) findViewById(R.id.editUrl);

        Intent intent = getIntent();
        String action = intent.getAction();
        if (action != null && action.equals(Intent.ACTION_SEND)) {
            String subject = intent.getStringExtra(Intent.EXTRA_SUBJECT);
            String text = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (subject != null) {
                mEditUrl.setText(subject);
            }
        }

        mListView = (ListView) findViewById(R.id.list);
        mSpinner = (Spinner) findViewById(R.id.spinner);
        mDownload = (Button) findViewById(R.id.download);
        mDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestFileDownload();
            }
        });
        mCancel = (Button) findViewById(R.id.cancel);
        mCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancelAllFileDownload();

            }
        });
        mRefreshButton = (Button) findViewById(R.id.refresh);
        mRefreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refreshListFromNetwork(false);
            }
        });

        init();

    }

    private void requestFileDownload() {
        String url = mEditUrl.getText().toString().trim();
        if (url.length() == 0) {
            return;
        }


        File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
                getLocalMediaSubDir());
        if (!dir.exists() && !dir.mkdirs()) {
            return;
        }

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setTitle(mItemList.get(mSelectedIndex).title);
        request.addRequestHeader("user-agent", "Mozilla/5.0 (Windows NT 6.1; rv:12.0) Gecko/20120403211507 Firefox/12.0");

        //request.setDescription("");

        String fileSubPath = getLocalMediaFileSubPath() + (ENABLE_RENAME_AFTER_DOWNLOAD ? TMP_EXT : "");
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_MUSIC, fileSubPath);

        mDownloadList.put(mDownloadManager.enqueue(request), fileSubPath);
        mEditUrl.setText("");
    }

    private String getLocalMediaFileSubPath() {

        String mediaUrl = mItemList.get(mSelectedIndex).mediaUrl;
        String subPath = getLocalMediaSubDir() + removeSpecialChars(mItemList.get(mSelectedIndex).title) +
                mediaUrl.substring(mediaUrl.lastIndexOf("."));

        Log.i(TAG, "getLocalMediaFileSubPath " + subPath);

        return subPath;
    }

    private String getLocalMediaSubDir() {

        return mCategoryAdapter.getItem(mCurrentUrlFeedIndex) + File.separator;

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mDownloadList != null) {
            mDownloadList.clear();
        }

        unregisterReceiver(mReceiver);

    }

    private void cancelAllFileDownload() {
        Set<Long> keys = mDownloadList.keySet();
        for (Long id : keys) {
            if (mDownloadManager.remove(id) > 0) {
                mDownloadList.remove(id);
            }
        }
    }

    private void init() {

        //- item
        ArrayList<CharSequence> urls = new ArrayList<>();
        TypedArray typedArray = getResources().obtainTypedArray(R.array.url_list);

        for (int index = 0; index < typedArray.length(); index++) {
            urls.add(typedArray.getText(index));
        }
        typedArray.recycle();


        //- feed item
        mFeedUrlList = new ArrayList<>();
        TypedArray array = getResources().obtainTypedArray(R.array.url_feed);

        for (int index = 0; index < typedArray.length(); index++) {
            mFeedUrlList.add(array.getText(index));
        }
        array.recycle();


        mAdapter = new ItemAdapter();
        mListView.setAdapter(mAdapter);

        mCategoryAdapter =
                ArrayAdapter.createFromResource(this, R.array.url_title, android.R.layout.simple_spinner_dropdown_item);
        mSpinner.setAdapter(mCategoryAdapter);
        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mCurrentUrlFeedIndex = position;
                refreshListFromNetwork(true);
                mEditUrl.setText("");
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });


        if (mDownloadManager == null) {
            mDownloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        }


        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                String action = intent.getAction();

                if (action.equals(DownloadManager.ACTION_DOWNLOAD_COMPLETE)) {
                    onDownloadCompleted(intent);
                } else if (action.equals(DownloadManager.ACTION_NOTIFICATION_CLICKED)) {

                } else if (action.equals(DownloadManager.ACTION_VIEW_DOWNLOADS)) {

                }
            }
        };

        registerReceiver(mReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        registerReceiver(mReceiver, new IntentFilter(DownloadManager.ACTION_NOTIFICATION_CLICKED));
        registerReceiver(mReceiver, new IntentFilter(DownloadManager.ACTION_VIEW_DOWNLOADS));


    }

    private void onDownloadCompleted(Intent intent) {

        long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0);
        if (id == 0) {
            Log.e(TAG, "onDownloadCompleted - failed to identify enqueued id");
            return;
        }

        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(id);

        Cursor cursor = mDownloadManager.query(query);
        if (cursor.getCount() > 0 && cursor.moveToFirst()) {
            int reason = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
            Log.i(TAG, "onDownloadCompleted - reason: " + reason);

            if (reason == DownloadManager.STATUS_SUCCESSFUL) {
                Toast.makeText(getApplicationContext(), String.format("Download completed [%s]",
                        new File(Environment.DIRECTORY_MUSIC,
                                mDownloadList.get(id))
                                .getAbsolutePath()), Toast.LENGTH_SHORT)
                        .show();
            } else {
                Toast.makeText(getApplicationContext(), String.format("Failed to download file: reason [%d]", reason),
                        Toast.LENGTH_SHORT).show();
            }


            if (!ENABLE_RENAME_AFTER_DOWNLOAD && reason == DownloadManager.STATUS_SUCCESSFUL) {

                String subPath = mDownloadList.get(id);

                final File file = new File(Environment.DIRECTORY_MUSIC, subPath);
                final File renameFile = new File(Environment.DIRECTORY_MUSIC,
                        subPath.substring(0, subPath.length() - TMP_EXT.length()));
                Log.d(TAG, "onDownloadCompleted - old path: " + file.getAbsolutePath());
                Log.d(TAG, "onDownloadCompleted - new path: " + renameFile.getAbsolutePath());

                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (!file.renameTo(renameFile)) {
                            Log.e(TAG, "onDownloadCompleted - failed to rename");
                        }
                    }
                }, 500);

            }
            mDownloadList.remove(id);
        }
    }

    private void refreshListFromNetwork(boolean useSavedData) {
        if (mCurrentUrlFeedIndex >= 0) {
            mDownloadXmlTask = new DownloadXmlTask(useSavedData);
            mDownloadXmlTask.execute(mFeedUrlList.get(mCurrentUrlFeedIndex).toString());

        }

    }

    // Uploads XML from stackoverflow.com, parses it, and combines it with
    // HTML markup. Returns HTML string.
    @WorkerThread
    private List<Item> loadXmlFromNetwork(String urlString) throws XmlPullParserException, IOException {

        Log.i(TAG, "loadXmlFromNetwork: " + urlString);

        InputStream stream = null;
        // Instantiate the parser
        XmlParser xmlParser = new XmlParser();
        List<Item> entries = null;

        try {
            stream = downloadUrl(urlString);
            entries = xmlParser.parse(stream);
            // Makes sure that the InputStream is closed after the app is
            // finished using it.
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (stream != null) {
                stream.close();
            }
        }

        return entries;
    }

    // Given a string representation of a URL, sets up a connection and gets
    // an input stream.
    private InputStream downloadUrl(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setReadTimeout(10000 /* milliseconds */);
        conn.setConnectTimeout(15000 /* milliseconds */);
        conn.setRequestMethod("GET");
        conn.setDoInput(true);
        // Starts the query
        conn.connect();
        return conn.getInputStream();
    }

    private void saveData(String json) {

        Log.i(TAG, "saveData " + json);

        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(
                    new File(getExternalCacheDir(), String.format(DATA_FILE_FORMAT, mCurrentUrlFeedIndex)));
            fileWriter.write(json);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fileWriter != null) {
                try {
                    fileWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }
    }

    private List<Item> readData() {
        FileReader fileReader = null;
        try {
            StringBuilder builder = new StringBuilder();

            char[] buffer = new char[8192];

            fileReader = new FileReader(
                    new File(getExternalCacheDir(), String.format(DATA_FILE_FORMAT, mCurrentUrlFeedIndex)));
            int length;
            while ((length = fileReader.read(buffer)) != -1) {
                builder.append(buffer, 0, length);
            }

            if (builder.length() > 0) {
                return mGson.fromJson(builder.toString(), new TypeToken<ArrayList<Item>>() {
                }.getType());
            }

            Log.i(TAG, "readData " + builder);

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fileReader != null) {
                try {
                    fileReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return null;
    }

    private class ItemAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return mItemList == null ? 0 : mItemList.size();
        }

        @Override
        public Object getItem(int position) {
            return mItemList == null ? null : mItemList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(MainActivity.this).inflate(R.layout.item, parent, false);
            }

            TextView title = ((TextView) convertView.findViewById(R.id.title));
            title.setText(mItemList.get(position).title);
            title.setTag(new Integer(position));
            title.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    TextView title = (TextView) v;
                    if (title != null) {
                        Integer index = (Integer) title.getTag();
                        if (index != null) {
                            Log.i(TAG, "onClick mSelectedIndex: " + index);
                            mSelectedIndex = index;
                            mEditUrl.setText(mItemList.get(index).mediaUrl);
                        }

                    }
                }
            });

            return convertView;
        }

    }

    // Implementation of AsyncTask used to download XML feed from stackoverflow.com.
    private class DownloadXmlTask extends AsyncTask<String, Void, List<Item>> {

        private ProgressDialog mProgressDialog;
        private boolean mUseSavedData;

        protected DownloadXmlTask(boolean useSavedData) {
            mUseSavedData = useSavedData;

        }

        @Override
        protected void onPreExecute() {
            mProgressDialog = ProgressDialog
                    .show(MainActivity.this, null, "Loading...", false, true, new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {

                            if (mDownloadXmlTask != null) {
                                mDownloadXmlTask.cancel(true);
                            }
                        }
                    });
        }

        @Override
        protected List<Item> doInBackground(String... urls) {
            try {
                List<Item> result;
                if (mUseSavedData) {
                    result = readData();
                    if (result != null) {
                        return result;
                    }
                }

                result = loadXmlFromNetwork(urls[0]);
                saveData(mGson.toJson(result));
                return result;
            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
        }

        @Override
        protected void onPostExecute(List<Item> result) {

            mItemList = result;

            if (mProgressDialog != null && mProgressDialog.isShowing()) {
                mProgressDialog.hide();
                mProgressDialog = null;
            }

            mAdapter.notifyDataSetChanged();

        }
    }

}

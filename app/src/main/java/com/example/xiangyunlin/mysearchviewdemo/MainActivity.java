package com.example.xiangyunlin.mysearchviewdemo;

import android.Manifest;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import static android.Manifest.permission.READ_CONTACTS;
import static android.Manifest.permission.WRITE_CONTACTS;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CONTACTS = 1;
    private List<Map<String,String>> list = new ArrayList<>();
    private MyContentObserver myObserver;
    private Handler handler = new Handler();
    HandlerThread mWorkerThread;
    private Handler mWorkerHandler;

    private MyRecycleAdapter mRecycleAdapter;
    private RecyclerView mRecyclerView;

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.d("test", "onConfigurationChanged is called !");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 監測DB
        registerContentObservers();

        // 創工人&MainThread進行分配任務
        mWorkerThread = new HandlerThread("HandlerThread");
        mWorkerThread.start();
        mWorkerHandler = new Handler(mWorkerThread.getLooper());

        // Use RecyclerView to list contacts
        mRecyclerView = (RecyclerView) findViewById(R.id.listView);
        final LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(layoutManager);
        mRecycleAdapter = new MyRecycleAdapter(this.list);
        mRecyclerView.setAdapter(mRecycleAdapter);

        // 確認是否取得使用者權限，進行手機DB讀取
        int permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            //未取得權限，向使用者要求允許權限
            ActivityCompat.requestPermissions(this, new String[]{READ_CONTACTS, WRITE_CONTACTS}, REQUEST_CONTACTS);
        } else{
            //已有權限，可進行檔案存取
            // Use workerHandler to do doInBackground things
            mWorkerHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Log.d("test", "doInBackground 3");
                    initdate();
                    mRecycleAdapter.setData(list);
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            mRecycleAdapter.notifyDataSetChanged();
                        }
                    });
                }
            }, 5000);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        //super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch(requestCode) {
            case REQUEST_CONTACTS:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //取得聯絡人權限，進行存取
                    // Use workerHandler to do doInBackground things
                    mWorkerHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Log.d("test", "doInBackground 2");
                            initdate();
                            mRecycleAdapter.setData(list);
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    mRecycleAdapter.notifyDataSetChanged();
                                }
                            });
                        }
                    });
                } else {
                    //使用者拒絕權限，顯示對話框告知
                    new AlertDialog.Builder(this)
                            .setMessage("必須允許聯絡人權限才能顯示資料")
                            .setPositiveButton("OK", null)
                            .show();
                }
                return;
        }
    }


    // Use RecyclerView to list contacts
    public class MyRecycleAdapter extends RecyclerView.Adapter<MyRecycleAdapter.ViewHolder> {
        private List<Map<String,String>> mArrayList;
        private List<Map<String,String>> mFilteredList = new ArrayList<>();

        public MyRecycleAdapter(List<Map<String,String>> arrayList) {
            mArrayList = arrayList;
            mFilteredList.addAll(arrayList);
        }

        public void setData(List<Map<String,String>> dataList) {
            mFilteredList.addAll(dataList);
        }

        // 進行篩選
        public void doFilter(String text) {
            // 之前有做setData，所以無論如何都要先把list清空
            mFilteredList.clear();
            if(!text.isEmpty()) {
                for (Map<String, String> contacts : mArrayList) {
                    if (contacts.get("name").toLowerCase().contains(text) || contacts.get("phoneNumber").contains(text)) {
                        mFilteredList.add(contacts);
                    }
                }
            } else {
                mFilteredList.addAll(mArrayList);
            }
            mRecycleAdapter.notifyDataSetChanged();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            TextView mNameView;
            TextView mPhoneNumberView;

            public ViewHolder(View itemView) {
                super(itemView);
                mNameView = itemView.findViewById(R.id.nameView);
                mPhoneNumberView = itemView.findViewById(R.id.phoneNumberView);
            }
        }


        @Override
        public MyRecycleAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if(viewType == 0) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.listview_item, parent, false);
                return new ViewHolder(view);
            } else if (viewType == 1) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.listview_item, parent, false);
                return new ViewHolder(view);
            }
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.listview_item, parent, false);
            return new ViewHolder(view);
        }


        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.mNameView.setText(mFilteredList.get(position).get("name"));
            holder.mPhoneNumberView.setText(mFilteredList.get(position).get("phoneNumber"));
        }

        @Override
        public int getItemCount() {
            return mFilteredList.size();
        }


    } // end of MyRecycleAdapter


    // query the data (name, phoneNumber)
    public void initdate() {
        if(list != null) {
            list.clear();
        }

        ContentResolver cr = getContentResolver();
        Cursor cursor = cr.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[]{ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER},
                null,
                null,
                null
        );
        while(cursor.moveToNext()){
            Map<String,String> map = new HashMap<>();
            String name = cursor.getString(0);
            String phoneNumber = cursor.getString(1);
            map.put("name", name);
            map.put("phoneNumber", phoneNumber);
            list.add(map);
            Collections.sort(list, mapComparator);
        }
        if (cursor != null) {
            cursor.close();
        }
    }

    // 將DB資料進行排序
    public Comparator<Map<String, String>> mapComparator = new Comparator<Map<String, String>>() {
        public int compare(Map<String, String> m1, Map<String, String> m2) {
            return m1.get("name").compareTo(m2.get("name"));
        }
    };


    // To notice DB data has changed
    private class MyContentObserver extends ContentObserver {

        public MyContentObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            Log.d("test", "onChange !");

            // Use AsyncTask to do doInBackground things
            AsyncTask asyncTask = new AsyncTask() {
                @Override
                protected Object doInBackground(Object[] objects) {
                    initdate();
                    return null;
                }

                @Override
                protected void onPostExecute(Object o) {
                    super.onPostExecute(o);
                    mRecycleAdapter.notifyDataSetChanged();
                }
            }.execute();
        }
    } // end of class MyContentObserver


    //註冊監聽
    private void registerContentObservers() {
        myObserver = new MyContentObserver(handler);
        getContentResolver().registerContentObserver(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, true, myObserver);
    }


    //取消監聽
    private void unregisterContentObservers() {
        if (myObserver != null) {
            getContentResolver().unregisterContentObserver(myObserver);
            myObserver = null;
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterContentObservers();
        Log.d("test", "onDestroy !");
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        MenuItem search = menu.findItem(R.id.search);
        SearchView searchView = (SearchView) search.getActionView();
        search(searchView);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        return super.onOptionsItemSelected(item);
    }


    private void search(SearchView searchView) {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (mRecycleAdapter != null) {
                    mRecycleAdapter.doFilter(newText);
                }
                return true;
            }
        });
    }

}

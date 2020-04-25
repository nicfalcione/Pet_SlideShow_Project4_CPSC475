package com.example.pets;

import androidx.annotation.LongDef;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Spinner;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    public List<Pet> petList = new ArrayList<>();
    private String petURL = "https://www.pcs.cnu.edu/~kperkins/pets/";

    private JSONArray jsonArray;

    private OnSharedPreferenceChangeListener preferenceChangeListener;
    private SharedPreferences myPrefs;

    private ViewPager2 vp;
    private ViewPager2_Adapter csa;

    /**
     * Simple Pet class for holding pet stuff
     */
    public class Pet {
        String name;
        String file;

        public Pet(String name, String file) {
            this.name = name;
            this.file = file;
        }
    }

    public void processJSON(String string) {
        try {
            petList.clear();
            if (string == null) {
                return;
            }
            JSONObject jsonObject = new JSONObject(string);
            jsonArray = jsonObject.getJSONArray(getString(R.string.app_name).toLowerCase());

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject object = jsonArray.getJSONObject(i);
                String name = object.getString("name");
                String file = object.getString("file");

                petList.add(new MainActivity.Pet(name, file));
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayShowTitleEnabled(true);

        myPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        preferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if (key.equals("URLPref")) {
                    petURL = myPrefs.getString(key, getString(R.string.pet_url));
                    Toast.makeText(getApplicationContext(), "URL is " + petURL, Toast.LENGTH_SHORT).show();
                    if (!isNetworkConnected()) {
                        //get a ref to the viewpager
                        vp=findViewById(R.id.view_pager);
                        //create an instance of the swipe adapter
                        csa = new ViewPager2_Adapter(MainActivity.this);

                        //set this viewpager to the adapter
                        vp.setAdapter(csa);
                        petList.clear();
                        csa.notifyDataSetChanged();
                    }
                    else {
                        runDownloadJSON();
                    }
                }
            }
        };

        myPrefs.registerOnSharedPreferenceChangeListener(preferenceChangeListener);

        if (!isNetworkConnected()) {
            //get a ref to the viewpager
            vp=findViewById(R.id.view_pager);
            //create an instance of the swipe adapter
            csa = new ViewPager2_Adapter(MainActivity.this);

            //set this viewpager to the adapter
            vp.setAdapter(csa);
            csa.notifyDataSetChanged();
            petList.clear();
        }
        else {
            Log.w("OnCreate called", "trying to download JSON");
            runDownloadJSON();
        }
    }

    public boolean isNetworkConnected() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = connectivityManager.getActiveNetworkInfo();
        if (info == null) {
            return false;
        }
        return (info.getType() == ConnectivityManager.TYPE_WIFI || info.getState() == NetworkInfo.State.CONNECTED);
    }

    public void runDownloadJSON() {
        new DownloadJSON().execute(new String[]{getString(R.string.json)});
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == R.id.action_settings) {
            startActivity(new Intent(this, PreferencesActivity.class));
        }
        return super.onOptionsItemSelected(menuItem);
    }


    private class DownloadJSON extends AsyncTask<String, Void, String> {

        private String myURL;
        private int statusCode = 0;
        private String TAG = "DownloadJSON";

        @Override
        protected String doInBackground(String... strings) {
            myURL = strings[0];

            try {
                URL url = new URL(petURL + myURL);

                // this does no network IO
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                // wrap in finally so that stream bis is sure to close
                // and we disconnect the HttpURLConnection
                BufferedReader in = null;
                try {

                    // this opens a connection, then sends GET & headers
                    connection.connect();

                    // lets see what we got make sure its one of
                    // the 200 codes (there can be 100 of them
                    // http_status / 100 != 2 does integer div any 200 code will = 2
                    statusCode = connection.getResponseCode();
                    if (statusCode / 100 != 2) {
                        Log.e(TAG, "Error-connection.getResponseCode returned "
                                + Integer.toString(statusCode));
                        return null;
                    }

                    in = new BufferedReader(new InputStreamReader(connection.getInputStream()), 8096);

                    // the following buffer will grow as needed
                    String myData;
                    StringBuffer sb = new StringBuffer();

                    while ((myData = in.readLine()) != null) {
                        sb.append(myData);
                        Log.w("Data", myData);
                    }
                    return sb.toString();

                } finally {
                    // close resource no matter what exception occurs
                    in.close();
                    connection.disconnect();
                }
            } catch (Exception exc) {
                return null;
            }
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
        }

        @Override
        protected void onPostExecute(String jsonArray) {
            super.onPostExecute(jsonArray);
            processJSON(jsonArray);

            //get a ref to the viewpager
            vp=findViewById(R.id.view_pager);
            //create an instance of the swipe adapter
            csa = new ViewPager2_Adapter(MainActivity.this);

            //set this viewpager to the adapter
            vp.setAdapter(csa);
            csa.notifyDataSetChanged();
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }
    }

    public class ViewPager2_Adapter extends RecyclerView.Adapter {
        private final Context ctx;
        private final LayoutInflater li;

        class PagerViewHolder extends RecyclerView.ViewHolder {
            private static final int UNINITIALIZED = -1;
            ImageView iv;
            TextView tv;
            int position=UNINITIALIZED;     //start off uninitialized, set it when we are populating
            //with a view in onBindViewHolder

            public PagerViewHolder(@NonNull View itemView) {
                super(itemView);
                iv = (ImageView)itemView.findViewById(R.id.imageView);
                tv = (TextView)itemView.findViewById(R.id.tv);
            }
        }

        private class GetImage extends AsyncTask<String, Void, Bitmap> {
            //ref to a viewholder
            private ViewPager2_Adapter.PagerViewHolder myVh;
            private String TAG = "ViewPagerImageDownload";
            private int statusCode = 0;
            private static final int        DEFAULTBUFFERSIZE = 50;
            private static final int        NODATA = -1;

            //since myVH may be recycled and reused
            //we have to verify that the result we are returning
            //is still what the viewholder wants
            private int original_position;

            public GetImage(ViewPager2_Adapter.PagerViewHolder myVh) {
                //hold on to a reference to this viewholder
                //note that its contents (specifically iv) may change
                //iff the viewholder is recycled
                this.myVh = myVh;
                //make a copy to compare later, once we have the image
                this.original_position = myVh.position;
            }

            public Bitmap downloadBitmap(String downloadURL) {
                try {
                    Thread.sleep(1000);
                    java.net.URL url1 = new URL(downloadURL);

                    // this does no network IO
                    HttpURLConnection connection = (HttpURLConnection) url1.openConnection();

                    // this opens a connection, then sends GET & headers
                    connection.connect();

                    statusCode = connection.getResponseCode();

                    if (statusCode / 100 != 2) {
                        Log.e(TAG, "Error-connection.getResponseCode returned "
                                + Integer.toString(statusCode));
                        return null;
                    }

                    InputStream is = connection.getInputStream();
                    BufferedInputStream bis = new BufferedInputStream(is);

                    // the following buffer will grow as needed
                    ByteArrayOutputStream baf = new ByteArrayOutputStream(DEFAULTBUFFERSIZE);
                    int current = 0;

                    // wrap in finally so that stream bis is sure to close
                    try {
                        while ((current = bis.read()) != NODATA) {
                            baf.write((byte) current);
                        }

                        // convert to a bitmap
                        byte[] imageData = baf.toByteArray();
                        return BitmapFactory.decodeByteArray(imageData, 0, imageData.length);
                    } finally {
                        // close resource no matter what exception occurs
                        bis.close();
                    }
                } catch (Exception exc) {
                    return null;
                }
            }

            @Override
            protected Bitmap doInBackground(String... param) {
                Log.w(TAG, param[0]);
                return downloadBitmap(param[0]);
            }

            @Override
            protected void onPostExecute(Bitmap param) {
                //got a result, if the following are NOT equal
                // then the view has been recycled and is being used by another
                // number DO NOT MODIFY
                if (this.myVh.position == this.original_position){
                    //still valid
                    //set the result on the main thread
                    Log.w(TAG, "Trying to set Bitmap");
                    myVh.iv.setImageBitmap(param);
                }
                else
                    Toast.makeText(ViewPager2_Adapter.this.ctx,"YIKES! Recycler view reused, my result is useless", Toast.LENGTH_SHORT).show();
            }
        }

        public ViewPager2_Adapter(Context ctx){
            super();
            this.ctx=ctx;

            //will use this to ceate swipe_layouts in onCreateViewHolder
            li=(LayoutInflater)ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }
        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            //call this when we need to create a brand new PagerViewHolder
            View view = li.inflate(R.layout.view_pager, parent, false);
            return new PagerViewHolder(view);   //the new one
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            //passing in an existing instance, reuse the internal resources
            //pass our data to our ViewHolder.
            ViewPager2_Adapter.PagerViewHolder viewHolder = (ViewPager2_Adapter.PagerViewHolder) holder;

            //set to some default image
            viewHolder.iv.setImageResource(R.drawable.error);
            if (!petList.isEmpty()) {
                viewHolder.tv.setText(petList.get(position).name);
            }
            else if (!isNetworkConnected()) {
                viewHolder.tv.setText(R.string.airplaneError);
            }
            else {
                viewHolder.tv.setText(R.string.badHostError);
            }
            viewHolder.position=position;       //remember which image this view is bound to

            //launch a thread to 'retreive' the image
            if (!petList.isEmpty()) {
                GetImage myTask = new GetImage(viewHolder);
                myTask.execute(new String[] {petURL + petList.get(position).file});
            }
        }

        @Override
        public int getItemCount() {
            //the size of the collection that contains the items we want to display
            if (petList.isEmpty()) {
                return 1;
            }
            return petList.size();
        }
    }
}

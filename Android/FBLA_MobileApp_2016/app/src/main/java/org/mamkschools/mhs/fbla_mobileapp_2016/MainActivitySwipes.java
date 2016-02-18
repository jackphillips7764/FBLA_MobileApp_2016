package org.mamkschools.mhs.fbla_mobileapp_2016;

import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.sqlite.SQLiteDatabase;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import android.widget.Toast;


import org.json.JSONArray;
import org.json.JSONObject;
import org.mamkschools.mhs.fbla_mobileapp_2016.lib.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mamkschools.mhs.fbla_mobileapp_2016.lib.PictureContract.*;

public class MainActivitySwipes extends AppCompatActivity implements View.OnClickListener{

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;
    private static File location;
    private static int picture = 0;
    private EvaluationScreen evaluationScreen;
    private double geoLong;
    private double geoLat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        //Must be first
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_activity_swipes);



        if(!Constants.PREFS_RESTORED){
            Constants.restorePrefs(getApplicationContext());
        }

       if(Constants.AUTHCODE == null || System.currentTimeMillis() >= Constants.AUTHCODE_EXP) {
           startActivity(new Intent(getApplicationContext(), LoginActivity.class));
       }




        new GetPicture().execute((Void) null);
        location = getFilesDir();


        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);


        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(this);
    }

    @Override
    protected void onResume(){
        super.onResume();
        Constants.restorePrefs(getApplicationContext());
    }

    @Override
    protected void onStop(){
        super.onStop();
        Constants.PREFS_RESTORED = false;
        Constants.savePrefs(getApplicationContext());
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main_activity_swipes, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        switch (id){
            case R.id.action_settings:
                startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
                return true;
            case R.id.action_logout:
                new Logout().execute((Void) null);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.fab:
                //Photo chooser (include default, cam, file (see Google Inbox on Android))
                    openImageIntent();
                break;
            default:
                Toast.makeText(this, "No Assigned Action", Toast.LENGTH_SHORT).show();

        }


    }


    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {
        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            // Return a PlaceholderFragment (defined as a static inner class below).
            //System.out.println(position);
            PictureHelper mDbHelper = new PictureHelper(getApplicationContext());
            SQLiteDatabase db = mDbHelper.getWritableDatabase();
            evaluationScreen = EvaluationScreen.newInstance(db, picture, location);
            return evaluationScreen;
        }

        @Override
        public int getCount() {
            return 1;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return "Section_" + position;
        }
    }

    /**
     * A placeholder fragment containing a simple view.
     */

    private Uri outputFileUri;
    private static final int YOUR_SELECT_PICTURE_REQUEST_CODE = 1;

    private void openImageIntent() {
        // Determine Uri of camera image to save.
        final File root = new File(Environment.getExternalStorageDirectory() + File.separator + "MyDir" + File.separator);
        root.mkdirs();
        final String fname = "img_"+ System.currentTimeMillis() + ".jpg";
        final File sdImageMainDirectory = new File(root, fname);
        outputFileUri = Uri.fromFile(sdImageMainDirectory);

        // Camera.
        final List<Intent> cameraIntents = new ArrayList<Intent>();
        final Intent captureIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        final PackageManager packageManager = getPackageManager();
        final List<ResolveInfo> listCam = packageManager.queryIntentActivities(captureIntent, 0);
        for(ResolveInfo res : listCam) {
            final String packageName = res.activityInfo.packageName;
            final Intent intent = new Intent(captureIntent);
            intent.setComponent(new ComponentName(res.activityInfo.packageName, res.activityInfo.name));
            intent.setPackage(packageName);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
            cameraIntents.add(intent);
        }

        // Filesystem.
        final Intent galleryIntent = new Intent();
        galleryIntent.setType("image/*");
        galleryIntent.setAction(Intent.ACTION_GET_CONTENT);

        // Chooser of filesystem options.
        final Intent chooserIntent = Intent.createChooser(galleryIntent, "Select Source");

        // Add the camera options.
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, cameraIntents.toArray(new Parcelable[cameraIntents.size()]));

        startActivityForResult(chooserIntent, YOUR_SELECT_PICTURE_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == YOUR_SELECT_PICTURE_REQUEST_CODE) {
                final boolean isCamera;
                if (data == null) {
                    isCamera = true;
                } else {
                    final String action = data.getAction();
                    if (action == null) {
                        isCamera = false;
                    } else {
                        isCamera = action.equals(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                    }
                }

                Uri selectedImageUri;
                if (isCamera) {
                    selectedImageUri = outputFileUri;
                } else {
                    selectedImageUri = data == null ? null : data.getData();
                }

                PicUploadParams uploadPic = new PicUploadParams();
                uploadPic.pics.put("picture",selectedImageUri);
                uploadPic.paramMap.put("title", selectedImageUri.toString());



                //TODO Get lat and long...
                uploadPic.paramMap.put("geolong", ""+-73.748687);
                uploadPic.paramMap.put("geolat", ""+40.934710);

                new PicUpload().execute(uploadPic);
            }
        }
    }
    private class GetPicture extends AsyncTask<Void, Void, Void> {

        private ArrayList<JSONObject> ret = new ArrayList<>();
        SecureAPI picture = SecureAPI.getInstance(MainActivitySwipes.this);

        @Override
        protected Void doInBackground(Void... params) {

            double geolong = -73.748687;
            double geolat = 40.934710;
            int amount = 25;
            int dist = 10;



            PictureHelper mDbHelper = new PictureHelper(getApplicationContext());
            SQLiteDatabase db = mDbHelper.getWritableDatabase();
            db.execSQL("Delete from " + PictureEntry.TABLE_NAME);
            ContentValues values = new ContentValues();



            try {
                JSONObject response = picture.HTTPSGET("picture/fetch?authcode=" + Constants.AUTHCODE
                        + "&geolong=" + geolong + "&geolat=" + geolat + "&amount="
                        + amount + "&ft_dist=" + dist);

                JSONArray array = response.getJSONArray("data");
                for(int i = 0; i < array.length(); i++ ){
                    values.put(PictureEntry.COLUMN_NAME_PICTURE_ID, array.getJSONObject(i).getInt("pid"));
                    values.put(PictureEntry.COLUMN_NAME_GEOLAT, array.getJSONObject(i).getDouble("geolat"));
                    values.put(PictureEntry.COLUMN_NAME_GEOLONG, array.getJSONObject(i).getDouble("geolong"));
                    values.put(PictureEntry.COLUMN_NAME_DIST, array.getJSONObject(i).getDouble("dist"));
                    values.put(PictureEntry.COLUMN_NAME_TITLE, array.getJSONObject(i).getString("title"));
                    values.put(PictureEntry.COLUMN_NAME_USERNAME, array.getJSONObject(i).getString("username"));
                    values.put(PictureEntry.COLUMN_NAME_VIEWS, array.getJSONObject(i).getInt("views"));
                    long newRowId;
                    newRowId = db.insert(
                            PictureEntry.TABLE_NAME,
                            "null",
                            values);
                    int pid = array.getJSONObject(i).getInt("pid");
                    File file = picture.HTTPSFETCHPIC("picture/" + pid + "?authcode="
                            + Constants.AUTHCODE, new File(getFilesDir(), "picture" + pid + ".jpg"));
                }
            }catch (Exception e){
                if(Constants.DEBUG_MODE){
                    util.log(e.getMessage());
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void v) {
            util.log("Done With Picture Download");
            mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
            mViewPager.setAdapter(mSectionsPagerAdapter);
        }
    }
    private class PicUpload extends AsyncTask<PicUploadParams, Void, Boolean> {

        private ArrayList<JSONObject> ret = new ArrayList<>();
        SecureAPI picture = SecureAPI.getInstance(MainActivitySwipes.this);

        @Override
        protected Boolean doInBackground(PicUploadParams... params) {

            try {
                JSONObject response = picture.HTTPSPOSTMULTI(Commands.Post.POSTPIC +
                        Commands.AUTHCODE_BASE + Constants.AUTHCODE,
                        params[0].paramMap, params[0].pics, getApplicationContext());
                if(response.getString("status").equals("success")){
                    return true;
                } else if(response.getString("status").equals("error")){
                    return false;
                } else {
                    throw new Exception("Impossible status");
                }
            }catch (Exception e){
                e.printStackTrace();
                if(Constants.DEBUG_MODE){
                    util.log(e.getMessage());

                }
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if(success){
                util.log("Upload worked");
            } else {
                util.log("Upload failed");
            }
        }
    }
    private class Logout extends AsyncTask<Void, Void, Void>{
        @Override
        protected Void doInBackground(Void ... v) {
            SecureAPI HTTPS = SecureAPI.getInstance(getApplicationContext());

            try {
                HTTPS.HTTPSGET(Commands.Get.LOGOUT + Constants.AUTHCODE);
                Constants.AUTHCODE_EXP = -1;
            } catch (Exception ex) {
                util.log(ex.getMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void v) {
            startActivity(new Intent(getApplicationContext(), LoginActivity.class));
            finish();
        }
    }

    private static class PicUploadParams{
        public Map<String, String> paramMap = new HashMap<String, String>();
        public Map<String, Uri> pics = new HashMap<String, Uri>();
    }
}

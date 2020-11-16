package ru.kamaz.itis.timer;

//import androidx.annotation.NonNull;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.KeyguardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Point;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.MediaStore;

import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;


import com.bumptech.glide.Glide;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import ru.kamaz.itis.timer.gallery.GalleryActivity;

import ru.kamaz.itis.timer.gallery.HelperUtils;
import ru.kamaz.itis.timer.gallery.domain.domain.Photo;
import ru.kamaz.itis.timer.gallery.presentation.MediaScannerBroadcast;
import ru.kamaz.itis.timer.interfaces.OnSurfaceCreatedListener;
import ru.kamaz.itis.timer.presenter.MainActivityPresenter;
import ru.kamaz.itis.timer.ui.MainActivityInterface;


import static ru.kamaz.itis.timer.CameraHelper.MEDIA_TYPE_IMAGE;
import static ru.kamaz.itis.timer.CameraHelper.MEDIA_TYPE_VIDEO;
import static ru.kamaz.itis.timer.CameraHelper.getOutputMediaFile;

public class MainActivity extends AppCompatActivity implements MainActivityInterface.View, MediaScannerBroadcast.MediaScannerBroadcastListener, OnSurfaceCreatedListener {
   // private MediaPhotoFragment mediaPhotoFragment;
    private MainActivityInterface.Presenter presenter;
    private MyApplicationInterface applicationInterface;
    private Chronometer timer;
    private boolean timerBool;
    private Camera mCamera;
    private CameraPreview camPreview;
    private MediaRecorder mediaRecorder;
    boolean howModNow =true;
    private File mOutputFile;
    private CameraHelper helperCam;
    private boolean isRecording = false;
    private Button  videoMode;
    private String TAG="sdsd";
    private Button  photoMode;
    private LinearLayout liner;
    private FrameLayout preview;
    public volatile Bitmap gallery_bitmap;
    private long offsite;
    static final int GALLERY_REQUEST = 1;
    List<Photo> galleryList;
    private Context context;
    ImageButton photoVideoButton;
    Bitmap bitmap = null;
    private static final int MEDIA_RECORDER_REQUEST = 0;
    public ImageButton bt_gallery;
    private MediaScannerBroadcast broadcast;

    private final String[] requiredPermissions = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA,
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Intent intent = getIntent();
        checkCameraHardware(this);
        applicationInterface = new MyApplicationInterface(this, savedInstanceState);
        presenter = new MainActivityPresenter();
        presenter.setView(this);
        presenter.init();
        videoMode.setVisibility(View.VISIBLE);
        photoMode.setVisibility(View.VISIBLE);
       //requestCameraPermissions();
       //mediaPhotoFragment = new MediaPhotoFragment();
    }



    @Override
    public void initVars() {
        videoMode = (Button) findViewById(R.id.videoMode);
        photoMode = (Button) findViewById(R.id.photoMode);
        preview = (FrameLayout) findViewById(R.id.preview);
        timer = (Chronometer) findViewById(R.id.timer);
        camPreview = new CameraPreview(this, mCamera, this);
        ViewGroup.LayoutParams photoParams = photoMode.getLayoutParams();
        ViewGroup.LayoutParams videoParams = videoMode.getLayoutParams();
        photoMode.setTextColor(Color.BLUE);
        videoMode.setTextColor(Color.WHITE);
        photoMode.setLayoutParams(photoParams);
        videoMode.setLayoutParams(videoParams);
        photoVideoButton = (ImageButton)findViewById(R.id.bt_PhotoVideo);
        bt_gallery = (ImageButton)findViewById(R.id.bt_gallery);
        broadcast = new MediaScannerBroadcast(this);
    }

    @Override
    public void updateGalleryList() {

    }

    @Override
    public void setListeners() {
        photoMode.setOnClickListener(new View.OnClickListener() {
            public void onClick(View View) {
                if(!howModNow){
                    photoVideoButton.setImageResource(R.drawable.circle_white);
                    howModNow=true;
                    DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
                    ViewGroup.LayoutParams videoParams = videoMode.getLayoutParams();
                    ViewGroup.LayoutParams photoParams = photoMode.getLayoutParams();
                    photoMode.setTextColor(Color.BLUE);
                    videoMode.setTextColor(Color.WHITE);
                    videoMode.setLayoutParams(videoParams);
                    photoMode.setLayoutParams(photoParams);

                }else {

                }
            }
        });
        bt_gallery.setOnClickListener(new View.OnClickListener() {
            public void onClick(View View) {
                Intent intent = new Intent(MainActivity.this, GalleryActivity.class);
                startActivity(intent);
            }
        });
        videoMode.setOnClickListener(new View.OnClickListener() {
            public void onClick(View View) {
                if(howModNow){
                    photoVideoButton.setImageResource(R.drawable.circle_red);
                    howModNow=false;
                    DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
                    ViewGroup.LayoutParams videoParams = videoMode.getLayoutParams();
                    ViewGroup.LayoutParams photoParams = photoMode.getLayoutParams();
                    photoMode.setTextColor(Color.WHITE);
                    videoMode.setTextColor(Color.BLUE);
                    videoMode.setLayoutParams(videoParams);
                    photoMode.setLayoutParams(photoParams);

                }else {

                }
            }
        });
        photoVideoButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View View) {

                if(!howModNow){
                    if (areCameraPermissionGranted()){
                        videoRecoding();
                        photoVideoButton.setClickable(false);
                        DateFormat beforeRecordingVideo= new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());

                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    Thread.sleep(1000);
                                    photoVideoButton.setClickable(true);

                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }

                        }
                        ).start();

                    } else {
                        requestCameraPermissions();
                    }

                }else {
                    takePhoto();
                    photoVideoButton.setEnabled(false);
                    bt_gallery.setImageBitmap(null);
                  // updateGalleryIcon();
                }
            }
        });
    }
    protected Bitmap updateGalleryIcon(Uri uri) {
        Bitmap songUri2 = null;
        ContentResolver contentResolver = context.getContentResolver();
        Uri songUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        try {
            songUri2 = MediaStore.Images.Media.getBitmap(contentResolver, songUri);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return songUri2;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);

        Bitmap bitmap = null;
        switch(requestCode) {
            case GALLERY_REQUEST:
                if(resultCode == RESULT_OK){
                    Uri selectedImage = imageReturnedIntent.getData();
                    try {
                        bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImage);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    bt_gallery.setImageBitmap(bitmap);

                }
        }
    }
    private void updateGalleryIconToBlank() {
        if( MyDebug.LOG )
            Log.d(TAG, "updateGalleryIconToBlank");
        ImageButton galleryButton = this.findViewById(R.id.bt_gallery);
        int bottom = galleryButton.getPaddingBottom();
        int top = galleryButton.getPaddingTop();
        int right = galleryButton.getPaddingRight();
        int left = galleryButton.getPaddingLeft();
        galleryButton.setImageBitmap(null);
        galleryButton.setImageResource(R.drawable.baseline_photo_library_white_48);
        galleryButton.setPadding(left, top, right, bottom);
        gallery_bitmap = null;
    }
    void updateGalleryIcon(Bitmap thumbnail) {
        if( MyDebug.LOG )
            Log.d(TAG, "updateGalleryIcon: " + thumbnail);
        ImageButton galleryButton = this.findViewById(R.id.bt_gallery);
        galleryButton.setImageBitmap(thumbnail);
        gallery_bitmap = thumbnail;
    }
    private boolean last_continuous_fast_burst; // whether the last photo operation was a continuous_fast_burst


void savingImage(final boolean started) {
    if( MyDebug.LOG )
        Log.d(TAG, "savingImage: " + started);

    this.runOnUiThread(new Runnable() {
        public void run() {


        }
    });
}

public void clearGalleryIcon(){
    bt_gallery.setImageBitmap(null);
}

    @SuppressLint("StaticFieldLeak")
    public void updateGalleryIcon() {
        long debug_time = 0;
        if( MyDebug.LOG ) {
            Log.d(TAG, "updateGalleryIcon");
            debug_time = System.currentTimeMillis();
        }
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String ghost_image_pref = sharedPreferences.getString(PreferenceKeys.GhostImagePreferenceKey, "preference_ghost_image_off");
        final boolean ghost_image_last = ghost_image_pref.equals("preference_ghost_image_last");

        new AsyncTask<Void, Void, Bitmap>() {
            private static final String TAG = "MainActivity/AsyncTask";
            private boolean is_video;

            protected Bitmap doInBackground(Void... params) {
                StorageUtils.Media media = applicationInterface.getStorageUtils().getLatestMedia();
                Bitmap thumbnail = null;

                KeyguardManager keyguard_manager = (KeyguardManager)MainActivity.this.getSystemService(Context.KEYGUARD_SERVICE);
                boolean is_locked = keyguard_manager != null && keyguard_manager.inKeyguardRestrictedInputMode();
                if( MyDebug.LOG )
                    Log.d(TAG, "is_locked?: " + is_locked);
                if( media != null && getContentResolver() != null && !is_locked ) {

                    if( ghost_image_last && !media.video ) {
                        if( MyDebug.LOG )
                            Log.d(TAG, "load full size bitmap for ghost image last photo");
                        try {
                            thumbnail = MediaStore.Images.Media.getBitmap(getContentResolver(), media.uri);
                            BitmapFactory.Options options = new BitmapFactory.Options();
                            InputStream is = getContentResolver().openInputStream(media.uri);
                            options.inJustDecodeBounds = true;
                            BitmapFactory.decodeStream(is, null, options);
                            int bitmap_width = options.outWidth;
                            int bitmap_height = options.outHeight;
                            Point display_size = new Point();
                            Display display = getWindowManager().getDefaultDisplay();
                            display.getSize(display_size);
                            if( display_size.x < display_size.y ) {
                                display_size.set(display_size.y, display_size.x);
                            }
                            if( bitmap_width < bitmap_height ) {
                                int dummy = bitmap_width;
                                bitmap_width = bitmap_height;
                                bitmap_height = dummy;
                            }
                            if( MyDebug.LOG ) {
                                Log.d(TAG, "bitmap_width: " + bitmap_width);
                                Log.d(TAG, "bitmap_height: " + bitmap_height);
                                Log.d(TAG, "display width: " + display_size.x);
                                Log.d(TAG, "display height: " + display_size.y);
                            }
                            options.inSampleSize = 1;
                            while( bitmap_height / (2*options.inSampleSize) >= display_size.y ) {
                                options.inSampleSize *= 2;
                            }
                            if( MyDebug.LOG ) {
                                Log.d(TAG, "inSampleSize: " + options.inSampleSize);
                            }
                            options.inJustDecodeBounds = false;
                            is.close();
                            is = getContentResolver().openInputStream(media.uri);
                            thumbnail = BitmapFactory.decodeStream(is, null, options);
                            if( thumbnail == null ) {
                                Log.e(TAG, "decodeStream returned null bitmap for ghost image last");
                            }
                            is.close();
                        }
                        catch(IOException e) {
                            Log.e(TAG, "failed to load bitmap for ghost image last");
                            e.printStackTrace();
                        }
                    }
                    if( thumbnail == null ) {
                        try {
                            if( media.video ) {
                                if( MyDebug.LOG )
                                    Log.d(TAG, "load thumbnail for video");
                                thumbnail = MediaStore.Video.Thumbnails.getThumbnail(getContentResolver(), media.id, MediaStore.Video.Thumbnails.MINI_KIND, null);
                                is_video = true;
                            }
                            else {
                                if( MyDebug.LOG )
                                    Log.d(TAG, "load thumbnail for photo");
                                thumbnail = MediaStore.Images.Thumbnails.getThumbnail(getContentResolver(), media.id, MediaStore.Images.Thumbnails.MINI_KIND, null);
                            }
                        }
                        catch(Throwable exception) {
                            // have had Google Play NoClassDefFoundError crashes from getThumbnail() for Galaxy Ace4 (vivalto3g), Galaxy S Duos3 (vivalto3gvn)
                            // also NegativeArraySizeException - best to catch everything
                            if( MyDebug.LOG )
                                Log.e(TAG, "exif orientation exception");
                            exception.printStackTrace();
                        }
                    }
                    if( thumbnail != null ) {
                        if( media.orientation != 0 ) {
                            if( MyDebug.LOG )
                                Log.d(TAG, "thumbnail size is " + thumbnail.getWidth() + " x " + thumbnail.getHeight());
                            Matrix matrix = new Matrix();
                            matrix.setRotate(media.orientation, thumbnail.getWidth() * 0.5f, thumbnail.getHeight() * 0.5f);
                            try {
                                Bitmap rotated_thumbnail = Bitmap.createBitmap(thumbnail, 0, 0, thumbnail.getWidth(), thumbnail.getHeight(), matrix, true);
                                // careful, as rotated_thumbnail is sometimes not a copy!
                                if( rotated_thumbnail != thumbnail ) {
                                    thumbnail.recycle();
                                    thumbnail = rotated_thumbnail;
                                }
                            }
                            catch(Throwable t) {
                                if( MyDebug.LOG )
                                    Log.d(TAG, "failed to rotate thumbnail");
                            }
                        }
                    }
                }
                return thumbnail;
            }

            @SuppressLint("StaticFieldLeak")
            protected void onPostExecute(Bitmap thumbnail) {
                if( MyDebug.LOG )
                    Log.d(TAG, "onPostExecute");
                // since we're now setting the thumbnail to the latest media on disk, we need to make sure clicking the Gallery goes to this
                applicationInterface.getStorageUtils().clearLastMediaScanned();
                if( thumbnail != null ) {
                    if( MyDebug.LOG )
                        Log.d(TAG, "set gallery button to thumbnail");
                    updateGalleryIcon(thumbnail);
                    applicationInterface.getDrawPreview().updateThumbnail(thumbnail, is_video, false); // needed in case last ghost image is enabled
                }
                else {
                    if( MyDebug.LOG )
                        Log.d(TAG, "set gallery button to blank");
                    updateGalleryIconToBlank();
                }
            }
        }.execute();
        if( MyDebug.LOG )
            Log.d(TAG, "updateGalleryIcon: total time to update gallery icon: " + (System.currentTimeMillis() - debug_time));
    }

    @Override
    protected void onPause() {
        presenter.onPause();
        super.onPause();
        releaseMediaRecorder();       // if you are using MediaRecorder, release it first
        releaseCamera();              // release the camera immediately on pause event
        preview.removeView(camPreview);
        camPreview = null;

        unregisterReceiver(broadcast);
    }

    @Override
    protected void onResume() {
        presenter.onResume();
        if (mCamera == null) {
            mCamera = getCameraInstance(0);
            camPreview = new CameraPreview(this, mCamera, this);
            preview.addView(camPreview);
        }
       // updateGalleryIcon();
        super.onResume();
        IntentFilter intentFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        intentFilter.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
        intentFilter.addDataScheme("file");
        intentFilter.addDataScheme("content");
        registerReceiver(broadcast, intentFilter);
    }

    private void releaseMediaRecorder(){
        if (mediaRecorder != null) {
            mediaRecorder.reset();
            mediaRecorder.release();
            mediaRecorder = null;
            mCamera.lock();
        }
    }

    private void releaseCamera(){
        if (mCamera != null){
            mCamera.release();
            mCamera = null;
        }
    }


    private boolean areCameraPermissionGranted() {

        for (String permission : requiredPermissions){
            if (!(ActivityCompat.checkSelfPermission(this, permission) ==
                    PackageManager.PERMISSION_GRANTED)){
                return false;
            }
        }
        return true;
    }
    private void requestCameraPermissions(){
        ActivityCompat.requestPermissions(
                this,
                requiredPermissions,
                MEDIA_RECORDER_REQUEST);
    }
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        if (MEDIA_RECORDER_REQUEST != requestCode) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }

        boolean areAllPermissionsGranted = true;
        for (int result : grantResults){
            if (result != PackageManager.PERMISSION_GRANTED){
                areAllPermissionsGranted = false;
                break;
            }
        }

        if (areAllPermissionsGranted){
            startCapture();
        } else {

            Toast.makeText(getApplicationContext(),
                    getString(R.string.app_name),
                    Toast.LENGTH_SHORT)
                    .show();
        }
    }

    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

            File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);

            if (pictureFile == null){
                Log.d(TAG, "Error creating media file, check storage permissions");
                return;
            }

                    try {
                        FileOutputStream fos = new FileOutputStream(pictureFile);
                        fos.write(data);
                        fos.close();
                    } catch (FileNotFoundException e) {
                        Log.d(TAG, "File not found: " + e.getMessage());
                    } catch (IOException e) {
                        Log.d(TAG, "Error accessing file: " + e.getMessage());
                    }

            photoVideoButton.setEnabled(true);

            camera.startPreview();

        }
    };
    private boolean videoRecoding(){
        if(isRecording){
            mediaRecorder.stop();
            releaseMediaRecorder();
            bt_gallery.setClickable(true);
            photoMode.setClickable(true);
            bt_gallery.setVisibility(View.VISIBLE);
            photoMode.setVisibility(View.VISIBLE);
            videoMode.setClickable(true);
            videoMode.setVisibility(View.VISIBLE);
            timer.setVisibility(View.INVISIBLE);
            timer.setBase(SystemClock.elapsedRealtime());
            offsite=0;
            timer.stop();
            isRecording = false;
        }else {
            if(prepareVideoRecorder()){
                isRecording= true;
                mediaRecorder.start();
                bt_gallery.setClickable(false);
                photoMode.setClickable(false);
                bt_gallery.setVisibility(View.INVISIBLE);
                photoMode.setVisibility(View.INVISIBLE);
                videoMode.setClickable(false);
                videoMode.setVisibility(View.INVISIBLE);
                timer.setVisibility(View.VISIBLE);
                timer.setBase(SystemClock.elapsedRealtime()- offsite);
                timer.start();
            }else releaseMediaRecorder();
        }


        return false;
    }
    private boolean takePhoto(){
        mCamera.takePicture(null, null, mPicture);
        clearGalleryIcon();
        updateGalleryIcon();
        return false;
    }

    private boolean prepareVideoRecorder(){
        releaseCamera();
        releaseMediaRecorder();
        mCamera = CameraHelper.getDefaultCameraInstance();
        Camera.Parameters parameters = mCamera.getParameters();
        List<Camera.Size> mSupportedPreviewSizes = parameters.getSupportedPreviewSizes();
        List<Camera.Size> mSupportedVideoSizes = parameters.getSupportedVideoSizes();
        Camera.Size optimalSize = CameraHelper
                .getOptimalVideoSize(mSupportedVideoSizes,
                        mSupportedPreviewSizes,
                        preview.getWidth(),
                        preview.getHeight());
        CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
        profile.videoFrameWidth = optimalSize.width;
        profile.videoFrameHeight = optimalSize.height;
        parameters.setPreviewSize(profile.videoFrameWidth, profile.videoFrameHeight);
        mCamera.setParameters(parameters);
        mediaRecorder = new MediaRecorder();
        mCamera.unlock();
        mediaRecorder.setCamera(mCamera);
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mediaRecorder.setVideoEncodingBitRate(700*1024);
        mediaRecorder.setVideoSize(640,360);
        mediaRecorder.setOutputFile(getOutputMediaFile(MEDIA_TYPE_VIDEO).toString());
        if(mCamera!=null){
            mediaRecorder.setPreviewDisplay(camPreview.getHolder().getSurface());
        }else {
            Toast toast = Toast.makeText(getApplicationContext(),
                    "Камера нет", Toast.LENGTH_LONG);
            toast.show();
        }
        try {
            mediaRecorder.prepare();
        } catch (IllegalStateException e) {
            Log.d(TAG, "IllegalStateException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        } catch (IOException e) {
            Log.d(TAG, "IOException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        }
        return true;
    }


    private void startCapture(){
        if (isRecording) {
            try {
                mediaRecorder.stop();  // stop the recording
            } catch (RuntimeException e) {

                Log.d(TAG, "RuntimeException: stop() is called immediately after start()");

                mOutputFile.delete();
            }
            releaseMediaRecorder(); // release the MediaRecorder object
            mCamera.lock();         // take camera access back from MediaRecorder


            isRecording = false;
            releaseCamera();


        } else {



            new MediaPrepareTask().execute(null, null, null);


        }
    }

    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            return true;
        } else {
            return false;
        }
    }
    public static Camera getCameraInstance(int cameraId){
        Camera c = null;

        try {
            if(cameraId == -1) {
                c = Camera.open(); // attempt to get a Camera instance
            } else {
                c = Camera.open(cameraId); // attempt to get a Camera instance
            }
        }
        catch (Exception e){

        }
        return c; // returns null if camera is unavailable
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    @Override
    public void mediaScannerBroadcastCallback() {
       // updateGalleryIcon();
    }

    @Override
    public void onCreateSurfaceFailed() {
        photoVideoButton.setClickable(false);
        Toast toast = Toast.makeText(this,
                "Камера нет", Toast.LENGTH_LONG);
        toast.show();
    }

    @Override
    public void onSurfaceCreated() {
        photoVideoButton.setClickable(true);
    }

    class MediaPrepareTask extends AsyncTask<Void, Void, Boolean> {


        @Override

        protected Boolean doInBackground(Void... voids) {

            if (prepareVideoRecorder()) {
                mediaRecorder.start();
                isRecording = true;

            } else {

                releaseMediaRecorder();
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (!result) {
                MainActivity.this.finish();
            }


        }
    }

}
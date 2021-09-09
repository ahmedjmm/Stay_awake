package com.dev.stayawake;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.common.internal.service.Common;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.SoundPool;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.util.Log;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.material.snackbar.Snackbar;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;


@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class MainActivity extends AppCompatActivity implements ServiceConnection {

    private static final String TAG = "MainActivity";
    private static CameraSourcePreview mPreview;
    private static GraphicOverlay mGraphicOverlay;
    private Camera mCamera;
    private Camera.Parameters parameters;

    private static final int RC_HANDLE_GMS = 9001;
    private static final int RC_HANDLE_CAMERA_PERM = 2;
    private static com.google.android.gms.analytics.Tracker mTracker;
    private static int CAMERA_FACING = CameraSource.CAMERA_FACING_FRONT;

    Intent cameraServiceIntent;
    public static CameraService cameraService;
    static SoundPool soundPool;
    static int soundPoolId;

    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;

    LocalBroadcastManager mLocalBroadcastManager;
    BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals("finishActivity")){
                finish();
            }
        }
    };

    private CameraManager mCameraManager;
    private static String mCameraId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final boolean isFlashAvailable = getApplicationContext().getPackageManager().
                hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
        if (!isFlashAvailable)
            Toast.makeText(getApplicationContext(), R.string.no_camera_flash, Toast.LENGTH_LONG).show();

        mCameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            mCameraId = mCameraManager.getCameraIdList()[0];
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
        IntentFilter mIntentFilter = new IntentFilter();
        mIntentFilter.addAction("finishActivity");
        mLocalBroadcastManager.registerReceiver(mBroadcastReceiver, mIntentFilter);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        sharedPreferences = getSharedPreferences(SaveData.__SaveData_Key, Context.MODE_PRIVATE);

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED){
            if(sharedPreferences.getBoolean("firstTimeUse", true)) {
                new AlertDialog.Builder(this).setTitle(R.string.useful_tips).setMessage(R.string.alert_dialog_tips).
                        setPositiveButton(R.string.ok, null).show();
                editor = sharedPreferences.edit();
                editor.putBoolean("firstTimeUse", false);
                editor.commit();
            }
            startCameraService();
        }
        else
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
                    RC_HANDLE_CAMERA_PERM);

        mPreview = findViewById(R.id.preview);
        mGraphicOverlay = findViewById(R.id.faceOverlay);

        Constants.ALARM_POINT = SaveData.getInstance(getApplicationContext()).getAlarmPoint();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton share_btn = findViewById(R.id.share_btn);
        share_btn.setOnClickListener(arg0 -> {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name));
            shareIntent.putExtra(Intent.EXTRA_TITLE, getString(R.string.app_name));
            String shareMessage= "\nApp help to stay awake\n\n";
            shareMessage = shareMessage + "https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID +"\n\n";
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);
            startActivity(Intent.createChooser(shareIntent, "share via"));
        });

        ToggleButton toggleButton = findViewById(R.id.onOffFlashlight);
        if(isFlashAvailable)
            toggleButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if(isChecked)
                        turnFlashlightOn();
                    else
                        turnFlashlightOff();
            });
        else
            toggleButton.setEnabled(false);

        FloatingActionButton switchCamera = findViewById(R.id.switch_camera);
        switchCamera.setOnClickListener(view -> {
            if (cameraService != null) {
                try {
                    cameraService.release();
                } catch (Exception ignored) { }
                finally {
                    if (CAMERA_FACING == CameraSource.CAMERA_FACING_FRONT) {
                        CAMERA_FACING = CameraSource.CAMERA_FACING_BACK;
                        toggleButton.setChecked(false);
                        toggleButton.setEnabled(false);
                        Toast.makeText(getApplicationContext(), R.string.cannot_use_flash, Toast.LENGTH_LONG).show();
                    }
                    else {
                        CAMERA_FACING = CameraSource.CAMERA_FACING_FRONT;
                        toggleButton.setEnabled(true);
                    }
                    cameraService.createCameraSource(CAMERA_FACING);
                    cameraService.startCameraSource();
                }
            }
        });
    }

    private void turnFlashlightOn() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                mCameraManager.setTorchMode(mCameraId, true);
            }
            catch (CameraAccessException ignored) { }
        }
        else {
            mCamera = Camera.open();
            parameters = mCamera.getParameters();
            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
            mCamera.setParameters(parameters);
            mCamera.startPreview();
        }
    }

    private void turnFlashlightOff() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                mCameraManager.setTorchMode(mCameraId, false);
            } catch (CameraAccessException ignore) { }
        }
        else {
            mCamera = Camera.open();
            parameters = mCamera.getParameters();
            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            mCamera.setParameters(parameters);
            mCamera.stopPreview();
            mCamera.release();
        }
    }

    private void startCameraService() {
        cameraServiceIntent = new Intent(getApplicationContext(), CameraService.class);
        startService(cameraServiceIntent);
    }

    //Restarts the camera
    @Override
    protected void onResume() {
        super.onResume();
        Intent cameraServiceIntent = new Intent(getApplicationContext(), CameraService.class);
        bindService(cameraServiceIntent, this, 0);
        if(cameraService != null) {
            cameraService.release();
            cameraService.createCameraSource(CAMERA_FACING);
            cameraService.startCameraSource();
            cameraService.setScreenName("Image~" + TAG);
            cameraService.send(new HitBuilders.ScreenViewBuilder().build());
        }
    }

    //Stops the camera
    @Override
    protected void onPause() {
        super.onPause();
        unbindService(this);
    }

    //Releases the resources associated with the camera source, the associated detector, and the rest of the processing pipeline.
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mLocalBroadcastManager.unregisterReceiver(mBroadcastReceiver);
    }

    /**
     * Callback for the result from requesting permissions. This method
     * is invoked for every call on {@link #requestPermissions(String[], int)}.
     * <p>
     * <strong>Note:</strong> It is possible that the permissions request interaction
     * with the user is interrupted. In this case you will receive empty permissions
     * and results arrays which should be treated as a cancellation.
     * </p>
     *
     * @param requestCode  The request code passed in {@link #requestPermissions(String[], int)}.
     * @param permissions  The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *                     which is either {@link PackageManager#PERMISSION_GRANTED}
     *                     or {@link PackageManager#PERMISSION_DENIED}. Never null.
     * @see #requestPermissions(String[], int)
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode != RC_HANDLE_CAMERA_PERM) {
            Log.d(TAG, "Got unexpected permission result: " + requestCode);
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }
//        permission granted
        if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCameraService();
            if(cameraService != null)
                cameraService.createCameraSource(CameraSource.CAMERA_FACING_BACK);
            if(sharedPreferences.getBoolean("firstTimeUse", true)) {
                new AlertDialog.Builder(this).setTitle(R.string.useful_tips).setMessage(R.string.alert_dialog_tips).
                        setPositiveButton(R.string.ok, null).show();
                editor = sharedPreferences.edit();
                editor.putBoolean("firstTimeUse", false);
                editor.commit();
            }
            return;
        }

//        permission not granted
        DialogInterface.OnClickListener listener = (dialog, id) -> finish();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Face Tracker sample").setMessage(R.string.no_camera_permission).setPositiveButton(R.string.ok, listener).show();
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        CameraService.MyBinder myBinder = (CameraService.MyBinder) service;
        cameraService = myBinder.getCameraService();
        cameraService.setScreenName("Image~" + TAG);
        cameraService.send(new HitBuilders.ScreenViewBuilder().build());
        if(soundPool == null){
            soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC,0);
            soundPoolId = soundPool.load(getApplicationContext(),R.raw.sound_1,1);
        }
        cameraService.showNotification();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        cameraService = null;
    }

    public static class CameraService extends Service {
        public IBinder myBinder = new MyBinder();
        public Context context;
        Activity activity;
        CameraSource mCameraSource;

        @Nullable
        @Override
        public IBinder onBind(Intent intent) {
            return myBinder;
        }

        @Override
        public void onCreate() {
            super.onCreate();
            activity = new MainActivity().getActivity();
            context = getApplicationContext();
            createCameraSource(CAMERA_FACING);

            // Obtain the shared Tracker instance.
            AnalyticsApplication application = (AnalyticsApplication) getApplication();
            mTracker = application.getDefaultTracker();
            soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC,0);
            soundPoolId = soundPool.load(getContext(),R.raw.sound_1,1);
        }

        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            createCameraSource(CAMERA_FACING);
            startCameraSource();
            mTracker.setScreenName("Image~" + TAG);
            mTracker.send(new HitBuilders.ScreenViewBuilder().build());
            String actionName = intent.getStringExtra("actionName");
            if(actionName != null){
                if(actionName.equals("dismiss")){
                    soundPool.stop(soundPoolId);
                    soundPool.release();
                    stopSelf();
                    LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(CameraService.this);
                    localBroadcastManager.sendBroadcast(new Intent("finishActivity"));
                }
            }
            return START_STICKY;
        }

        void showNotification(){
            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
            Intent openActivity = new Intent(this, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, openActivity, PendingIntent.FLAG_UPDATE_CURRENT);
            Intent dismiss = new Intent(this, com.dev.stayawake.NotificationReceiver.class).setAction(AnalyticsApplication.dismiss);
            PendingIntent dismissPendingIntent = PendingIntent.getBroadcast(this, 0, dismiss, PendingIntent.FLAG_CANCEL_CURRENT);
            Notification notification = new NotificationCompat.Builder(this, AnalyticsApplication.camera_service_notification_channel_id)
                    .setSmallIcon(R.mipmap.ic_launcher).setLargeIcon(bitmap).setContentIntent(pendingIntent).setContentTitle("open eyes")
                    .addAction(R.mipmap.ic_launcher, "stop running in background", dismissPendingIntent)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText("Open eyes is running in background."))
                    .setPriority(NotificationCompat.PRIORITY_HIGH).setOnlyAlertOnce(true).setVisibility(NotificationCompat.VISIBILITY_PUBLIC).build();
            startForeground(1, notification);
        }

        public Context getContext(){
            return context;
        }

        public void setScreenName(String s) {
            mTracker.setScreenName(s);
        }

        public void send(Map<String, String> build) {
            mTracker.send(build);
        }

        public void release() {
            mCameraSource.release();
        }

        public class MyBinder extends Binder{
            CameraService getCameraService(){
                return CameraService.this;
            }
        }

        /**
         * Creates and starts the camera.  Note that this uses a higher resolution in comparison
         * to other detection examples to enable the barcode detector to detect small barcodes
         * at long distances.
         */
        private void createCameraSource(int CAMERA_FACING) {
            FaceDetector detector = new FaceDetector.Builder(getApplicationContext()).setProminentFaceOnly(true)
                    .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS).build();
            detector.setProcessor(new MultiProcessor.Builder<>(new GraphicFaceTrackerFactory()).build());
            if (!detector.isOperational()) {
                // Note: The first time that an app using face API is installed on a device, GMS will
                // download a native library to the device in order to do detection.  Usually this
                // completes before the app is run for the first time.  But if that download has not yet
                // completed, then the above call will not detect any faces.
                //
                // isOperational() can be used to check if the required native library is currently
                // available.  The detector will automatically become operational once the library
                // download completes on device.
                Log.i(TAG, "Face detector dependencies are not yet available.");
            }
            mCameraSource = new CameraSource.Builder(getApplicationContext(), detector).setRequestedPreviewSize(640, 480)
                    .setFacing(CAMERA_FACING).setRequestedFps(30.0f).build();
        }

        //==============================================================================================
        // Camera Source Preview
        //==============================================================================================

        /**
         * Starts or restarts the camera source, if it exists.  If the camera source doesn't exist yet
         * (e.g., because onResume was called before the camera source was created), this will be called
         * again when the camera source is created.
         */
        private void startCameraSource() {
            // check that the device has play services available.
            int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(getApplicationContext());
            if (code != ConnectionResult.SUCCESS) {
                Dialog dlg = GoogleApiAvailability.getInstance().getErrorDialog(activity, code, RC_HANDLE_GMS);
                Objects.requireNonNull(dlg).show();
            }
            if (mCameraSource != null) {
                try {
                    mPreview.start(mCameraSource, mGraphicOverlay);
                }
                catch (IOException e) {
                    Log.e(TAG, "Unable to start camera source.", e);
                    mCameraSource.release();
                    mCameraSource = null;
                }
            }
        }

        //==============================================================================================
        // Graphic Face Tracker
        //==============================================================================================

        /**
         * Factory for creating a face tracker to be associated with a new face.  The multiprocessor
         * uses this factory to create face trackers as needed -- one for each individual.
         */
        private static class GraphicFaceTrackerFactory implements MultiProcessor.Factory<Face> {
            @Override
            public Tracker<Face> create(Face face) {
                return new GraphicFaceTracker(mGraphicOverlay);
            }
        }

        /**
         * Face tracker for each detected individual. This maintains a face graphic within the app's
         * associated face overlay.
         */
        private static class GraphicFaceTracker extends Tracker<Face> {
            private final GraphicOverlay mOverlay;
            private final FaceGraphic mFaceGraphic;
            int count = 0;

            GraphicFaceTracker(GraphicOverlay overlay) {
                mOverlay = overlay;
                mFaceGraphic = new FaceGraphic(overlay);
            }

            /**
             * Start tracking the detected face instance within the face overlay.
             */
            @Override
            public void onNewItem(int faceId, Face item) {
                mFaceGraphic.setId(faceId);
            }

            /**
             * Update the position/characteristics of the face within the overlay.
             */
            @Override
            public void onUpdate(FaceDetector.Detections<Face> detectionResults, Face face) {
                mOverlay.add(mFaceGraphic);
                mFaceGraphic.updateFace(face);

                float left = face.getIsLeftEyeOpenProbability();
                float right = face.getIsRightEyeOpenProbability();

                if ((left == Face.UNCOMPUTED_PROBABILITY) || (right == Face.UNCOMPUTED_PROBABILITY)) {
                    // At least one of the eyes was not detected.
                    return;
                }

                if(!isEyeOpenProbability(left,right)) {
                    count++;
                    if(count == 8) {
                        soundPool.play(soundPoolId, 1, 1, 1, 0, 1);
                        count = 0;
                    }
                }
                else {
                    count = 0;
                }
            }

            private boolean isEyeOpenProbability(float isLeftEyeOpenProbability,float isRightEyeOpenProbability) {
                boolean isEyeOpened = true;
                //Log.e(TAG,"Constants.ALARM_POINT - > "+Constants.ALARM_POINT+"");
                if ((isLeftEyeOpenProbability < (float)Constants.ALARM_POINT * 0.01) || (isRightEyeOpenProbability < (float)Constants.ALARM_POINT * 0.01)) {
                    isEyeOpened = false;
                }
                return isEyeOpened;
            }

            /**
             * Hide the graphic when the corresponding face was not detected.  This can happen for
             * intermediate frames temporarily (e.g., if the face was momentarily blocked from
             * view).
             */
            @Override
            public void onMissing(FaceDetector.Detections<Face> detectionResults) {
                mOverlay.remove(mFaceGraphic);
            }

            /**
             * Called when the face is assumed to be gone for good. Remove the graphic annotation from
             * the overlay.
             */
            @Override
            public void onDone() {
                mOverlay.remove(mFaceGraphic);
            }
        }
    }

    private Activity getActivity() {
        return this;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            // [START custom_event]
            mTracker.send(new HitBuilders.EventBuilder().setCategory("MenuItem").setAction("action_settings").build());
            // [END custom_event]
            Intent i = new Intent(MainActivity.this, SetupActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(i);
            return true;
        }
        if (id == R.id.app_info) {
            // [START custom_event]
            mTracker.send(new HitBuilders.EventBuilder().setCategory("MenuItem").setAction("app_info").build());
            // [END custom_event]
            Intent i = new Intent(MainActivity.this, AppInfoActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(i);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
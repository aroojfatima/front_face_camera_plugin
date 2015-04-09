package com.cameraPlugin;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.os.*;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.*;
import android.widget.ImageView;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CameraCanvasView extends Activity implements SurfaceHolder.Callback{
	private SurfaceHolder m_surfaceHolder;
    private Camera m_camera;
    private int m_previewCameraRotationDegree = 0;
    private int m_saveCameraRotationDegree = 0;


    boolean bUsecamera = true;
    boolean bPreviewRunning = false;
    boolean bFlash = false; // true: Flash ON, false: Flash Off
    boolean bRevert = false; // true: back camera, false: front camera

    private SurfaceView m_surfaceview;

    private ImageView m_imgFlash;
    private ImageView m_imgRevert;
    private ImageView m_imgCapture;
    private ImageView m_imgClose;

    // DestinationType
    public static final int DestinationTypeDataURL = 0;
    public static final int DestinationTypeFileURI = 1;

    // EncodingType
    public static final int EncodingTypeJPEG = 0;
    public static final int EncodingTypePNG = 1;

    // CameraPosition
    public static final int CameraPositionBack = 1;
    public static final int CameraPositionFront = 2;

    public static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final String DATE_FORMAT = "yyyy-MM-dd";

    // options
    private int _quality = 85;
    private int _destType = DestinationTypeDataURL;
    private boolean _allowEdit = false;
    private int _encodeType = EncodingTypeJPEG;
    private boolean _saveToPhotoAlbum = false;
    private boolean _correctOrientation = true;

    private int _width = 200;
    private int _height = 180;
    CameraCanvas cc= new CameraCanvas();
	// Create an ExifHelper to save the exif data that is lost during compression
	ExifHelper exif = new ExifHelper();

    Handler customHandler = new Handler();

    ProgressDialog m_prgDialog;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getResources().getIdentifier("canvascamera", "layout", getPackageName()));

        _quality = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getInt(CameraCanvas.QUALITY , 85);
        _destType = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getInt(CameraCanvas.DESTTYPE , DestinationTypeDataURL);
        _allowEdit = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean(CameraCanvas.ALLOWEDIT, false);
        _encodeType = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getInt(CameraCanvas.ENCODETYPE, EncodingTypeJPEG);
        _saveToPhotoAlbum = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean(CameraCanvas.SAVETOPHOTOALBUM, false);
        _correctOrientation = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean(CameraCanvas.CORRECTORIENTATION, true);
        _width = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getInt(CameraCanvas.WIDTH , 200);
        _height = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getInt(CameraCanvas.HEIGHT , 180);

        bFlash = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean(CameraCanvas.FLASH, false);
        bRevert = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean(CameraCanvas.REVERT, false);
        
        getControlVariables();
        initializeUI();

       setCameraRotationDegree();
    }

    @Override
    protected void onResume() {
        super.onResume();

        initializeUI();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();


    }

    @SuppressWarnings("deprecation")
	private void getControlVariables()
    {
        m_imgFlash = (ImageView) findViewById(getResources().getIdentifier("imgFlash", "id", getPackageName()));

        m_imgRevert = (ImageView) findViewById(getResources().getIdentifier("imgRevert", "id", getPackageName()));
        m_imgCapture = (ImageView) findViewById(getResources().getIdentifier("imgCapture", "id", getPackageName()));
        m_imgClose = (ImageView) findViewById(getResources().getIdentifier("imgClose", "id", getPackageName()));

        m_surfaceview = (SurfaceView) findViewById(getResources().getIdentifier("surfaceView", "id", getPackageName()));
        m_surfaceHolder = m_surfaceview.getHolder();
        m_surfaceHolder.addCallback(this);
        m_surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        m_prgDialog = new ProgressDialog(this);
    }

    private void initializeUI()
    {
        if (bFlash)
            m_imgFlash.setImageResource(getResources().getIdentifier("video_sprites_focus_inactive", "drawable", getPackageName()));
        else
            m_imgFlash.setImageResource(getResources().getIdentifier("video_sprites_focus", "drawable", getPackageName()));

        if (bRevert)
            m_imgRevert.setImageResource(getResources().getIdentifier("video_sprites_revert", "drawable", getPackageName()));
        else
            m_imgRevert.setImageResource(getResources().getIdentifier("video_sprites_revert_inactive", "drawable", getPackageName()));

        m_imgFlash.setOnClickListener(flashClickListener);

        m_imgRevert.setOnClickListener(revertClickListener);
        m_imgCapture.setOnClickListener(captureClickListener);
        m_imgClose.setOnClickListener(closeClickListener);
    }

    private View.OnClickListener flashClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            if (m_camera == null || !bRevert)
                return;

            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(CameraCanvasView.this).edit();

            Parameters p = m_camera.getParameters();

            if (bFlash)
            {
                p.setFlashMode(Parameters.FLASH_MODE_OFF);
                m_imgFlash.setImageResource(getResources().getIdentifier("video_sprites_focus", "drawable", getPackageName()));

                editor.putBoolean(CameraCanvas.FLASH, false);
            }
            else
            {
                p.setFlashMode(Parameters.FLASH_MODE_TORCH);
                m_imgFlash.setImageResource(getResources().getIdentifier("video_sprites_focus_inactive","drawable", getPackageName()));

                editor.putBoolean(CameraCanvas.FLASH, true);
            }

            m_camera.setParameters(p);

            editor.commit();

            bFlash = !bFlash;
        }
    };

    private View.OnClickListener revertClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            if (m_camera != null)
            {
                m_camera.stopPreview();
                m_camera.release();
                m_camera = null;
            }

            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(CameraCanvasView.this).edit();

            if (bRevert)
            {
                m_camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);

                m_imgRevert.setImageResource(getResources().getIdentifier("video_sprites_revert_inactive", "drawable", getPackageName()));
                editor.putBoolean(CameraCanvas.REVERT, false);

                m_imgFlash.setImageResource(getResources().getIdentifier("video_sprites_focus", "drawable", getPackageName()));
                bFlash = false;
                editor.putBoolean(CameraCanvas.FLASH, false);
            }
            else
            {
                m_camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);

                m_imgRevert.setImageResource(getResources().getIdentifier("video_sprites_revert", "drawable", getPackageName()));

                editor.putBoolean(CameraCanvas.REVERT, true);
            }

            bRevert = !bRevert;

            editor.commit();

            try
            {
                Camera.Parameters parameters = m_camera.getParameters();

                setCameraRotationDegree();

                m_camera.setDisplayOrientation(m_previewCameraRotationDegree);
                m_camera.setParameters(parameters);
                m_camera.setPreviewDisplay(m_surfaceHolder);
                m_camera.startPreview();
                bPreviewRunning = true;


            } catch (IOException e) {
                //e.printStackTrace();
            }
        }
    };

    public static Bitmap rotate(Bitmap bitmap, int degree) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        Matrix mtx = new Matrix();
        mtx.postRotate(degree);

        return Bitmap.createBitmap(bitmap, 0, 0, w, h, mtx, true);
    }

    @SuppressLint("SimpleDateFormat")
	private String writeTakedImageDataToStorage(byte[] data) {
        File sdDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File pictureFileDir = new File(sdDir, "CameraAPIDemo");

        if (!pictureFileDir.exists())
            pictureFileDir.mkdir();

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyymmddhhmmss");
        String date = dateFormat.format(new Date());
        String photoFile = "picture" + date + ".jpg";
        String filename = pictureFileDir.getPath() + File.separator + photoFile;
        File pictureFile = new File(filename);
        try
        {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            fos.write(data);
            fos.close();
        } catch (Exception e) {
            Log.d("CameraCanvas", "File " + filename + " not saved: " + e.getMessage());
        }
        return filename;
    }

    private View.OnClickListener captureClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            m_prgDialog.setMessage("Please wait for saving...");
            m_prgDialog.setCancelable(false);
            m_prgDialog.show();

            m_camera.takePicture(null, null, new PictureCallback() {
                public void onPictureTaken(byte[] data, Camera camera) {
                	int rotate = 0;
                	
                	try {
                        if (_encodeType == EncodingTypeJPEG) {
                            exif.createInFile(cc.getTempDirectoryPath() + "/.Pic.jpg");
                            exif.readExifData();
                            rotate = exif.getOrientation();
                        } else if (_encodeType == EncodingTypePNG) {
                            exif.createInFile(cc.getTempDirectoryPath() + "/.Pic.png");
                            exif.readExifData();
                            rotate = exif.getOrientation();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    Bitmap original = BitmapFactory.decodeByteArray(data, 0, data.length);

                    //_correctOrientation
                    if (_correctOrientation)
                        original = getRotatedBitmap(rotate, original, exif); //rotate(original, m_saveCameraRotationDegree);

                    // resize to width x height
                    Bitmap resized = Bitmap.createScaledBitmap(original, _width, _height, true);
                    ByteArrayOutputStream blob = new ByteArrayOutputStream();
                    if (_encodeType == EncodingTypeJPEG)
                        resized.compress(Bitmap.CompressFormat.JPEG, _quality, blob);
                    else
                        resized.compress(Bitmap.CompressFormat.PNG, 0, blob);

                    // save image to album
                    if (_saveToPhotoAlbum)
                    { 
                    	Log.v(null,"Here in photo save " );
                        MediaStore.Images.Media.insertImage(getContentResolver(), original, "CameraCanvas", "Taked by CameraCanvas");
                    }

                    JSONObject returnInfo = new JSONObject();
                    try
                    { Log.v(null,"destination type " + _destType);
                        if (_destType == DestinationTypeFileURI)
                        {
                            String strPath = writeTakedImageDataToStorage(blob.toByteArray());

                            returnInfo.put("imageURI", strPath);
                        }
                        else
                        {
                            byte[] retData = blob.toByteArray();
                            // base64 encoded string
                            String base64String = Base64.encodeToString(retData, Base64.NO_WRAP);
                            
                            returnInfo.put("imageURI", base64String);
                            //Log.v(null,"Return Info " + returnInfo);
                        }

                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyymmddhhmmss");
                        String date = dateFormat.format(new Date());
                        returnInfo.put("lastModifiedDate", date);
                        
                        returnInfo.put("size", String.valueOf(blob.size()));
                        returnInfo.put("type", (_encodeType == EncodingTypeJPEG ? "image/jpeg" : "image/png"));
                    }
                    catch (JSONException ex)
                    {
                        Log.v(null, "Exception occured"+ex.getMessage());
                    	return;
                    }
                    Log.v(null,"shared info " + CameraCanvas.sharedCanvasCamera);
                   
                    CameraCanvas.sharedCanvasCamera.onTakePicture(returnInfo);
                    
                    m_prgDialog.dismiss();
                    
                    m_camera.startPreview();
                    finish();
                    //new CameraCanvas().canvasCameraCallback.success(returnInfo);
                }
            });
        }
    };

    private View.OnClickListener closeClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            finish();
        }
    };

    private void setCameraRotationDegree()
    {
        Display display = ((WindowManager)getSystemService(WINDOW_SERVICE)).getDefaultDisplay();

        if (bRevert)    // Back Camera
        { Log.v(null,"hereeeee"+display.getRotation());
            if(display.getRotation() == Surface.ROTATION_0 || display.getRotation() == 0)
            { Log.v(null,"hereeeee in 0");
                m_previewCameraRotationDegree = 0;
            }
            else if(display.getRotation() == Surface.ROTATION_270 || display.getRotation() == 1)
            { Log.v(null,"hereeeee in 1");
                m_previewCameraRotationDegree = 270;
            }
            else if(display.getRotation() == Surface.ROTATION_180 || display.getRotation() == 2 )
            { Log.v(null,"hereeeee in 2");
                m_previewCameraRotationDegree = 180;
            }
            else if(display.getRotation() == Surface.ROTATION_90 || display.getRotation() == 3)
            { Log.v(null,"hereeeee in 3");
                m_previewCameraRotationDegree = 90;
            }

            m_saveCameraRotationDegree = m_previewCameraRotationDegree;
        }
        else    // Front Camera
        { Log.v(null,"hereeeee----------------------"+display.getRotation());
            if(display.getRotation() == Surface.ROTATION_0 || display.getRotation() == 0)
            {
                m_previewCameraRotationDegree = 0;
                m_saveCameraRotationDegree = 0;
            }
            else if(display.getRotation() == Surface.ROTATION_90 || display.getRotation() == 3)
            {
                m_previewCameraRotationDegree = 90;
                m_saveCameraRotationDegree = 90;
            }
            else if(display.getRotation() == Surface.ROTATION_180 || display.getRotation() == 2)
            {
                m_previewCameraRotationDegree = 180;
                m_saveCameraRotationDegree = 180;
            }
            else if(display.getRotation() == Surface.ROTATION_270 || display.getRotation() == 1)
            {
                m_previewCameraRotationDegree = 270;
                m_saveCameraRotationDegree = 270;
            }
        }
    }

    public void surfaceCreated(SurfaceHolder holder)
    {
        System.out.println("onsurfacecreated");

        if (bUsecamera) {

            if (bRevert)
            {
                m_camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
                m_imgRevert.setImageResource(getResources().getIdentifier("video_sprites_revert", "drawable", getPackageName()));
            }
            else
            {
                m_camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
                m_imgRevert.setImageResource(getResources().getIdentifier("video_sprites_revert_inactive", "drawable", getPackageName()));
            }

            try
            {
                m_camera.setPreviewDisplay(m_surfaceHolder);
            } catch (IOException e)
            {
                //e.printStackTrace();
            }

            bPreviewRunning = true;
        }
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
    {
        System.out.println("onsurface changed");

        if (bUsecamera)
        {
            if (bPreviewRunning)
            {
                Camera.Parameters parameters = m_camera.getParameters();

                if (bFlash)
                    parameters.setFlashMode(Parameters.FLASH_MODE_TORCH);
                else
                    parameters.setFlashMode(Parameters.FLASH_MODE_OFF);

                setCameraRotationDegree();
                m_camera.setDisplayOrientation(m_previewCameraRotationDegree);

                m_camera.setParameters(parameters);

                m_camera.startPreview();
            }
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {

        if (m_camera != null && bUsecamera)
        {
            m_camera.release();
            m_camera = null;

            bPreviewRunning = false;
        }
    }
    public Bitmap getRotatedBitmap(int rotate, Bitmap bitmap, ExifHelper exif) {
        Matrix matrix = new Matrix();
        if (rotate == 180) {
            matrix.setRotate(rotate);
        } else {
            matrix.setRotate(rotate, (float) bitmap.getWidth() / 2, (float) bitmap.getHeight() / 2);
        }

        try
        {
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            exif.resetOrientation();
        }
        catch (OutOfMemoryError oom)
        {
            // You can run out of memory if the image is very large:
            // http://simonmacdonald.blogspot.ca/2012/07/change-to-camera-code-in-phonegap-190.html
            // If this happens, simply do not rotate the image and return it unmodified.
            // If you do not catch the OutOfMemoryError, the Android app crashes.
        }

        return bitmap;
    }

}

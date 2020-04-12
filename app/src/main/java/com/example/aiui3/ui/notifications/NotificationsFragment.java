package com.example.aiui3.ui.notifications;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.CameraX;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureConfig;

import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import androidx.lifecycle.LifecycleOwner;


import android.annotation.SuppressLint;
import android.content.Context;

import android.content.Intent;
import android.content.pm.PackageManager;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import android.graphics.Matrix;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;

import android.provider.MediaStore;
import android.util.Rational;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.example.aiui3.ImageSaver;
import com.example.aiui3.MainActivity;
import com.example.aiui3.R;
import com.example.aiui3.ui.dashboard.DashboardFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import static android.app.Activity.RESULT_OK;

public class NotificationsFragment extends Fragment implements LifecycleOwner {
    private int REQUEST_CODE_PERMISSIONS = 10; //arbitrary number, can be changed accordingly
    private final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA",
            "android.permission.WRITE_EXTERNAL_STORAGE", "android.permission.READ_EXTERNAL_STORAGE"};
    TextureView txView;
    ImageCapture imgCap = null;
    private CameraX.LensFacing lensFacing = CameraX.LensFacing.BACK;
    private ImageButton switchButton;
    private ImageButton uploadButton;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_notifications, container, false);

        return root;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        BottomNavigationView navView = getActivity().findViewById(R.id.nav_view);

        navView.getMenu().getItem(0).setEnabled(true);
        navView.getMenu().getItem(1).setEnabled(true);
        navView.getMenu().getItem(2).setEnabled(false);
        txView = getActivity().findViewById(R.id.view_finder);
        if (allPermissionsGranted()) {
            startCamera(); //start camera if permission has been granted by user

        } else{
            requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
    }
    @SuppressLint("RestrictedApi")
    private void startCamera() {
        //make sure there isn't another camera instance running before starting
        bindCameraUseCases();



        switchButton = getActivity().findViewById(R.id.switch_button);
        uploadButton = getActivity().findViewById(R.id.upload_button);

        switchButton.setOnClickListener(v -> {
            lensFacing = lensFacing == CameraX.LensFacing.FRONT ? CameraX.LensFacing.BACK : CameraX.LensFacing.FRONT;
            try {
                // Only bind use cases if we can query a camera with this orientation
                CameraX.getCameraWithLensFacing(lensFacing);
                bindCameraUseCases();
            } catch (CameraInfoUnavailableException e) {
                // Do nothing
            }
        });
        uploadButton.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_PICK,
                    android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI);
            final int ACTIVITY_SELECT_IMAGE = 1234;
            startActivityForResult(i, ACTIVITY_SELECT_IMAGE);
        });

        getActivity().findViewById(R.id.capture_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File file = new File(Environment.getExternalStorageDirectory() + "/aiuiImages/" + System.currentTimeMillis() + ".jpg");
                imgCap.takePicture(file, new ImageCapture.OnImageSavedListener() {
                    @Override
                    public void onImageSaved(@NonNull File file) {
                        //String msg = "Photo capture succeeded: " + file.getAbsolutePath();
                        String msg = "Photo captured!";
                        DashboardFragment.bitmap = rotate(file);
                        Toast.makeText(getActivity().getBaseContext(), msg,Toast.LENGTH_LONG).show();
                        File[] files = {file};
                        new FileTask((MainActivity) getActivity(), getContext(), true).execute(files);

                    }

                    @Override
                    public void onError(@NonNull ImageCapture.UseCaseError useCaseError, @NonNull String message, @Nullable Throwable cause) {
                        String msg = "Photo capture failed: " + message;
                        Toast.makeText(getActivity().getBaseContext(), msg,Toast.LENGTH_LONG).show();
                        if(cause != null){
                            cause.printStackTrace();
                        }
                    }
                });
            }
        });

    }

    private void updateTransform(){
        /*
         * compensates the changes in orientation for the viewfinder, bc the rest of the layout stays in portrait mode.
         * methinks :thonk:
         * imgCap does this already, this class can be commented out or be used to optimise the preview
         */
        Matrix mx = new Matrix();
        float w = txView.getMeasuredWidth();
        float h = txView.getMeasuredHeight();

        float centreX = w / 2f; //calc centre of the viewfinder
        float centreY = h / 2f;

        int rotationDgr;
        int rotation = (int)txView.getRotation(); //cast to int bc switches don't like floats

        switch(rotation){ //correct output to account for display rotation
            case Surface.ROTATION_0:
                rotationDgr = 0;
                break;
            case Surface.ROTATION_90:
                rotationDgr = 90;
                break;
            case Surface.ROTATION_180:
                rotationDgr = 180;
                break;
            case Surface.ROTATION_270:
                rotationDgr = 270;
                break;
            default:
                return;
        }

        mx.postRotate((float)rotationDgr, centreX, centreY);
        txView.setTransform(mx); //apply transformations to textureview
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        //start camera when permissions have been granted otherwise exit app
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == REQUEST_CODE_PERMISSIONS){
            if(allPermissionsGranted()){
                Toast.makeText(getActivity(), "Permissions granted by the user.", Toast.LENGTH_SHORT).show();
                startCamera();
            } else{
                Toast.makeText(getActivity(), "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
                getActivity().finish();
            }
        }
    }

    private boolean allPermissionsGranted(){
        //check if req permissions have been granted
        for(String permission : REQUIRED_PERMISSIONS){

            if(ContextCompat.checkSelfPermission(getContext(), permission) != PackageManager.PERMISSION_GRANTED){
                return false;
            }
        }
        return true;
    }

    private void line(File file, Context c, boolean b){
        Bitmap bmp;
        if (b)
            bmp = rotate(file);
        else
            bmp = BitmapFactory.decodeFile(file.getPath());
        //bmp = getResizedBitmap(bmp, 400);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] bytes = stream.toByteArray();
        Python py = Python.getInstance();
        PyObject pyf = py.getModule("lines");
        PyObject pym = pyf.callAttr("addLines", bytes);
        PyObject pym2 = pyf.callAttr("onlyLines", bytes);
        byte[] bytes2 = pym.toJava(byte[].class);
        byte[] bytes3 = pym2.toJava(byte[].class);
        DashboardFragment.linesBitMap = BitmapFactory.decodeByteArray(bytes2, 0, bytes2.length);
        DashboardFragment.onlyLinesBitMap = BitmapFactory.decodeByteArray(bytes3, 0, bytes3.length);
        new ImageSaver(c).
                setFileName("myImage.png").
                setDirectoryName(file.getName()).
                save(DashboardFragment.bitmap);
        new ImageSaver(c).
                setFileName("myImage2.png").
                setDirectoryName(file.getName()).
                save(DashboardFragment.linesBitMap);
        new ImageSaver(c).
                setFileName("myImage3.png").
                setDirectoryName(file.getName()).
                save(DashboardFragment.onlyLinesBitMap);
    }

    private Bitmap rotate(File file){
        Bitmap myBitmap = BitmapFactory.decodeFile(file.getPath());
        if(lensFacing == CameraX.LensFacing.BACK)
            return RotateBitmap(myBitmap, 90);
        else
            return RotateBitmap(myBitmap, 270);
    }
    private Bitmap RotateBitmap(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    private class FileTask extends AsyncTask<File, Void, Void> {
        MainActivity activity;
        Context c;
        File f;
        boolean b;

        FileTask(MainActivity a, Context c, boolean b){
            this.activity = a;
            this.c = c;
            this.b = b;
        }
        protected Void doInBackground(File... files) {
            DashboardFragment.onlyLinesBitMap = null;
            DashboardFragment.linesBitMap = null;
            f = files[0];
            line(f, c, b);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            ImageView i = activity.findViewById(R.id.img);
            ProgressBar pb = activity.findViewById(R.id.indeterminateBar);
            ToggleButton tb = activity.findViewById(R.id.linesToggle);
            ToggleButton tb2 = activity.findViewById(R.id.linesButton);
            if (i != null && pb != null && tb.isChecked()){
                i.setImageBitmap(DashboardFragment.linesBitMap);
                pb.setVisibility(View.INVISIBLE);
            }
            if (i != null && pb != null && tb2.isChecked()){
                i.setImageBitmap(DashboardFragment.onlyLinesBitMap);
                pb.setVisibility(View.INVISIBLE);
            }

            Toast.makeText(activity, "Images loaded!", Toast.LENGTH_SHORT).show();
        }
    }
    private void bindCameraUseCases() {
        // Make sure that there are no other use cases bound to CameraX
        CameraX.unbindAll();

        int aspRatioW = txView.getWidth(); //get width of screen
        int aspRatioH = txView.getHeight(); //get height
        Rational asp = new Rational (aspRatioW, aspRatioH); //aspect ratio
        Size screen = new Size(aspRatioW, aspRatioH); //size of the screen

        //config obj for preview/viewfinder thingy.
        PreviewConfig pConfig = new PreviewConfig.Builder().setLensFacing(lensFacing).setTargetAspectRatio(asp).setTargetResolution(screen).build();
        Preview preview = new Preview(pConfig); //lets build it

        preview.setOnPreviewOutputUpdateListener(
                new Preview.OnPreviewOutputUpdateListener() {
                    //to update the surface texture we have to destroy it first, then re-add it
                    @Override
                    public void onUpdated(Preview.PreviewOutput output){
                        ViewGroup parent = (ViewGroup) txView.getParent();
                        parent.removeView(txView);
                        parent.addView(txView, 0);

                        txView.setSurfaceTexture(output.getSurfaceTexture());
                        updateTransform();
                    }
                });

        /* image capture */

        //config obj, selected capture mode
        ImageCaptureConfig imgCapConfig = new ImageCaptureConfig.Builder().setLensFacing(lensFacing).setCaptureMode(ImageCapture.CaptureMode.MIN_LATENCY)
                .setTargetRotation(getActivity().getWindowManager().getDefaultDisplay().getRotation()).build();
        imgCap = new ImageCapture(imgCapConfig);
        // Apply declared configs to CameraX using the same lifecycle owner
        CameraX.bindToLifecycle(this, preview, imgCap);
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        switch(requestCode) {
            case 1234:
                if(resultCode == RESULT_OK){
                    Uri selectedImage = data.getData();
                    String[] filePathColumn = {MediaStore.Images.Media.DATA};

                    Cursor cursor = getActivity().getContentResolver().query(selectedImage, filePathColumn, null, null, null);
                    cursor.moveToFirst();

                    int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                    String filePath = cursor.getString(columnIndex);
                    cursor.close();
                    File file = new File(filePath);
                    String msg = "Photo selected!";
                    DashboardFragment.bitmap = BitmapFactory.decodeFile(filePath);
                    Toast.makeText(getActivity().getBaseContext(), msg,Toast.LENGTH_LONG).show();
                    File filev = new File(Environment.getExternalStorageDirectory() + "/aiuiImages/" + file.getName() + ".jpg");

                    InputStream in = null;
                    OutputStream out = null;
                    try {
                        in = new FileInputStream(file);
                        out = new FileOutputStream(filev);
                        byte[] buf = new byte[1024];
                        int len;
                        while ((len = in.read(buf)) > 0) {
                            out.write(buf, 0, len);
                        }
                        in.close();
                        out.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }


                    // Copy the bits from instream to outstream

                    File[] files = {filev};
                    new FileTask((MainActivity) getActivity(), getContext(), false).execute(files);
                    /* Now you have choosen image in Bitmap format in object "yourSelectedImage". You can use it in way you want! */
                }
        }

    }
    private Bitmap getResizedBitmap(Bitmap image, int maxSize) {
        int width = image.getWidth();
        int height = image.getHeight();

        float bitmapRatio = (float)width / (float) height;
        if (bitmapRatio > 0) {
            width = maxSize;
            height = (int) (width / bitmapRatio);
        } else {
            height = maxSize;
            width = (int) (height * bitmapRatio);
        }
        return Bitmap.createScaledBitmap(image, width, height, true);
    }
}

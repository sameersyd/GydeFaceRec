package com.haze.sameer.gydefacerec;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class CameraActivity extends AppCompatActivity implements SurfaceHolder.Callback{

    Camera camera;
    Camera.PictureCallback jpegCallback;
    SurfaceView mSurfaceView;
    SurfaceHolder mSurfaceHolder;
    int currentCameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
    final int CAMERA_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        mSurfaceView = (SurfaceView)findViewById(R.id.surfaceView);
        mSurfaceHolder = mSurfaceView.getHolder();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.CAMERA}, CAMERA_REQUEST_CODE);
        }else{
            mSurfaceHolder.addCallback(this);
        }

        ImageView mToggle = (ImageView)findViewById(R.id.homeone_rotatecamera);
        ImageView mCapture = (ImageView)findViewById(R.id.homeone_captureimage);
        mToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                try {
                    camera.stopPreview();
                    camera.release();
                    if (currentCameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
                        currentCameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
                        ((GlobalContext) CameraActivity.this.getApplicationContext()).cameraFacingBack = false;
                    } else {
                        currentCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
                        ((GlobalContext) CameraActivity.this.getApplicationContext()).cameraFacingBack = true;
                    }

                    camera = Camera.open(currentCameraId);
                    Camera.CameraInfo info = new Camera.CameraInfo();
                    Camera.getCameraInfo(Camera.CameraInfo.CAMERA_FACING_FRONT, info);
                    try {
                        camera.setPreviewDisplay(mSurfaceView.getHolder());
                    } catch (IOException e) {
                        Toast.makeText(CameraActivity.this, e + "", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(CameraActivity.this, e + "", Toast.LENGTH_SHORT).show();
                    }
                    try {
                        Camera.Parameters parameters = camera.getParameters();
                        parameters.setJpegQuality(100);
//                        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                        parameters.set("orientation", "portrait");
//                        parameters.setRotation(90);
                        camera.setDisplayOrientation(90);
                        camera.setParameters(parameters);
                    }catch (Exception e){Toast.makeText(CameraActivity.this, e+"", Toast.LENGTH_SHORT).show();}
                    camera.startPreview();
                }catch (Exception e){Toast.makeText(CameraActivity.this, e+"", Toast.LENGTH_SHORT).show();}

            }
        });
        mCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                captureImage();
            }
        });

        jpegCallback = new Camera.PictureCallback(){
            @Override
            public void onPictureTaken(byte[] bytes, Camera camera) {

                Bitmap decodedBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

                String fileLocation = SaveImageToStorage(decodedBitmap);
                if(fileLocation!= null){
                    final Intent intent = new Intent(CameraActivity.this, ResultActivity.class);
                    intent.putExtra("direct","camera");
                    if(currentCameraId == Camera.CameraInfo.CAMERA_FACING_BACK){
                        intent.putExtra("which_direct","facing_back");
                    } else {
                        intent.putExtra("which_direct","facing_front");
                    }
                    //*******************??//
                    final Dialog nro = new Dialog(CameraActivity.this);
                    nro.requestWindowFeature(Window.FEATURE_NO_TITLE);
                    nro.setContentView(R.layout.new_or_old);
                    Button newUser = (Button)nro.findViewById(R.id.nro_newUserBtn);
                    Button oldUser = (Button)nro.findViewById(R.id.nro_oldUserBtn);
                    newUser.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            intent.putExtra("nroUser","newUser");
                            nro.dismiss();
                            startActivity(intent);
                        }
                    });
                    oldUser.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            intent.putExtra("nroUser","oldUser");
                            nro.dismiss();
                            startActivity(intent);
                        }
                    });
                    Window window = nro.getWindow();
                    window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
                    window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                    nro.show();
                    //*******************??//
                    //startActivity(intent);
                    return;
                }
            }
        };

    }

    public String SaveImageToStorage(Bitmap bitmap){
        String fileName = "imageToSend";
        try{
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
            FileOutputStream fo = this.openFileOutput(fileName, Context.MODE_PRIVATE);
            fo.write(bytes.toByteArray());
            fo.close();
        }catch(Exception e){
            e.printStackTrace();
            Toast.makeText(CameraActivity.this, e+"", Toast.LENGTH_SHORT).show();
            fileName = null;
        }
        return fileName;
    }

    private void captureImage() {
        camera.takePicture(null, null, jpegCallback);
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {

        camera = Camera.open(currentCameraId);
        ((GlobalContext) CameraActivity.this.getApplicationContext()).cameraFacingBack = false;

        try {
            Camera.Parameters parameters;
            parameters = camera.getParameters();
            parameters.set("orientation", "portrait");
//            parameters.setRotation(90);
//            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            parameters.setJpegQuality(100);
            camera.setParameters(parameters);
        }catch (Exception e){
            Toast.makeText(CameraActivity.this, e+"", Toast.LENGTH_SHORT).show();
        }
        try {
            camera.setPreviewDisplay(surfaceHolder);
        } catch (IOException e) {
            Toast.makeText(CameraActivity.this, e+"", Toast.LENGTH_SHORT).show();
        } catch (Exception e){
            Toast.makeText(CameraActivity.this, e+"", Toast.LENGTH_SHORT).show();
        }

        camera.setDisplayOrientation(90);
        camera.startPreview();

    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }
    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case CAMERA_REQUEST_CODE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Thanks for the permission", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(CameraActivity.this, "Please provide the permission", Toast.LENGTH_LONG).show();
                }
                break;
            }
        }
    }

    @Override
    public void onResume(){
        super.onResume();
    }

    public void onPause() {
        super.onPause();
        releaseCamera();
    }

    private void releaseCamera() {
        if (camera != null) {
            camera.setPreviewCallback(null);
            camera.release();
            camera = null;
        }
    }

}

package com.haze.sameer.gydefacerec;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.airbnb.lottie.LottieAnimationView;
import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.StringRequestListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;

public class ResultActivity extends AppCompatActivity {

    Bitmap bitmap,rotateBitmap;
    String direct,which_direct,nroUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        AndroidNetworking.initialize(getApplicationContext());
        try {
            bitmap = BitmapFactory.decodeStream(getApplication().openFileInput("imageToSend"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            finish();
            return;
        }

        Bundle extras = getIntent().getExtras();
        if(extras != null) {
            direct = extras.getString("direct");
            which_direct = extras.getString("which_direct");
            nroUser = extras.getString("nroUser");
        }

        ImageView mImage = (ImageView) findViewById(R.id.imageCaptured);
        if (direct.equals("camera")){
            if (which_direct.equals("facing_back")){
                rotateBitmap = rotate(bitmap,90);
                SaveImageToStorage(rotateBitmap,"imageToSend");
            }else if (which_direct.equals("facing_front")){
                rotateBitmap = rotate(bitmap,270);
                SaveImageToStorage(rotateBitmap,"imageToSend");
            }
        }

        mImage.setImageBitmap(rotateBitmap);
        onImage(rotateBitmap);

    }

    private Bitmap rotate(Bitmap decodedBitmap, int deg) {
        int w = decodedBitmap.getWidth();
        int h = decodedBitmap.getHeight();

        Matrix matrix = new Matrix();
        matrix.setRotate(deg);

        return Bitmap.createBitmap(decodedBitmap, 0, 0, w, h, matrix, true);

    }

    public String SaveImageToStorage(Bitmap bitmap,String passedFilename){
        String fileName = passedFilename;
        try{
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
            FileOutputStream fo = ResultActivity.this.openFileOutput(fileName, Context.MODE_PRIVATE);
            fo.write(bytes.toByteArray());
            fo.close();
        }catch(Exception e){
            e.printStackTrace();
            fileName = null;
        }
        return fileName;
    }

    public void onImage(Bitmap bitmap) {

        final Dialog loadDialog = new Dialog(ResultActivity.this);
        loadDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        loadDialog.setContentView(R.layout.loading_one);
        loadDialog.setOnKeyListener(new Dialog.OnKeyListener() {

            @Override
            public boolean onKey(DialogInterface arg0, int keyCode,
                                 KeyEvent event) {
                // TODO Auto-generated method stub
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    loadDialog.dismiss();
                }
                return true;
            }
        });
        LottieAnimationView animSelect;
        animSelect = (LottieAnimationView)loadDialog.findViewById(R.id.loading_one);
        animSelect.setAnimation("blueline.json");
        animSelect.playAnimation();
        animSelect.loop(true);

        Window window = loadDialog.getWindow();
        window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);

        loadDialog.show();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG,70,out);
        Bitmap decoded = BitmapFactory.decodeStream(new ByteArrayInputStream(out.toByteArray()));

        String bs64 = convertToBase64(decoded);
        Log.v("CHECK bitmap:",bs64);

        AndroidNetworking.post("https://api-us.faceplusplus.com/facepp/v3/detect")
                .addBodyParameter("api_key","YI5svBBZ0c7bcj2qrp4gwH5cejX44T6Q")
                .addBodyParameter("api_secret","oxt1GfiJv3byHD3FWoAyaL7acYxvQ0ET")
                .addBodyParameter("image_base64",bs64)
                .addBodyParameter("return_attributes","emotion,gender,age").build()
                .getAsString(new StringRequestListener() {
                    @Override
                    public void onResponse(String response) {
                        Log.v("CHECK response:",response);
                        try {
                            JSONObject rootObject = new JSONObject(response);
                            JSONArray faceArray = rootObject.getJSONArray("faces");
                            if (faceArray.length() > 1){
                                loadDialog.dismiss();
                                new AlertDialog.Builder(ResultActivity.this).setIcon(R.drawable.face_smileee).setTitle("GYDE")
                                        .setMessage("Multiple faces detected!")
                                        .setPositiveButton("Retry", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                finish();
                                                return;
                                            }
                                        }).show();
                                return;
                            }
                            JSONObject faceObject = new JSONObject(faceArray.get(0).toString());
                            JSONObject attributes = faceObject.getJSONObject("attributes");
                            JSONObject emotion = attributes.getJSONObject("emotion");
                            final Map<String,Double> map = new HashMap<>();
                            map.put("sadness",emotion.getDouble("sadness"));
                            map.put("neutral",emotion.getDouble("neutral"));
                            map.put("disgust",emotion.getDouble("disgust"));
                            map.put("anger",emotion.getDouble("anger"));
                            map.put("surprise",emotion.getDouble("surprise"));
                            map.put("fear",emotion.getDouble("fear"));
                            map.put("happiness",emotion.getDouble("happiness"));
                            Map.Entry<String, Double> detectedFacialEmotion = null;
                            for (Map.Entry<String, Double> entry : map.entrySet())
                            {
                                if (detectedFacialEmotion == null || entry.getValue().compareTo(detectedFacialEmotion.getValue()) > 0)
                                {
                                    detectedFacialEmotion = entry;
                                }
                            }
                            JSONObject age = attributes.getJSONObject("age");
                            JSONObject gender = attributes.getJSONObject("gender");

                            displayResult(loadDialog,detectedFacialEmotion.getKey(),age.getInt("value")+"",gender.getString("value")+"");

                        } catch (JSONException e) {
                            loadDialog.dismiss();
                            Toast.makeText(ResultActivity.this, ""+e, Toast.LENGTH_SHORT).show();
                            new AlertDialog.Builder(ResultActivity.this).setIcon(R.drawable.face_smileee).setTitle("GYDE")
                                    .setMessage("No face detected. Try once again!")
                                    .setPositiveButton("Retry", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            finish();
                                            return;
                                        }
                                    }).show();
                        }catch (Exception e){
                            loadDialog.dismiss();
                            new AlertDialog.Builder(ResultActivity.this).setIcon(R.drawable.retry).setTitle("GYDE")
                                    .setMessage("Something went wrong!")
                                    .setPositiveButton("Retry", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            finish();
                                            return;
                                        }
                                    }).show();
                            Toast.makeText(ResultActivity.this, ""+e, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(ANError anError) {
                        loadDialog.dismiss();
                        new AlertDialog.Builder(ResultActivity.this).setIcon(R.drawable.retry).setTitle("GYDE")
                                .setMessage("Something went wrong!")
                                .setPositiveButton("Retry", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        finish();
                                        return;
                                    }
                                }).show();
                        Toast.makeText(ResultActivity.this, anError+" 216", Toast.LENGTH_SHORT).show();
                    }
                });

    }

    private void displayResult(Dialog loadDialog,String emotion,String age,String gender){
        loadDialog.dismiss();
        if (nroUser.equals("newUser")){
            newUser(emotion,age,gender);
        }else if (nroUser.equals("oldUser")){
            oldUser(emotion,age,gender);
        }
    }

    private void newUser(String emotion,String age,String gender){
        Dialog result = new Dialog(this);
        result.requestWindowFeature(Window.FEATURE_NO_TITLE);
        result.setContentView(R.layout.new_user_result);
        result.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        result.setOnKeyListener(new Dialog.OnKeyListener() {

            @Override
            public boolean onKey(DialogInterface arg0, int keyCode,
                                 KeyEvent event) {
                // TODO Auto-generated method stub
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    finish();
                }
                return true;
            }
        });

        TextView ResAge = (TextView)result.findViewById(R.id.result_newUser_age);
        TextView ResGender = (TextView)result.findViewById(R.id.result_newUser_gender);
        TextView ResMood = (TextView)result.findViewById(R.id.result_newUser_mood);

        ResMood.setText(emotion);
        ResGender.setText(gender);
        ResAge.setText(age+" (approx.)");

        Window window = result.getWindow();
        window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        result.show();
    }

    private void oldUser(String emotion,String age,String gender){
        Dialog result = new Dialog(this);
        result.requestWindowFeature(Window.FEATURE_NO_TITLE);
        result.setContentView(R.layout.old_user_result);
        result.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        result.setOnKeyListener(new Dialog.OnKeyListener() {

            @Override
            public boolean onKey(DialogInterface arg0, int keyCode,
                                 KeyEvent event) {
                // TODO Auto-generated method stub
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    finish();
                }
                return true;
            }
        });

        TextView ResAge = (TextView)result.findViewById(R.id.result_oldUser_age);
        TextView ResGender = (TextView)result.findViewById(R.id.result_oldUser_gender);
        TextView ResMood = (TextView)result.findViewById(R.id.result_oldUser_mood);

        ResMood.setText(emotion);
        ResGender.setText(gender);
        ResAge.setText(age+" (approx.)");

        Window window = result.getWindow();
        window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        result.show();
    }

    private String convertToBase64(Bitmap image) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 70, baos);
        byte[] byteArrayImage = baos.toByteArray();
        String encodedImage = Base64.encodeToString(byteArrayImage, Base64.DEFAULT);
        return encodedImage;
    }

}

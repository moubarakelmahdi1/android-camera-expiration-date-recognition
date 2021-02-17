package dev.edmt.androidcamerarecognitiontext;

import android.Manifest;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    SurfaceView cameraView;
    TextView textView;
    CameraSource cameraSource;
    final int RequestCameraPermissionID = 1001;
    private static Pattern pattern;
    private static Matcher matcher;
    public String expiryDate;

    private CoordinatorLayout coordinatorLayout;
    protected Button snackButton;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case RequestCameraPermissionID: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    try {
                        cameraSource.start(cameraView.getHolder());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            }
            break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        coordinatorLayout = (CoordinatorLayout)findViewById(R.id.coordinatorLayout);
        final Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if(getSupportActionBar()!=null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        snackButton = (Button)findViewById(R.id.snackbarButton);
        snackButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Snackbar snackbar = Snackbar.make(coordinatorLayout,expiryDate,Snackbar.LENGTH_LONG);
                expiryDate = "No new expiry date found";
                snackbar.show();
            }
        });
        cameraView = (SurfaceView) findViewById(R.id.surface_view);
        textView = (TextView) findViewById(R.id.text_view);


        TextRecognizer textRecognizer = new TextRecognizer.Builder(getApplicationContext()).build();
        if (!textRecognizer.isOperational()) {
            Log.w("MainActivity", "Detector dependencies are not yet available");
        } else {

            cameraSource = new CameraSource.Builder(getApplicationContext(), textRecognizer)
                    .setFacing(CameraSource.CAMERA_FACING_BACK)
                    .setRequestedPreviewSize(1280, 1024)
                    .setRequestedFps(2.0f)
                    .setAutoFocusEnabled(true)
                    .build();

            cameraView.getHolder().addCallback(new SurfaceHolder.Callback() {
                @Override
                public void surfaceCreated(SurfaceHolder surfaceHolder) {

                    try {
                        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission.CAMERA},
                                    RequestCameraPermissionID);
                            return;
                        }
                        cameraSource.start(cameraView.getHolder());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

                }

                @Override
                public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                    cameraSource.stop();
                }
            });

            textRecognizer.setProcessor(new Detector.Processor<TextBlock>() {
                @Override
                public void release() {

                }

                public Integer printDate(String format){
                    int res = 0;
                    String[] result = new String[2];
                    int j=0;
                    while(matcher.find()) {
                        res = 1;
                        SimpleDateFormat formater;
                        if(format == "ddMMyyyy")
                        {
                            formater = new SimpleDateFormat("dd"+matcher.group().charAt(2)+"MM"+matcher.group().charAt(2)+"yyyy");
                        }
                        else if(format == "ddMMyy")
                        {
                            formater = new SimpleDateFormat("dd"+matcher.group().charAt(2)+"MM"+matcher.group().charAt(2)+"yy");
                        }
                        else if(format == "MMyyyy")
                        {
                            formater = new SimpleDateFormat("MM"+matcher.group().charAt(2)+"yyyy");
                        }
                        else{
                            formater = new SimpleDateFormat("MM"+matcher.group().charAt(2)+"yy");
                        }
                        try {
                            Date date = formater.parse(matcher.group());
                            textView.setText(formater.format(date));
                            expiryDate = formater.format(date);
                            if(formater.format(date)!=result[0]&&formater.format(date)!=result[1]){
                                result[j]=matcher.group();
                                j = j+1;
                            }
                        } catch (java.text.ParseException e) {
                            e.printStackTrace();
                        }
                        if(j==2){
                            try {
                                Date firstDate = formater.parse(result[0]);
                                Date secondDate = formater.parse(result[1]);
                                if(firstDate.compareTo(secondDate)>0){
                                    textView.setText(formater.format(firstDate));
                                    expiryDate = formater.format(firstDate);
                                }
                                else{
                                    textView.setText(formater.format(secondDate));
                                    expiryDate = formater.format(secondDate);
                                }
                            } catch (java.text.ParseException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    if(res == 0){
                        expiryDate = "No new expiry date found";
                    }
                    return res;
                }
                @Override
                public void receiveDetections(Detector.Detections<TextBlock> detections) {

                    final SparseArray<TextBlock> items = detections.getDetectedItems();
                    if(items.size() != 0)
                    {
                        textView.post(new Runnable() {
                            @Override
                            public void run() {
                                StringBuilder stringBuilder = new StringBuilder();
                                pattern = pattern.compile("\\d{2}\\/\\d{2}\\/\\d{4}|\\d{2}\\.\\d{2}\\.\\d{4}|\\d{2}\\-\\d{2}\\-\\d{4}");
                                for(int i =0;i<items.size();++i)
                                {
                                    TextBlock item = items.valueAt(i);
                                    stringBuilder.append(item.getValue());
                                }
                                matcher = pattern.matcher(stringBuilder.toString());
                                if(printDate("ddMMyyyy")==0){
                                    pattern = pattern.compile("\\d{2}\\/\\d{2}\\/\\d{2}|\\d{2}\\.\\d{2}\\.\\d{2}|\\d{2}\\-\\d{2}\\-\\d{2}");
                                    matcher = pattern.matcher(stringBuilder.toString());
                                    if(printDate("ddMMyy")==0){
                                        pattern = pattern.compile("\\d{2}\\/\\d{4}|\\d{2}\\.\\d{4}|\\d{2}\\-\\d{4}");
                                        matcher = pattern.matcher(stringBuilder.toString());
                                        if(printDate("MMyyyy")==0){
                                            pattern = pattern.compile("\\d{2}\\/\\d{2}|\\d{2}\\.\\d{2}|\\d{2}\\-\\d{2}");
                                            matcher = pattern.matcher(stringBuilder.toString());
                                            printDate("");
                                        }
                                    }
                                }
                            }
                        });
                    }
                }
            });
        }
    }
}

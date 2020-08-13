package com.example.mlptest2;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ApplicationErrorReport;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.util.Log;
import android.util.TimingLogger;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.gpu.GpuDelegate;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;
import java.util.Random;

import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    private static String MODEL_NAME;
    private int repitionCount = 5;

    /** Load TF Lite model from assets. */
    private static MappedByteBuffer loadModelFile(AssetManager assetManager, String model_name) throws IOException {
        AssetFileDescriptor fileDescriptor = assetManager.openFd(model_name);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        Button computeButton = (Button) findViewById(R.id.button);


        // everything happens within this listener when a button is pressed
        computeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                float[][] results = new float[25][30];
                Context context = getApplicationContext();
                AssetManager manager = context.getAssets();

                GpuDelegate delegate = new GpuDelegate();
                Interpreter.Options options = (new Interpreter.Options()).addDelegate(delegate);

                InputStream inputStream = null;
                MappedByteBuffer loadedFile = null;

                float[][][] inputArrays = new float[30][][];
                float[][][] outputArrays = new float[30][][];

//                 PRE-PROCESSING .CSV FILES
                for(int i = 0 ; i < 30; i++){

                    String testFile = "hapt_walk_l64_s4_testing_user_" + (i+1) + "_sized.csv";
                    Log.d("DAVID: ", testFile);

                    try {
                        inputStream = context.getAssets().open(testFile);
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.d("David", "Catch on reading from assets testFile:  " + testFile);
                    }

                    CSVFile csvFile = new CSVFile(inputStream);

                    float[][] csvValues = csvFile.read();
                    inputArrays[i] = csvValues;

                    float outputArray[][] = new float[csvValues.length][1];
                    outputArrays[i] = outputArray;

                }

                Interpreter models[] = new Interpreter[25];

                // pre-process models
                for(int i = 1; i <= 25; i++){
                    MODEL_NAME = "hapt_walk_mlp_oneout_nomean_"+ i + "__l64_s4200-mobile-nightly.tflite";

                    try {
                        loadedFile = loadModelFile(manager, MODEL_NAME);
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.d("David", "Catch on loading .tflite");
                    }

                    models[i-1] = new Interpreter(loadedFile);



                }


                double[][][] timeslist = new double[repitionCount][25][30];


                double startTime = System.currentTimeMillis();

                for(int k = 0; k < repitionCount; k++){
                    Log.d("David", "Trial: " + (k + 1));

                    // MODEL LOOP
                    for(int model = 1; model <= 25; model++){

                        Interpreter tflite = models[model-1];
//                      Interpreter tfliteGPU = new Interpreter(loadedFile, options);

                        // USER DATA LOOP
                        for(int userData = 1; userData <= 30; userData++){
     //                     Log.d("David", "MODEL: " + model + " TEST CSV: " + userData);

                            double time1 = System.currentTimeMillis();
                            tflite.run(inputArrays[userData-1], outputArrays[c-1]);
//                          tfliteGPU.run(inputArrays[userData-1], outputArrays[userData-1]);

                            double time2 = System.currentTimeMillis() - time1;
                            timeslist[k][model-1][userData-1] = time2;
//                          Log.d("David", "Time Millis: " + time2);

                            // This calculates the average probability from all windows and puts it in the results array
//                          float averageProb = 0;
//                          for(int i = 0; i < inputArrays[userData-1].length; i++)
//                          averageProb += outputArrays[userData-1][i][0];
//                          averageProb = averageProb/inputArrays[userData-1].length;

//                          results[model-1][userData-1] = averageProb;
                        }

                    }

                }

                double avgtime = 0;
                double finaltimeslist[][] = new double[25][30];

                // loop to calculate average time
                for (int model = 0; model < 25; model++)
                {
                    for (int userData = 0; userData < 30; userData++)
                    {
                        for (int k = 0; k < repitionCount; k++)
                        {
                         avgtime += timeslist[k][model][userData];
                        }
                        finaltimeslist[model][userData] = avgtime/repitionCount;
                        avgtime = 0;
                    }
                }

                double elapsedTime = (System.currentTimeMillis() - startTime);

                delegate.close();



                Log.d("David", "Done with accuracy matrix making!");
                ((TextView)findViewById(R.id.textViewNotification)).setText("Done with accuracy matrix making!");


                StringBuilder builder = new StringBuilder();



                // The rest of this code is to write the results Array
                // to a .csv and store it in on-device storage.
                for(int a = 0; a < 25; a++){
                    for(int b = 0; b < 30; b++) {

                        builder.append((finaltimeslist[a][b]));


                        if(b == 29){
                            if(a == 24){
                                break;
                            }

                            builder.append("\n");
                        }
                        else {
                            builder.append(", ");
                        }

                    }

                }

                String CSVout = builder.toString();

                File path = getApplicationContext().getFilesDir();
                File file = new File(path, "timingsAvgMLP200_" + repitionCount + "trials.csv");

                Log.d("David", file.toString());

                FileOutputStream stream = null;
                try {
                    stream = new FileOutputStream(file);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                try {
                    stream.write(CSVout.getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        stream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                ((TextView)findViewById(R.id.textViewNotification)).setText("File Save Completed. Time taken GPU: " + elapsedTime + " ms");


            }

    });




    }
}
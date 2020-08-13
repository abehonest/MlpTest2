package com.example.mlptest2;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;


public class CSVFile {
    InputStream inputStream;


    private int WINDOW_SIZE = 64;
    private int SENSOR_TOTAL = WINDOW_SIZE * 6;
    public int TOTAL_WINDOWS = 1000;

    public CSVFile(InputStream inputStream){
        this.inputStream = inputStream;
    }

    // Reads in a CSV file and turns it into a float array
    public float[][] read(){
        float[][] resultList = new float[TOTAL_WINDOWS][SENSOR_TOTAL];
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        try {
            String csvLine;

            int j = 0;

            int newlineCount = 0;

            while ((csvLine = reader.readLine()) != null) {
                newlineCount++;
                String[] row = csvLine.split(",");
                    for (int i = 0; i < SENSOR_TOTAL; i++) {
                        resultList[j][i] = Float.parseFloat(row[i]);
                    }
                j++;
            }

            float[][] readOut = new float[newlineCount][SENSOR_TOTAL];
            for(int i = 0; i < newlineCount; i++){
                for (int k = 0; k < SENSOR_TOTAL; k++)
                {
                    readOut[i][k] = resultList[i][k];
                }
            }
            resultList = readOut;
        }
        catch (IOException ex) {
            throw new RuntimeException("Error in reading CSV file: "+ex);
        }
        finally {
            try {
                inputStream.close();
            }
            catch (IOException e) {
                throw new RuntimeException("Error while closing input stream: "+e);
            }
        }
        return resultList;
    }
}


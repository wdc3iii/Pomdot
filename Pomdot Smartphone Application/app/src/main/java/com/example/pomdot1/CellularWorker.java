package com.example.pomdot1;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;


import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class CellularWorker extends Worker {

    private boolean result;
    public static final String API_KEY_ARG = "api_key";
    public static final String URL_FORMAT_ARG = "urlFormat";
    public static final String FIELD_INDEX_ARG = "fieldIndex";
    public static final String RESULT_ARG = "result";
    public static final String INDEX_ARG = "index";

    public CellularWorker(Context appContext, WorkerParameters workerParams) {
        super(appContext, workerParams);
        this.result = false;
    }

    @NonNull
    @Override
    public Result doWork() {
        boolean success = true;
        int intValue = -1;
        float floatValue = -1000;
        String urlFormat = getInputData().getString(URL_FORMAT_ARG);
        String api_key = getInputData().getString(API_KEY_ARG);
        int index = getInputData().getInt(FIELD_INDEX_ARG, -1);

        try {
            URL url = new URL(String.format(urlFormat, index, api_key));
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            try {
                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                JSONObject jsonObject = this.readStream(in);
                if (index <= 2) {
                    floatValue = this.parseJsonForFloat(jsonObject, index);
                } else {
                    intValue = this.parseJsonForInt(jsonObject, index);
                }
            } catch (IOException ex) {
                Log.e("IOException", "getInputStream(): " + ex.getMessage());
                success = false;
            } catch (ParseException ex) {
                Log.e("ParseExepction", "getInputStream(): " + ex.getMessage());
                success = false;
            } finally {
                urlConnection.disconnect();
            }
        } catch (MalformedURLException ex) {
            Log.e("MalformedURLException", ex.getMessage());
            success = false;
        } catch (IOException ex) {
            Log.e("IOException", "openConnection(): " + ex.getMessage());
            success = false;
        }
        Data output;
        if (index <= 2) {
            output = new Data.Builder()
                    .putFloat(RESULT_ARG, floatValue)
                    .putInt(INDEX_ARG, index)
                    .build();
        } else {
            output = new Data.Builder()
                    .putInt(RESULT_ARG, intValue)
                    .putInt(INDEX_ARG, index)
                    .build();
        }

        return success ? Result.success(output) : Result.failure();
    }

    public JSONObject readStream(InputStream inputStream) throws ParseException, IOException {
        JSONParser jsonParser = new JSONParser();
        JSONObject jsonObject = (JSONObject) jsonParser.parse(new InputStreamReader(inputStream, "UTF-8"));
        return jsonObject;
    }

    public int parseJsonForInt(JSONObject jsonObject, int fieldIndex) {
        JSONArray feeds = (JSONArray) jsonObject.get("feeds");
        JSONObject feed = (JSONObject) feeds.get(0);
        return Integer.parseInt((String) feed.get(String.format("field%d", fieldIndex)));
    }

    public float parseJsonForFloat(JSONObject jsonObject, int fieldIndex) {
        JSONArray feeds = (JSONArray) jsonObject.get("feeds");
        JSONObject feed = (JSONObject) feeds.get(0);
        return Float.parseFloat((String) feed.get(String.format("field%d", fieldIndex)));
    }
}


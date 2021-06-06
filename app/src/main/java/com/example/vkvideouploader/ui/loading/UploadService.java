package com.example.vkvideouploader.ui.loading;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSink;

public class UploadService extends Service {

    public static final String COMMAND = "COMMAND";
    public static final String LAUNCH = "LAUNCH";
    public static final String PAUSE = "PAUSE";
    public static final String URL = "URL";
    public static final String URI = "URI";
    public static final String CURRENTLENGTH = "CURRENTLENGTH";
    public static final String SIZE = "SIZE";
    public static final String ONFAILURE = "ONFAILURE";
    public static final String CANCEL = "CANCEL";

    private Call call;
    private OkHttpClient client;
    private int currentLength;
    private byte[] buffer;
    private URL url;
    private boolean pause;
    private int size;
    private Uri uri;
    private InputStream inputStream;


    public void onCreate() {
        super.onCreate();
    }

    @SuppressLint("CommitPrefEdits")
    public int onStartCommand(Intent intent, int flags, int startId) {
        switch (intent.getStringExtra(COMMAND)) {
            case LAUNCH:
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
                try {
                    url = new URL(preferences.getString(URL, "").replace("https", "http"));
                } catch (MalformedURLException e) {
                    Toast.makeText(getApplicationContext(),
                            "Bad source video", Toast.LENGTH_SHORT).show();
                    stopSelf();
                }
                uri = Uri.parse(preferences.getString(URI, ""));
                client = new OkHttpClient();
                buffer = new byte[65536 * 32];
                currentLength = intent.getIntExtra(CURRENTLENGTH, 0);
                pause = false;
                try {
                    inputStream = getContentResolver().openInputStream(uri);
                    inputStream.skip(currentLength);
                    size = inputStream.available();
                } catch (IOException e) {
                    Toast.makeText(getApplicationContext(),
                            "Failed to continue uploading", Toast.LENGTH_SHORT).show();
                    stopSelf();
                }
                ExecutorService executorService = Executors.newSingleThreadExecutor();
                executorService.submit(this::upload);
                break;
            case PAUSE:
                saveData();
                pause = true;
                break;
            case CANCEL:
                pause = true;
                break;
        }
        return super.onStartCommand(intent, flags, startId);
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    void upload() {
        int bytesRead = 0;
        try {
            if (!pause && (bytesRead = inputStream.read(buffer)) != -1) {
                int finalBytesRead = bytesRead;
                RequestBody requestBody = new RequestBody() {
                    @Override
                    public MediaType contentType() {
                        return MediaType.parse("multipart/form-data");
                    }

                    @Override
                    public long contentLength() {
                        return finalBytesRead;
                    }

                    @Override
                    public void writeTo(@NotNull BufferedSink sink) throws IOException {
                        sink.write(buffer, 0, finalBytesRead);
                    }
                };

                RequestBody requestBody1 = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("video_file", "video", requestBody)
                        .build();

                Request request = new Request.Builder()
                        .url(url)
                        .addHeader("Content-Range", "bytes " + currentLength + "-" + (currentLength + bytesRead - 1) + '/' + size)
                        .addHeader("Session-Id", UUID.randomUUID().toString())
                        .post(requestBody1)
                        .build();

                int finalLeftRange = currentLength;
                call = client.newCall(request);
                Context context = this;
                call.enqueue(new Callback() {
                    @Override
                    public void onFailure(@NotNull Call call, @NotNull IOException e) {
                        Log.d("onfailure video", e.getMessage());
                        Intent intent = new Intent(LoadingFragment.BROADCAST_ACTION)
                                .putExtra(ONFAILURE, true);
                        sendBroadcast(intent);
                    }

                    @Override
                    public void onResponse(@NotNull Call call, @NotNull Response response) {
                        Log.d("onResponse video", response.message());
                        Intent intent = new Intent(LoadingFragment.BROADCAST_ACTION)
                                .putExtra(CURRENTLENGTH, finalLeftRange + finalBytesRead)
                                .putExtra(SIZE, size);
                        if (finalLeftRange + finalBytesRead >= size) {
                            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
                            preferences.edit().remove(LoadingFragment.UPLOADING).apply();
                            preferences.edit().remove(UploadService.CURRENTLENGTH).apply();
                        }
                        call.cancel();
                        sendBroadcast(intent);
                        currentLength += finalBytesRead;
                        upload();
                    }

                });
            }
        } catch (IOException e) {
            Toast.makeText(getApplicationContext(),
                    "Bad source video", Toast.LENGTH_SHORT).show();
            stopSelf();
        }
    }

    private void saveData() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        preferences.edit().putInt(UploadService.CURRENTLENGTH, currentLength).apply();
        preferences.edit().putInt(UploadService.SIZE, size).apply();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        saveData();
        client.dispatcher().executorService().shutdown();
        try {
            client.cache().close();
            inputStream.close();
        } catch (IOException ignored) {
        }
    }
}

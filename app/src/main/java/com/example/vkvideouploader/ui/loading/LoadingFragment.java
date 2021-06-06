package com.example.vkvideouploader.ui.loading;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.vkvideouploader.R;
import com.example.vkvideouploader.ui.uploaded.UploadedFragment;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class LoadingFragment extends Fragment {

    private Button addVideo;
    private LinearLayout progress;
    private ProgressBar progressBar;
    private Button pause;
    private Button cancel;
    private final static String PAUSE = "PAUSE";
    private final static String CONTINUE = "CONTINUE";
    private String videoUri;
    private String uploadURL;
    private String title;
    private BroadcastReceiver br;

    public final static String BROADCAST_ACTION = "com.example.vkvideouploader.ui.loading";

    public final static String UPLOADING = "UPLOADING";
    public static String FOREGROUND = "FOREGROUND";

    @SuppressLint("CommitPrefEdits")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_loading, container, false);
        progress = view.findViewById(R.id.progress);
        addVideo = view.findViewById(R.id.b_add_video);
        addVideo.setOnClickListener(v -> {
            Intent i = new Intent(getContext(), AddVideoActivity.class);
            someActivityResultLauncher.launch(i);
        });

        setHasOptionsMenu(true);

        progressBar = view.findViewById(R.id.progress_bar);
        pause = view.findViewById(R.id.b_pause);
        cancel = view.findViewById(R.id.b_cancel);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());

        pause.setText(PAUSE);
        pause.setOnClickListener(v -> {
            if (pause.getText() == PAUSE) {
                doUpload(UploadService.PAUSE, null, null);
                pause.setText(CONTINUE);
                preferences.edit().putBoolean(PAUSE, true).apply();
            } else {
                doUpload(UploadService.LAUNCH, videoUri, uploadURL);
                pause.setText(PAUSE);
                preferences.edit().remove(PAUSE).apply();
            }
        });

        cancel.setOnClickListener(v -> {
            doUpload(UploadService.CANCEL, videoUri, uploadURL);
            done();
        });

        br = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if (intent.getBooleanExtra(UploadService.ONFAILURE, false)) {
                    pause.setText(CONTINUE);
                    preferences.edit().putBoolean(PAUSE, true).apply();
                } else {
                    int currentLenght = intent.getIntExtra(UploadService.CURRENTLENGTH, 0);
                    int size = intent.getIntExtra(UploadService.SIZE, 0);
                    update(currentLenght, size);
                }
            }
        };
        IntentFilter intentFilter = new IntentFilter(BROADCAST_ACTION);
        requireActivity().registerReceiver(br, intentFilter);


        if (preferences.getString(UPLOADING, "").equals(UPLOADING)) {
            change();
            update(preferences.getInt(UploadService.CURRENTLENGTH, 0), preferences.getInt(UploadService.SIZE, 0));
            if (preferences.getBoolean(PAUSE, false)) {
                pause.setText(CONTINUE);
            }
        }

        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        requireActivity().unregisterReceiver(br);
    }

    private void change() {
        addVideo.setVisibility(View.GONE);
        progress.setVisibility(View.VISIBLE);
    }

    private final ActivityResultLauncher<Intent> someActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    String uri = data.getStringExtra("uri");

                    title = data.getStringExtra("title");
                    uploadURL = data.getStringExtra("url");
                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
                    preferences.edit().putString(UploadService.URL, uploadURL).apply();
                    change();

                    ExecutorService executorService = Executors.newSingleThreadExecutor();
                    executorService.submit(() -> {
                        File imageFile = new File(getContext().getFilesDir(), "newFile");
                        try (InputStream inputStream = getContext().getContentResolver().openInputStream(Uri.parse(uri))) {
                            Files.copy(inputStream, imageFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                            videoUri = Uri.fromFile(imageFile).toString();
                            preferences.edit().putString(UploadService.URI, videoUri).apply();
                            doUpload(UploadService.LAUNCH, videoUri, uploadURL);
                        } catch (IOException e) {
                            Toast.makeText(getContext(),
                                    "Bad source video", Toast.LENGTH_SHORT).show();
                        }
                    });

                }
            });

    @SuppressLint("CommitPrefEdits")
    private void doUpload(String command, String uri, String url) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        preferences.edit().putString(UPLOADING, UPLOADING).apply();
        Intent intent = new Intent(getContext(), UploadService.class)
                .putExtra(UploadService.CURRENTLENGTH, preferences.getInt(UploadService.CURRENTLENGTH, 0))
                .putExtra(UploadService.COMMAND, command);
        requireActivity().startService(intent);
    }


    @SuppressLint("CommitPrefEdits")
    public void update(int currentLength, int size) {
        if (size == 0)
            return;
        float q = (float) currentLength / size;
        float p = progressBar.getMax() * q;
        progressBar.setProgress((int) p);
        if (currentLength >= size) {
            done();
        }
    }

    @SuppressLint("CommitPrefEdits")
    private void done() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        preferences.edit()
                .remove(UPLOADING)
                .remove(UploadService.CURRENTLENGTH)
                .remove(PAUSE)
                .apply();
        UploadedFragment.getInstance().newVideo(title);
        getActivity().runOnUiThread(() -> {
            addVideo.setVisibility(View.VISIBLE);
            progress.setVisibility(View.GONE);
        });
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.setting_menu, menu);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        if (preferences.getBoolean(FOREGROUND, false)) {
            menu.findItem(R.id.foreground).setChecked(true);
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @SuppressLint({"NonConstantResourceId", "CommitPrefEdits"})
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        switch (item.getItemId()) {
            case R.id.foreground:
                preferences.edit().putBoolean(FOREGROUND, true).apply();
                if (!item.isChecked()) {
                    item.setChecked(true);
                    return true;
                }
            case R.id.background:
                preferences.edit().remove(FOREGROUND).apply();
                if (!item.isChecked()) {
                    item.setChecked(true);
                    return true;
                }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @SuppressLint("CommitPrefEdits")
    @Override
    public void onPause() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        if (preferences.getBoolean(FOREGROUND, false) && pause.getText() == PAUSE) {
            doUpload(UploadService.PAUSE, null, null);
            pause.setText(CONTINUE);
            preferences.edit().putBoolean(PAUSE, true).apply();
        }
        super.onPause();
    }
}
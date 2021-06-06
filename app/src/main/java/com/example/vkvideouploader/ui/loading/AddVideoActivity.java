package com.example.vkvideouploader.ui.loading;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.vkvideouploader.R;
import com.vk.api.sdk.VK;
import com.vk.api.sdk.VKApiCallback;
import com.vk.sdk.api.video.VideoService;
import com.vk.sdk.api.video.dto.VideoSaveResult;

import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class AddVideoActivity extends AppCompatActivity {

    private static final int RESULT_OK = -1;
    private EditText title;
    private EditText description;
    private Uri videoUri;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_video);
        title = findViewById(R.id.et_title);
        description = findViewById(R.id.et_description);

        final Button buttonLoading = findViewById(R.id.b_loading);
        buttonLoading.setText(R.string.button_loading_text);
        buttonLoading.setOnClickListener(v -> {
            Intent i = new Intent(
                    Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
            someActivityResultLauncher.launch(i);
        });

        final Button save = findViewById(R.id.b_save);
        save.setText(R.string.button_save);
        save.setOnClickListener(v -> {
            if (title.getText().toString().equals("")) {
                Toast.makeText(getApplicationContext(),
                        "You didn't enter a title", Toast.LENGTH_SHORT).show();
            } else if (description.getText().toString().equals("")) {
                Toast.makeText(getApplicationContext(),
                        "You didn't enter a description", Toast.LENGTH_SHORT).show();
            } else if (videoUri == null) {
                Toast.makeText(getApplicationContext(),
                        "You didn't select a video", Toast.LENGTH_SHORT).show();
            } else {
                getUploadUrl(videoSaveResult -> {
                    Intent i = new Intent();
                    i.putExtra("title", title.getText().toString());
                    i.putExtra("uri", videoUri.toString());
                    i.putExtra("url", videoSaveResult.getUploadUrl());
                    setResult(Activity.RESULT_OK, i);
                    finish();
                });
            }
        });

    }


    private void getUploadUrl(Consumer<VideoSaveResult> onLoaded) {
        VK.execute(new VideoService().videoSave(
                title.getText().toString(),
                description.getText().toString(),
                false,
                false,
                null,
                null,
                null,
                null,
                null,
                false,
                false,
                false
        ), new VKApiCallback<VideoSaveResult>() {
            @Override
            public void success(VideoSaveResult videoSaveResult) {
                Log.i("video sucsess", "video sucsess");
                onLoaded.accept(videoSaveResult);
            }

            @Override
            public void fail(@NotNull Exception e) {
                e.printStackTrace();
            }
        });
    }

    ActivityResultLauncher<Intent> someActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Intent data = result.getData();
                    videoUri = data.getData();
                }
            });
}
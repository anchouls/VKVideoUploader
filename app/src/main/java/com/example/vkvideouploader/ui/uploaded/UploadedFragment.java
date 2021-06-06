package com.example.vkvideouploader.ui.uploaded;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.vkvideouploader.R;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class UploadedFragment extends Fragment {

    private static VideoAdapter videoAdapter;
    private static UploadedFragment instance;
    private List<String> list;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_uploaded, container, false);
        instance = this;

        RecyclerView videoRecyclerView = view.findViewById(R.id.list_video);
        LinearLayoutManager l = new LinearLayoutManager(view.getContext());
        l.setOrientation(RecyclerView.VERTICAL);
        videoRecyclerView.setLayoutManager(l);
        videoAdapter = new VideoAdapter();
        videoRecyclerView.setAdapter(videoAdapter);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        Gson gson = new Gson();
        String json = preferences.getString("list", "");
        if (!json.equals("")) {
            Type listType = new TypeToken<ArrayList<String>>() {
            }.getType();
            list = gson.fromJson(json, listType);
            UploadedFragment.videoAdapter.setItems(list);
        } else {
            list = new ArrayList<>();
        }

        return view;
    }

    public void newVideo(String s) {
        UploadedFragment.videoAdapter.setItem(s);
        list.add(s);
    }

    public static UploadedFragment getInstance() {
        return instance;
    }


    @SuppressLint("CommitPrefEdits")
    @Override
    public void onPause() {
        super.onPause();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        preferences.edit().remove("list").apply();
        Gson gson = new Gson();
        String json = gson.toJson(list);
        preferences.edit().putString("list", json).apply();
    }

}
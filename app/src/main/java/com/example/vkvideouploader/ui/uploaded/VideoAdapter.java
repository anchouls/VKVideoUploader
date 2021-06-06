package com.example.vkvideouploader.ui.uploaded;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.NotNull;

import com.example.vkvideouploader.R;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class VideoAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<String> list;

    public VideoAdapter() {
        list = new ArrayList<>();
    }

    @Override
    public @NotNull
    VideoViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.video_item_view, parent, false);
        return new VideoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ((VideoAdapter.VideoViewHolder) holder).bind(list.get(position));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public void setItem(String s) {
        list.add(s);
        notifyDataSetChanged();
    }

    public void setItems(Collection<String> s) {
        list.addAll(s);
        notifyDataSetChanged();
    }

    class VideoViewHolder extends RecyclerView.ViewHolder {
        private final TextView nameTextView;

        public VideoViewHolder(View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.text_view);
        }

        public void bind(String s) {
            nameTextView.setText(s);
        }
    }
}

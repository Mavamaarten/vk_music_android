package com.icapps.vkmusic.adapter;

import android.databinding.DataBindingUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.icapps.vkmusic.R;
import com.icapps.vkmusic.databinding.LayoutItemPlaylistBinding;
import com.icapps.vkmusic.model.api.VkApiAlbum;

import java.util.List;

/**
 * Created by maartenvangiel on 23/09/16.
 */
public class VkAlbumAdapter extends RecyclerView.Adapter<VkAlbumAdapter.ViewHolder> {
    private final List<VkApiAlbum> playlists;
    private final VkAudioAdapterListener listener;

    public VkAlbumAdapter(List<VkApiAlbum> playlists, VkAudioAdapterListener listener) {
        this.playlists = playlists;
        this.listener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutItemPlaylistBinding binding = DataBindingUtil.inflate(LayoutInflater.from(parent.getContext()), R.layout.layout_item_playlist, parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        VkApiAlbum audio = playlists.get(position);
        holder.bind(audio);
    }

    @Override
    public int getItemCount() {
        return playlists.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        LayoutItemPlaylistBinding binding;

        ViewHolder(LayoutItemPlaylistBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(VkApiAlbum playlist) {
            binding.getRoot().setOnClickListener(v -> listener.onPlaylistClicked(playlist, getAdapterPosition()));
            binding.setPlaylist(playlist);
            binding.executePendingBindings();
        }
    }

    public interface VkAudioAdapterListener {
        void onPlaylistClicked(VkApiAlbum playlist, int position);
    }
}

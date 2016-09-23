package com.icapps.vkmusic.adapter;

import android.databinding.DataBindingUtil;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.icapps.vkmusic.R;
import com.icapps.vkmusic.databinding.LayoutItemAudioBinding;
import com.vk.sdk.api.model.VKApiAudio;
import com.vk.sdk.api.model.VkAudioArray;

/**
 * Created by maartenvangiel on 23/09/16.
 */
public class VkAudioAdapter extends RecyclerView.Adapter<VkAudioAdapter.ViewHolder> {
    private final VkAudioArray audioArray;
    private final VkAudioAdapterListener listener;

    public VkAudioAdapter(VkAudioArray audioArray, VkAudioAdapterListener listener) {
        this.audioArray = audioArray;
        this.listener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutItemAudioBinding binding = DataBindingUtil.inflate(LayoutInflater.from(parent.getContext()), R.layout.layout_item_audio, parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        VKApiAudio audio = audioArray.get(position);
        holder.bind(audio);
    }

    @Override
    public int getItemCount() {
        return audioArray.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder{
        LayoutItemAudioBinding binding;

        ViewHolder(LayoutItemAudioBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(VKApiAudio audio){
            binding.getRoot().setOnClickListener(v -> listener.onAudioClicked(audio, getAdapterPosition()));
            binding.audioOptions.setOnClickListener(v -> {
                PopupMenu menu = new PopupMenu(v.getContext(), v);
                menu.getMenuInflater().inflate(R.menu.menu_audio_options, menu.getMenu());
                menu.setOnMenuItemClickListener(item -> listener.onAudioMenuItemClicked(audio, getAdapterPosition(), item.getItemId()));
                menu.show();
            });
            binding.setAudio(audio);
            binding.executePendingBindings();
        }
    }

    public interface VkAudioAdapterListener{
        void onAudioClicked(VKApiAudio audio, int position);
        boolean onAudioMenuItemClicked(VKApiAudio audio, int position, int menuItemId);
    }
}

package com.icapps.vkmusic.adapter;

import android.databinding.DataBindingUtil;
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

    public VkAudioAdapter(VkAudioArray audioArray) {
        this.audioArray = audioArray;
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

    static class ViewHolder extends RecyclerView.ViewHolder{
        LayoutItemAudioBinding binding;

        ViewHolder(LayoutItemAudioBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(VKApiAudio audio){
            binding.setAudio(audio);
            binding.executePendingBindings();
        }
    }
}

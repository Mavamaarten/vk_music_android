package com.icapps.vkmusic.adapter;

import android.content.Context;
import android.content.res.Resources;
import android.databinding.DataBindingUtil;
import android.support.annotation.Nullable;
import android.support.v4.content.res.ResourcesCompat;
import android.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.icapps.vkmusic.R;
import com.icapps.vkmusic.databinding.LayoutItemAudioBinding;
import com.vk.sdk.api.model.VKApiAudio;
import com.vk.sdk.api.model.VkAudioArray;

import java.util.Collections;

/**
 * Created by maartenvangiel on 23/09/16.
 */
public class VkAudioAdapter extends RecyclerView.Adapter<VkAudioAdapter.ViewHolder> implements ItemTouchHelperAdapter {
    private final VkAudioArray audioArray;
    private final VkAudioAdapterListener listener;
    private final Resources resources;
    private final boolean reorderable;
    private final StartDragListener startDragListener;

    private VKApiAudio currentAudio;

    public VkAudioAdapter(VkAudioArray audioArray, VkAudioAdapterListener listener, Context context, boolean reorderable, @Nullable StartDragListener startDragListener) {
        this.audioArray = audioArray;
        this.listener = listener;
        this.resources = context.getResources();
        this.reorderable = reorderable;
        this.startDragListener = startDragListener;
    }

    public void setCurrentAudio(VKApiAudio currentAudio) {
        this.currentAudio = currentAudio;
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutItemAudioBinding binding = DataBindingUtil.inflate(LayoutInflater.from(parent.getContext()), R.layout.layout_item_audio, parent, false);
        binding.setReorderable(reorderable);
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

    @Override
    public void onItemMove(int fromPosition, int toPosition) {
        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                Collections.swap(audioArray, i, i + 1);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                Collections.swap(audioArray, i, i - 1);
            }
        }
        notifyItemMoved(fromPosition, toPosition);

        VKApiAudio movedItem = audioArray.get(toPosition);
        if(currentAudio != null && movedItem.id == currentAudio.id){
            listener.onCurrentAudioMoved(toPosition);
        }
    }

    @Override
    public void onItemDismiss(int position) {
        VKApiAudio currentAudio = audioArray.get(position);
        if(currentAudio.getId() != this.currentAudio.getId()){
            audioArray.remove(position);
            notifyItemRemoved(position);
        } else {
            notifyItemChanged(position);
        }
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        LayoutItemAudioBinding binding;

        ViewHolder(LayoutItemAudioBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(VKApiAudio audio) {
            binding.getRoot().setOnClickListener(v -> listener.onAudioClicked(audio, getAdapterPosition()));
            binding.audioOptions.setOnClickListener(v -> {
                PopupMenu menu = new PopupMenu(v.getContext(), v);
                menu.getMenuInflater().inflate(R.menu.menu_audio_options, menu.getMenu());
                menu.setOnMenuItemClickListener(item -> listener.onAudioMenuItemClicked(audio, getAdapterPosition(), item.getItemId()));
                menu.show();
            });
            binding.dragHandle.setOnTouchListener((v, event) -> {
                if(startDragListener == null){
                    return false;
                }
                startDragListener.startDrag(this);
                return true;
            });
            binding.setAudio(audio);
            binding.executePendingBindings();

            if (currentAudio == null || audio.id != currentAudio.id) {
                binding.audioTitle.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, null, null);
            } else {
                binding.audioTitle.setCompoundDrawablesRelativeWithIntrinsicBounds(
                        ResourcesCompat.getDrawable(resources, R.drawable.ic_now_playing_indicator, null),
                        null,
                        null,
                        null
                );
            }
        }
    }

    public interface VkAudioAdapterListener {
        void onAudioClicked(VKApiAudio audio, int position);

        void onCurrentAudioMoved(int toPosition);

        boolean onAudioMenuItemClicked(VKApiAudio audio, int position, int menuItemId);
    }
}

package com.icapps.vkmusic.fragment;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.icapps.vkmusic.R;
import com.icapps.vkmusic.VkApplication;
import com.icapps.vkmusic.adapter.ItemTouchHelperCallback;
import com.icapps.vkmusic.adapter.StartDragListener;
import com.icapps.vkmusic.adapter.VkAudioAdapter;
import com.icapps.vkmusic.base.BaseMusicFragment;
import com.icapps.vkmusic.databinding.FragmentPlaybackqueueBinding;
import com.vk.sdk.VKAccessToken;
import com.vk.sdk.api.model.VKApiAudio;

import javax.inject.Inject;

public class PlaybackQueueFragment extends BaseMusicFragment implements VkAudioAdapter.VkAudioAdapterListener, StartDragListener {
    @Inject VKAccessToken accessToken;

    private FragmentPlaybackqueueBinding binding;
    private VkAudioAdapter adapter;
    private ItemTouchHelper touchHelper;

    public PlaybackQueueFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        adapter = new VkAudioAdapter(playbackQueue, this, getContext(), true, this);
        adapter.setCurrentAudio(currentAudio.get());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_playbackqueue, container, false);

        binding.rcvAudio.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        binding.rcvAudio.setAdapter(adapter);

        ItemTouchHelper.Callback callback = new ItemTouchHelperCallback(adapter);
        touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(binding.rcvAudio);

        return binding.getRoot();
    }

    @Override
    protected void injectDependencies() {
        ((VkApplication) getActivity().getApplication()).getUserComponent().inject(this);
    }

    public void updatePlaybackQueue() {
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onCurrentAudioChanged(VKApiAudio currentAudio) {
        adapter.setCurrentAudio(currentAudio);
    }

    @Override
    public void onAudioClicked(VKApiAudio audio, int position) {
        musicService.playAudio(playbackQueue, position);
    }

    @Override
    public void onCurrentAudioMoved(int toPosition) {
        musicService.setCurrentIndex(toPosition);
    }

    @Override
    public boolean onAudioMenuItemClicked(final VKApiAudio audio, int position, int menuItemId) {
        switch(menuItemId){
            case R.id.action_play:
                musicService.playAudio(playbackQueue, position);
                break;

            case R.id.action_play_next:
                playbackQueue.remove(position);
                musicService.addTrackAsNextInQueue(audio);
                break;

            case R.id.action_add_to_playlist:
                // TODO: implement
                break;
        }
        return true;
    }

    @Override
    public void startDrag(RecyclerView.ViewHolder viewHolder) {
        touchHelper.startDrag(viewHolder);
    }
}

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
import com.icapps.vkmusic.activity.MainActivity;
import com.icapps.vkmusic.adapter.ItemTouchHelperCallback;
import com.icapps.vkmusic.adapter.StartDragListener;
import com.icapps.vkmusic.adapter.VkAudioAdapter;
import com.icapps.vkmusic.base.BaseMusicFragment;
import com.icapps.vkmusic.databinding.FragmentPlaybackqueueBinding;
import com.icapps.vkmusic.dialog.AddTrackToPlaylistDialogFragment;
import com.icapps.vkmusic.service.MusicService;
import com.icapps.vkmusic.util.DownloadUtil;
import com.vk.sdk.api.model.VKApiAudio;

public class PlaybackQueueFragment extends BaseMusicFragment implements VkAudioAdapter.VkAudioAdapterListener, StartDragListener {
    private FragmentPlaybackqueueBinding binding;
    private VkAudioAdapter adapter;
    private ItemTouchHelper touchHelper;

    public PlaybackQueueFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        adapter = new VkAudioAdapter(playbackQueue, this, getContext());
        adapter.setReorderable(true);
        adapter.setStartDragListener(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_playbackqueue, container, false);

        binding.rcvAudio.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        binding.rcvAudio.setAdapter(adapter);
        binding.clearQueue.setOnClickListener(v -> onClearQueueClicked());

        ItemTouchHelper.Callback callback = new ItemTouchHelperCallback(adapter);
        touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(binding.rcvAudio);

        updateQueueSizeLabel();

        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (musicService == null || musicService.getState() == MusicService.PlaybackState.STOPPED) {
            adapter.setCurrentAudio(null);
        } else {
            adapter.setCurrentAudio(currentAudio.get());
        }
        getActivity().setTitle(R.string.playback_queue);
    }

    @Override
    protected void injectDependencies() {
        ((VkApplication) getActivity().getApplication()).getUserComponent().inject(this);
    }

    public void updatePlaybackQueue() {
        if (isVisible()) {
            adapter.notifyDataSetChanged();
            updateQueueSizeLabel();
        }
    }

    private void updateQueueSizeLabel() {
        if (playbackQueue.size() == 1) {
            binding.queueSize.setText(R.string.track_in_queue);
        } else {
            binding.queueSize.setText(getString(R.string.tracks_in_queue, playbackQueue.size()));
        }
    }

    @Override
    public void onPlaybackStateChanged(MusicService.PlaybackState state) {
        if (musicService == null || state == MusicService.PlaybackState.STOPPED) {
            adapter.setCurrentAudio(null);
        } else {
            adapter.setCurrentAudio(currentAudio.get());
        }
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
        switch (menuItemId) {
            case R.id.action_play:
                musicService.playAudio(playbackQueue, position);
                break;

            case R.id.action_play_next:
                playbackQueue.remove(position);
                musicService.addTrackAsNextInQueue(audio);
                break;

            case R.id.action_add_to_playlist:
                Bundle arguments = new Bundle();
                arguments.putParcelable(AddTrackToPlaylistDialogFragment.KEY_AUDIO, audio);

                AddTrackToPlaylistDialogFragment addTrackToPlaylistDialogFragment = new AddTrackToPlaylistDialogFragment();
                addTrackToPlaylistDialogFragment.setArguments(arguments);
                addTrackToPlaylistDialogFragment.show(getActivity().getFragmentManager(), "playlist_selection");
                break;

            case R.id.action_track_radio:
                ((MainActivity) getActivity()).startRadio(audio);
                break;

            case R.id.action_download:
                DownloadUtil.downloadTrack(getContext(), audio);
                break;
        }
        return true;
    }

    private void onClearQueueClicked() {
        musicService.clearQueue();
    }

    @Override
    public void startDrag(RecyclerView.ViewHolder viewHolder) {
        touchHelper.startDrag(viewHolder);
    }
}

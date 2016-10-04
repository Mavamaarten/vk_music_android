package com.icapps.vkmusic.fragment;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.icapps.vkmusic.R;
import com.icapps.vkmusic.VkApplication;
import com.icapps.vkmusic.activity.MainActivity;
import com.icapps.vkmusic.adapter.VkAudioAdapter;
import com.icapps.vkmusic.base.BaseMusicFragment;
import com.icapps.vkmusic.databinding.FragmentRadioBinding;
import com.icapps.vkmusic.dialog.AddTrackToPlaylistDialogFragment;
import com.icapps.vkmusic.service.MusicService;
import com.icapps.vkmusic.util.DownloadUtil;
import com.vk.sdk.api.VKApi;
import com.vk.sdk.api.VKApiConst;
import com.vk.sdk.api.VKError;
import com.vk.sdk.api.VKParameters;
import com.vk.sdk.api.VKRequest;
import com.vk.sdk.api.VKResponse;
import com.vk.sdk.api.model.VKApiAudio;
import com.vk.sdk.api.model.VkAudioArray;

import icepick.State;
import rx.functions.Action0;

/**
 * Created by maartenvangiel on 28/09/16.
 */
public class RadioFragment extends BaseMusicFragment implements VkAudioAdapter.VkAudioAdapterListener {
    @State VkAudioArray audioArray;
    @State boolean startRadioWhenShown;
    @State VKApiAudio radioTrack;

    private FragmentRadioBinding binding;
    private VkAudioAdapter adapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (audioArray == null) {
            audioArray = new VkAudioArray();
        }
    }

    public void setStartRadioWhenShown(boolean startRadioWhenShown) {
        this.startRadioWhenShown = startRadioWhenShown;
    }

    public void startRadio(@Nullable VKApiAudio radioTrack) {
        startRadioWhenShown = false;
        loadRadio(radioTrack, () -> {
            if (audioArray.size() > 0) {
                onAudioClicked(audioArray.get(0), 0);
            }
        });
    }

    public void loadRadio(@Nullable VKApiAudio radioTrack, @Nullable Action0 onCompletedCallback) {
        binding.loadingIndicator.setVisibility(View.VISIBLE);

        VKParameters parameters;
        if (radioTrack == null) {
            parameters = VKParameters.from(VKApiConst.COUNT, 100, "shuffle", 1);
        } else {
            parameters = VKParameters.from("target_audio", radioTrack.owner_id + "_" + radioTrack.id, VKApiConst.COUNT, 100, "shuffle", 1);
        }

        VKApi.audio().getRecommendations(parameters).executeWithListener(new VKRequest.VKRequestListener() {
            @Override
            public void onComplete(VKResponse response) {
                audioArray.clear();
                audioArray.addAll((VkAudioArray) response.parsedModel);
                adapter.notifyDataSetChanged();
                binding.loadingIndicator.setVisibility(View.GONE);

                if (audioArray.size() == 0) {
                    Snackbar.make(binding.getRoot(), R.string.no_similar_tracks, Snackbar.LENGTH_LONG).show();
                    binding.noData.getRoot().setVisibility(View.VISIBLE);
                } else {
                    binding.noData.getRoot().setVisibility(View.GONE);
                }

                if (onCompletedCallback != null) onCompletedCallback.call();
            }

            @Override
            public void onError(VKError error) {
                Snackbar.make(binding.rcvAudio, R.string.error_loading_radio, Snackbar.LENGTH_LONG);
                binding.loadingIndicator.setVisibility(View.GONE);
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_radio, container, false);

        adapter = new VkAudioAdapter(audioArray, this, getContext());
        binding.rcvAudio.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        binding.rcvAudio.setAdapter(adapter);

        if (audioArray.size() == 0) {
            loadRadio(radioTrack, null);
        }

        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();

        getActivity().setTitle(R.string.radio);

        if (musicService == null || musicService.getState() == MusicService.PlaybackState.STOPPED) {
            adapter.setCurrentAudio(null);
        } else {
            adapter.setCurrentAudio(currentAudio.get());
        }

        if (startRadioWhenShown) {
            startRadio(radioTrack);
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
    protected void injectDependencies() {
        ((VkApplication) getActivity().getApplication()).getUserComponent().inject(this);
    }

    @Override
    public void onAudioClicked(VKApiAudio audio, int position) {
        musicService.playAudio(audioArray, position);
    }

    @Override
    public void onCurrentAudioMoved(int toPosition) {
        // Not applicable to Radio
    }

    @Override
    public boolean onAudioMenuItemClicked(VKApiAudio audio, int position, int menuItemId) {
        switch (menuItemId) {
            case R.id.action_play:
                musicService.playAudio(audioArray, position);
                break;

            case R.id.action_play_next:
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

    public void setRadioTrack(VKApiAudio radioTrack) {
        this.radioTrack = radioTrack;
    }
}

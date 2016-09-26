package com.icapps.vkmusic.fragment;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.icapps.vkmusic.R;
import com.icapps.vkmusic.VkApplication;
import com.icapps.vkmusic.base.BaseFragment;
import com.icapps.vkmusic.databinding.FragmentNowPlayingBinding;
import com.icapps.vkmusic.model.albumart.AlbumArtProvider;
import com.icapps.vkmusic.service.MusicService;
import com.vk.sdk.api.model.VKApiAudio;

import javax.inject.Inject;

import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;


public class NowPlayingFragment extends BaseFragment {
    @Inject AlbumArtProvider albumArtProvider;

    private FragmentNowPlayingBinding binding;
    private PlaybackControlsListener listener;

    public NowPlayingFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_now_playing, container, false);

        binding.next.setOnClickListener(v -> listener.onNextClicked());
        binding.previous.setOnClickListener(v -> listener.onPreviousClicked());
        binding.playPause.setOnClickListener(v -> listener.onPlayPauseClicked());
        binding.playPauseTop.setOnClickListener(v -> listener.onPlayPauseClicked());
        binding.playbackPosition.

        return binding.getRoot();
    }

    @Override
    public void onAttach(Context context) {
        if (context instanceof PlaybackControlsListener) {
            listener = (PlaybackControlsListener) context;
        } else {
            throw new RuntimeException("Context does not implement PlaybackControlsListener");
        }

        super.onAttach(context);
    }

    public void setCurrentAudio(VKApiAudio currentAudio) {
        binding.setCurrentAudio(currentAudio);

        albumArtProvider.getAlbumArtUrl(currentAudio.artist + " - " + currentAudio.title)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(url -> {
                    Glide.with(getContext())
                            .load(url)
                            .centerCrop()
                            .into(binding.albumSmall);

                    Glide.with(getContext())
                            .load(url)
                            .centerCrop()
                            .into(binding.albumLarge);
                }, throwable -> {
                    // TODO Uhhh do nothing?
                });
    }

    public void setPlaybackPosition(int playbackPosition){
        binding.setPlaybackPosition(playbackPosition);
    }

    public void setPlaybackState(MusicService.PlaybackState playbackState){
        binding.setPlaybackState(playbackState);
    }

    @Override
    protected void inject() {
        ((VkApplication)getActivity().getApplication()).getUserComponent().inject(this);
    }

    public interface PlaybackControlsListener {
        void onPreviousClicked();

        void onNextClicked();

        void onPlayPauseClicked();
    }
}

package com.icapps.vkmusic.fragment;

import android.databinding.DataBindingUtil;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;

import com.bumptech.glide.Glide;
import com.icapps.vkmusic.R;
import com.icapps.vkmusic.VkApplication;
import com.icapps.vkmusic.base.BaseMusicFragment;
import com.icapps.vkmusic.databinding.FragmentNowPlayingBinding;
import com.icapps.vkmusic.model.albumart.AlbumArtProvider;
import com.icapps.vkmusic.service.MusicService;
import com.vk.sdk.api.model.VKApiAudio;

import javax.inject.Inject;

public class NowPlayingFragment extends BaseMusicFragment {
    private FragmentNowPlayingBinding binding;
    private Drawable placeholderDrawable;

    public NowPlayingFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_now_playing, container, false);

        placeholderDrawable = VectorDrawableCompat.create(getResources(), R.drawable.ic_album_placeholder, null);
        DrawableCompat.setTint(placeholderDrawable, ResourcesCompat.getColor(getResources(), R.color.md_grey_600, null));
        binding.albumSmall.setImageDrawable(placeholderDrawable);
        binding.albumLarge.setImageDrawable(placeholderDrawable);

        if (currentAudio.get() != null) {
            onCurrentAudioChanged(currentAudio.get());
        }
        if (currentAlbumArtUrl.get() != null) {
            onCurrentAlbumArtChanged(currentAlbumArtUrl.get());
        }

        binding.next.setOnClickListener(v -> onNextClicked());
        binding.previous.setOnClickListener(v -> onPreviousClicked());
        binding.playPause.setOnClickListener(v -> onPlayPauseClicked());
        binding.playPauseTop.setOnClickListener(v -> onPlayPauseClicked());
        binding.playbackPosition.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Do nothing
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                musicService.stopPlaybackPositionUpdating();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                musicService.seek(seekBar.getProgress());
            }
        });

        binding.setCurrentAudio(currentAudio.get());
        return binding.getRoot();
    }

    private void onPlayPauseClicked() {
        musicService.playPause();
    }

    private void onPreviousClicked() {
        musicService.playPreviousTrackInQueue();
    }

    private void onNextClicked() {
        musicService.playNextTrackInQueue();
    }

    public void setPlaybackPosition(int playbackPosition) {
        binding.setPlaybackPosition(playbackPosition);
    }

    public void setPlaybackState(MusicService.PlaybackState playbackState) {
        binding.setPlaybackState(playbackState);
    }

    @Override
    protected void injectDependencies() {
        ((VkApplication) getActivity().getApplication()).getUserComponent().inject(this);
    }

    @Override
    protected void onCurrentAudioChanged(VKApiAudio currentAudio) {
        binding.setCurrentAudio(currentAudio);
    }

    @Override
    protected void onCurrentAlbumArtChanged(String currentAlbumArtUrl) {
        Glide.with(this)
                .load(currentAlbumArtUrl)
                .error(R.drawable.ic_album_placeholder)
                .placeholder(R.drawable.ic_album_placeholder)
                .into(binding.albumLarge);

        Glide.with(this)
                .load(currentAlbumArtUrl)
                .error(R.drawable.ic_album_placeholder)
                .placeholder(R.drawable.ic_album_placeholder)
                .into(binding.albumSmall);
    }
}

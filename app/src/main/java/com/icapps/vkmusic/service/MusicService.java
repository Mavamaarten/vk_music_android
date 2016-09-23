package com.icapps.vkmusic.service;

import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.vk.sdk.api.model.VKApiAudio;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by maartenvangiel on 23/09/16.
 */
public class MusicService extends Service implements MediaPlayer.OnPreparedListener {
    private final IBinder binder = new MusicServiceBinder();
    private MediaPlayer mediaPlayer;
    private final List<MusicServiceListener> listeners = new ArrayList<>();
    private Thread playbackPositionThread;
    private PlaybackState state = PlaybackState.STOPPED;

    @Override
    public void onCreate() {
        super.onCreate();
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setOnPreparedListener(this);
    }

    public void setState(PlaybackState state) {
        this.state = state;
        for (MusicServiceListener listener : listeners) {
            listener.onPlaybackStateChanged(state);
        }
    }

    public void addMusicServiceListener(MusicServiceListener listener) {
        listeners.add(listener);
    }

    public void removeMusicServiceListener(MusicServiceListener listener) {
        listeners.remove(listener);
    }

    public void playAudio(VKApiAudio audio) {
        System.out.println("PlayAudio " + audio);
        try {
            mediaPlayer.reset();
            mediaPlayer.setDataSource(audio.url);
            mediaPlayer.prepareAsync();
            setState(PlaybackState.PREPARING);
        } catch (IOException e) {
            for (MusicServiceListener listener : listeners) {
                listener.onMusicServiceException(e);
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mp.start();
        setState(PlaybackState.PLAYING);
        if (playbackPositionThread != null) playbackPositionThread.interrupt();
        playbackPositionThread = new PlaybackPositionThread();
        playbackPositionThread.start();
    }

    public class PlaybackPositionThread extends Thread {
        @Override
        public void run() {
            while (!interrupted() && mediaPlayer != null) {
                for (MusicServiceListener listener : listeners) {
                    listener.onPlaybackPositionChanged(mediaPlayer.getCurrentPosition() / 1000);
                }
                try {
                    sleep(1000);
                } catch (InterruptedException ignored) {
                }
            }
        }
    }

    public class MusicServiceBinder extends Binder {
        public MusicService getService() {
            return MusicService.this;
        }
    }

    public enum PlaybackState {
        STOPPED,
        PREPARING,
        PLAYING,
        PAUSED
    }

    public interface MusicServiceListener {
        void onMusicServiceException(Exception ex);

        void onPlaybackStateChanged(PlaybackState state);

        void onPlaybackPositionChanged(int position);
    }
}

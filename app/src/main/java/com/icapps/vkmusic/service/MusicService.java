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
public class MusicService extends Service implements MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener {
    private final IBinder binder = new MusicServiceBinder();
    private final List<MusicServiceListener> listeners = new ArrayList<>();
    private MediaPlayer mediaPlayer;
    private Thread playbackPositionThread;
    private PlaybackState state = PlaybackState.STOPPED;
    private VKApiAudio currentAudio;

    @Override
    public void onCreate() {
        super.onCreate();
        System.out.println("Service started");
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setOnCompletionListener(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        System.out.println("Service destroyed");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    public void setState(PlaybackState state) {
        this.state = state;
        for (MusicServiceListener listener : listeners) {
            listener.onPlaybackStateChanged(state);
        }
        System.out.println(state.name());
    }

    public void addMusicServiceListener(MusicServiceListener listener) {
        listeners.add(listener);
        listener.onPlaybackStateChanged(state);
        if (currentAudio != null) {
            listener.onCurrentAudioChanged(currentAudio);
        }
    }

    public void removeMusicServiceListener(MusicServiceListener listener) {
        listeners.remove(listener);
    }

    public void playAudio(VKApiAudio audio) {
        try {
            mediaPlayer.reset();
            mediaPlayer.setDataSource(audio.url);
            mediaPlayer.prepareAsync();
            currentAudio = audio;
            setState(PlaybackState.PREPARING);
        } catch (IOException e) {
            for (MusicServiceListener listener : listeners) {
                listener.onMusicServiceException(e);
            }
            setState(PlaybackState.STOPPED);
        }
    }

    public PlaybackState getState() {
        return state;
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

    public void pause() {
        if (state == PlaybackState.PLAYING) {
            mediaPlayer.pause();
            setState(PlaybackState.PAUSED);
        }
    }

    public void resume() {
        mediaPlayer.start();
        setState(PlaybackState.PLAYING);
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        setState(PlaybackState.STOPPED);
        for (MusicServiceListener listener : listeners) {
            listener.onMusicServiceException(new Exception("An exception occurred while playing back your track"));
        }
        return false;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        setState(PlaybackState.STOPPED);
    }

    public VKApiAudio getCurrentAudio() {
        return currentAudio;
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

        void onCurrentAudioChanged(VKApiAudio audio);
    }
}

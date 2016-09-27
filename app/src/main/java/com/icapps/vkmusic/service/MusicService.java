package com.icapps.vkmusic.service;

import android.app.Service;
import android.content.Intent;
import android.databinding.ObservableField;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.icapps.vkmusic.VkApplication;
import com.vk.sdk.api.model.VKApiAudio;
import com.vk.sdk.api.model.VkAudioArray;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;

/**
 * Created by maartenvangiel on 23/09/16.
 */
public class MusicService extends Service implements MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnSeekCompleteListener {
    @Inject VkAudioArray playbackQueue;
    @Inject ObservableField<VKApiAudio> currentAudio;

    private final IBinder binder = new MusicServiceBinder();
    private final List<MusicServiceListener> listeners = new ArrayList<>();
    private MediaPlayer mediaPlayer;
    private Thread playbackPositionThread;
    private PlaybackState state = PlaybackState.STOPPED;
    private int currentIndex;

    @Override
    public void onCreate() {
        super.onCreate();

        ((VkApplication) getApplication()).getAppComponent().inject(this);

        System.out.println("MusicService started");
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnSeekCompleteListener(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        System.out.println("MusicService destroyed");
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
    }

    public void removeMusicServiceListener(MusicServiceListener listener) {
        listeners.remove(listener);
    }

    /**
     * Plays a track, and sets the playback queue to the "source" of tracks that was used to initiate the playback
     *
     * @param audioArray the collection of tracks to be played
     * @param position   the index of the track to play
     */
    public void playAudio(VkAudioArray audioArray, int position) {
        playAudio(audioArray.get(position));

        if (!playbackQueue.equals(audioArray)) {
            playbackQueue.clear();
            playbackQueue.addAll(audioArray);
        }

        currentIndex = position;
        for (MusicServiceListener listener : listeners) {
            listener.onPlaybackQueueChanged();
        }
    }

    /**
     * Plays a single track, without setting the playback queue
     *
     * @param audio the track to be played
     */
    public void playAudio(VKApiAudio audio) {
        try {
            mediaPlayer.reset();
            mediaPlayer.setDataSource(audio.url);
            mediaPlayer.prepareAsync();
            currentAudio.set(audio);
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

    /**
     * Adds a track as the next track in the queue
     * Removes duplicate/existing instances of the track from the queue
     *
     * @param audio the track to be added to the queue
     */
    public void addTrackAsNextInQueue(VKApiAudio audio) {
        Iterator<VKApiAudio> iterator = playbackQueue.listIterator();
        while (iterator.hasNext()) {
            final VKApiAudio a = iterator.next();
            if (a.id == audio.id) iterator.remove();
        }

        playbackQueue.add(Math.min(currentIndex + 1, playbackQueue.size()), audio);
        for (MusicServiceListener listener : listeners) {
            listener.onPlaybackQueueChanged();
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
        startPlaybackPositionUpdating();
    }

    public void startPlaybackPositionUpdating() {
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
        if (currentIndex < playbackQueue.size() - 1) {
            playNextTrackInQueue();
        } else {
            setState(PlaybackState.STOPPED);
        }
    }

    public void playNextTrackInQueue() {
        if (currentIndex < playbackQueue.size() - 1) {
            VKApiAudio audio = playbackQueue.get(++currentIndex);
            playAudio(audio);
        }
    }

    public void playPreviousTrackInQueue() {
        if (currentIndex > 0) {
            VKApiAudio audio = playbackQueue.get(--currentIndex);
            playAudio(audio);
        }
    }

    public void stopPlaybackPositionUpdating() {
        if (playbackPositionThread != null) {
            playbackPositionThread.interrupt();
        }
    }

    public void seek(int position) {
        mediaPlayer.seekTo(position * 1000);
    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {
        startPlaybackPositionUpdating();
    }

    public void setCurrentIndex(int currentIndex) {
        this.currentIndex = currentIndex;
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public void clearQueue() {
        playbackQueue.clear();
        for (MusicServiceListener listener : listeners) {
            listener.onPlaybackQueueChanged();
        }
    }

    public class PlaybackPositionThread extends Thread {
        @Override
        public void run() {
            while (!isInterrupted() && mediaPlayer != null) {
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

        void onPlaybackQueueChanged();
    }
}

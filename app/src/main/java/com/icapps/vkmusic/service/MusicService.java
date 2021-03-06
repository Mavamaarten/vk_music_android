package com.icapps.vkmusic.service;

import android.app.Service;
import android.content.Intent;
import android.databinding.Observable;
import android.databinding.ObservableBoolean;
import android.databinding.ObservableField;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.icapps.vkmusic.R;
import com.icapps.vkmusic.VkApplication;
import com.icapps.vkmusic.activity.MainActivity;
import com.icapps.vkmusic.model.albumart.AlbumArtProvider;
import com.vk.sdk.api.VKApi;
import com.vk.sdk.api.VKParameters;
import com.vk.sdk.api.VKRequest;
import com.vk.sdk.api.VKResponse;
import com.vk.sdk.api.model.VKApiAudio;
import com.vk.sdk.api.model.VkAudioArray;

import java.io.IOException;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import javax.inject.Inject;
import javax.inject.Named;

import io.paperdb.Paper;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by maartenvangiel on 23/09/16.
 */
public class MusicService extends Service implements MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnSeekCompleteListener {
    public static final String KEY_ACTION = "MUSIC_ACTION";
    public static final int ACTION_PLAY_PAUSE = 1;
    public static final int ACTION_PREVIOUS = 2;
    public static final int ACTION_NEXT = 3;
    public static final int ACTION_DISMISS = 4;
    public static final int ACTION_OPEN_ACTIVITY = 5;
    public static final int ACTION_ADD = 6;

    @Inject VkAudioArray playbackQueue;
    @Inject ObservableField<VKApiAudio> currentAudio;
    @Inject ObservableField<Bitmap> currentAlbumArt;
    @Inject AlbumArtProvider albumArtProvider;
    @Inject
    @Named("shuffle")
    ObservableBoolean shuffleSetting;
    @Inject
    @Named("repeat")
    ObservableBoolean repeatSetting;

    private final IBinder binder = new MusicServiceBinder();
    private final List<MusicServiceListener> listeners = new ArrayList<>();

    private MusicNotificationManager notificationManager;

    private MediaPlayer mediaPlayer;
    private Handler mainThreadHandler;
    private Thread playbackPositionThread;
    private PlaybackState state = PlaybackState.STOPPED;
    private int currentIndex;
    private Random random = new Random();
    private Observable.OnPropertyChangedCallback onShuffleChangedCallback;
    private Observable.OnPropertyChangedCallback onRepeatChangedCallback;

    public void onCreate() {
        super.onCreate();

        ((VkApplication) getApplication()).getAppComponent().inject(this);

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnSeekCompleteListener(this);

        notificationManager = new MusicNotificationManager(this);

        if (currentAudio.get() != null) {
            notificationManager.updateNotification(currentAudio.get());
        }

        if (currentAlbumArt.get() == null) {
            Glide.with(MusicService.this)
                    .load((String) Paper.book().read("currentAlbumArtUrl"))
                    .asBitmap()
                    .error(R.drawable.ic_album_placeholder)
                    .into(new SimpleTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(Bitmap bitmap, GlideAnimation<? super Bitmap> glideAnimation) {
                            currentAlbumArt.set(bitmap);
                            notificationManager.updateNotificationBitmap(bitmap);
                        }
                    });
        }

        mainThreadHandler = new Handler();

        onShuffleChangedCallback = new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable observable, int i) {
                Paper.book().write("shuffle", shuffleSetting);
            }
        };
        onRepeatChangedCallback = new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable observable, int i) {
                Paper.book().write("repeat", repeatSetting);
            }
        };
        shuffleSetting.addOnPropertyChangedCallback(onShuffleChangedCallback);
        repeatSetting.addOnPropertyChangedCallback(onRepeatChangedCallback);

        System.out.println("MusicService started");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mediaPlayer.release();
        shuffleSetting.removeOnPropertyChangedCallback(onShuffleChangedCallback);
        repeatSetting.removeOnPropertyChangedCallback(onRepeatChangedCallback);
        System.out.println("MusicService destroyed");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        if (intent != null && intent.hasExtra(KEY_ACTION)) {
            switch (intent.getIntExtra(KEY_ACTION, 0)) {
                case ACTION_PLAY_PAUSE:
                    playPause();
                    break;

                case ACTION_PREVIOUS:
                    playPreviousTrackInQueue();
                    break;

                case ACTION_NEXT:
                    playNextTrackInQueue();
                    break;

                case ACTION_DISMISS:
                    mediaPlayer.stop();
                    notificationManager.destroyNotification();

                    for (MusicServiceListener listener : listeners) {
                        listener.onFinishRequested();
                    }

                    stopSelf();
                    break;

                case ACTION_OPEN_ACTIVITY:
                    if (listeners.size() == 0) {
                        Intent mainActivityIntent = new Intent(this, MainActivity.class);
                        mainActivityIntent.putExtra(MainActivity.KEY_INITIAL_FRAGMENT, MainActivity.FRAG_NOW_PLAYING);
                        mainActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                        startActivity(mainActivityIntent);
                    }
                    break;

                case ACTION_ADD:
                    final VKApiAudio audio = currentAudio.get();
                    VKApi.audio().add(VKParameters.from("audio_id", audio.id, "owner_id", audio.owner_id)).executeWithListener(new VKRequest.VKRequestListener() {
                        @Override
                        public void onComplete(VKResponse response) {
                            notificationManager.showAddCompleteIndicator();
                        }
                    });
                    break;
            }
        }

        return START_STICKY;
    }

    public void setState(PlaybackState state) {
        this.state = state;

        mainThreadHandler.post(() -> {
            for (MusicServiceListener listener : listeners) {
                listener.onPlaybackStateChanged(state);
            }
        });

        notificationManager.updateNotificationPlaybackState(state);
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
        if (audioArray.size() == 0 || position < 0 || position > audioArray.size() - 1) {
            return;
        }

        playAudio(audioArray.get(position));

        if (!playbackQueue.equals(audioArray)) {
            playbackQueue.clear();
            playbackQueue.addAll(audioArray);
        }

        savePlaybackQueue();

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
        new Thread(() -> {
            try {
                mediaPlayer.reset();
                mediaPlayer.setDataSource(audio.url);
                mediaPlayer.prepareAsync();

                currentAudio.set(audio);
                saveCurrentAudio();

                setState(PlaybackState.PREPARING);

                notificationManager.createNotification();
                notificationManager.updateNotification(audio);

                loadAlbumArt(audio);
            } catch (IOException e) {
                for (MusicServiceListener listener : listeners) {
                    listener.onMusicServiceException(e);
                }
                setState(PlaybackState.STOPPED);
            }
        }).start();
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
        savePlaybackQueue();

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

    public void loadAlbumArt(VKApiAudio audio) {
        Bitmap placeHolder = BitmapFactory.decodeResource(getResources(), R.drawable.ic_album_placeholder);
        currentAlbumArt.set(placeHolder);
        notificationManager.updateNotificationBitmap(placeHolder);

        albumArtProvider.getAlbumArtUrl(audio.artist + " - " + audio.title)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(url -> {
                    saveAlbumArtUrl(url);

                    Glide.with(MusicService.this)
                            .load(url)
                            .asBitmap()
                            .error(R.drawable.ic_album_placeholder)
                            .into(new SimpleTarget<Bitmap>() {
                                @Override
                                public void onResourceReady(Bitmap bitmap, GlideAnimation<? super Bitmap> glideAnimation) {
                                    currentAlbumArt.set(bitmap);
                                    notificationManager.updateNotificationBitmap(bitmap);
                                }
                            });
                }, throwable -> {
                    saveAlbumArtUrl("");
                    currentAlbumArt.set(placeHolder);
                    notificationManager.updateNotificationBitmap(placeHolder);
                });
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
        if (shuffleSetting.get()) {
            int randomIndex = random.nextInt(playbackQueue.size());
            currentIndex = randomIndex;
            VKApiAudio audio = playbackQueue.get(randomIndex);
            playAudio(audio);
        } else {
            if (currentIndex < playbackQueue.size() - 1) { // Valid currentIndex and there are more tracks in the queue
                VKApiAudio audio = playbackQueue.get(++currentIndex);
                playAudio(audio);
            } else if (currentIndex > playbackQueue.size() && playbackQueue.size() > 0) { // Invalid currentIndex: restart from 0
                currentIndex = 0;
                VKApiAudio audio = playbackQueue.get(currentIndex);
                playAudio(audio);
            } else if(currentIndex == playbackQueue.size() - 1 && repeatSetting.get()){ // Last track and repeat is enabled
                currentIndex = 0;
                VKApiAudio audio = playbackQueue.get(currentIndex);
                playAudio(audio);
            }
        }
    }

    public void playPreviousTrackInQueue() {
        if ((currentIndex - 1) >= 0 && (currentIndex < playbackQueue.size())) { // Valid currentIndex and there are more tracks in the queue
            VKApiAudio audio = playbackQueue.get(--currentIndex);
            playAudio(audio);
        } else if (currentIndex >= playbackQueue.size() && playbackQueue.size() > 0) { // Invalid currentIndex: restart from 0;
            currentIndex = 0;
            VKApiAudio audio = playbackQueue.get(currentIndex);
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

    public void clearQueue() {
        playbackQueue.clear();
        savePlaybackQueue();
        for (MusicServiceListener listener : listeners) {
            listener.onPlaybackQueueChanged();
        }
    }

    public void savePlaybackQueue() {
        Paper.book().write("playbackQueue", playbackQueue);
    }

    public void saveCurrentAudio() {
        Paper.book().write("currentAudio", currentAudio);
    }

    public void saveAlbumArtUrl(String currentAlbumArtUrl) {
        Paper.book().write("currentAlbumArtUrl", currentAlbumArtUrl);
    }

    public void playPause() {
        switch (state) {
            case STOPPED:
                if (currentAudio.get() != null) {
                    playAudio(currentAudio.get());
                }
                break;
            case PREPARING:
                break;
            case PLAYING:
                pause();
                break;
            case PAUSED:
                resume();
                break;
        }
    }

    public class MusicServiceBinder extends Binder {
        public MusicService getService() {
            return MusicService.this;
        }
    }

    public class PlaybackPositionThread extends Thread {
        @Override
        public void run() {
            while (!isInterrupted() && mediaPlayer != null) {
                try {
                    for (MusicServiceListener listener : listeners) {
                        listener.onPlaybackPositionChanged(mediaPlayer.getCurrentPosition() / 1000);
                    }
                    sleep(1000);
                } catch (InterruptedException | ConcurrentModificationException ignored) {
                }
            }
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

        void onFinishRequested();
    }
}

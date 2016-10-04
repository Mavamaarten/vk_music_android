package com.icapps.vkmusic.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.NotificationCompat;
import android.widget.RemoteViews;

import com.icapps.vkmusic.R;
import com.vk.sdk.api.model.VKApiAudio;

/**
 * Created by maartenvangiel on 28/09/16.
 */
class MusicNotificationManager {
    private final static int NOTIFICATION_ID = 1337;

    private Notification notification;
    private RemoteViews notificationViewSmall;
    private RemoteViews notificationViewLarge;
    private android.app.NotificationManager notificationManager;
    private MediaSessionCompat mediaSession;

    private final Context context;

    MusicNotificationManager(Context context) {
        this.context = context;
    }

    private boolean isNotificationShown() {
        return notification != null;
    }

    void createNotification() {
        if (notification != null) return;

        createMediaSession();

        Intent playPauseIntent = new Intent(context, MusicService.class);
        playPauseIntent.putExtra(MusicService.KEY_ACTION, MusicService.ACTION_PLAY_PAUSE);
        PendingIntent playPausePendingIntent = PendingIntent.getService(context, MusicService.ACTION_PLAY_PAUSE, playPauseIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent previousIntent = new Intent(context, MusicService.class);
        previousIntent.putExtra(MusicService.KEY_ACTION, MusicService.ACTION_PREVIOUS);
        PendingIntent previousPendingIntent = PendingIntent.getService(context, MusicService.ACTION_PREVIOUS, previousIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent nextIntent = new Intent(context, MusicService.class);
        nextIntent.putExtra(MusicService.KEY_ACTION, MusicService.ACTION_NEXT);
        PendingIntent nextPendingIntent = PendingIntent.getService(context, MusicService.ACTION_NEXT, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent addIntent = new Intent(context, MusicService.class);
        addIntent.putExtra(MusicService.KEY_ACTION, MusicService.ACTION_ADD);
        PendingIntent addPendingIntent = PendingIntent.getService(context, MusicService.ACTION_ADD, addIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent dismissIntent = new Intent(context, MusicService.class);
        dismissIntent.putExtra(MusicService.KEY_ACTION, MusicService.ACTION_DISMISS);
        PendingIntent dismissPendingIntent = PendingIntent.getService(context, MusicService.ACTION_DISMISS, dismissIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent openActivityIntent = new Intent(context, MusicService.class);
        openActivityIntent.putExtra(MusicService.KEY_ACTION, MusicService.ACTION_OPEN_ACTIVITY);
        PendingIntent openActivityPendingIntent = PendingIntent.getService(context, MusicService.ACTION_OPEN_ACTIVITY, openActivityIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        notificationViewSmall = new RemoteViews(context.getPackageName(), R.layout.layout_notification_small);
        notificationViewSmall.setTextViewText(R.id.title, context.getString(R.string.no_track_playing));
        notificationViewSmall.setTextViewText(R.id.artist, "");
        notificationViewSmall.setOnClickPendingIntent(R.id.play_pause, playPausePendingIntent);
        notificationViewSmall.setOnClickPendingIntent(R.id.previous, previousPendingIntent);
        notificationViewSmall.setOnClickPendingIntent(R.id.next, nextPendingIntent);
        notificationViewSmall.setOnClickPendingIntent(R.id.dismiss, dismissPendingIntent);

        notificationViewLarge = new RemoteViews(context.getPackageName(), R.layout.layout_notification_large);
        notificationViewLarge.setTextViewText(R.id.title, context.getString(R.string.no_track_playing));
        notificationViewLarge.setTextViewText(R.id.artist, "");
        notificationViewLarge.setOnClickPendingIntent(R.id.play_pause, playPausePendingIntent);
        notificationViewLarge.setOnClickPendingIntent(R.id.previous, previousPendingIntent);
        notificationViewLarge.setOnClickPendingIntent(R.id.next, nextPendingIntent);
        notificationViewLarge.setOnClickPendingIntent(R.id.dismiss, dismissPendingIntent);
        notificationViewLarge.setOnClickPendingIntent(R.id.add, addPendingIntent);

        notification = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(openActivityPendingIntent)
                .setContentTitle("vk Music")
                .setContent(notificationViewSmall)
                .setCustomBigContentView(notificationViewLarge)
                .build();
        notification.flags |= Notification.FLAG_NO_CLEAR;

        notificationManager = (android.app.NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    private void createMediaSession() {
        ComponentName receiver = new ComponentName(context.getPackageName(), RemoteReceiver.class.getName());
        mediaSession = new MediaSessionCompat(context, "MusicService", receiver, null);
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mediaSession.setPlaybackState(
                new PlaybackStateCompat.Builder()
                        .setState(PlaybackStateCompat.STATE_PLAYING, 0, 0)
                        .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE)
                        .build()
        );

        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        audioManager.requestAudioFocus(focusChange -> {}, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        mediaSession.setActive(true);
    }

    void destroyNotification() {
        if (!isNotificationShown()) return;
        notificationManager.cancel(NOTIFICATION_ID);
        notification = null;

        mediaSession.release();
    }

    void updateNotificationBitmap(Bitmap bitmap) {
        if (!isNotificationShown()) return;
        notificationViewSmall.setImageViewBitmap(R.id.album_small, bitmap);
        notificationViewLarge.setImageViewBitmap(R.id.album_large, bitmap);
        notificationManager.notify(NOTIFICATION_ID, notification);

        mediaSession.setMetadata(new MediaMetadataCompat.Builder()
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, bitmap)
                .build());
    }

    void updateNotificationPlaybackState(MusicService.PlaybackState state) {
        if (!isNotificationShown()) return;

        switch (state) {
            case STOPPED:
            case PAUSED:
                notificationViewLarge.setImageViewResource(R.id.play_pause, R.drawable.ic_play);
                notificationViewSmall.setImageViewResource(R.id.play_pause, R.drawable.ic_play);
                break;
            case PREPARING:
            case PLAYING:
                notificationViewLarge.setImageViewResource(R.id.play_pause, R.drawable.ic_pause);
                notificationViewSmall.setImageViewResource(R.id.play_pause, R.drawable.ic_pause);
                break;
        }
        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    void updateNotification(VKApiAudio currentAudio) {
        if (!isNotificationShown()) return;

        mediaSession.setMetadata(new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, currentAudio.artist)
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, currentAudio.title)
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, currentAudio.duration * 1000)
                .build());

        notificationViewSmall.setTextViewText(R.id.title, currentAudio.title);
        notificationViewSmall.setTextViewText(R.id.artist, currentAudio.artist);

        notificationViewLarge.setTextViewText(R.id.title, currentAudio.title);
        notificationViewLarge.setTextViewText(R.id.artist, currentAudio.artist);

        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    void showAddCompleteIndicator() {
        new Thread(() -> {
            notificationViewLarge.setImageViewResource(R.id.add, R.drawable.ic_check);
            notificationManager.notify(NOTIFICATION_ID, notification);

            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                return;
            }

            notificationViewLarge.setImageViewResource(R.id.add, R.drawable.ic_add);
            notificationManager.notify(NOTIFICATION_ID, notification);
        }).start();
    }
}

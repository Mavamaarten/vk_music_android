package com.icapps.vkmusic.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
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

    private final Context context;

    MusicNotificationManager(Context context) {
        this.context = context;
    }

    private boolean isNotificationShown(){
        return notification != null;
    }

    void createNotification() {
        if(notification != null) return;

        Intent playPauseIntent = new Intent(context, MusicService.class);
        playPauseIntent.putExtra(MusicService.KEY_ACTION, MusicService.ACTION_PLAY_PAUSE);
        PendingIntent playPausePendingIntent = PendingIntent.getService(context, MusicService.ACTION_PLAY_PAUSE, playPauseIntent, 0);

        Intent previousIntent = new Intent(context, MusicService.class);
        previousIntent.putExtra(MusicService.KEY_ACTION, MusicService.ACTION_PREVIOUS);
        PendingIntent previousPendingIntent = PendingIntent.getService(context, MusicService.ACTION_PREVIOUS, previousIntent, 0);

        Intent nextIntent = new Intent(context, MusicService.class);
        nextIntent.putExtra(MusicService.KEY_ACTION, MusicService.ACTION_NEXT);
        PendingIntent nextPendingIntent = PendingIntent.getService(context, MusicService.ACTION_NEXT, nextIntent, 0);

        Intent dismissIntent = new Intent(context, MusicService.class);
        dismissIntent.putExtra(MusicService.KEY_ACTION, MusicService.ACTION_DISMISS);
        PendingIntent dismissPendingIntent = PendingIntent.getService(context, MusicService.ACTION_DISMISS, dismissIntent, 0);

        Intent openActivityIntent = new Intent(context, MusicService.class);
        openActivityIntent.putExtra(MusicService.KEY_ACTION, MusicService.ACTION_OPEN_ACTIVITY);
        PendingIntent openActivityPendingIntent = PendingIntent.getService(context, MusicService.ACTION_OPEN_ACTIVITY, openActivityIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        notificationViewSmall = new RemoteViews(context.getPackageName(), R.layout.layout_notification_small);
        notificationViewSmall.setTextViewText(R.id.title, context.getString(R.string.no_track_playing));
        notificationViewSmall.setTextViewText(R.id.artist, "");

        notificationViewLarge = new RemoteViews(context.getPackageName(), R.layout.layout_notification_large);
        notificationViewLarge.setTextViewText(R.id.title, context.getString(R.string.no_track_playing));
        notificationViewLarge.setTextViewText(R.id.artist, "");
        notificationViewLarge.setOnClickPendingIntent(R.id.play_pause, playPausePendingIntent);
        notificationViewLarge.setOnClickPendingIntent(R.id.previous, previousPendingIntent);
        notificationViewLarge.setOnClickPendingIntent(R.id.next, nextPendingIntent);
        notificationViewLarge.setOnClickPendingIntent(R.id.dismiss, dismissPendingIntent);

        notification = new NotificationCompat.Builder(context)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(openActivityPendingIntent)
                .setContentTitle("vk Music")
                .setContent(notificationViewSmall)
                .setCustomBigContentView(notificationViewLarge)
                .build();
        notification.flags |= Notification.FLAG_NO_CLEAR;

        notificationManager = (android.app.NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    void destroyNotification(){
        if(!isNotificationShown()) return;
        notificationManager.cancel(NOTIFICATION_ID);
        notification = null;
    }

    void updateNotificationBitmap(Bitmap bitmap) {
        if(!isNotificationShown()) return;
        notificationViewSmall.setImageViewBitmap(R.id.album_small, bitmap);
        notificationViewLarge.setImageViewBitmap(R.id.album_large, bitmap);
        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    void updateNotificationPlaybackState(MusicService.PlaybackState state) {
        if(!isNotificationShown()) return;

        switch (state) {
            case STOPPED:
            case PAUSED:
                notificationViewLarge.setImageViewResource(R.id.play_pause, R.drawable.ic_play_bitmap);
                break;
            case PREPARING:
            case PLAYING:
                notificationViewLarge.setImageViewResource(R.id.play_pause, R.drawable.ic_pause_bitmap);
                break;
        }
        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    void updateNotification(VKApiAudio currentAudio) {
        if(!isNotificationShown()) return;

        notificationViewSmall.setTextViewText(R.id.title, currentAudio.title);
        notificationViewSmall.setTextViewText(R.id.artist, currentAudio.artist);

        notificationViewLarge.setTextViewText(R.id.title, currentAudio.title);
        notificationViewLarge.setTextViewText(R.id.artist, currentAudio.artist);

        notificationManager.notify(NOTIFICATION_ID, notification);
    }

}

package com.icapps.vkmusic.util;

import android.Manifest;
import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.widget.Toast;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;
import com.vk.sdk.api.model.VKApiAudio;

/**
 * Created by maartenvangiel on 28/09/16.
 */
public class DownloadUtil {

    public static void downloadTrack(Context context, VKApiAudio audio){
        Dexter.checkPermission(new PermissionListener() {
            @Override
            public void onPermissionGranted(PermissionGrantedResponse response) {
                DownloadManager.Request downloadRequest = new DownloadManager.Request(Uri.parse(audio.url));
                downloadRequest.allowScanningByMediaScanner();
                downloadRequest.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

                String audioName = audio.artist + " - " + audio.title;
                downloadRequest.setDescription(audioName);
                downloadRequest.setDestinationInExternalPublicDir(Environment.DIRECTORY_MUSIC, makeLegalFilename(audioName, "mp3"));

                DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
                downloadManager.enqueue(downloadRequest);
            }

            @Override
            public void onPermissionDenied(PermissionDeniedResponse response) {
                Toast.makeText(context, "Please allow storage permission for downloading tracks.", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {
                token.continuePermissionRequest();
            }
        }, Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }

    private static String makeLegalFilename(String filename, String extension){
        return filename.replaceAll("[^a-zA-Z0-9' &\\.\\-]", "_").trim() + "." + extension;
    }

}

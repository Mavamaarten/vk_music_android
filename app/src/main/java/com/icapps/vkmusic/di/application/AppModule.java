package com.icapps.vkmusic.di.application;

import android.app.Application;
import android.content.Context;

import com.icapps.vkmusic.model.albumart.AlbumArtProvider;
import com.icapps.vkmusic.model.albumart.BingAlbumArtProvider;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * Created by maartenvangiel on 13/09/16.
 */
@Module
public class AppModule {

    private final Application application;

    public AppModule(Application application) {
        this.application = application;
    }

    @Provides
    @Singleton
    Context provideApplicationContext() {
        return application;
    }

    @Provides
    @Singleton
    AlbumArtProvider provideAlbumArtProvider(){
        return new BingAlbumArtProvider();
    }
}

package com.icapps.vkmusic.di.application;

import com.icapps.vkmusic.di.user.UserComponent;
import com.icapps.vkmusic.di.user.UserModule;
import com.icapps.vkmusic.service.MusicService;

import javax.inject.Singleton;

import dagger.Component;

/**
 * Created by maartenvangiel on 13/09/16.
 */
@Singleton
@Component(modules = {AppModule.class})
public interface AppComponent {
    UserComponent plus(UserModule userModule);

    void inject(MusicService musicService);
}

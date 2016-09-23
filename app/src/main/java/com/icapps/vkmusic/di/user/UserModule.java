package com.icapps.vkmusic.di.user;

import com.vk.sdk.VKAccessToken;

import dagger.Module;
import dagger.Provides;

/**
 * Created by maartenvangiel on 13/09/16.
 */
@Module
public class UserModule {

    private final VKAccessToken accessToken;

    public UserModule(VKAccessToken accessToken) {
        this.accessToken = accessToken;
    }

    @Provides
    @UserScope
    VKAccessToken provideAccessToken(){
        return accessToken;
    }

}

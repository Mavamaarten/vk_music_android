package com.icapps.vkmusic.di.user;

import com.vk.sdk.VKAccessToken;
import com.vk.sdk.api.model.VKApiUser;

import dagger.Module;
import dagger.Provides;

/**
 * Created by maartenvangiel on 13/09/16.
 */
@Module
public class UserModule {

    private final VKAccessToken accessToken;
    private final VKApiUser user;

    public UserModule(VKAccessToken accessToken, VKApiUser user) {
        this.accessToken = accessToken;
        this.user = user;
    }

    @Provides
    @UserScope
    VKAccessToken provideAccessToken() {
        return accessToken;
    }

    @Provides
    @UserScope
    VKApiUser provideApiUser() {
        return user;
    }

}

package com.icapps.vkmusic.di.user;

import com.vk.sdk.api.model.VKApiUser;

import dagger.Module;
import dagger.Provides;

/**
 * Created by maartenvangiel on 13/09/16.
 */
@Module
public class UserModule {

    private final VKApiUser user;

    public UserModule(VKApiUser user) {
        this.user = user;
    }

    @Provides
    @UserScope
    VKApiUser provideApiUser() {
        return user;
    }

}

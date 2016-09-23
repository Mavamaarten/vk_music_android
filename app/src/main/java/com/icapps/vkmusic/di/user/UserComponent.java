package com.icapps.vkmusic.di.user;

import com.icapps.vkmusic.activity.MainActivity;

import dagger.Subcomponent;

/**
 * Created by maartenvangiel on 13/09/16.
 */
@UserScope
@Subcomponent(modules = {UserModule.class})
public interface UserComponent {
    void inject(MainActivity mainActivity);
}

package com.icapps.vkmusic.di.user;

import com.icapps.vkmusic.activity.MainActivity;
import com.icapps.vkmusic.fragment.MyAudioFragment;
import com.icapps.vkmusic.fragment.NowPlayingFragment;

import dagger.Subcomponent;

/**
 * Created by maartenvangiel on 13/09/16.
 */
@UserScope
@Subcomponent(modules = {UserModule.class})
public interface UserComponent {
    void inject(MainActivity mainActivity);

    void inject(MyAudioFragment myAudioFragment);

    void inject(NowPlayingFragment nowPlayingFragment);
}

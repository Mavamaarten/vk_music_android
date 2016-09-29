package com.icapps.vkmusic.di.user;

import com.icapps.vkmusic.activity.MainActivity;
import com.icapps.vkmusic.base.BaseMusicFragment;
import com.icapps.vkmusic.dialog.AddTrackToPlaylistDialogFragment;
import com.icapps.vkmusic.fragment.MyAudioFragment;
import com.icapps.vkmusic.fragment.NowPlayingFragment;
import com.icapps.vkmusic.fragment.PlaybackQueueFragment;
import com.icapps.vkmusic.fragment.RadioFragment;
import com.icapps.vkmusic.fragment.SearchFragment;

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

    void inject(SearchFragment searchFragment);

    void inject(PlaybackQueueFragment playbackQueueFragment);

    void inject(BaseMusicFragment baseMusicFragment);

    void inject(AddTrackToPlaylistDialogFragment addTrackToPlaylistDialogFragment);

    void inject(RadioFragment radioFragment);
}

package com.icapps.vkmusic.activity;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.view.View;

import com.icapps.vkmusic.R;
import com.icapps.vkmusic.VkApplication;
import com.icapps.vkmusic.base.BaseActivity;
import com.icapps.vkmusic.databinding.ActivityMainBinding;
import com.icapps.vkmusic.fragment.MyAudioFragment;
import com.icapps.vkmusic.fragment.NowPlayingFragment;
import com.icapps.vkmusic.model.albumart.AlbumArtProvider;
import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.materialdrawer.AccountHeader;
import com.mikepenz.materialdrawer.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileSettingDrawerItem;
import com.mikepenz.materialdrawer.model.SectionDrawerItem;
import com.vk.sdk.VKAccessToken;
import com.vk.sdk.VKSdk;
import com.vk.sdk.api.model.VKApiAudio;
import com.vk.sdk.api.model.VKApiUser;

import org.json.JSONException;

import javax.inject.Inject;

public class MainActivity extends BaseActivity implements NowPlayingFragment.PlaybackControlsListener, MyAudioFragment.AudioInteractionListener {
    @Inject VKAccessToken accessToken;
    @Inject VKApiUser user;
    @Inject AlbumArtProvider albumArtProvider;

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        setSupportActionBar(binding.toolbar);

        createNavigationDrawer();

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.content_main, new MyAudioFragment(), MyAudioFragment.class.getName())
                .replace(R.id.content_slidingpanel, new NowPlayingFragment(), NowPlayingFragment.class.getName())
                .commit();
    }

    private void createNavigationDrawer() {
        String photo = "https://vk.com/images/camera_100.png";
        try {
            photo = user.fields.getString("photo_big");
        } catch (JSONException ignored) {
        }

        AccountHeader header = new AccountHeaderBuilder()
                .withActivity(this)
                .withHeaderBackground(R.drawable.header)
                .addProfiles(
                        new ProfileDrawerItem()
                                .withEmail(user.first_name)
                                .withIcon(photo),

                        new ProfileSettingDrawerItem()
                                .withName(getString(R.string.log_out))
                                .withOnDrawerItemClickListener((view, position, drawerItem) -> {
                                    onLogoutClicked(view);
                                    return true;
                                })
                )
                .build();

        PrimaryDrawerItem aboutItem = new PrimaryDrawerItem()
                .withIdentifier(1)
                .withName(getString(R.string.about))
                .withIcon(GoogleMaterial.Icon.gmd_info);

        PrimaryDrawerItem settingsItem = new PrimaryDrawerItem()
                .withIdentifier(2)
                .withName(R.string.settings)
                .withIcon(GoogleMaterial.Icon.gmd_settings);

        PrimaryDrawerItem nowPlayingItem = new PrimaryDrawerItem()
                .withIdentifier(3)
                .withName("Now playing")
                .withIcon(GoogleMaterial.Icon.gmd_playlist_play);

        PrimaryDrawerItem myAudioItem = new PrimaryDrawerItem()
                .withIdentifier(4)
                .withName("My audio")
                .withIcon(GoogleMaterial.Icon.gmd_music_note)
                .withSetSelected(true);

        new DrawerBuilder().withActivity(this)
                .withToolbar(binding.toolbar)
                .withAccountHeader(header)
                .addDrawerItems(
                        myAudioItem,
                        nowPlayingItem,
                        new SectionDrawerItem().withName(getString(R.string.playlists))
                )
                .addStickyDrawerItems(settingsItem, aboutItem)
                .build();
    }

    @Override
    protected void inject() {
        ((VkApplication) getApplication()).getUserComponent().inject(this);
    }

    public void onLogoutClicked(View sender) {
        VKSdk.logout();

        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void onPreviousClicked() {

    }

    @Override
    public void onNextClicked() {

    }

    @Override
    public void onPlayPauseClicked() {

    }

    @Override
    public void onAudioClicked(VKApiAudio audio) {
        ((NowPlayingFragment) getSupportFragmentManager().findFragmentByTag(NowPlayingFragment.class.getName())).setCurrentAudio(audio);
    }
}

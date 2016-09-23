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
import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.materialdrawer.AccountHeader;
import com.mikepenz.materialdrawer.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileSettingDrawerItem;
import com.mikepenz.materialdrawer.model.SectionDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.vk.sdk.VKAccessToken;
import com.vk.sdk.VKSdk;
import com.vk.sdk.api.model.VKApiUser;

import javax.inject.Inject;

public class MainActivity extends BaseActivity {
    @Inject
    VKAccessToken accessToken;
    @Inject
    VKApiUser user;

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        setSupportActionBar(binding.toolbar);

        createNavigationDrawer();

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.content_main, new MyAudioFragment(), MyAudioFragment.class.getName())
                .commit();
    }

    private void createNavigationDrawer() {
        AccountHeader header = new AccountHeaderBuilder()
                .withActivity(this)
                .withHeaderBackground(R.drawable.header)
                .addProfiles(
                        new ProfileDrawerItem()
                                .withEmail(user.first_name)
                                .withIcon(user.photo.getImageForDimension(64, 64)),

                        new ProfileSettingDrawerItem()
                                .withName(getString(R.string.log_out))
                                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                                    @Override
                                    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                                        onLogoutClicked(view);
                                        return true;
                                    }
                                })
                )
                .build();

        PrimaryDrawerItem homeItem = new PrimaryDrawerItem()
                .withIdentifier(1)
                .withName(R.string.home)
                .withIcon(GoogleMaterial.Icon.gmd_home);

        PrimaryDrawerItem settingsItem = new PrimaryDrawerItem()
                .withIdentifier(1)
                .withName(R.string.settings)
                .withIcon(GoogleMaterial.Icon.gmd_settings)
                .withSetSelected(true);

        new DrawerBuilder().withActivity(this)
                .withToolbar(binding.toolbar)
                .withAccountHeader(header)
                .addDrawerItems(homeItem, settingsItem, new SectionDrawerItem().withName(R.string.playlists))
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

}

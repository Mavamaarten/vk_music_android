package com.icapps.vkmusic.activity;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.databinding.DataBindingUtil;
import android.databinding.ObservableField;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.SearchView;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.gson.Gson;
import com.icapps.vkmusic.R;
import com.icapps.vkmusic.VkApplication;
import com.icapps.vkmusic.base.BaseActivity;
import com.icapps.vkmusic.databinding.ActivityMainBinding;
import com.icapps.vkmusic.dialog.AddTrackToPlaylistDialogFragment;
import com.icapps.vkmusic.fragment.MyAudioFragment;
import com.icapps.vkmusic.fragment.NowPlayingFragment;
import com.icapps.vkmusic.fragment.PlaybackQueueFragment;
import com.icapps.vkmusic.fragment.PlaylistFragment;
import com.icapps.vkmusic.fragment.SearchFragment;
import com.icapps.vkmusic.model.albumart.AlbumArtProvider;
import com.icapps.vkmusic.model.api.VkApiAlbum;
import com.icapps.vkmusic.model.api.VkApiAlbumArrayResponse;
import com.icapps.vkmusic.service.MusicService;
import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.materialdrawer.AccountHeader;
import com.mikepenz.materialdrawer.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileSettingDrawerItem;
import com.mikepenz.materialdrawer.model.SectionDrawerItem;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import com.vk.sdk.VKAccessToken;
import com.vk.sdk.VKSdk;
import com.vk.sdk.api.VKApi;
import com.vk.sdk.api.VKApiConst;
import com.vk.sdk.api.VKParameters;
import com.vk.sdk.api.VKRequest;
import com.vk.sdk.api.VKResponse;
import com.vk.sdk.api.model.VKApiAudio;
import com.vk.sdk.api.model.VKApiUser;
import com.vk.sdk.api.model.VkAudioArray;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class MainActivity extends BaseActivity implements MusicService.MusicServiceListener, AddTrackToPlaylistDialogFragment.AddTrackToPlaylistListener {
    public static final String KEY_INITIAL_FRAGMENT = "INITIAL_FRAGMENT";
    public static final int FRAG_MYAUDIO = 0;
    public static final int FRAG_QUEUE = 1;
    public static final int FRAG_NOW_PLAYING = 2;

    @Inject VKAccessToken accessToken;
    @Inject VKApiUser user;
    @Inject AlbumArtProvider albumArtProvider;
    @Inject VkAudioArray playbackQueue;
    @Inject ObservableField<VKApiAudio> currentAudio;
    @Inject Gson gson;

    private MusicService musicService;

    private Menu optionsMenu;
    private Drawer drawer;
    private PrimaryDrawerItem searchItem;
    private PrimaryDrawerItem myAudioItem;
    private List<PrimaryDrawerItem> playlistDrawerItems;

    private MyAudioFragment myAudioFragment;
    private NowPlayingFragment nowPlayingFragment;
    private PlaybackQueueFragment playbackQueueFragment;

    private ActivityMainBinding binding;
    private ServiceConnection serviceConnection;

    private List<VkApiAlbum> playlists;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        setSupportActionBar(binding.toolbar);

        createNavigationDrawer();

        setTitle(R.string.my_audio);

        myAudioFragment = new MyAudioFragment();
        nowPlayingFragment = new NowPlayingFragment();
        playbackQueueFragment = new PlaybackQueueFragment();

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.content_slidingpanel, nowPlayingFragment, NowPlayingFragment.class.getName())
                .commit();

        final int launchFragment = getIntent().getIntExtra(KEY_INITIAL_FRAGMENT, FRAG_MYAUDIO);
        switch (launchFragment) {
            case FRAG_MYAUDIO:
                showMyAudioFragment();
                break;
            case FRAG_QUEUE:
            case FRAG_NOW_PLAYING:
                showPlaybackQueueFragment();
                break;
        }

        if (launchFragment == FRAG_NOW_PLAYING) {
            binding.slidinglayout.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
        }

        playlistDrawerItems = new ArrayList<>();
        playlists = new ArrayList<>();
        loadPlaylists();
    }

    private void loadPlaylists() {
        VKApi.audio().getAlbums(VKParameters.from(VKApiConst.OWNER_ID, user.getId())).executeWithListener(new VKRequest.VKRequestListener() {
            @Override
            public void onComplete(VKResponse response) {
                VkApiAlbumArrayResponse albumsResponse = gson.fromJson(response.responseString, VkApiAlbumArrayResponse.class);
                playlists.clear();
                playlists.addAll(albumsResponse.getResponse().getItems());

                for(VkApiAlbum album : playlists){
                    String fixedTitle;
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                        fixedTitle = Html.fromHtml(album.getTitle(), Html.FROM_HTML_MODE_LEGACY).toString();
                    } else {
                        fixedTitle = Html.fromHtml(album.getTitle()).toString();
                    }
                    album.setTitle(fixedTitle);

                    PrimaryDrawerItem item = new PrimaryDrawerItem()
                            .withIdentifier(album.getId())
                            .withName(album.getTitle())
                            .withOnDrawerItemClickListener((view, position, drawerItem) -> {
                                showPlaylistFragment(album);
                                return false;
                            });

                    drawer.addItem(item);
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        initializeService();
    }

    @Override
    protected void onStop() {
        super.onStop();
        musicService.removeMusicServiceListener(this);
        unbindService(serviceConnection);
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

        myAudioItem = new PrimaryDrawerItem()
                .withIdentifier(3)
                .withName(R.string.my_audio)
                .withIcon(GoogleMaterial.Icon.gmd_music_note)
                .withOnDrawerItemClickListener((view, position, drawerItem) -> {
                    showMyAudioFragment();
                    return true;
                })
                .withSetSelected(true);

        PrimaryDrawerItem nowPlayingItem = new PrimaryDrawerItem()
                .withIdentifier(4)
                .withName(R.string.playback_queue)
                .withIcon(GoogleMaterial.Icon.gmd_playlist_play)
                .withOnDrawerItemClickListener((view, position, drawerItem) -> {
                    showPlaybackQueueFragment();
                    return true;
                });

        searchItem = new PrimaryDrawerItem()
                .withIdentifier(5)
                .withName(R.string.search)
                .withIcon(GoogleMaterial.Icon.gmd_search)
                .withOnDrawerItemClickListener((view, position, drawerItem) -> {
                    showSearchFragment();
                    return true;
                });

        drawer = new DrawerBuilder().withActivity(this)
                .withToolbar(binding.toolbar)
                .withAccountHeader(header)
                .addDrawerItems(
                        myAudioItem,
                        nowPlayingItem,
                        searchItem,
                        new SectionDrawerItem().withName(getString(R.string.playlists))
                )
                .addStickyDrawerItems(settingsItem, aboutItem)
                .build();
    }

    private void initializeService() {
        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                musicService = ((MusicService.MusicServiceBinder) service).getService();
                musicService.addMusicServiceListener(MainActivity.this);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                musicService = null;
            }
        };

        Intent intent = new Intent(this, MusicService.class);
        startService(intent);
        bindService(intent, serviceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void inject() {
        ((VkApplication) getApplication()).getUserComponent().inject(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main_toolbar, menu);
        this.optionsMenu = menu;

        MenuItem searchMenuItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchMenuItem.getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                SearchFragment searchFragment = (SearchFragment) getSupportFragmentManager().findFragmentByTag(SearchFragment.class.getName());
                if (searchFragment == null) {
                    Bundle arguments = new Bundle();
                    arguments.putString("query", query);

                    searchFragment = new SearchFragment();
                    searchFragment.setArguments(arguments);

                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.content_main, searchFragment, SearchFragment.class.getName())
                            .commit();

                    setTitle(R.string.search_results);
                } else {
                    searchFragment.search(query);
                }

                drawer.setSelection(searchItem, false);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        return super.onCreateOptionsMenu(menu);
    }

    private void showMyAudioFragment() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.content_main, myAudioFragment, MyAudioFragment.class.getName())
                .commit();

        setTitle(R.string.my_audio);

        drawer.closeDrawer();
    }

    private void showSearchFragment() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.content_main, new SearchFragment(), SearchFragment.class.getName())
                .commit();

        setTitle(R.string.search_results);
        drawer.closeDrawer();

        optionsMenu.findItem(R.id.action_search).expandActionView();
    }

    private void showPlaybackQueueFragment() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.content_main, playbackQueueFragment, PlaybackQueueFragment.class.getName())
                .commit();

        setTitle(R.string.playback_queue);

        drawer.closeDrawer();
    }

    private void showPlaylistFragment(VkApiAlbum playlist) {
        Bundle arguments = new Bundle();
        arguments.putParcelable(PlaylistFragment.KEY_PLAYLIST, playlist);

        PlaylistFragment playlistFragment = new PlaylistFragment();
        playlistFragment.setArguments(arguments);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.content_main, playlistFragment, PlaylistFragment.class.getName())
                .commit();

        setTitle(playlist.getTitle());

        drawer.closeDrawer();
    }

    public void onLogoutClicked(View sender) {
        VKSdk.logout();
        ((VkApplication) getApplication()).releaseUserComponent();

        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        if (searchItem.isSelected()) {
            drawer.setSelection(myAudioItem);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onMusicServiceException(Exception ex) {
        Snackbar.make(binding.getRoot(), "Music service exception! " + ex, Snackbar.LENGTH_LONG).show();
    }

    @Override
    public void onPlaybackStateChanged(MusicService.PlaybackState state) {
        nowPlayingFragment.setPlaybackState(state);
        if (currentAudio.get() == null) {
            binding.slidinglayout.setPanelHeight(0);
        } else {
            binding.slidinglayout.setPanelHeight((int) getResources().getDimension(R.dimen.bottom_sheet_height));
        }
    }

    @Override
    public void onPlaybackPositionChanged(int position) {
        nowPlayingFragment.setPlaybackPosition(position);
    }

    @Override
    public void onPlaybackQueueChanged() {
        playbackQueueFragment.updatePlaybackQueue();
    }

    @Override
    public void onFinishRequested() {
        finishAffinity();
    }

    @Override
    public void onAudioAddedToPlaylist(VKApiAudio audio, VkApiAlbum playlist) {
        Snackbar.make(binding.slidinglayout, "Track successfully added to " + playlist.getTitle(), Snackbar.LENGTH_SHORT).show();
    }
}

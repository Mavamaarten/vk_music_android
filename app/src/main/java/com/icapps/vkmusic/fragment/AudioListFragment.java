package com.icapps.vkmusic.fragment;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.icapps.vkmusic.R;
import com.icapps.vkmusic.VkApplication;
import com.icapps.vkmusic.activity.MainActivity;
import com.icapps.vkmusic.adapter.VkAudioAdapter;
import com.icapps.vkmusic.base.BaseMusicFragment;
import com.icapps.vkmusic.databinding.FragmentAudioListBinding;
import com.icapps.vkmusic.dialog.AddTrackToPlaylistDialogFragment;
import com.icapps.vkmusic.model.api.VkApiAlbum;
import com.icapps.vkmusic.service.MusicService;
import com.icapps.vkmusic.util.DownloadUtil;
import com.vk.sdk.api.VKApi;
import com.vk.sdk.api.VKApiConst;
import com.vk.sdk.api.VKError;
import com.vk.sdk.api.VKParameters;
import com.vk.sdk.api.VKRequest;
import com.vk.sdk.api.VKResponse;
import com.vk.sdk.api.model.VKApiAudio;
import com.vk.sdk.api.model.VkAudioArray;

import org.json.JSONException;

import icepick.State;

public class AudioListFragment extends BaseMusicFragment implements VkAudioAdapter.VkAudioAdapterListener, SwipeRefreshLayout.OnRefreshListener {
    public static final String KEY_PLAYLIST = "PLAYLIST";
    public static final String KEY_LIST_TYPE = "LIST_TYPE";

    @State VkAudioArray audioArray;

    private FragmentAudioListBinding binding;
    private VkAudioAdapter adapter;
    private VkApiAlbum playlist;
    private AudioListType listType;
    private String searchQuery;

    public enum AudioListType {
        MY_AUDIO,
        PLAYLIST,
        SEARCH,
        POPULAR
    }

    public AudioListFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        listType = (AudioListType) getArguments().getSerializable(KEY_LIST_TYPE);

        if (audioArray == null) {
            audioArray = new VkAudioArray();
        }
        adapter = new VkAudioAdapter(audioArray, this, getContext());

        switch (listType) {
            case MY_AUDIO:
                adapter.setIsMyAudio(true);
                break;

            case PLAYLIST:
                adapter.setIsPlaylist(true);
                playlist = getArguments().getParcelable(KEY_PLAYLIST);
                break;
        }
    }

    @Override
    public void onRefresh() {
        loadData();
    }

    public void search(String searchQuery) {
        this.searchQuery = searchQuery;
        if (isVisible()) {
            loadData();
        }
    }

    public void loadData() {
        binding.swiperefresh.setRefreshing(true);

        VKParameters parameters = null;
        switch (listType) {
            case MY_AUDIO:
                parameters = VKParameters.from();
                break;

            case PLAYLIST:
                parameters = VKParameters.from(VKApiConst.ALBUM_ID, playlist.getId());
                break;

            case SEARCH:
                parameters = VKParameters.from(VKApiConst.Q, searchQuery, VKApiConst.COUNT, 100);
                break;

            case POPULAR:
                parameters = VKParameters.from("only_eng", 1);
                break;
        }

        VKRequest.VKRequestListener listener = new VKRequest.VKRequestListener() {
            @Override
            public void onComplete(VKResponse response) {
                audioArray.clear();
                audioArray.addAll((VkAudioArray) response.parsedModel);
                adapter.notifyDataSetChanged();
                binding.swiperefresh.setRefreshing(false);

                if (audioArray.size() == 0) {
                    binding.noData.getRoot().setVisibility(View.VISIBLE);
                } else {
                    binding.noData.getRoot().setVisibility(View.GONE);
                }
            }

            @Override
            public void onError(VKError error) {
                Snackbar.make(binding.rcvAudio, "Error loading search results", Snackbar.LENGTH_LONG);
                binding.swiperefresh.setRefreshing(false);
            }
        };

        if (listType == AudioListType.SEARCH) {
            VKApi.audio().search(parameters).executeWithListener(listener);
        } else if (listType == AudioListType.POPULAR) {
            VKApi.audio().getPopular(parameters).executeWithListener(listener);
        } else {
            VKApi.audio().get(parameters).executeWithListener(listener);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_audio_list, container, false);

        binding.rcvAudio.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        binding.rcvAudio.setAdapter(adapter);
        binding.swiperefresh.setOnRefreshListener(this);

        if (audioArray.size() == 0 && (listType == AudioListType.PLAYLIST || listType == AudioListType.MY_AUDIO || listType == AudioListType.POPULAR)) {
            loadData();
        } else if (listType == AudioListType.SEARCH && searchQuery != null) {
            loadData();
        }

        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();

        switch (listType) {
            case MY_AUDIO:
                getActivity().setTitle(R.string.my_audio);
                break;

            case PLAYLIST:
                getActivity().setTitle(playlist.getTitle());
                break;

            case SEARCH:
                getActivity().setTitle(R.string.search_results);
                break;

            case POPULAR:
                getActivity().setTitle(R.string.popular);
                break;
        }

        if (musicService == null || musicService.getState() == MusicService.PlaybackState.STOPPED) {
            adapter.setCurrentAudio(null);
        } else {
            adapter.setCurrentAudio(currentAudio.get());
        }
    }

    @Override
    protected void injectDependencies() {
        ((VkApplication) getActivity().getApplication()).getUserComponent().inject(this);
    }

    @Override
    public void onPlaybackStateChanged(MusicService.PlaybackState state) {
        if (musicService == null || state == MusicService.PlaybackState.STOPPED) {
            adapter.setCurrentAudio(null);
        } else {
            adapter.setCurrentAudio(currentAudio.get());
        }
    }

    @Override
    public void onAudioClicked(VKApiAudio audio, int position) {
        musicService.playAudio(audioArray, position);
    }

    @Override
    public void onCurrentAudioMoved(int toPosition) {
        // Not applicable to SearchFragment
    }

    @Override
    public boolean onAudioMenuItemClicked(VKApiAudio audio, int position, int menuItemId) {
        switch (menuItemId) {
            case R.id.action_play:
                musicService.playAudio(audioArray, position);
                break;

            case R.id.action_play_next:
                musicService.addTrackAsNextInQueue(audio);
                break;

            case R.id.action_add_to_playlist:
                Bundle arguments = new Bundle();
                arguments.putParcelable(AddTrackToPlaylistDialogFragment.KEY_AUDIO, audio);

                AddTrackToPlaylistDialogFragment addTrackToPlaylistDialogFragment = new AddTrackToPlaylistDialogFragment();
                addTrackToPlaylistDialogFragment.setArguments(arguments);
                addTrackToPlaylistDialogFragment.show(getActivity().getFragmentManager(), "playlist_selection");
                break;

            case R.id.action_track_radio:
                ((MainActivity) getActivity()).startRadio(audio);
                break;

            case R.id.action_remove_from_my_audio:
            case R.id.action_remove_from_playlist:
                removeAudio(audio, position);
                break;

            case R.id.action_download:
                DownloadUtil.downloadTrack(getContext(), audio);
                break;
        }
        return true;
    }

    private void removeAudio(VKApiAudio audio, int position) {
        VKApi.audio().delete(VKParameters.from("audio_id", audio.id, "owner_id", audio.owner_id)).executeWithListener(new VKRequest.VKRequestListener() {
            @Override
            public void onComplete(VKResponse response) {
                try {
                    int responseCode = response.json.getInt("response");
                    if (responseCode == 1) {
                        audioArray.remove(position);
                        adapter.notifyItemRemoved(position);
                        Snackbar.make(binding.getRoot(), R.string.track_removed, Snackbar.LENGTH_SHORT).show();
                    }
                } catch (JSONException e) {
                    onError(null);
                }
            }

            @Override
            public void onError(VKError error) {
                Snackbar.make(binding.getRoot(), R.string.error_deleting_track, Snackbar.LENGTH_LONG).show();
            }
        });
    }
}

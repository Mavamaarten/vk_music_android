package com.icapps.vkmusic.dialog;

import android.app.DialogFragment;
import android.content.Context;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.LinearLayoutManager;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.gson.Gson;
import com.icapps.vkmusic.R;
import com.icapps.vkmusic.VkApplication;
import com.icapps.vkmusic.adapter.VkAlbumAdapter;
import com.icapps.vkmusic.databinding.LayoutDialogPlaylistselectionBinding;
import com.icapps.vkmusic.model.api.VkApiAlbum;
import com.icapps.vkmusic.model.api.VkApiAlbumArrayResponse;
import com.vk.sdk.api.VKApi;
import com.vk.sdk.api.VKApiConst;
import com.vk.sdk.api.VKError;
import com.vk.sdk.api.VKParameters;
import com.vk.sdk.api.VKRequest;
import com.vk.sdk.api.VKResponse;
import com.vk.sdk.api.model.VKApiAudio;
import com.vk.sdk.api.model.VKApiUser;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

/**
 * Created by maartenvangiel on 28/09/16.
 */
public class AddTrackToPlaylistDialogFragment extends DialogFragment implements VkAlbumAdapter.VkAudioAdapterListener {
    public static final String KEY_AUDIO = "AUDIO";

    @Inject VKApiUser user;
    @Inject Gson gson;

    private LayoutDialogPlaylistselectionBinding binding;
    private AddTrackToPlaylistListener listener;
    private VkAlbumAdapter adapter;
    private List<VkApiAlbum> playlists = new ArrayList<>();
    private VKApiAudio audioToAdd;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((VkApplication) getActivity().getApplication()).getUserComponent().inject(this);
        audioToAdd = getArguments().getParcelable(KEY_AUDIO);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.layout_dialog_playlistselection, container, false);

        adapter = new VkAlbumAdapter(playlists, this);
        binding.rcvPlaylists.setLayoutManager(new LinearLayoutManager(inflater.getContext(), LinearLayoutManager.VERTICAL, false));
        binding.rcvPlaylists.setAdapter(adapter);

        return binding.getRoot();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof AddTrackToPlaylistListener) {
            listener = (AddTrackToPlaylistListener) context;
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        binding.loadingIndicator.setVisibility(View.VISIBLE);
        VKApi.audio().getAlbums(VKParameters.from(VKApiConst.OWNER_ID, user.getId())).executeWithListener(new VKRequest.VKRequestListener() {
            @Override
            public void onComplete(VKResponse response) {
                VkApiAlbumArrayResponse albumsResponse = gson.fromJson(response.responseString, VkApiAlbumArrayResponse.class);
                playlists.clear();
                playlists.addAll(albumsResponse.getResponse().getItems());

                for (VkApiAlbum album : playlists) {
                    String fixedTitle;
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                        fixedTitle = Html.fromHtml(album.getTitle(), Html.FROM_HTML_MODE_LEGACY).toString();
                    } else {
                        fixedTitle = Html.fromHtml(album.getTitle()).toString();
                    }
                    album.setTitle(fixedTitle);
                }

                adapter.notifyDataSetChanged();
                binding.loadingIndicator.setVisibility(View.GONE);
            }

            @Override
            public void onError(VKError error) {
                binding.loadingIndicator.setVisibility(View.VISIBLE);
                Snackbar.make(binding.rcvPlaylists, "Error loading playlists", Snackbar.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onPlaylistClicked(VkApiAlbum playlist, int position) {
        binding.loadingIndicator.setVisibility(View.VISIBLE);
        VKApi.audio().moveToAlbum(VKParameters.from(VKApiConst.ALBUM_ID, playlist.getId(), "audio_ids", audioToAdd.getId())).executeWithListener(new VKRequest.VKRequestListener() {
            @Override
            public void onComplete(VKResponse response) {
                if(listener != null){
                    listener.onAudioAddedToPlaylist(audioToAdd, playlist);
                }
                dismissAllowingStateLoss();
            }
        });
    }

    public interface AddTrackToPlaylistListener {
        void onAudioAddedToPlaylist(VKApiAudio audio, VkApiAlbum playlist);
    }
}

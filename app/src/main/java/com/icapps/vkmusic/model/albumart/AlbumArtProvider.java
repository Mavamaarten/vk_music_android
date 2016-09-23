package com.icapps.vkmusic.model.albumart;

import rx.Observable;

/**
 * Created by maartenvangiel on 23/09/16.
 */
public interface AlbumArtProvider {

    Observable<String> getAlbumArtUrl(String query);

}

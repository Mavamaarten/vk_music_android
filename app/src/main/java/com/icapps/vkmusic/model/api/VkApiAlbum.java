package com.icapps.vkmusic.model.api;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by maartenvangiel on 28/09/16.
 */
public class VkApiAlbum implements Parcelable {
    private long id;
    private long owner_id;
    private String title;

    public long getId() {
        return id;
    }

    public long getOwner_id() {
        return owner_id;
    }

    public String getTitle() {
        return title;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.id);
        dest.writeLong(this.owner_id);
        dest.writeString(this.title);
    }

    public VkApiAlbum() {
    }

    protected VkApiAlbum(Parcel in) {
        this.id = in.readLong();
        this.owner_id = in.readLong();
        this.title = in.readString();
    }

    public static final Parcelable.Creator<VkApiAlbum> CREATOR = new Parcelable.Creator<VkApiAlbum>() {
        @Override
        public VkApiAlbum createFromParcel(Parcel source) {
            return new VkApiAlbum(source);
        }

        @Override
        public VkApiAlbum[] newArray(int size) {
            return new VkApiAlbum[size];
        }
    };

    public void setTitle(String title) {
        this.title = title;
    }
}

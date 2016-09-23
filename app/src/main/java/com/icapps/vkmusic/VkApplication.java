package com.icapps.vkmusic;

import android.app.Application;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.icapps.vkmusic.activity.LoginActivity;
import com.icapps.vkmusic.di.application.AppComponent;
import com.icapps.vkmusic.di.application.AppModule;
import com.icapps.vkmusic.di.application.DaggerAppComponent;
import com.icapps.vkmusic.di.user.UserComponent;
import com.icapps.vkmusic.di.user.UserModule;
import com.mikepenz.materialdrawer.util.AbstractDrawerImageLoader;
import com.mikepenz.materialdrawer.util.DrawerImageLoader;
import com.vk.sdk.VKAccessToken;
import com.vk.sdk.VKAccessTokenTracker;
import com.vk.sdk.VKSdk;
import com.vk.sdk.api.model.VKApiUser;

/**
 * Created by maartenvangiel on 13/09/16.
 */
public class VkApplication extends Application {
    private AppComponent appComponent;
    private UserComponent userComponent;
    private VKAccessTokenTracker vkAccessTokenTracker = new VKAccessTokenTracker() {
        @Override
        public void onVKAccessTokenChanged(@Nullable VKAccessToken oldToken, @Nullable VKAccessToken newToken) {
            if (newToken == null) {
                Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                startActivity(intent);
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        appComponent = DaggerAppComponent.builder()
                .appModule(new AppModule(this))
                .build();

        vkAccessTokenTracker.startTracking();
        VKSdk.initialize(this);

        DrawerImageLoader.init(new AbstractDrawerImageLoader() {
            @Override
            public void set(ImageView imageView, Uri uri, Drawable placeholder) {
                Glide.with(imageView.getContext())
                        .load(uri)
                        .placeholder(placeholder)
                        .into(imageView);
            }
        });
    }

    public AppComponent getAppComponent() {
        return appComponent;
    }

    public UserComponent getUserComponent() {
        return userComponent;
    }

    public UserComponent createUserComponent(VKAccessToken accessToken, VKApiUser user) {
        userComponent = appComponent.plus(new UserModule(accessToken, user));
        return userComponent;
    }

    public void releaseUserComponent() {
        userComponent = null;
    }
}

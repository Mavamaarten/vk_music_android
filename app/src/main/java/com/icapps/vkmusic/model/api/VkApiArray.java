package com.icapps.vkmusic.model.api;

import java.util.List;

/**
 * Created by maartenvangiel on 28/09/16.
 */
public class VkApiArray<T> {
    private int count;
    private List<T> items;

    public int getCount() {
        return count;
    }

    public List<T> getItems() {
        return items;
    }
}

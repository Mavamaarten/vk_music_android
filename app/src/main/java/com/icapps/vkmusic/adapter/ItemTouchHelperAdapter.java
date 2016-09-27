package com.icapps.vkmusic.adapter;

/**
 * Created by maartenvangiel on 27/09/16.
 */
public interface ItemTouchHelperAdapter {
    void onItemMove(int fromPosition, int toPosition);
    void onItemDismiss(int position);
}
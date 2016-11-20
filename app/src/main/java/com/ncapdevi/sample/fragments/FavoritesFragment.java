package com.ncapdevi.sample.fragments;

import android.os.Bundle;
import android.view.View;

/**
 * Created by niccapdevila on 3/26/16.
 */
public class FavoritesFragment extends BaseFragment {

    public static FavoritesFragment  newInstance(int instance) {
        Bundle args = new Bundle();
        args.putInt(ARGS_INSTANCE, instance);
        FavoritesFragment fragment = new FavoritesFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mButton != null) {
            mButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mFragmentNavigation != null) {
                        mFragmentNavigation.pushFragment(FavoritesFragment.newInstance(mInt+1));
                    }
                }
            });
            mButton.setText(getClass().getSimpleName() + " " + mInt);
        }

    }
}

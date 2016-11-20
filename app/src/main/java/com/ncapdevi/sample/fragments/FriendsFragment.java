package com.ncapdevi.sample.fragments;

import android.os.Bundle;
import android.view.View;

/**
 * Created by niccapdevila on 3/26/16.
 */
public class FriendsFragment extends BaseFragment {

    public static FriendsFragment newInstance(int instance) {
        Bundle args = new Bundle();
        args.putInt(ARGS_INSTANCE, instance);
        FriendsFragment fragment = new FriendsFragment();
        fragment.setArguments(args);
        return fragment;
    }



    @Override
    public void onStart() {
        super.onStart();
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mFragmentNavigation != null) {
                    mFragmentNavigation.pushFragment(FriendsFragment.newInstance(mInt+1));
                }
            }
        });
        mButton.setText(getClass().getSimpleName() + " " + mInt);
    }
}

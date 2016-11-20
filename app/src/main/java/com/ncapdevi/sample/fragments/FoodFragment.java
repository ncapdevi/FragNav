package com.ncapdevi.sample.fragments;

import android.os.Bundle;
import android.view.View;

/**
 * Created by niccapdevila on 3/26/16.
 */
public class FoodFragment extends BaseFragment {

    public static FoodFragment  newInstance(int instance) {
        Bundle args = new Bundle();
        args.putInt(ARGS_INSTANCE, instance);
        FoodFragment fragment = new FoodFragment();
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
                    mFragmentNavigation.pushFragment(FoodFragment.newInstance(mInt+1));
                }
            }
        });
        mButton.setText(getClass().getSimpleName() + " " + mInt);
    }
}

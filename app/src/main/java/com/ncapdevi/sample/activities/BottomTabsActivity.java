package com.ncapdevi.sample.activities;

import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;

import com.ncapdevi.fragnav.FragNavController;
import com.ncapdevi.sample.R;
import com.ncapdevi.sample.fragments.BaseFragment;
import com.ncapdevi.sample.fragments.FavoritesFragment;
import com.ncapdevi.sample.fragments.FoodFragment;
import com.ncapdevi.sample.fragments.FriendsFragment;
import com.ncapdevi.sample.fragments.NearbyFragment;
import com.ncapdevi.sample.fragments.RecentsFragment;
import com.roughike.bottombar.BottomBar;
import com.roughike.bottombar.OnMenuTabClickListener;

public class BottomTabsActivity extends AppCompatActivity implements BaseFragment.FragmentNavigation, FragNavController.NavListener {
    private BottomBar mBottomBar;
    private FragNavController mNavController;

    //Better convention to properly name the indices what they are in your app
    private final int INDEX_RECENTS = FragNavController.TAB1;
    private final int INDEX_FAVORITES = FragNavController.TAB2;
    private final int INDEX_NEARBY = FragNavController.TAB3;
    private final int INDEX_FRIENDS = FragNavController.TAB4;
    private final int INDEX_FOOD = FragNavController.TAB5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.ncapdevi.sample.R.layout.activity_bottom_tabs);

        mNavController =
                new FragNavController(savedInstanceState, getSupportFragmentManager(), R.id.container,this,5, INDEX_NEARBY);

        mBottomBar = BottomBar.attach(this, savedInstanceState);
        mBottomBar.setItemsFromMenu(R.menu.menu_bottombar, new OnMenuTabClickListener() {
            @Override
            public void onMenuTabSelected(@IdRes int menuItemId) {
                switch (menuItemId) {
                    case R.id.bb_menu_recents:
                        mNavController.switchTab(INDEX_RECENTS);
                        break;
                    case R.id.bb_menu_favorites:
                        mNavController.switchTab(INDEX_FAVORITES);
                        break;
                    case R.id.bb_menu_nearby:
                        mNavController.switchTab(INDEX_NEARBY);
                        break;
                    case R.id.bb_menu_friends:
                        mNavController.switchTab(INDEX_FRIENDS);
                        break;
                    case R.id.bb_menu_food:
                        mNavController.switchTab(INDEX_FOOD);
                        break;
                }
            }

            @Override
            public void onMenuTabReSelected(@IdRes int menuItemId) {
                mNavController.clearStack();
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (mNavController.getCurrentStack().size() > 1) {
            mNavController.pop();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mNavController.onSaveInstanceState(outState);
    }

    @Override
    public void pushFragment(Fragment fragment) {
        mNavController.push(fragment);
    }

    @Override
    public void onTabTransaction(Fragment fragment, int index) {
            //do tabby stuff
    }

    @Override
    public void onFragmentTransaction(Fragment fragment) {
        //do fragmentty stuff. Maybe change title, I'm not going to tell you how to live your life
    }

    @Override
    public Fragment getRootFragment(int index) {
        switch (index) {
            case INDEX_RECENTS:
                return RecentsFragment.newInstance(0);
            case INDEX_FAVORITES:
                return FavoritesFragment.newInstance(0);
            case INDEX_NEARBY:
                return NearbyFragment.newInstance(0);
            case INDEX_FRIENDS:
                return FriendsFragment.newInstance(0);
            case INDEX_FOOD:
                return FoodFragment.newInstance(0);
        }
        throw new IllegalStateException("Need to send an index that we know");
    }






}




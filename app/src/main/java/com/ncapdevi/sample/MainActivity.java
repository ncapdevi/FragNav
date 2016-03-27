package com.ncapdevi.sample;

import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;

import com.ncapdevi.fragnav.NavController;
import com.roughike.bottombar.BottomBar;
import com.roughike.bottombar.OnMenuTabClickListener;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements BaseFragment.FragmentNavigation {
    private BottomBar mBottomBar;
    private NavController mNavController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.ncapdevi.sample.R.layout.activity_main);

        List<Fragment> fragments = new ArrayList<>(5);

        fragments.add(RecentsFragment.newInstance(0));
        fragments.add(FavoritesFragment.newInstance(0));
        fragments.add(NearbyFragment.newInstance(0));
        fragments.add(FriendsFragment.newInstance(0));
        fragments.add(FoodFragment.newInstance(0));

        mNavController = new NavController(getSupportFragmentManager(),R.id.container,fragments);

        mBottomBar = BottomBar.attach(this, savedInstanceState);
        mBottomBar.setItemsFromMenu(R.menu.menu_bottombar, new OnMenuTabClickListener() {
            @Override
            public void onMenuTabSelected(@IdRes int menuItemId) {
                switch (menuItemId) {
                    case R.id.bb_menu_recents:
                        mNavController.switchTab(NavController.TAB1);
                        break;
                    case R.id.bb_menu_favorites:
                        mNavController.switchTab(NavController.TAB2);
                        break;
                    case R.id.bb_menu_nearby:
                        mNavController.switchTab(NavController.TAB3);
                        break;
                    case R.id.bb_menu_friends:
                        mNavController.switchTab(NavController.TAB4);
                        break;
                    case R.id.bb_menu_food:
                        mNavController.switchTab(NavController.TAB5);
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
    public void pushFragment(Fragment fragment) {
        mNavController.push(fragment);
    }
}

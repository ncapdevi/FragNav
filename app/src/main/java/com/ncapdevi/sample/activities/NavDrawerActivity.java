package com.ncapdevi.sample.activities;

import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.ncapdevi.fragnav.FragNavController;
import com.ncapdevi.sample.R;
import com.ncapdevi.sample.fragments.BaseFragment;
import com.ncapdevi.sample.fragments.FavoritesFragment;
import com.ncapdevi.sample.fragments.FoodFragment;
import com.ncapdevi.sample.fragments.FriendsFragment;
import com.ncapdevi.sample.fragments.NearbyFragment;
import com.ncapdevi.sample.fragments.RecentsFragment;

import java.util.ArrayList;
import java.util.List;

public class NavDrawerActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, BaseFragment.FragmentNavigation {
    //Better convention to properly name the indices what they are in your app
    private final int INDEX_RECENTS = FragNavController.TAB1;
    private final int INDEX_FAVORITES = FragNavController.TAB2;
    private final int INDEX_NEARBY = FragNavController.TAB3;
    private final int INDEX_FRIENDS = FragNavController.TAB4;
    private final int INDEX_FOOD = FragNavController.TAB5;
    private FragNavController mNavController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nav_drawer);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);

        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        List<Fragment> fragments = new ArrayList<>(5);

        fragments.add(RecentsFragment.newInstance(0));
        fragments.add(FavoritesFragment.newInstance(0));
        fragments.add(NearbyFragment.newInstance(0));
        fragments.add(FriendsFragment.newInstance(0));
        fragments.add(FoodFragment.newInstance(0));

        mNavController =
                new FragNavController(savedInstanceState, getSupportFragmentManager(), R.id.container, fragments, INDEX_RECENTS);
        mNavController.setDefaultTransitionMode(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        }else if (mNavController.getCurrentStack().size() > 1) {
            mNavController.popFragment();
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
    public boolean onNavigationItemSelected(MenuItem item) {
        switch (item.getItemId()) {
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
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void pushFragment(Fragment fragment) {
        mNavController.pushFragment(fragment);
    }
}

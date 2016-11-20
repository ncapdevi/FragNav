package com.ncapdevi.sample.activities;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.ncapdevi.fragnav.FragNavController;
import com.ncapdevi.sample.R;
import com.ncapdevi.sample.fragments.BaseFragment;

public class DrawerActivity extends AppCompatActivity implements BaseFragment.FragmentNavigation {

    //Better convention to properly name the indices what they are in your app
    private final int INDEX_RECENTS = FragNavController.TAB1;
    private final int INDEX_FAVORITES = FragNavController.TAB2;
    private final int INDEX_NEARBY = FragNavController.TAB3;
    private final int INDEX_FRIENDS = FragNavController.TAB4;
    private final int INDEX_FOOD = FragNavController.TAB5;

    private FragNavController mNavController;
    private DrawerLayout mDrawerLayout;

    private ArrayAdapter<String> mAdapter;
    private ListView mDrawerList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drawer);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.navList);
        addDrawerItems();
    }

    private void addDrawerItems() {
        String[] osArray = {"Android", "iOS", "Windows", "OS X", "Linux"};
        mAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, osArray);
        mDrawerList.setAdapter(mAdapter);
    }

    @Override
    public void pushFragment(Fragment fragment) {

    }
}

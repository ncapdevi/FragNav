package com.ncapdevi.sample.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.widget.Button
import com.ncapdevi.fragnav.FragNavController

import com.ncapdevi.sample.R


//Better convention to properly name the indices what they are in your app
const val INDEX_RECENTS = FragNavController.TAB1
const val INDEX_FAVORITES = FragNavController.TAB2
const val INDEX_NEARBY = FragNavController.TAB3
const val INDEX_FRIENDS = FragNavController.TAB4
const val INDEX_FOOD = FragNavController.TAB5
const val INDEX_RECENTS2 = FragNavController.TAB6
const val INDEX_FAVORITES2 = FragNavController.TAB7
const val INDEX_NEARBY2 = FragNavController.TAB8
const val INDEX_FRIENDS2 = FragNavController.TAB9
const val INDEX_FOOD2 = FragNavController.TAB10
const val INDEX_RECENTS3 = FragNavController.TAB11
const val INDEX_FAVORITES3 = FragNavController.TAB12

class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(com.ncapdevi.sample.R.layout.activity_main)

        val btnBottomTabs = findViewById<View>(R.id.btnBottomTabs) as Button
        btnBottomTabs.setOnClickListener { startActivity(Intent(this@MainActivity, BottomTabsActivity::class.java)) }

        val btnNavDrawer = findViewById<View>(R.id.btnNavDrawer) as Button
        btnNavDrawer.setOnClickListener { startActivity(Intent(this@MainActivity, NavDrawerActivity::class.java)) }
    }
}
package com.ncapdevi.sample.activities

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Button

import com.ncapdevi.sample.R

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
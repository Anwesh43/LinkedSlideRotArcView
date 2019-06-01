package com.anwesh.uiprojects.linkedsliderotarcview

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.anwesh.uiprojects.sliderotarcviewq.SlideRotArcView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SlideRotArcView.create(this)
    }
}

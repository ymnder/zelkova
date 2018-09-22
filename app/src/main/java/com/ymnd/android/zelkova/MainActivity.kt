package com.ymnd.android.zelkova

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val build = TeaBuilder()
                .category(arrayListOf())
                .id(0)
                .name("EarlGrey")
                .price(1000)
                .build()
        Log.v("TEST", build.toString())
        Log.v("TEST", teaBuilder {
            id(100)
            name("Assam")
            price(1200)
            category(arrayListOf())
        }.toString())
//        ingestBuilder(0, Action(0)){
//            index(0)
//        }
    }
}

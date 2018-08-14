package com.ymnd.android.zelkova

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ymnd.android.builder.IngestBuilder
import com.ymnd.android.builder.ingestBuilder

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ingestBuilder(0, Action(0)){

        }
    }
}

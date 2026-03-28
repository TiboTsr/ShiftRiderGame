package com.example.jeubateau

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class RulesActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setContentView(R.layout.activity_rules)

        val btnRetour = findViewById<Button>(R.id.btn_retour_rules)
        btnRetour.setOnClickListener {
            finish()
        }
    }
}
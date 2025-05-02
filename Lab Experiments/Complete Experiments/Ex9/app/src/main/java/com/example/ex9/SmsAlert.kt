package com.example.ex9

import android.os.Bundle
import android.widget.TextView
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.example.ex9.databinding.ActivitySmsAlertBinding

class SmsAlert : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sms_alert)
        val showmsg =findViewById<TextView>(R.id.showmsg)
        val extras=intent.extras
        showmsg.text=extras!!.getString("sms")
    }
}
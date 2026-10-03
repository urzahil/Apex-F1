package com.example

import android.app.Application

class ApexApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: ApexApplication
            private set
    }
}

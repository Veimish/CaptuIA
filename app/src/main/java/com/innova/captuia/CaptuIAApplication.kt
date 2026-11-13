package com.innova.captuia

import android.app.Application
import com.innova.captuia.di.AppContainer

class CaptuIAApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

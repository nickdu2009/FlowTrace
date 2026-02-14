package com.flowtrace

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class FlowTraceApplication : Application() {

  override fun onCreate() {
    super.onCreate()

    // 初始化 Timber（仅 Debug 模式植入）
    if (BuildConfig.DEBUG) {
      Timber.plant(Timber.DebugTree())
    }

    Timber.d("FlowTrace Application started")
  }
}


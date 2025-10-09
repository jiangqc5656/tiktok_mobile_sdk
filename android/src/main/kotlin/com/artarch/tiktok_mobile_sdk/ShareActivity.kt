package com.artarch.tiktok_mobile_sdk

import android.app.Activity
import android.os.Bundle

class ShareActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val resultIntent = intent
        // 可以解析 resultIntent，通知 Flutter 分享结果
        finish()
    }
}

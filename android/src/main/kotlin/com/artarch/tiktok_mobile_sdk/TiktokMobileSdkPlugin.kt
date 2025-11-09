package com.artarch.tiktok_mobile_sdk

import android.app.Activity
import android.content.Intent
import androidx.annotation.NonNull
import com.tiktok.open.sdk.auth.AuthApi
import com.tiktok.open.sdk.auth.AuthRequest
import com.tiktok.open.sdk.auth.utils.PKCEUtils
import com.tiktok.open.sdk.share.Format
import com.tiktok.open.sdk.share.MediaType
import com.tiktok.open.sdk.share.ShareApi
import com.tiktok.open.sdk.share.ShareRequest
import com.tiktok.open.sdk.share.model.MediaContent
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry

/** FlutterTiktokSdkPlugin */
class TiktokMobileSdkPlugin :
        FlutterPlugin, MethodCallHandler, ActivityAware, PluginRegistry.NewIntentListener {
    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity
    private lateinit var channel: MethodChannel
    private lateinit var authApi: AuthApi

    var activity: Activity? = null
    private var activityPluginBinding: ActivityPluginBinding? = null
    private var loginResult: Result? = null
    private var printLog = false

    override fun onAttachedToEngine(
            @NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding
    ) {
        channel =
                MethodChannel(flutterPluginBinding.binaryMessenger, "com.artarch.tiktok_mobile_sdk")
        channel.setMethodCallHandler(this)
    }

    private var clientKey: String? = null
    private var codeVerifier: String = ""
    private var redirectUrl: String = ""

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        println(call.method)
        when (call.method) {
            "setup" -> {
                val activity = activity
                if (activity == null) {
                    result.error(
                            "no_activity_found",
                            "There is no valid Activity found to present TikTok SDK Login screen.",
                            null
                    )
                    return
                }

                clientKey = call.argument<String?>("clientKey")
                printLog = call.argument<Boolean>("printLog") ?: false
                authApi = AuthApi(activity = activity)
                result.success(null)
            }
            "login" -> {
                val scope = call.argument<String>("scope")
                val state = call.argument<String>("state")
                redirectUrl = call.argument<String>("redirectUri") ?: ""
                var browserAuthEnabled = call.argument<Boolean>("browserAuthEnabled")

                codeVerifier = PKCEUtils.generateCodeVerifier()
                if (printLog) {
                    println(
                            "clientKey: $clientKey, codeVerifier: $codeVerifier, redirectUrl: $redirectUrl"
                    )
                }

                val request =
                        AuthRequest(
                                clientKey = clientKey ?: "",
                                scope = scope ?: "",
                                redirectUri = redirectUrl,
                                state = state,
                                codeVerifier = codeVerifier,
                        )
                //        val authType = if (browserAuthEnabled == true) {
                //          AuthApi.AuthMethod.ChromeTab
                //        } else {
                //          AuthApi.AuthMethod.TikTokApp
                //        }
                var authType = AuthApi.AuthMethod.ChromeTab
                authApi.authorize(request, authType)
                loginResult = result
            }
            "share" -> {
                val mediaPathsFlutter =
                        call.argument<List<String>>("localIdentifiers") ?: emptyList()
                // 转换为 ArrayList<String>
                val mediaPaths: ArrayList<String> = ArrayList(mediaPathsFlutter)

                val redirectUri = call.argument<String>("redirectUri") ?: ""
                val greenScreenEnabled = call.argument<Boolean>("greenScreenEnabled") ?: false
                val isVideo = call.argument<Boolean>("isVideo") ?: false

                shareToTikTok(mediaPaths, redirectUri, isVideo, greenScreenEnabled, result)
            }
            else -> result.notImplemented()
        }
    }

    private fun shareToTikTok(
            mediaPaths: ArrayList<String>,
            redirectUri: String,
            isVideo: Boolean,
            greenScreenEnabled: Boolean,
            result: Result
    ) {
        val activity =
                activity
                        ?: run {
                            result.error("no_activity_found", "No Activity found", null)
                            return
                        }

        try {
            // Step 1: 初始化 ShareApi
            val shareApi = ShareApi(activity = activity)

            // Step 2: 创建 MediaContent
            val mediaContent =
                    MediaContent(
                            mediaType =
                                    if (isVideo) {
                                        MediaType.VIDEO
                                    } else {
                                        MediaType.IMAGE
                                    },
                            mediaPaths = mediaPaths
                    )

            // Step 3: 分享格式
            val shareFormat =
                    if (greenScreenEnabled) {
                        Format.GREEN_SCREEN
                    } else {
                        Format.DEFAULT
                    }

            // Step 4: 构建 ShareRequest
            val request =
                    ShareRequest(
                            clientKey = clientKey ?: "",
                            mediaContent = mediaContent,
                            shareFormat = shareFormat,
                            packageName = activity.packageName,
                            resultActivityFullPath =
                                    "${activity.packageName}.ShareActivity" // 你需要创建一个空的
                            // ShareActivity 来接收回调
                            )
            if (printLog) {
                println("clientKey: $clientKey, mediaContent: $mediaContent")
            }

            // 调用 SDK 分享
            shareApi.share(request)

            // 返回 Flutter 成功
            result.success(mapOf("state" to "success"))
        } catch (e: Exception) {
            result.error("share_error", e.localizedMessage, null)
        }
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        bindActivityBinding(binding)
    }

    override fun onDetachedFromActivityForConfigChanges() {
        unbindActivityBinding()
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        bindActivityBinding(binding)
    }

    override fun onDetachedFromActivity() {
        unbindActivityBinding()
    }

    private fun bindActivityBinding(binding: ActivityPluginBinding) {
        activity = binding.activity
        activityPluginBinding = binding
        binding.addOnNewIntentListener(this)
    }

    private fun unbindActivityBinding() {
        activityPluginBinding?.removeOnNewIntentListener(this)
        activity = null
        activityPluginBinding = null
    }

    override fun onNewIntent(intent: Intent): Boolean {
        authApi.getAuthResponseFromIntent(intent, redirectUrl = redirectUrl)?.let {
            val authCode = it.authCode
            if (authCode.isNotEmpty()) {
                var resultMap =
                        mapOf(
                                "authCode" to authCode,
                                "state" to it.state,
                                "grantedPermissions" to it.grantedPermissions,
                                "codeVerifier" to codeVerifier
                        )
                loginResult?.success(resultMap)
            } else {
                // Returns an error if authentication fails
                loginResult?.error(
                        it.errorCode.toString(),
                        it.errorMsg,
                        null,
                )
            }
        }
        return true
    }
}

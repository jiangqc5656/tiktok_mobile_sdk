import Flutter
import TikTokOpenAuthSDK
import TikTokOpenSDKCore
import TikTokOpenShareSDK
import UIKit

public class TiktokMobileSdkPlugin: NSObject, FlutterPlugin {
  public static func register(with registrar: FlutterPluginRegistrar) {
    let channel = FlutterMethodChannel(
      name: "com.artarch.tiktok_mobile_sdk", binaryMessenger: registrar.messenger())
    let instance = TiktokMobileSdkPlugin()
    registrar.addMethodCallDelegate(instance, channel: channel)
    registrar.addApplicationDelegate(instance)
  }

  public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
    switch call.method {
    case "setup":
      setup(call, result: result)
    case "login":
      login(call, result: result)
    case "share":
      share(call, result: result)
    default:
      result(FlutterMethodNotImplemented)
      return
    }
  }

  private var printLog: Bool = false
  func setup(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
    guard let args = call.arguments as? [String: Any] else {
      result(FlutterError.nilArgument)
      return
    }

    printLog = args["printLog"] as? Bool ?? false
    return result(nil)
  }

  private var authRequest: TikTokAuthRequest?
  func login(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
    guard let args = call.arguments as? [String: Any] else {
      result(FlutterError.nilArgument)
      return
    }

    guard let scope = args["scope"] as? String else {
      result(FlutterError.failedArgumentField("scope", type: String.self))
      return
    }

    guard let redirectURI = args["redirectUri"] as? String else {
      result(FlutterError.failedArgumentField("redirectURI", type: String.self))
      return
    }

    guard let browserAuthEnabled = args["browserAuthEnabled"] as? Bool else {
      result(FlutterError.failedArgumentField("browserAuthEnabled", type: Bool.self))
      return
    }

    let scopes = scope.split(separator: ",")
    let scopesSet = Set<String>(scopes.map { String($0) })

    if printLog {
      print(
        "tiktok login = redirectURI: \(redirectURI) scopes: \(scopesSet) redirectURI: \(redirectURI)"
      )
    }

    let authRequest = TikTokAuthRequest(scopes: scopesSet, redirectURI: redirectURI)
    authRequest.isWebAuth = browserAuthEnabled
    self.authRequest = authRequest
    authRequest.send { [weak self] response in
      guard let self = self, let authRequest = response as? TikTokAuthResponse else { return }
      if authRequest.error == nil {
        let resultMap: [String: String?] = [
          "authCode": authRequest.authCode,
          "codeVerifier": self.authRequest?.pkce.codeVerifier,
          "state": authRequest.state,
          "grantedPermissions": (authRequest.grantedPermissions)?.joined(separator: ","),
        ]
        result(resultMap)
      } else {
        result(
          FlutterError(
            code: String(authRequest.errorCode.rawValue),
            message: authRequest.errorDescription,
            details: nil
          ))
      }
    }
  }

  private var shareRequest: TikTokShareRequest?
  func share(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
    guard let args = call.arguments as? [String: Any] else {
      result(FlutterError.nilArgument)
      return
    }

    guard let redirectURI = args["redirectUri"] as? String else {
      result(FlutterError.failedArgumentField("redirectURI", type: String.self))
      return
    }

    guard let localIdentifiers = args["localIdentifiers"] as? [String] else {
      result(FlutterError.failedArgumentField("localIdentifiers", type: [String].self))
      return
    }

    guard let isVideo = args["isVideo"] as? Bool else {
      result(FlutterError.failedArgumentField("isVideo", type: Bool.self))
      return
    }

    guard let greenScreenEnabled = args["greenScreenEnabled"] as? Bool else {
      result(FlutterError.failedArgumentField("greenScreenEnabled", type: Bool.self))
      return
    }

    if printLog {
      print(
        "tiktok share = localIdentifiers: \(localIdentifiers) redirectURI: \(redirectURI) isVideo: \(isVideo) greenScreenEnabled: \(greenScreenEnabled)"
      )
    }

    let shareRequest = TikTokShareRequest(
      localIdentifiers: localIdentifiers, mediaType: isVideo ? .video : .image,
      redirectURI: redirectURI)
    shareRequest.shareFormat = greenScreenEnabled ? .normal : .greenScreen
    self.shareRequest = shareRequest
    shareRequest.send { [weak self] response in
      guard let self = self, let shareResponse = response as? TikTokShareResponse else { return }
      if shareResponse.errorCode == .noError {
        print("Share Succeeded!")
        let resultMap: [String: Bool] = [
          "state": true
        ]
        result(resultMap)
      } else {
        print("Share Failed!")
        result(
          FlutterError(
            code: String(shareResponse.errorCode.rawValue),
            message: shareResponse.errorDescription ?? "", details: nil))
      }
    }
  }
}

extension FlutterError {
  static let nilArgument = FlutterError(
    code: "argument.nil",
    message: "Expect an argument when invoking channel method, but it is nil.", details: nil
  )

  static func failedArgumentField<T>(_ fieldName: String, type: T.Type) -> FlutterError {
    return .init(
      code: "argument.failedField",
      message: "Expect a `\(fieldName)` field with type <\(type)> in the argument, "
        + "but it is missing or type not matched.",
      details: fieldName)
  }
}

# tiktok_mobile_sdk

Flutter plugin for TikTok OpenSDK login and share on Android and iOS.

## Features

- TikTok authorization login
- TikTok media share
- Android and iOS support
- PKCE support in login response

## Support

- Flutter `>=3.3.0`
- Dart SDK `^3.6.1`
- Android and iOS only

## Installation

Add the package to your `pubspec.yaml`:

```yaml
dependencies:
  tiktok_mobile_sdk: ^0.0.14
```

## Usage

Import the package:

```dart
import 'package:tiktok_mobile_sdk/tiktok_mobile_sdk.dart';
```

Initialize the SDK before login or share:

```dart
await TikTokSDK.instance.setup(
  clientKey: 'your_client_key',
  printLog: true,
);
```

Login example:

```dart
final result = await TikTokSDK.instance.login(
  permissions: {
    TikTokPermissionType.userInfoBasic,
    TikTokPermissionType.videoList,
  },
  redirectUri: 'your_redirect_uri',
  browserAuthEnabled: false,
  state: 'optional_state',
);

if (result.status == TikTokLoginStatus.success) {
  print('authCode: ${result.authCode}');
  print('codeVerifier: ${result.codeVerifier}');
  print('grantedPermissions: ${result.grantedPermissions}');
}
```

Share example:

```dart
final result = await TikTokSDK.instance.share(
  localIdentifiers: ['/path/to/video.mp4'],
  redirectUri: 'your_redirect_uri',
  isVideo: true,
  greenScreenEnabled: false,
);

if (result.status == TikTokShareStatus.success) {
  print('share success');
}
```

## API overview

### `setup`

```dart
Future<void> setup({
  required String clientKey,
  bool printLog = false,
})
```

- `clientKey`: TikTok Open Platform client key
- `printLog`: enables native debug logs

### `login`

```dart
Future<TikTokLoginResult> login({
  required Set<TikTokPermissionType> permissions,
  required String redirectUri,
  bool? browserAuthEnabled,
  String? state,
})
```

- `permissions`: requested TikTok scopes
- `redirectUri`: callback redirect URI configured in TikTok Open Platform
- `browserAuthEnabled`: use browser login instead of TikTok app on supported platforms
- `state`: optional request state

### `share`

```dart
Future<TikTokShareResult> share({
  required List<String> localIdentifiers,
  required String redirectUri,
  required bool isVideo,
  bool greenScreenEnabled = false,
})
```

- `localIdentifiers`: local file paths on Android or media local identifiers on iOS
- `redirectUri`: callback redirect URI
- `isVideo`: whether the shared media is video
- `greenScreenEnabled`: use TikTok green screen share format

## Permissions

Available login scopes are exposed by `TikTokPermissionType`, including:

- `userInfoBasic`
- `userInfoProfile`
- `userInfoStats`
- `videoList`
- `videoPublish`
- `videoUpload`

Use only scopes that are approved for your TikTok app.

## Platform notes

- Call `setup` before invoking `login` or `share`
- Configure your TikTok app `clientKey` and redirect URI in TikTok Open Platform
- iOS share requires valid local identifiers from the photo library
- Android share requires accessible local media file paths

## Example

See the sample app in [`example/lib/main.dart`](example/lib/main.dart).

## Repository

- Homepage: <https://github.com/jiangqc5656/tiktok_mobile_sdk>
- Issue tracker: <https://github.com/jiangqc5656/tiktok_mobile_sdk/issues>

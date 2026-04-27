import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tiktok_mobile_sdk/tiktok_mobile_sdk.dart';

void main() {
  const MethodChannel channel = MethodChannel('com.artarch.tiktok_mobile_sdk');

  TestWidgetsFlutterBinding.ensureInitialized();

  final dummyTikTokLoginResult = {
    "authCode": "authCode",
    "codeVerifier": "codeVerifier",
    "grantedPermissions": TikTokPermissionType.values.map((e) => e.scopeName).join(','),
  };

  setUp(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger.setMockMethodCallHandler(
      channel,
      (MethodCall methodCall) async {
      switch (methodCall.method) {
        case 'setup':
          return null;
        case 'login':
          return dummyTikTokLoginResult;
      }
        return null;
      },
    );
  });

  tearDown(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger.setMockMethodCallHandler(channel, null);
  });

  test('setup', () async {
    await TikTokSDK.instance.setup(clientKey: 'clientKey');
  });

  test('login', () async {
    final result = await TikTokSDK.instance.login(permissions: TikTokPermissionType.values.toSet(), redirectUri: "...");
    expect(result.status, TikTokLoginStatus.success);
    expect(result.state, null);
    expect(result.authCode, 'authCode');
    expect(result.codeVerifier, 'codeVerifier');
    expect(result.grantedPermissions, TikTokPermissionType.values.toSet());
  });
}

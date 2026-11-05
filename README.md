# StelChat Android prototype

## Local emoji cache

App-private `cacheDir/emoji-v1` stores only requested images, with a 50 MiB
image-byte cap and oldest-used eviction toward 40 MiB. Up to two downloads run
concurrently, each capped at 2 MiB. The WebView uses the local `/emoji-cache/`
route; arbitrary URLs/files cannot be requested through it. Integrity sidecars,
atomic image replacement, duplicate request coalescing and 30-second failure
backoff protect storage and traffic. Settings shows usage and cache deletion.
Android may clear this directory; images are then fetched again as needed.
No storage permission is required. Metadata/chat API access is still separate.

Native tests: `android/gradlew -p android testDebugUnitTest`.
Cache unit tests and APK build passed locally. Verified on the connected SM-S918N:
local-route image rendering, cache file creation, reuse after process restart,
settings usage display, and emoji rendering in the recent-chat list. Existing
settings were preserved during APK replacement. See scripts/cache-device.cjs.

Desktop 0.1.15 presentation port, kept separate from the Windows app and web
server. The private-use push relay is deployed separately; no public Android
repository or Play Store release has been made.

## Features

- LIVE, member chats, recent chats; expanded room messages and emoji rendering.
- Last selected member, theme, unread markers, member notification preferences,
  and per-member/per-session mute settings stored on this device.
- Foreground SSE uses a native HTTPS connection (not a CORS bypass or a hidden
  server credential). Pause closes SSE; resume fetches a fresh snapshot.
- Android notification permission is requested only when enabling system alerts.
- FCM data-message handler checks preferences, room mutes, freshness and duplicate
  IDs in native code, including when the web renderer is not running.
- Settings clearly report missing Firebase setup. No claim that background push
  works until a real device screen-off test passes.

## Build / device test

```powershell
npm ci
npm test
npm run sync
cd android
.\gradlew.bat assembleDebug
E:\AndroidSDK\platform-tools\adb.exe install -r app\build\outputs\apk\debug\app-debug.apk
E:\AndroidSDK\platform-tools\adb.exe shell am start -n xyz.stelchat.android/.MainActivity
```

Use JDK 21+ and Android SDK platform 36 / build tools required by Gradle. The local
SDK path belongs in ignored `android/local.properties`. Debug builds alone enable
WebView debugging. Release signing must use a private keystore, never committed.

`npm run dev` provides a browser preview with a local API proxy; it cannot test
Android permission dialogs, push delivery or device lifecycle.

## Firebase setup / private deployment

1. Create a Firebase project and register Android package `xyz.stelchat.android`.
   Analytics is not required or included.
2. Place its `google-services.json` in `android/app/` (ignored by Git), rebuild.
3. Enable FCM and provision a narrowly scoped server service account. Keep its
   private key OUT of the APK, source repository and chat. Configure the relay's
   `GOOGLE_APPLICATION_CREDENTIALS` only on the trusted sender host.
4. `push-relay/` contains a separately deployable **prototype** SSE-to-FCM sender.
   It only sends public member events; no device-token database is needed because
   devices subscribe to member/event topics. Do not run duplicate relay instances.
5. Test permission denied/granted, first launch, screen off, muted room, repeated
   events, reconnect and force-stop before any release. Production relay deployment
   is managed separately using `push-relay/stelchat-push.service`; operational
   commands and delivery limits are documented in `push-relay/OPERATIONS.md`.

### Delivery limits / pending production work

- FCM is best-effort. Topic delivery prioritizes throughput over latency; it is
  appropriate for public streamer events but not guaranteed instant delivery.
- A force-stopped app or manufacturer battery restrictions may prevent delivery.
- Relay SSE is not durable; a disconnect may lose notifications. A production
  durable outbox is still required if push-event recovery is a release requirement.
  The history API remains the authoritative chat record.
- No long-running foreground service, battery exemption request, boot service or
  perpetual phone-side polling is used to pretend to guarantee background delivery.
- System alert sound follows Android notification-channel volume. The in-app
  volume slider controls foreground-only synthesized sounds. Sound-only preference
  does not subscribe to background pushes; enable system notifications for those.
- App updates / signing / Play Store listing are not connected to Windows releases.

## Privacy / licensing (prototype disclosure)

### Mobile notification settings (2026-10-29)

- Member-level live/chat switches control system notifications in foreground and background.
- One app-wide mode selects sound, vibration, or silent. Android notification volume,
  channel overrides and Do Not Disturb still take precedence. No separate in-app sound.
- Legacy desktop/sound switches migrate by union; previously enabled events stay enabled.
- The relay interprets CHZZK timezone-less `openDate` as Korea time; old/silent events remain suppressed.
- Notifications appear immediately, then gain a 160px portrait when available without alerting twice.
  Portrait fetches accept only versioned StelChat profile paths and are bounded by size/time.
  The private portrait cache holds at most 32 files of 256KiB each (separate from emoji cache).
- No phone was connected during this update. APK build, policy tests and browser UI tests
  were completed; physical-device sound/vibration/portrait verification remains pending.

The app uses public StelChat chat/profile APIs. Settings are device-local and Android
backup is disabled. FCM, when enabled, processes a device registration token and
topic subscriptions through Google to deliver notifications. StelChat's relay does
not store those device tokens. Do not describe this Android app as collecting or
processing no information at all. A public Android-specific privacy policy must be
reviewed before release; do not silently reuse the web policy unchanged.

No CHZZK cookies, GitHub tokens, server passwords or Firebase service-account keys
belong in this project. Desktop third-party notices are in
`public/desktop/THIRD_PARTY_NOTICES.md`; Capacitor (MIT), AndroidX (Apache-2.0), and
Firebase SDK notices must also accompany a public Android release.

Sources: [Android message delivery](https://developer.android.com/social-and-messaging/guides/communication/receiving-messages),
[FCM Android receiving](https://firebase.google.com/docs/cloud-messaging/android/receive-messages),
[FCM topic semantics](https://firebase.google.com/docs/cloud-messaging/topic-messaging).

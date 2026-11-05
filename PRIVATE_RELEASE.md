# Private Android distribution

This repository is private. GitHub Releases distributes a personal-test APK;
there is no Play Store release and no public distribution approval implied.

The first release uses the existing debug signing identity to remain compatible
with the app already installed for testing. Treat it as a prerelease, not a
production-signed public APK. Keep its signing key private. A later change to
the signing identity requires an explicit device migration plan.

Firebase client configuration is embedded in an APK for push delivery, as with
normal Firebase Android apps. Firebase administrator/service-account private
keys, CHZZK cookies, server passwords and signing keys are never distributed.
The ignored `android/app/google-services.json` must be supplied privately when
rebuilding. The server push relay is deployed separately; do not run a second
relay when creating the X development variant.

The separate `stelchat-android-x` repository is an independent development copy,
not a GitHub-managed fork. It starts from the same source. X features are not
implemented yet; avoid installing another variant with the same package ID and
signing identity side by side until an application-ID strategy is chosen.

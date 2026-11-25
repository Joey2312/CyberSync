# CyberSync

A cyberpunk music player for Android (and Windows), using YouTube Music as its source.

**CyberSync is a modified version of [SimpMusic](https://github.com/maxrave-dev/SimpMusic) by maxrave-dev and its
contributors.** It is an independent fork: it is not made, endorsed or supported by the SimpMusic project. Please report
problems with CyberSync here, not to SimpMusic.

## Download

Get `CyberSync.apk` from the [latest release](https://github.com/Joey2312/CyberSync/releases/latest). The Android app can
also update itself from here: Settings > Check for update.

## Licence

CyberSync is free software under the **GNU General Public License, version 3** — see [LICENSE](LICENSE). You may use, study,
share and change it under those terms. It comes with **no warranty**, to the extent the law allows.

- SimpMusic and its `core` library: Copyright (C) maxrave-dev and contributors, GPL-3.0.
- What was changed, and when, is listed in [CHANGES.md](CHANGES.md).
- Bundled fonts are under the SIL Open Font License 1.1: see [licenses](licenses).
- The libraries used, with their licences, are listed in the app under Settings > Third party libraries.

YouTube and YouTube Music are trademarks of Google LLC. CyberSync is not affiliated with Google.

## Source code

The tag of each release holds the source that release was built from. The `core` directory, a separate repository upstream
([maxrave-dev/core](https://github.com/maxrave-dev/core)), is included here in full because CyberSync changes it.

## Building

Needs JDK 21 or newer and the Android SDK. Put the SDK path in `local.properties`:

```properties
sdk.dir=/path/to/Android/Sdk
```

Android:

```bash
./gradlew :androidApp:assembleRelease -PisFullBuild=false --no-configuration-cache
```

To sign with your own key, add these to `local.properties`. Never commit that file or the key.

```properties
CYBERSYNC_STORE_FILE=your-key.jks
CYBERSYNC_STORE_PASSWORD=...
CYBERSYNC_KEY_ALIAS=...
CYBERSYNC_KEY_PASSWORD=...
```

A build signed with your key is a different app to Android: uninstall the released one before installing it.

Windows:

```bash
./gradlew :composeApp:mpvSetupAll --no-configuration-cache
./gradlew :desktopApp:createDistributable -PisFullBuild=false --no-configuration-cache
```

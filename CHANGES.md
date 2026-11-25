# Changes from SimpMusic

CyberSync is a modified version of SimpMusic, changed from 21 November 2026 onward. This file is the notice of modification
that section 5(a) of the GPL asks for.

Based on:

- [maxrave-dev/SimpMusic](https://github.com/maxrave-dev/SimpMusic) at commit `f78a4fd6da9680e5771f4763194d2895d26914fd` (14 November 2026)
- [maxrave-dev/core](https://github.com/maxrave-dev/core) at commit `8413a257cbe45d102a9f101f2760e35b549e4d9e` (12 November 2026), kept in `core/`

Left out of this repository because no build uses them: the store listing, screenshots, changelogs, GitHub workflows and
editor configuration of the original project.

## 2.6.3 (25 November 2026)

- No artwork anywhere: covers, portraits, avatars, backdrops, canvases and animated covers are no longer loaded or drawn, in the app, its notifications or its widgets. The sound-reactive bar in Now Playing stays
- Home shelves are rectangular tiles
- Home no longer loses the shelves built from the listener's picks on a slow connection

## 2.6.2 (25 November 2026)

- Home is built from the listener's own top artists and songs, asked for on first launch and editable in Settings

## 2.6.1 (24 November 2026)

- In-app update: downloads the APK from this repository's releases and opens the installer (Android)
- Notifications restyled, with the CyberSync status bar icon
- The loading animation finishes its pass before a page appears

## 2.6.0 (21 to 23 November 2026)

- Renamed to CyberSync, with its own application id, logo and icons
- Cyberpunk look in dark theme: colours, fonts, cut-corner shapes, buttons, player bar, loader, sidebar and settings
- Sound-reactive Now Playing artwork, with the audio analysis behind it on Android and desktop
- Artwork replaced by generated plates
- Update check reads this repository's GitHub releases instead of SimpMusic's
- The original project's review, star and blog prompts removed
- Fixed a crash at startup on narrow screens

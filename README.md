# ATV — Smart IPTV (Native Android Starter)

Kotlin + AndroidX Media3 ExoPlayer starter for Android phones and Android TV.

## Included
- Native Media3 playback with ExoPlayer, HLS, DASH and RTSP modules
- M3U / M3U8 URL playlist import and basic parsing
- Channel list and playlist-provided channel numbers
- Remote / keyboard channel up/down, play/pause and numeric channel tuning (OK confirms)
- Audio track listing and Bangla → Hindi → English preferred-language selection
- Dark TV-oriented interface
- GitHub Actions cloud APK build

## Not yet included
This is a first working starter, not the complete requested product: Xtream Codes login, XMLTV EPG, favorites, search, multiple playlist profiles, local M3U file picker, subtitles UI, advanced settings, automatic numeric-zap timeout overlay and polished focus restoration still need implementation.

## Build without a PC (GitHub Actions)
1. Create a GitHub account and a new repository named `ATV-Smart-IPTV`.
2. Upload all files/folders in this project to the repository root.
3. Open **Actions** and enable workflows if prompted.
4. Run **Android APK Build** (or push to `main`).
5. Open the completed workflow run and download artifact `atv-debug-apk`.
6. Extract `app-debug.apk` and install it on your Android device / Android TV.

## Build locally
Requires JDK 17 and Android SDK Platform 35:
```bash
gradle assembleDebug
```
APK: `app/build/outputs/apk/debug/app-debug.apk`

## Important
- No TV streams are bundled. Add only playlists you are authorized to access.
- Codec and protocol compatibility depends on the Android device, stream, DRM and network.
- This starter stores the playlist URL in app preferences; do not use sensitive credentials.
- Media3 modules do not guarantee every codec/container will work on every TV device.

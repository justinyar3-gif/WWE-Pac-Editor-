# WWE Wii PAC Editor by Jatin — Android app

This Android Studio project wraps the supplied `pac-inspector.html` editor in a native Android WebView and adds Android storage integration.

## Included Android features

- Open `.pac` files from a file manager with **Open with → WWE Wii PAC Editor** (the app also accepts generic content URIs from file managers that label PAC files as binary files).
- Choose files from the Android Storage Access Framework for Inject, Inject as BPE, Add file, and PNG/JPG TPL replacement.
- Save extracted entries, ZIP exports, PNG previews, decompressed data, and edited archives into the device's **Downloads** folder.
- Keep the original PAC editor's parsing/editing code and its supported-format limitations.

## Build the APK

1. Open this folder in Android Studio (Giraffe or newer; current Android Gradle Plugin 8.7.3).
2. Allow Gradle sync and install Android SDK Platform 35 if prompted.
3. Select **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
4. The debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

You can also push this project to GitHub. The included workflow `.github/workflows/build-apk.yml` builds the debug APK and publishes it as a downloadable workflow artifact.

## Notes

- The PAC editor itself runs locally in the WebView; PAC file contents are not uploaded by this app.
- The original HTML loads JSZip from its existing CDN URL. Therefore, the **Extract all (.zip)** action needs an internet connection unless JSZip is bundled locally later. Other archive operations do not depend on JSZip.
- Large archives require sufficient free RAM because the editor loads the archive into memory and the Android bridge uses a Base64 transfer when opening/saving files.
- As in the original tool, keep an untouched backup of an archive before injecting, deleting, renaming, or adding entries.
- Format limitations described in the original editor remain unchanged (for example, Add/Delete are not supported for every archive type).

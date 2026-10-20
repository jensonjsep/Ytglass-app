# YouTube MP3 Glass - Android

Search a song, play it online, or download it as an MP3. Glass-style UI, 6 colour themes,
a download Activity tab and a Library tab that plays your songs one after another.

## Get the APK (no Android Studio needed)
1. Create a free account at github.com and make a **new repository** (private is fine).
2. Upload **everything in this folder** (including the hidden `.github` folder) to it:
   on the repo page choose *Add file -> Upload files* and drag the folder's contents in.
   (If the hidden folder doesn't upload, use GitHub Desktop or `git push` instead.)
3. Open the **Actions** tab -> *Build APK* -> wait ~5-8 minutes for the green tick.
   (If it didn't start by itself: *Run workflow*.)
4. Open the finished run, scroll to **Artifacts**, download `YouTubeMP3-Glass-apk`, unzip it.
5. Copy `app-debug.apk` to your phone, open it, and allow "Install unknown apps" when asked.

If the build turns red, open the failed step, copy the error lines and send them to me.

## Or build in Android Studio
Open this folder in Android Studio (Ladybug or newer), let it sync, press **Run**.
(Needs JDK 17; Studio bundles it.)

## Notes
- Songs are saved to **Music/YouTube MP3** on the phone, so other music apps can see them.
- MP3 conversion uses a community FFmpeg build. If it lacks an MP3 encoder, the song is saved
  as M4A instead and the Activity tab says so.
- YouTube changes its internals often. If search/play/download suddenly fails everywhere,
  bump `NewPipeExtractor` in `app/build.gradle.kts` to the newest release and rebuild.
- Only download content you have the right to; downloading from YouTube can violate its Terms.

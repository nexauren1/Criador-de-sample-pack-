# Sample Pack Creator

A private Android sound-design studio for generating, processing, previewing and organizing sample packs.

## Product language

- The application interface is entirely in Portuguese.
- Exported product names, category folders, sample filenames and README files are in English.

## Privacy

- No user accounts, online services or cloud upload.
- The merged manifest explicitly removes the Internet permission.
- Audio is imported with Android's Storage Access Framework.
- Categories, synthesized sounds and finished projects are stored locally.
- Synthesis, decoding, processing and ZIP export run on the device.

## Main features

- Premium dark Jetpack Compose interface.
- Custom categories with editable names, icons and colors.
- Offline synthesis from zero: Kick, Snare, Clap, Hi-Hat, Bass, Percussion, Synth, FX, Pluck and Tom.
- Procedural sound design using oscillators, envelopes, noise, pitch movement, filtering and variation parameters.
- Advanced synthesis controls for semitone tuning, decay, brightness and saturation/drive.
- Generated samples saved to a persistent local sound library.
- Audio preview and playback using AndroidX Media3 ExoPlayer.
- Import and preview through the Android document picker.
- Local decoding through Android MediaExtractor/MediaCodec when a compatible device codec is available (MP3, AAC/M4A, OGG, FLAC, WAV and other device-supported formats).
- Normalization of decoded audio to 16-bit PCM WAV for processing.
- Eight processing families: Punchy, Deep, Distorted, Reverse, Echo, LoFi, Bright and Warm, with changing parameters on repeated cycles.
- Independent sample quantity and English filename prefix for every category.
- Reuse synthesized files in a new pack.
- ZIP export into Downloads/Sample Pack Creator.
- English README with pack contents, counts, format notes and usage reminders.
- Persistent project library with sample-file preview and local project deletion.

## Build requirements

- JDK 17.
- Android SDK Platform 37.
- Android Gradle Plugin 9.3.1.
- Gradle 9.5.0.
- Kotlin Compose Compiler Plugin 2.4.21.
- Compose BOM 2026.08.00.
- AndroidX Media3 ExoPlayer 1.11.1.

Open the repository in a compatible Android Studio version and run the app configuration. A GitHub Actions workflow runs unit tests and creates a debug APK artifact on pushes to the main branch.

## Audio compatibility note

Media3 handles playback for the file types supported by the Android device. Processing imported compressed audio uses the platform decoder; exact support depends on the device's codec implementations. If a source cannot be decoded, the exporter retains it as an original file instead of claiming that it created effect variations from it.

Procedurally synthesized samples are new mono 16-bit PCM WAV audio and are available for immediate preview.

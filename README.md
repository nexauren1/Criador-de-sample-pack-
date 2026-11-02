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
- 65 individual offline effects with independent enable/disable and intensity controls, including reverse, reverb, echo, delay, ping-pong, stutter, repeater, chorus, flanger, phaser, robot, ring modulation, chord layers, pitch, dynamics and texture effects.
- Effect chains are rendered offline, and each generated variation receives a distinct variation pass.
- Per-category target quantities (1–250) are processed independently, for example 55 Kick and 45 Clap samples.
- A preflight review screen checks category targets, selected sources, enabled effects and cover art before creation.
- Processing progress includes audio conversion, rendering, structural review, ZIP integrity checks and final save.
- Optional cover image embedded in the final pack as cover.jpg/png/webp.
- Independent sample quantity and English filename prefix for every category.
- Reuse synthesized files in a new pack.
- ZIP export into Downloads/Nexa/Sample Pack (ZIP is used for broad compatibility; 7z is not currently generated).
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

Open the repository in a compatible Android Studio version and run the app configuration. The GitHub Actions workflow runs unit tests and builds both debug and optimized release APKs. The current personal release APK uses the debug signing key for device testing. Before store publishing, configure a permanent private release keystore using GitHub Actions secrets; never commit keystore files or passwords to the repository.

## Audio compatibility note

Media3 handles playback for the file types supported by the Android device. Processing imported compressed audio uses the platform decoder; exact support depends on the device's codec implementations. If a source cannot be decoded, the exporter retains it as an original file instead of claiming that it created effect variations from it.

Procedurally synthesized samples are new mono 16-bit PCM WAV audio and are available for immediate preview.

## Permanent private APK signing

For stable install-over-update behavior and Play Store submission, create one private Android keystore and keep it backed up offline. Configure these four values under GitHub repository Settings -> Secrets and variables -> Actions:

- SIGNING_KEYSTORE_BASE64 (Base64-encoded keystore file)
- SIGNING_STORE_PASSWORD
- SIGNING_KEY_ALIAS
- SIGNING_KEY_PASSWORD

The workflow uses the permanent key only when all four are configured. Never place the keystore, passwords, or encoded keystore in Git commits. The repository is currently public; the GitHub Actions secret store is the correct location for signing credentials.
\n## Latest build\n\nVersion 1.3.1: review screen, circular render progress, cover image, independent quantity validation, 65 controllable audio effects, and ZIP integrity verification.\n
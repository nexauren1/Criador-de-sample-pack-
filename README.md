# Sample Pack Creator

A private Android sound-design workspace for creating and organizing sample packs.

## Product language

- App interface: Portuguese.
- Exported product names, sample filenames and README files: English.

## Privacy

- No user accounts.
- No cloud upload or app network permission.
- Audio is selected through Android's Storage Access Framework.
- Category settings are stored locally on the device.
- Audio processing and ZIP creation run locally.

## Current implementation

- Dark, premium Compose interface.
- Persistent category manager: add, edit, delete, choose emoji icon and color.
- Create a pack with a custom product name and genre.
- Select multiple audio files for each category through Android's document picker.
- Set a separate target quantity for every category.
- Create a folder per category, sample filenames in English and a README.txt in English.
- Export the folder structure as a ZIP into Downloads/Sample Pack Creator.
- Offline PCM WAV (16-bit) variation effects: Punchy, Deep, Distorted, Reverse, Echo, LoFi, Bright and Warm.
- Unsupported audio formats are preserved as originals and are not falsely reported as processed variations.

## Build

1. Open the repository root in Android Studio.
2. Use JDK 17 or newer.
3. Install Android SDK Platform 37.
4. Sync Gradle and run the app configuration.

The project uses Android Gradle Plugin 9.5.0, Gradle 9.6.0, Kotlin Compose Compiler Plugin 2.4.21 and Compose BOM 2026.09.00.

## Important audio note

The first processor supports uncompressed PCM WAV, 16-bit, mono or stereo. MP3, AAC, 24-bit WAV, float WAV and other unsupported encodings are copied as original files. They are never renamed as effect variations. More decoders and synthesis tools can be added in later milestones.

# Darkest Fear

A libGDX desktop and Android port of the J2ME game **Darkest Fear**, running the
original game code on top of a small J2ME → libGDX compatibility layer.

![MainMenu](public/main_menu.png)
![Gameplay](public/gameplay.png)
![Android](public/android_gameplay.png)

## Build

The original game is **not** included. You must supply the **v1.55** JAR yourself
and place it at:

```
./libs/darkest_fear_v155.jar
```

Requires JDK 21.

### Desktop

```
./gradlew lwjgl3:run
```

### Android

Requires the Android SDK (set `ANDROID_HOME`, or add an `sdk.dir` entry to
`local.properties`).

Build a debug APK:

```
./gradlew :android:assembleDebug
```

The APK is written to `android/build/outputs/apk/debug/android-debug.apk`.

Build and install directly to a connected device:

```
./gradlew :android:installDebug
```

## Controls

| Key | Action |
| --- | --- |
| Arrow keys | Navigate |
| Enter / Space | Select |
| Q | Left soft key |
| E | Right soft key |
| 0 – 9 | Number keys |

## Status

- [x] Boot
- [x] Main menu
- [x] Text rendering
- [x] Input
- [x] Save data
- [x] Sound
- [x] Gameplay
- [x] Android

## Port vs Emulator

There's nothing to emulate for J2ME applications, since J2ME is a software platform, not hardware. The main difference is that J2ME's APIs are long deprecated and unusable on modern platforms, so they need to be reimplemented from scratch. It's basically what J2ME-Loader does but the main difference here is it that one is more general targeted the other is targeted to running a single target and has more wiring into that said target. But neither is an emulator as we know it.

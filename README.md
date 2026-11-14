# Darkest Fear

A libGDX desktop port of the J2ME game **Darkest Fear**, running the original
game code on top of a small J2ME → libGDX compatibility layer.

![MainMenu](public/main_menu.png)

## Build

The original game is **not** included. You must supply the **v1.55** JAR yourself
and place it at:

```
./libs/darkest_fear_v155.jar
```

Then run:

```
./gradlew lwjgl3:run
```

Requires JDK 21.

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
- [ ] Sound
- [ ] Gameplay

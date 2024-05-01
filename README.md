# Zealot Sniper (Version 2)

Snipes zealots for you with the frozen scythe in hypixel skyblock. [Version 1](https://github.com/middleclicker/ZealotSniper) was based on computer vision and pyautogui, but the mouse controls were fumbly and a pain to implement, leading to it's discontinuation.

Consider giving this repository a star!

## Features

Built-in Fullbright and Togglesprint module (both enabled by default). Default ClickGUI keybind is RSHIFT.

### Zealot Sniper module

Default keybind is "R".

Core Features
- Aims and shoots at Zealots

Safety features
- Admin detector
- NPC detector
- Lobby shutdown detector
- Death detector
- Detects when player is looking at you for too long
- Detects if your name appears in chat messages
- Two movement modes: SneakForward and JumpSneak
- Randomized time interval for switching between movement modes
- Two targeting modes: Distance based and Rotation based. Default is rotation, but will switch to distance when Special Zealot spawns.

## Installation

Head to releases and download the jar. The mod is only compatible for 1.12.2 forge. To run it, put it in your mods folder.

## Building from source

To build from source, clone the repository and run `./gradlew build`.

## Development

To setup a development environment, clone the repository and run `./gradlew setupDecompWorkspace`. Then, open the `build.gradle` file in your IDE (e.g. Intellij) and select `Open as Project`.

## Credits

Version 2 now uses the [CatClient](https://github.com/XeonLyfe/1.12.2-Client-Base/tree/main) as its base. It saved me a lot of time, so credit to its authors.

## Contributing

Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change.

## Other

ALL free skyblock clients are RATs. I found that out the hard way because I was an idiot and lost around 2 bill. I'm was really fucking annoyed and decided to make my own. The project probably contains bad code, as it was rushed to completion during exam season.

If you don't trust this client, you can check through the source code.

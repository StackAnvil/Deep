# Deep

[![Build](https://github.com/StackAnvil/Deep/actions/workflows/ci.yml/badge.svg)](https://github.com/StackAnvil/Deep/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

Fabric `26.3` utility client by StackAnvil.

> [!NOTE]
> Deep is built on [AnarchyClient](https://github.com/6b6t/AnarchyClient) (MIT). It is not affiliated with RK_01's or Lenni0451's private client of the same name and uses none of its code.

## Features

- Rivet-based in-game GUI with Vulkan and OpenGL support through Blaze3D.
- Combat, movement, world, render, and HUD modules inherited from AnarchyClient.
- Title screen buttons for the Deep menu and the account manager.
- Account manager for Microsoft and offline accounts. Microsoft sessions are encrypted with a password you choose, so the files alone cannot be used to log in.
- Notebot that plays `.nbs`, MIDI, and other song files from `config/deep/songs`. It matches notes to note blocks with the right instrument and has a song browser (`/deep notebot browse`).
- Automatic config backups in `config/deep/backups` on every start. The newest 50 are kept.

## How to use

1. Download the latest Fabric jar from https://github.com/StackAnvil/Deep/releases/latest.
2. Put it in your Fabric `minecraft/mods` folder.
3. Start the game and press `Right Shift` to open the menu. You can change the key in Minecraft's keybind settings.

## Development

```bash
./gradlew build
./gradlew runClient
```

Rivet is pulled from `com.github.Lenni0451.rivet:core:8dfcc3dd17`. Its source is at https://github.com/Lenni0451/rivet.

## Local Git checks

Install [Lefthook](https://lefthook.dev/installation/) 1.13.6 or newer and Python 3.10 or newer, then run `lefthook install` once in each clone.

The commit hook checks staged whitespace. The push hook runs `./gradlew check` when source, resource, or Gradle files change. Preview the plan with:

```bash
python3 scripts/check-push.py --all --dry-run --verbose
```

## License

[MIT](LICENSE). Third-party licenses are listed in [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

# Treefall

Treefall is a Fabric and NeoForge mod that lets you chop the base of a tree with an axe and bring the whole tree down at once.

When a valid tree is felled:

- Connected tree logs are broken automatically.
- Natural leaves are removed instantly.
- Log drops are spawned normally, respecting the axe enchantments.
- XP is awarded for the felled logs.
- If the player has valid saplings in their inventory, saplings are replanted at detected trunk bases.

Sneaking while breaking a log can be used to bypass tree felling and break only the single block.

## Built Log Protection

Treefall protects player-built log structures by default. Log houses, bridges, decorations, and other connected log builds should not chain-break when chopped.

If you want connected log builds to be felled too, enable it in the config:

```properties
allow_built_logs=true
```

The default is:

```properties
allow_built_logs=false
```

## Configuration

Treefall creates a config file at:

```text
config/treefall.properties
```

Available options:

```properties
max_logs=512
xp_per_log=1
durability_cost=true
sneak_bypass=true
allow_built_logs=false
```

Changes take effect on the next server or world load.

## Requirements

- Minecraft 26.2
- Java 25 or newer
- Ion Multiplatform API 0.1.0
- Fabric Loader 0.19.2 and Fabric API, or NeoForge 26.2.0.6-beta

## Build

Build both loader jars:

```text
gradlew build
```

The output jars are written to:

```text
fabric/build/libs
neoforge/build/libs
```

## Project Layout

```text
common/   Shared Treefall logic and config.
fabric/   Fabric entrypoint and metadata.
neoforge/ NeoForge entrypoint and metadata.
```

## License

This project is licensed under CC0-1.0.

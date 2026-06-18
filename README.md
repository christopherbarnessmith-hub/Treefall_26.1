# Treefall

Treefall is a Fabric mod that lets you chop the base of a tree with an axe and bring the whole tree down at once.

When a valid tree is felled:

- Connected tree logs are broken automatically.
- Natural leaves are removed instantly.
- Log drops are spawned normally, respecting the axe enchantments.
- XP is awarded for the felled logs.
- If the player has valid saplings in their inventory, saplings are replanted at the detected trunk bases.

Sneaking while breaking a log can be used to bypass tree felling and break only the single block.

## Built Log Protection

Treefall protects player-built log structures by default. Log houses, bridges, decorations, and other connected log builds should not chain-break when chopped.

If you want the old behavior, where connected log builds can be felled too, enable it in the config:

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
# Maximum number of logs a tree can have before felling is aborted.
max_logs=512

# XP awarded per log when a tree is felled. Set to 0 to disable.
xp_per_log=1

# If true, the axe loses 1 durability per log broken during felling.
durability_cost=true

# If true, sneaking while breaking a log skips felling.
sneak_bypass=true

# If true, connected player-built log structures can be felled too.
allow_built_logs=false
```

Changes take effect on the next server or world load.

## Requirements

- Minecraft 26.1.2
- Fabric Loader 0.19.2 or newer
- Fabric API
- Java 25 or newer

## License

This project is licensed under CC0-1.0.

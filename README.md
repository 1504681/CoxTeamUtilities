# RuneLite Plugin Template

A ready-to-go template for building RuneLite plugins. Includes build config, dev launcher, test setup, and all the boilerplate.

## Quick Start

1. Clone this repo
2. Rename the package from `com.myplugin` to your plugin's package
3. Update `MyPlugin.java`, `MyConfig.java`, `runelite_plugin.json`, `runelite-plugin.properties`, `settings.gradle`, and `build.gradle` with your plugin name
4. Run `./gradlew run` to launch RuneLite with your plugin loaded

## Commands

| Command | Description |
|---------|-------------|
| `./gradlew run` | Launch RuneLite with plugin loaded (includes all your plugin hub plugins) |
| `./gradlew build` | Compile and run tests |
| `./gradlew clean build` | Full rebuild |
| `./gradlew installPlugin` | Install JAR to `~/.runelite/externalPlugins` |
| `./gradlew shadowJar` | Build fat JAR with all dependencies |

## Project Structure

```
src/
  main/
    java/com/myplugin/
      MyPlugin.java          -- Main plugin class
      MyConfig.java          -- Config interface (shows in RuneLite settings)
    resources/
      runelite_plugin.json   -- Plugin metadata (excluded from main JAR, included in installPlugin)
  test/
    java/com/myplugin/
      MyPluginLauncher.java  -- Dev launcher (runs RuneLite with plugin)
      MyPluginTest.java      -- Unit tests with Guice/Mockito
```

## Important Notes

- **JDK 11 required** for running (JDK 21 can compile but crashes at runtime due to Guice ASM issues)
- **gradle.properties** points to JDK 11 install path — update if yours is different
- **@Provides for Config** is REQUIRED or Guice injection fails silently
- **Don't use Lombok** in plugin classes — causes Guice ClassReader crashes
- **runelite_plugin.json** must be excluded from main resources but included in installPlugin task
- The dev launcher (`./gradlew run`) loads ALL your installed plugin hub plugins too

## Adding Features

### Overlay
Create a class extending `Overlay` or `OverlayPanel`, inject it, register in `startUp()`.

### Sidebar Panel
Create a class extending `PluginPanel`, create a `NavigationButton`, add via `clientToolbar.addNavigation()`.

### Config Options
Add `@ConfigItem` methods to your config interface. Supports boolean, int, String, Color, enums.

### Event Handling
Add `@Subscribe` methods for events like `GameTick`, `ItemContainerChanged`, `MenuEntryAdded`, etc.

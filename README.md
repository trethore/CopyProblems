# CopyProblems

An IntelliJ plugin that copies problems from the Problems and Code Analysis tool windows to the clipboard.

## Installation

Bundle the plugin:

```bash
./gradlew buildPlugin
```

Then install it manually in IntelliJ IDEA:

1. Open **Settings**.
2. Select **Plugins**.
3. Open the gear menu and select **Install Plugin from Disk...**.
4. Select the generated ZIP from `build/distributions/`.
5. Restart IntelliJ IDEA if requested.

## Useful Commands

Run IntelliJ in a development sandbox with the plugin installed:

```bash
./gradlew runIde
```

Build, test, and bundle the plugin:

```bash
./gradlew check buildPlugin
```

The installable plugin ZIP is generated in `build/distributions`

## License

Licensed under the [MIT License](./LICENSE) by Titouan Réthoré.

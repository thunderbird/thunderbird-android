# Resource Mover CLI

This is a command line interface that will move resources from one module to another.

## Usage

You can run the script with the following command:

```bash
./scripts/resource-mover --from <source-module-path> --to <target-module-path> --keys <keys-to-move>
```

The **source-module-path** should be the path to the module that contains the resources you want to move.

The **target-module-path** should be the path to the module where you want to move the resources.

The **keys-to-move** should be the keys of the resources you want to move. You can pass multiple keys separated by a comma.

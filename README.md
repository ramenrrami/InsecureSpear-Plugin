[![Modrinth](https://img.shields.io/modrinth/v/insecurespear?label=MODRINTH)](https://modrinth.com/plugin/insecurespear)

# OP-Spear 
This is the official spear plugin from the insecure smp. This plugin is an op-spear that's given by command. the spear contains following effect:
```sharpness 10, efficiency 10, unbreaking 10, mending, knockback 3, fire aspect 2, frost walker 3, lunge 4, wind burst 3```.
The spear also has the ability while holding it in the main/off-hand you've get ```haste 10, saturation 1, swiftness 3, strenght 2```.
This plugin is a custom made one for the insecure smp, it supports proxy by turning it on in the config, and it has database configurations. 

## Using
Permissions:
```insecurespears.adminuse```: for all admin commands.
Commands:
```/insecurespears give {player}``` gives the player the an op-spear
```/insecurespears check``` gives you a list with all owners of an op-spear (includes ender chest and normal ones)
```/insecurespears removeall``` deletes every op-spear from the world/server
```/insecurespears reload``` for reloading the configs
```/insecurespears credits``` (the only command that can be access by default players)

## Building
Clone the repository, ```cd``` into it, then ```./gradlew build```.

The output file will be located in ```./InsecureSpears/build/libs/InsecureSpears.jar```.

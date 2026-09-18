# How GardenEssentials Works

GardenEssentials uses GardenCore for balances and integration events.

## Account linking

A Minecraft player runs `/link` to create a short-lived one-time code. In Discord, the user runs `/minecraft-link connect` in Iris and submits the code. Both sides store the association in the shared MariaDB database.

## Minecraft XP

A scheduled service checks online players. XP is only queued for players who are online, not AFK, and currently linked to a Discord account. Iris consumes the event and adds the XP to the Discord-side member record.

## Reports

`/report bug`, `/report player`, and `/staffalert` publish integration events for Iris.
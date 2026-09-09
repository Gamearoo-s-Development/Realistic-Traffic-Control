# Changelog

## Version 4.2.0

### Added
- Power Grid integration: live voltage can power crossing relays, and wired lamps/street lights stay dark when the circuit is dead
- Ghost approach signals: a red/powered Create or Extended Signals train signal next to a border shunt acts as the approach circuit for distant chunkloaded trains
- Horizontal poles and connecting poles now stack/extend like Create shafts

### Fixed
- Distant crossing flicker (one flash per carriage) and gates that stayed off until the island
- Immersive Railroading rolling stock still not being detected by relays and shunts
- Border scans timing out before ~400-block approaches (`borderTimeout` default is now 512)

## Version 4.1.3

### Added
- Combined left/right arrow display mode (`ARROW_BOTH`) on message boards
- Immersive Railroading train scanning for relays and shunts (same corridor scan as Create)

### Fixed
- Crossing lamp cantilever frames now show in the configuration menu and inventory
- Crossing lamp pole z-fighting and the extra white connect stub
- Traffic lights and poles rendering as purple missing-model cubes
- Street light models, rotation, and 1-head frame recipes
- Message board text size and preview sync

## Version 4.1.2

### Added
- Crafting recipes for digital signs, message boards, and digital sign controllers
- Message board pages, schedule, and linking are built into the message board block

### Fixed
- Signpack folder path now resolves correctly on Linux (same `.minecraft/tc_signpacks` location as Windows)
- Digital sign controller sync: timing-only sync, unlimited links, page cycling, follower timing lock

### Removed
- Separate message board controller block (use the message board GUI instead)

## Version 2.3.0

### Added
- Fya At night only (red in the day)

### Fixed
- Bug fixes and stability improvements

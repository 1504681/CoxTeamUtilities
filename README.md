# CoX Team Utilities

RuneLite plugin for Chambers of Xeric Challenge Mode teams. A sidebar panel where you pick your roles, see what potions you have and still need, and claim the potions each room is going to drop. With the Party plugin everyone in the party sees the same thing.

## Roles

Tick the roles you're doing. You can tick more than one.

| Room | Role | Checked |
|---|---|---|
| Tightrope | Venator bow | charged Venator bow |
| Tightrope | Chins | grey, red or black chinchompas |
| Tightrope | Telegrab | standard spellbook, 1 law rune |
| Muttadile | ZGS | Zamorak godsword |
| Muttadile | Entangler | standard spellbook, 4 nature runes |

Items count from your inventory, worn equipment, private storage and rune pouch. Only the law and nature runes are checked for the spells, not the elemental runes.

If something is missing the role turns red in the sidebar, an overlay lists it at the raid lobby and inside the raid until it starts, and a chat message (only you see it) repeats it when you enter.

## Supplies

Overload, Xeric's aid, Revitalisation and Prayer enhance doses, split into inventory, private storage, shared storage and claimed drops. Set the doses you want by Olm with the number next to each potion and the row shows how many you're short.

The game only sends storage contents when you open the storage unit, so both storages show `?` until you've opened them in the current raid.

## Drops

Every potion a room drops is a box you can click to claim. Counts start from the OSRS Wiki drop tables:

| Room | Overload | Xeric's aid | Revitalisation | Prayer enhance | Split overload |
|---|---|---|---|---|---|
| Tekton | 2 | | 1 | 1 | |
| Vanguards | 1 to 3 | 4 | 2 | 1 | 1 |
| Vespula | 1 | 2 | 1 | 1 | |
| Vasa | 1 | 2 | | | |
| Muttadile | 2 | 1 | 1 | 2 | |

A split overload is the elder, twisted and kodai that Vanguards drop, claimed as one. It counts as 4 overload doses, and so does a set you're carrying (as many doses as the smallest of the three has). Vasa's 2 twisted aren't listed.

The wiki has no numbers for larger teams, so use `-` and `+` to set what your team size gets. Edited counts are kept between raids. The Vanguards overload count is random, so it goes back to 1 after each raid. Right click a room name to add a potion the table doesn't list. Claims are cleared when you leave the raid.

## Party

Join a party with the core Party plugin. Each member with this plugin shows up under Team with their roles, missing items and doses carried. Claims and drop count edits are shared.

## Settings

| Setting | Default |
|---|---|
| Doses needed: Overload / Xeric's aid / Revitalisation / Prayer enhance | 4 / 12 / 8 / 4 |
| Count claimed drops towards what you need | on |
| Count shared storage towards what you need | off |
| Count split overloads as overload doses | on |
| Missing item overlay | on |
| Keep overlay during the raid | off |
| Include party members in reminders | on |
| Chat message on entry | on |
| Notify on entry | off |

## Changelog

1.0.0: first release.

## License

BSD 2-Clause, see LICENSE.

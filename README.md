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

Overload, Xeric's aid, Revitalisation and Prayer enhance, split into inventory, private storage, shared storage and claimed drops, shown as doses or as potions (setting). The `need` number is the sum of the **Needed per room** table underneath: type how much you drink at each room and the row shows how much you're short. Hover a room name to see what you still have to drink from that room on.

The table has a **Team** and a **Solo** tab. Solo adds a Stamina column for the running at Olm. Both tabs share the same numbers unless *Separate doses for solo raids* is on. Inside a raid the plugin picks the tab from the raid's party size. Defaults, all at Olm: team 1 Overload, 6 Xeric's aid, 3 Revitalisation, 1 Prayer enhance; solo the same with 4 Revitalisation and 1 Stamina.

When you walk into Olm you get a chat line with whatever you're still short.

The game only sends storage contents when you open the storage unit, so both storages show `?` until you've opened them in the current raid.

## Settings

| Setting | Default |
|---|---|
| Show supplies as | doses |
| Separate doses for solo raids | off |
| Count claimed drops towards what you need | on |
| Count shared storage towards what you need | off |
| Count split overloads as overload doses | on |
| Missing item overlay | on |
| Keep overlay during the raid | off |
| Include party members in reminders | on |
| Chat message on entry | on |
| Olm entry reminder | on |
| Notify on entry | off |

## Changelog

1.0.0: first release.

## License

BSD 2-Clause, see LICENSE.

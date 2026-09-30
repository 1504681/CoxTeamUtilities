# CoX Team Utilities

RuneLite plugin for Chambers of Xeric Challenge Mode teams. A sidebar panel where you pick your roles, get reminded of the items they need, and claim the potions each room is going to drop. With the Party plugin everyone in the party sees the same thing. Storage unit plans and the doses you need for Olm are in the separate [CoX Storage Planner](https://github.com/1504681/CoxStoragePlanner) plugin.

## Roles

Tick the roles you're doing. You can tick more than one.

| Room | Role | Checked |
|---|---|---|
| Tightrope | Lurer | charged Venator bow, or grey, red or black chinchompas |
| Tightrope | Telegrabber | standard spellbook, 1 law rune |
| Tightrope | Crosser | nothing |
| Muttadile | ZGS | Zamorak godsword |
| Muttadile | Entangler | standard spellbook, 4 nature runes |

Items count from your inventory, worn equipment, private storage and rune pouch. Only the law and nature runes are checked for the spells, not the elemental runes.

If something is missing the role turns red in the sidebar, an overlay lists it at the raid lobby and inside the raid until it starts, and a chat message (only you see it) repeats it when you enter. Overlay and chat message are for teams; a setting turns them on for solo raids too.

## Claims

Folded away under the Team section until you open it; the header shows how many claims the party has made. Every potion a room drops is a box you can click to claim. Click takes all 4 doses, or whatever the others left. Click again to drop the claim.

To share a potion, right click its box:

- `Take 1 dose` to `Take 4 doses` sets how many doses are yours.
- `Sip at` ticks the rooms you sip at, from the room that drops it up to Olm. One more room than doses takes one more dose if there is one.

Under the boxes each shared potion lists who holds it in order: `#1 You: 2 doses (Tekton, Vanguards), then drop for Bob`, then `Bob: pick up, 2 doses (Vespula, Vasa)`. Whoever sips at the earliest room holds it first. Without rooms it's whoever claimed first.

Ironmen can't pick up a potion someone else has held, so an ironman always holds it first and then drops it for the others. An ironman can claim doses of a potion others have already claimed: a click takes one sip in front of them, and their claims shrink to what's left. Only one ironman can be on a potion. The plugin reads the account type from the game, there's nothing to set. A blue box has doses left: click it to be the one who picks it up. This is a plan everyone can see, the plugin doesn't watch who drinks or drops.

Counts start from the OSRS Wiki drop tables:

| Room | Overload | Xeric's aid | Revitalisation | Prayer enhance | Split overload |
|---|---|---|---|---|---|
| Tekton | 2 | | 1 | 1 | |
| Vanguards | 1 to 3 | 4 | 2 | 1 | 1 |
| Vespula | 1 | 2 | 1 | 1 | |
| Vasa | 1 | 2 | | | |
| Muttadile | 2 | 1 | 1 | 2 | |

A split overload is the elder, twisted and kodai that Vanguards drop, claimed as one. A set you're carrying counts as overload doses (as many as the smallest of the three has). Vasa's 2 twisted aren't listed.

The wiki has no numbers for larger teams, so use `-` and `+` to set what your team size gets. Edited counts are kept between raids. The Vanguards overload count is random, so it goes back to 1 after each raid. Right click a room name to add a potion the table doesn't list. Claims are cleared when you leave the raid.

## Party

Join a party with the core Party plugin. Each member with this plugin shows up under Team with their roles and missing items. Claims and drop count edits are shared.

## Settings

| Setting | Default |
|---|---|
| Missing item overlay | on |
| Keep overlay during the raid | off |
| Include party members in reminders | on |
| Chat message on entry | on |
| Role reminders in solo raids | off |
| Notify on entry | off |

## TYFR

The button at the bottom drops all your claims and all your roles, for when the raid is over.

## Changelog

1.0.0: first release.

## License

BSD 2-Clause, see LICENSE.

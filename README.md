# CoX Team Utilities

RuneLite plugin for Chambers of Xeric Challenge Mode teams. A sidebar panel where you pick your roles, see what potions you have and still need, and claim the potions each room is going to drop. With the Party plugin everyone in the party sees the same thing.

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

## Supplies

Overload, Xeric's aid, Revitalisation and Prayer enhance in your inventory and private storage (shared storage too if the setting is on), shown as doses or as potions (setting), against the `need` number you type next to each: what you want to have when you get to Olm. Short rows go red with how much more to pick up. Claims don't count until the potion is actually in your inventory.

**Team** and **Solo** at the top switch between two sets of `need` numbers; with *Stamina in solo raids* on, Solo adds a Stamina row for the running at Olm. Both use the same numbers unless *Separate doses for solo raids* is on. Inside a raid the plugin picks team or solo from the raid's party size. Defaults: team 1 Overload, 6 Xeric's aid, 3 Revitalisation, 1 Prayer enhance; solo the same with 4 Revitalisation and 1 Stamina.

When you walk into Olm you get a chat line with whatever you're still short.

Under the rows a grid shows what each party member holds in inventory + private storage, the shared storage, and the total. `3?` is an inventory whose private storage hasn't been opened yet; `×` is a member without the plugin (the game doesn't show other players' inventories, so there's nothing to know).

The game only sends a storage's contents while its interface is open, so both storages show `?` until someone has opened them in the current raid (a party member's view of the shared storage is used if you haven't opened it yourself). A deposit or withdrawal made as the interface closes doesn't come back from the game either, so the plugin works those out from what left or entered your inventory.

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

## Chests

Each storage unit in the raid gets its own plan: what to put in and what to take out. Open a storage unit and the chest appears under **Chests**, named after its room (`Ice Demon`, `Farming 1`, `Farming 2`, the two farming rooms are told apart by which comes first). Rename it, then fill the two lists: one item per line, matched from the start of the name so `Xeric's aid` is any dose, `*` and `?` as wildcards (`*chinchompa`, `Dragon *`), `Stinkhorn mushroom, 3` for a number, `everything` to empty the inventory. Numbers are quantities, so a stack of 14 juice counts as 14. A Put in line without a number means all of them; with one (`Endarkened*, 11`) the step is done once that many went in since you opened the storage, or the storage already holds that many.

The easy way to fill the lists is to click: tick **Mark by clicking** and, with a storage open, left-clicking an item in the storage adds it to Take out and one in the side inventory to Put in (with no storage open, inventory items go to the chest picked in the sidebar). Every click adds one more, so three clicks on a stinkhorn make `Stinkhorn mushroom, 3`, and the order you click is the withdraw order. `Unmark` on the right-click menu takes one away. Marking is off again when you TYFR or restart the client.

Tick **Withdraw in this order** and the list becomes steps 1, 2, 3. While the storage is open an overlay lists the steps and ticks them off as your inventory changes, and the items still to move glow: in the side inventory what goes in, in the storage what comes out. Ordered lists glow only the next item, or all of them in a gradient from the first colour to the last with their numbers (setting). Colours and the pulse are settings too.

## Party

Join a party with the core Party plugin. Each member with this plugin shows up under Team with their roles and missing items, and in the Supplies grid with their doses. Claims, drop count edits and the shared storage contents are shared.

## Settings

| Setting | Default |
|---|---|
| Show supplies as | doses |
| Separate doses for solo raids | off |
| Stamina in solo raids | off |
| Count shared storage towards what you need | off |
| Count split overloads as overload doses | on |
| Missing item overlay | on |
| Keep overlay during the raid | off |
| Include party members in reminders | on |
| Chat message on entry | on |
| Olm entry reminder | on |
| Role reminders in solo raids | off |
| Chest steps overlay | off |
| Glow items | on |
| Ordered withdraw glow | only the next one |
| Glow colour / gradient end colour | cyan / pink |
| Pulse | on |
| Notify on entry | off |

## TYFR

The button at the bottom drops all your claims and all your roles, for when the raid is over.

## Changelog

1.0.0: first release.

## License

BSD 2-Clause, see LICENSE.

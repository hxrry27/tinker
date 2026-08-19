# tinker

a survival-safe, allowlisted block-state editor for paper / purpur and derivative servers

think debug stick, but it only ever touches the cosmetic bits, and you (as a server admin) have
complete control over what blocks you allow to be interacted with in this way

this whole plugin/mod combo was birthed from my annoyance of making sick builds with worldedit / 
axiom and not being able to recreate these in survival minecraft smps with my friends

i took inspiration from the Armor Poser mod setup, meaning this does require the client side mod
which you can find on the [tinker fabric mod](https://github.com/hxrry27/tinker-fabric) page

---

## how it works

turn the mode on with the keybind (default G), then right-click blocks

### connecting blocks - fences, panes, iron bars, walls

these are edited **spatially**

whichever side/face you click, that's the one that will change

| where you click | fences / panes / bars | walls |
|---|---|---|
| toward a side | toggle that side's connection | cycle that side NONE / LOW / TALL |
| centre of the top | nothing, that's the post | toggle `up` (the centre post) |

there are limits here, you can't make blocks dissapear for example

### stairs

stairs are **sculpted**. the block is treated as a 2x2x2 grid of eight little cubes

clicking a little cube eighth will change it, that's how you get inner and outer corners in places vanilla would never put them

it can't degrade a stair into a slab or a upgrade to afull block - only stair states are in the lookup table, so a pattern that isn't a stair has no match and the click does nothing - stops pesky dupes

### everything else

trapdoors, doors, fence gates, barrels, furnaces etc; right-click changes its state

feedback goes to the action bar - `oak stairs > stairs: NORTH/BOTTOM/INNER_LEFT`,
`glass pane > north: true`

---

## note: there is no supression at play

worth understanding, because it explains why edits may revert

when you place a block in MC, it updates its neighbours, and although there are many glitches / bugs designed to stop this or alter the way that works, this mod is not designed to change / amend those

if you edit a block such as a wall, then place a block next to it and trigger an update, it WILL reverse your wall changes

for this reason, it's heavily reccomended to **build first, tinker AFTERWARDS**

---

## commands and permissions

| command | permission |
|---|---|
| `/tinker reload` | `tinker.reload` |

that's it. tada!

| permission | grants |
|---|---|
| `tinker.use` | the tinker tool |
| `tinker.reload` | `/tinker reload` |

both default to **false**
nothing is granted implicitly, not even to operators - gate them behind whatever system you like in luckperms

note that `tinker.use` alone isn't enough
tinker mode also requires a completed handshake with the client mod, so a player on a vanilla client gets nothing regardless of their permissions

i chose this because the kind of players using this, will already be used to mods such as armor poser which run on a similar handshake requirement
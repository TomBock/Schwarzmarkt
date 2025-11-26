## Phoenix Schwarzmarkt (Black Market)
A fully featured auction & black-market system for Minecraft (Paper).  
Supports server- and player-auctions, custom GUIs, cron-based scheduling and complete bidding logs.

*Note: This version is customized for the Phoenix server environment.*  
*A generic version can easily be created later (see notes at bottom).*

## Features
- Server- and Player-Auctions that can be started/stopped separately
- Completely customizable GUIs
- Separate Server setup, Player setup and Winnings GUI
- Scheduling via Commands (e.g. can be done via Cron Jobs)
- Permission rewards (for titles)
- Full audit log for every bid 

## Commands
### User Commands
| Command                         | Description                                                                     |
|---------------------------------|---------------------------------------------------------------------------------|
| `/schwarzmarkt bieten <amount>` | Places a bid on the currently selected auction (after clicking on it in the GUI |

### Admin / Console Commands
| Command                                          | Description                                                              |
|--------------------------------------------------|--------------------------------------------------------------------------|
| `/schwarzmarkt info`                             | Shows information about the current auction                              |
| `/schwarzmarkt start [server/player]`            | Starts the server/player black market auction (can be used in cron jobs) |
| `/schwarzmarkt stop [server/player]`             | Stops the server/player black market auction (can be used in cron jobs)  |
| `/schwarzmarkt show <playerName>`                | Opens the black market GUI (to be run by Citizen)                        |
| `/schwarzmarkt setup server`                     | Opens the server auction setup GUI                                       |
| `/schwarzmarkt setup <playerName>`               | Opens the player auction setup GUI (to be run by Citizen)                |
| `/schwarzmarkt showsetup <playerName>`           | Opens the admin view of the player setup GUI of the player               |
| `/schwarzmarkt gewinne <playerName>`             | Opens the winnings GUI for the specified player (to be run by citizen)   |
| `/schwarzmarkt titel <displayname> <permission>` | Creates a new title item with the given display name and permission node |

## Permissions
| Permission Node      | Description                      | Default |
|----------------------|----------------------------------|---------|
| `schwarzmarkt.use`   | Zugriff auf das Schwarzmarkt GUI | true    |
| `schwarzmarkt.admin` | Zugriff auf Admin Commands       | op      |

## Configs
- _msg.yml_ - Player-facing chat messages
- _gui.yml_ - GUI configuration
- _bids.log_ - Log file for server auctions
- _player_bids.log_ - Log file for player auctions

## Log Reference
The system logs **every bid**, and if something fails, the log helps identify what happened.  
Here are the important messages and what they mean:

```yaml
<timestamp> | ROLLBACK FAILED | Auction ID: <id> | Bid Amount: <amount> | Player: <uuid>
#Means the bid could not be cancelled correctly when there was an error while withdrawing money from the player
#RESULT: The player might win the auction without having payed anything

<timestamp> | WINNINGS FAILED | Auction ID: <id> | Winner: <uuid> | Item: <item>
#Means the player did not receive the winnings for the auction correctly
#RESULT: Give the player the item by hand (use setup command to get it)

<timestamp> | RETURN BID FAILED | Winner: <uuid> | Amount: <amount>
#Means the player did not get their bids back after loosing the auction
#RESULT: Give the player the money by hand

<timestamp> | ITEM RETURN FAILED | Winner: <uuid> | Item: <item>
#Means the player did not get their items back after it was not sold in the auction

<timestamp> DEPOSIT FAILED | Amount: <amount> | Player: <uuid>
#Means the player did not get their money back after taking an item out of their schwarzmarkt setup

<timestamp> WITHDRAW FAILED | Amount: <amount> | Player: <uuid>
#Means the player did not get their money after putting an item into their schwarzmarkt setup

<timestamp> ITEM REMOVAL FAILED | Amount: <amount> | Player: <uuid>
#Means the item could not be removed from the player's schwarzmarkt setup after being sold
```
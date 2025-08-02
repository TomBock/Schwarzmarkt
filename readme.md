## Phoenix Schwarzmarkt plugin
.jar files can be found in /target

### Commands:
#### USER
- /schwarzmarkt bieten \<anzahl> #Bieten für die ausgewählte auktion (klappt nur nach auswahl eines im menu)

#### ADMIN
- /schwarzmarkt info - Aktuelle Auktion sehen
- /schwarzmarkt start \<server/spieler> - Startet den Schwarzmarkt (via Cron Job)
- /schwarzmarkt stop \<server/spieler> - Stoppt den Schwarzmarkt (via Cron Job)
- /schwarzmarkt show \<player> - Öffnet den Marktplatz (von citizen auszuführen)
- /schwarmzarkt setup \<server/spieler> - Öffnet Player GUI für Schwarzmarkt
- /schwarzmarkt showsetup \<player> Zum abholen der gewinne
- /schwarzmarkt gewinne \<player> Zum abholen der gewinne
- /schwarzmarkt titel \<displayname> \<permission> - Hilfe command um titel zu erstellen

### Configs:
- _msg.yml_ defines texts & chat messages for the plugin
- _gui.yml_ defines the GUIs for the plugin
- _bids.log_ logs all bids

### Permissions:
- schwarzmarkt.admin - Zugriff auf Admin Commands (default: op)
- schwarzmarkt.user - Zugriff auf User Commands (default: true)

### bids.log
Log file for all bids. If there is a complain, check for the following errors:

```yaml
<timestamp> | ROLLBACK FAILED | Auction ID: <id> | Player: <uuid> | Bid Amount: <amount>
#Means the bid could not be cancelled correctly when there was an error while withdrawing money from the player
#RESULT: The player might win the auction without having payed anything

<timestamp> | WINNINGS FAILED | Auction ID: <id> | Player: <uuid> | Item: <item>
#Means the player did not receive the winnings for the auction correctly
#RESULT: Give the player the item by hand (use setup command to get it)

<timestamp> | RETURN BID FAILED | Player: <uuid> | Amount: <amount>
#Means the player did not get their bids back after loosing the auction
#RESULT: Give the player the money by hand

<timestamp> | RETURN BID FAILED | Player: <uuid> | Amount: <amount>
#Means the player did not get their bids back after loosing the auction

<timestamp> DEPOSIT FAILED | Amount: <amount> | Player: <uuid>
#Means the player did not get their money back after taking an item out of their schwarzmarkt setup
#Use /schwarzmarkt 

ITEM REMOVAL FAILED
#A sold item could not be removed from the schwarzmarkt setup of a given user

REVENUE FAILED
#A player did not get the cash for an item

ITEM RETURN FAILED
# A player did not get their item back after trying and failing to sell it in an auction
```
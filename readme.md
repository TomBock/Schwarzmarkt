## Phoenix Schwarzmarkt plugin
.jar files can be found in /target

### Commands:
#### USER
- /schwarzmarkt gewinne #User können ihr item abholen
- /schwarzmarkt bieten \<anzahl> #Bieten für die ausgewählte auktion (klappt nur nach auswahl eines im menu)

#### ADMIN
- /schwarmzarkt setup - Öffnet Admin GUI für Schwarzmarkt
- /schwarzmarkt info - Aktuelle Auktion sehen
- /schwarzmarkt start - Startet den Schwarzmarkt (via Cron Job)
- /schwarzmarkt stop - Stoppt den Schwarzmarkt (via Cron Job)
- /schwarzmarkt show \<player> - Öffnet den Marktplatz (von citizen auszuführen)
- /schwarzmarkt titel \<displayname> \<permission> - Hilfe command um titel zu erstellen
- /schwarzmarkt setinvitem \<inventary> \<key> - Setzt ein item in einer inventar gui yml

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
```
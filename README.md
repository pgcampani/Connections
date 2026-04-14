# CONNECTIONS GAME 
Implementazione del gioco Connections del NYT, sviluppata in Java utilizzando networking TCP/UDP, NIO e programmazione concorrente 

## Panoramica 
I giocatori devono individuare gruppi di 4 parole correlate a partire da un insieme di 16. 
Il server gestisce round globali in cui tutti i giocatori autenticati competono simultaneamente. 
La comunicazione avviene tramite: 
- `TCP` per i comandi di gioco 
- `UDP` per notifiche asincrone (inizio nuova partita, risultati finali)

## Architettura 
```text
┌─────────────────────────────────────────────────────────┐
│                        SERVER                           │
│                                                         │
│  ServerMain → GameServer                                │
│       │                                                 │
│       ├── ThreadPool (ClientHandler × N)  ← TCP         │
│       ├── GameScheduler (startNewGame/endGame)          │
│       ├── PersistenceTask (saveUsers/saveState)         │
│       └── ShutdownHook                                  │
│                                                         │
│  GameManager ←→ UserManager ←→ UDPNotifier → UDP        │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│                        CLIENT                           │
│                                                         │
│  ClientMain (Main Thread) ←→ TCP ←→ Server              │
│       └── UDPListener (Daemon Thread) ← UDP             │
└─────────────────────────────────────────────────────────┘
```

## Funzionalità 
- `Round globali`: tutti i giocatori condividono la stessa partita
- `Punteggio in tempo reale`: +6 per ogni gruppo corretto, -4 per ogni proposta errata. (massimo punteggio realizzabile per round: 18 punti)
- `Notifiche UDP asincrone`: risultati di fine round (personalizzati) e avvio nuovi round (globale)
- `Persistenza dello stato`: il server riprende dall'ultimo stato dopo un riavvio 
- `Classifica globale e classifica di ogni partita`
- `Statistiche giocatore`: win/loss rate, streak, distribuzione errori

## Struttura del progetto
```text
progetto_lab3/
├── server/                     # File sorgente server
│   ├── game/                   # Logica di gioco (GameManager, GameLoader, ...)
│   ├── ServerMain.java
│   ├── GameServer.java
│   ├── ClientHandler.java
│   ├── UserManager.java
│   ├── ServerConfig.java
│   ├── PersistenceTask.java
│   ├── User.java
│   └── UDPNotifier.java
│
├── client/                     # File sorgente client
│   ├── ClientMain.java
│   ├── ClientCommands.java
│   └── UDPListener.java
│
├── messages/                   # Classi condivise
│   ├── requests/
│   ├── responses/
│   ├── Group.java
│   ├── JsonUtils.java
│   └── NetworkUtils.java
│
├── config/
│   ├── ServerConfig.properties
│   └── ClientConfig.properties
│
├── data/
│   └── Connections_Data.json   # Dati di gioco (911 round)
│
├── gson-2.13.2.jar
├── ServerMain.jar
├── ClientMain.jar
└── relazione-laboratorio3-Campani-585480.pdf
```

## Compilazione 
### Server
```bash
javac -cp "gson-2.13.2.jar" -d out server/game/*.java server/*.java messages/*.java messages/responses/*.java messages/requests/*.java
```
### Client
```bash
javac -cp "gson-2.13.2.jar" -d out client/*.java messages/*.java messages/requests/*.java messages/responses/*.java
```

## Esecuzione 
```bash
java -jar ServerMain.jar
java -jar ClientMain.jar
```

## Configurazione 
### Server
| Parametro                   | Default                    | Descrizione                      |
| --------------------------- | -------------------------- | -------------------------------- |
| server.tcp.port             | 50000                      | Porta TCP                        |
| server.threadpool.size      | 10                         | Dimensione thread pool           |
| server.game.duration        | 120                        | Durata round (secondi)           |
| server.words.file           | data/Connections_Data.json | File dati gioco                  |
| server.users.file           | data/users.json            | File utenti                      |
| server.game.state.file      | data/game_state.json       | Stato partite                    |
| server.persistence.interval | 30                         | Intervallo salvataggio (secondi) |

### Client
| Parametro       | Default   | Descrizione      |
| --------------- | --------- | ---------------- |
| server.host     | localhost | Indirizzo server |
| server.tcp.port | 50000     | Porta TCP server |


## Comandi Client
| Comando           | Stato           | Descrizione                                  |
| ----------------- | --------------- | -------------------------------------------- |
| register          | Non autenticato | Registra un nuovo utente                     |
| login             | Non autenticato | Login e avvio listener UDP                   |
| logout            | Autenticato     | Logout e chiusura socket UDP                 |
| update credential | Entrambi        | Aggiorna username/password                   |
| submit proposal   | Autenticato     | Invia 4 parole (es. HEAT, JAZZ, BUCKS, NETS) |
| request game info | Autenticato     | Info su una partita                          |
| game stats        | Autenticato     | Statistiche partita                          |
| leaderboard       | Autenticato     | Classifica globale                           |
| player stats      | Autenticato     | Statistiche personali                        |
| help              | Entrambi        | Mostra comandi                               |
| exit              | Entrambi        | Disconnessione                               |

## Protocollo
La comunicazione utilizza un protocollo JSON line-based su TCP.
- Ogni messaggio è un oggetto JSON terminato da \n
- il campo operation identifica il tipo di richiesta

## Persistenza 
Vengono mantenuti due file JSON: 
- `user.json` per account, statistiche e stato corrente dei giocatori
- `game_state.json` per mantenere l'indice della prossima partita e le statistiche delle partite 
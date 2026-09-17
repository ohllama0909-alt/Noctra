# NoctraMod — Social System

**Status:** ARCHIVED / FUTURE FEATURE  
**Typ:** Context Document + PRD  
**Erstellt:** 2026-06-23  
**Autor:** Snenjih

---

## 1. Kontext & Hintergrund

NoctraMod ist eine rein client-seitige Fabric-Mod für Minecraft 1.21.11 (Env: `client`). Sie fügt kein Server-Content hinzu, sondern nur client-seitige Features. Das Social-System sprengt diesen Rahmen absichtlich: Es ist das erste Feature der Mod, das externe Infrastruktur außerhalb von Minecraft erfordert. Die Entscheidung, es trotzdem zu bauen, ergibt sich aus der Community-Vision: Die Mod soll nicht nur Tools liefern, sondern eine eigene Spieler-Community um sich aufbauen.

Das Social-System besteht aus vier Teilkomponenten:

1. **Nametag-Badge** — Visuelles Erkennungszeichen für Mod-Nutzer im Spiel (client-only, Teil dieser Mod)
2. **Freundesliste** — Spieler als Freunde hinzufügen, verwalten, online-Status sehen (benötigt Backend)
3. **Echtzeit-Chat** — Direktnachrichten zwischen Freunden, unabhängig vom Minecraft-Server (benötigt Backend + WebSocket)
4. **Daily Screenshot + Streaks** — Täglich einen Screenshot teilen, Streaks aufbauen à la Snapchat (benötigt Backend + Dateispeicher)

Das **Nametag-Badge** ist der einzige Teil, der als reine Mod-Erweiterung ohne Backend umsetzbar ist und wird separat implementiert (siehe `tasks/visual/nametag_badge.md`). Alle anderen Teile erfordern einen eigenständigen Backend-Server.

---

## 2. Vision & Ziele

### Vision
Mandatory-Nutzer erkennen sich gegenseitig im Spiel, können unabhängig vom aktuellen Minecraft-Server miteinander kommunizieren und bauen durch tägliche Screenshot-Shares gemeinsame Streaks auf — ähnlich wie Snapchat, aber eingebettet in den Minecraft-Kontext.

### Ziele
- Gemeinschaftsgefühl unter Mandatory-Nutzern fördern
- Cross-Server-Kommunikation ermöglichen (Freunde chatten auch wenn sie auf verschiedenen Servern sind)
- Tägliche Engagement-Loop durch Screenshot-Streaks schaffen
- Authentifizierung ohne separates Account-System (via Mojang-Auth)

### Nicht-Ziele
- Kein Server-Content, keine Gameplay-Eingriffe auf Drittserver
- Kein Ersatz für Discord oder andere externe Plattformen
- Keine öffentlichen Profil-Seiten oder Social-Media-Feed

---

## 3. User Stories

### Freundesliste
- Als Spieler möchte ich einen anderen Mandatory-Nutzer über seinen Minecraft-Namen als Freund hinzufügen, ohne seinen Discord oder sonstige Kontaktdaten zu kennen.
- Als Spieler möchte ich sehen, welche meiner Freunde gerade online sind (in Minecraft, via Mod).
- Als Spieler möchte ich Freundschaftsanfragen annehmen oder ablehnen.
- Als Spieler möchte ich einen Freund jederzeit wieder entfernen.

### Chat
- Als Spieler möchte ich einem Freund eine Direktnachricht senden, egal auf welchem Server er gerade spielt.
- Als Spieler möchte ich eine Benachrichtigung erhalten, wenn ich eine neue Nachricht bekomme, egal ob ich im Spiel oder im Menü bin.
- Als Spieler möchte ich meinen Chat-Verlauf mit jedem Freund einsehen können.
- Als Spieler möchte ich Nachrichten als gelesen markieren.

### Daily Screenshot + Streaks
- Als Spieler möchte ich täglich einen meiner Minecraft-Screenshots an Freunde schicken.
- Als Spieler möchte ich sehen, was meine Freunde heute geteilt haben.
- Als Spieler möchte ich einen Streak-Counter sehen, wie viele Tage ich in Folge mit einem Freund geteilt habe.
- Als Spieler möchte ich eine Erinnerung bekommen, wenn ich meinen Tages-Screenshot noch nicht geteilt habe.
- Als Spieler möchte ich aus dem Screenshot-Manager (bereits in der Mod vorhanden) direkt teilen können.

### Nametag-Badge
- Als Spieler möchte ich sofort erkennen, welche Spieler auf demselben Server ebenfalls Mandatory nutzen.
- Als Spieler möchte ich das Badge deaktivieren können, wenn ich es nicht sehen will.

---

## 4. Technische Architektur

### Gesamtbild

```
┌─────────────────────────────────┐        ┌──────────────────────────────────┐
│   Minecraft Client (Mod)        │        │   Mandatory Backend Server       │
│                                 │        │                                  │
│  SocialManager                  │◄──────►│  REST API  (HTTP/S)              │
│    - FriendService              │        │    /auth/verify                  │
│    - ChatService                │        │    /friends/*                    │
│    - ScreenshotService          │◄──────►│    /screenshots/*                │
│    - StreakService              │        │    /streaks/*                    │
│                                 │        │                                  │
│  WebSocketClient                │◄──────►│  WebSocket Server                │
│    - onMessage()                │        │    /ws (realtime chat)           │
│    - onNotification()           │        │                                  │
│                                 │        │  Database (PostgreSQL)           │
│  SocialScreen (UI)              │        │    users, friends, messages,     │
│    FriendsListScreen            │        │    screenshots, streaks          │
│    ChatScreen                   │        │                                  │
│    DailyShareScreen             │        │  File Storage                    │
│                                 │        │    screenshots/ (S3 oder lokal)  │
└─────────────────────────────────┘        └──────────────────────────────────┘
                                                        ▲
                                                        │ verifiziert
                                                        ▼
                                           ┌──────────────────────────────────┐
                                           │  Mojang Authentication API       │
                                           │  https://sessionserver.mojang.com│
                                           │  /session/minecraft/hasJoined    │
                                           └──────────────────────────────────┘
```

### Authentifizierung (ohne eigenes Account-System)

Der Minecraft-Client kennt seinen eigenen Session-Token (`mc.getSession().getAccessToken()`). Der Login-Flow nutzt die Mojang-Auth:

1. Mod generiert einen zufälligen `serverId`-String
2. Client ruft `mc.getSessionService().joinServer(profile, accessToken, serverId)` auf
3. Client sendet `{ uuid, username, serverId }` an `POST /auth/verify`
4. Backend ruft Mojang's `hasJoined?username=X&serverId=Y` auf
5. Mojang bestätigt → Backend gibt JWT zurück
6. Mod speichert JWT in `ModConfig` für nachfolgende Requests

JWT läuft nach 30 Tagen ab, dann Re-Auth. Kein Passwort nötig — der Minecraft-Account ist der Beweis.

---

## 5. Backend — API Design

**Base URL:** `https://api.mandatory.gg/v1` (Beispiel-Domain)

### Auth

| Method | Path | Body | Response |
|---|---|---|---|
| `POST` | `/auth/verify` | `{ uuid, username, serverId }` | `{ token: JWT }` |
| `GET` | `/auth/me` | — | `{ uuid, username, joinedAt }` |

### Freunde

| Method | Path | Body | Response |
|---|---|---|---|
| `GET` | `/friends` | — | `[{ uuid, username, online, lastSeen }]` |
| `POST` | `/friends/request` | `{ targetUsername }` | `{ requestId }` |
| `GET` | `/friends/requests` | — | `[{ from: { uuid, username }, createdAt }]` |
| `POST` | `/friends/requests/:id/accept` | — | `{ ok: true }` |
| `POST` | `/friends/requests/:id/decline` | — | `{ ok: true }` |
| `DELETE` | `/friends/:uuid` | — | `{ ok: true }` |

### Chat

| Method | Path | Body | Response |
|---|---|---|---|
| `GET` | `/messages/:friendUuid` | — | `[{ id, from, content, sentAt, read }]` |
| `POST` | `/messages/:friendUuid` | `{ content }` | `{ messageId, sentAt }` |
| `POST` | `/messages/:friendUuid/read` | — | `{ ok: true }` |

Echtzeit-Nachrichten laufen über WebSocket. Nach Connect sendet der Server neue Nachrichten als Push-Events:
```json
{ "type": "message", "from": "uuid", "content": "...", "sentAt": "..." }
{ "type": "friend_request", "from": { "uuid": "...", "username": "..." } }
{ "type": "streak_reminder", "friendUuid": "..." }
```

### Screenshots & Streaks

| Method | Path | Body | Response |
|---|---|---|---|
| `POST` | `/screenshots/share` | `multipart: file + friendUuids[]` | `{ screenshotId }` |
| `GET` | `/screenshots/today` | — | `[{ from: { uuid, username }, url, sharedAt }]` |
| `GET` | `/streaks` | — | `[{ friendUuid, username, days, lastShareDate }]` |
| `GET` | `/streaks/:friendUuid` | — | `{ days, myLastShare, theirLastShare }` |

Screenshot-Regeln:
- Pro Tag **ein** Screenshot pro Freundschaft (beidseitig nötig für Streak-Erhalt)
- Streak bricht ab, wenn einer der beiden 24h nicht geteilt hat (Midnight UTC als Grenze)
- Screenshots werden nach **48 Stunden** automatisch gelöscht (Snapchat-Prinzip), Streak-Zahl bleibt

---

## 6. Datenmodell (Datenbank)

### Tabellen (PostgreSQL)

```sql
-- Nutzer (via Mojang-Auth angelegt)
CREATE TABLE users (
    uuid        UUID PRIMARY KEY,
    username    VARCHAR(16) NOT NULL,
    joined_at   TIMESTAMPTZ DEFAULT now(),
    last_seen   TIMESTAMPTZ
);

-- Freundschaften
CREATE TABLE friendships (
    id          SERIAL PRIMARY KEY,
    user_a      UUID REFERENCES users(uuid),
    user_b      UUID REFERENCES users(uuid),
    created_at  TIMESTAMPTZ DEFAULT now(),
    UNIQUE(user_a, user_b),
    CHECK(user_a < user_b)  -- kanonische Reihenfolge vermeidet Duplikate
);

-- Freundschaftsanfragen
CREATE TABLE friend_requests (
    id          SERIAL PRIMARY KEY,
    from_user   UUID REFERENCES users(uuid),
    to_user     UUID REFERENCES users(uuid),
    sent_at     TIMESTAMPTZ DEFAULT now(),
    status      VARCHAR(10) DEFAULT 'pending' -- pending | accepted | declined
);

-- Nachrichten
CREATE TABLE messages (
    id          BIGSERIAL PRIMARY KEY,
    from_user   UUID REFERENCES users(uuid),
    to_user     UUID REFERENCES users(uuid),
    content     TEXT NOT NULL,
    sent_at     TIMESTAMPTZ DEFAULT now(),
    read_at     TIMESTAMPTZ
);

-- Screenshots
CREATE TABLE screenshots (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    from_user   UUID REFERENCES users(uuid),
    file_key    VARCHAR(256) NOT NULL,  -- Pfad im Dateispeicher
    shared_at   TIMESTAMPTZ DEFAULT now(),
    expires_at  TIMESTAMPTZ DEFAULT now() + interval '48 hours'
);

-- Screenshot-Empfänger (shared mit welchen Freunden)
CREATE TABLE screenshot_recipients (
    screenshot_id UUID REFERENCES screenshots(id) ON DELETE CASCADE,
    to_user       UUID REFERENCES users(uuid),
    viewed_at     TIMESTAMPTZ,
    PRIMARY KEY(screenshot_id, to_user)
);

-- Streaks
CREATE TABLE streaks (
    user_a          UUID REFERENCES users(uuid),
    user_b          UUID REFERENCES users(uuid),
    days            INT DEFAULT 0,
    last_share_a    DATE,  -- letzter Tag, an dem user_a geteilt hat
    last_share_b    DATE,  -- letzter Tag, an dem user_b geteilt hat
    updated_at      TIMESTAMPTZ DEFAULT now(),
    PRIMARY KEY(user_a, user_b),
    CHECK(user_a < user_b)
);
```

---

## 7. Client-Side (Mod) — Neue Klassen

### Paketstruktur

```
de.snenjih.noctra.social/
    SocialManager.java          -- Singleton, verwaltet Auth + alle Services
    auth/
        MojangAuthFlow.java     -- joinServer() + POST /auth/verify → JWT
        TokenStore.java         -- JWT in ModConfig speichern/laden
    net/
        ApiClient.java          -- HTTP-Client (java.net.http.HttpClient)
        WsClient.java           -- WebSocket-Client (java.net.http.HttpClient WS)
        WsEvent.java            -- Sealed interface für eingehende WS-Events
    friends/
        FriendService.java      -- REST-Calls für Freundesliste
        Friend.java             -- Record: uuid, username, online, lastSeen
    chat/
        ChatService.java        -- REST + WS für Nachrichten
        ChatMessage.java        -- Record: id, fromUuid, content, sentAt, read
    screenshot/
        ScreenshotShareService.java  -- Multipart-Upload
        DailyShare.java              -- Record: fromUsername, url, sharedAt
    streak/
        StreakService.java      -- REST für Streak-Daten
        StreakEntry.java        -- Record: friendUuid, username, days
    ui/
        SocialScreen.java       -- Hauptmenü: Tabs für Friends / Chat / Daily
        FriendListWidget.java   -- Scrollbare Freundesliste
        ChatWidget.java         -- Nachrichtenansicht + Eingabe
        DailyShareWidget.java   -- Heutiger Screenshot-Feed + Upload-Button
        StreakBadge.java        -- Flame-Icon + Zahl (wird im FriendListWidget genutzt)
```

### Integration in bestehende Mod

- **`NoctraMod.onInitializeClient()`**: `SocialManager.init()` aufrufen, asynchron (kein Block des Main-Thread)
- **`MainMenuScreen`**: Button "Social" → `SocialScreen` öffnen
- **`ScreenshotGalleryScreen`**: Button "Teilen" an Screenshot-Kacheln → `ScreenshotShareService.share(file, friendUuids)`
- **`NotificationManager`**: Eingehende WS-Events als In-Game-Notification anzeigen (Freundschaftsanfrage, neue Nachricht, Streak-Erinnerung)

---

## 8. Backend — Technologie-Stack (Empfehlung)

| Komponente | Empfehlung | Alternative |
|---|---|---|
| Sprache | **Node.js + TypeScript** | Spring Boot (Java) |
| Web-Framework | **Fastify** | Express |
| WebSocket | **ws** (Fastify-Plugin) | Socket.io |
| Datenbank | **PostgreSQL** | SQLite (für Anfang) |
| ORM | **Drizzle** | Prisma |
| Dateispeicher | **Lokales FS + Nginx** | Cloudflare R2, S3 |
| Auth | Mojang-Session-Verify + **JWT** (jose) | — |
| Hosting | **Hetzner VPS (CX22 ~4€/Mo)** | Railway, Fly.io |
| Reverse Proxy | **Caddy** (Auto-TLS) | nginx |

Node.js ist sinnvoll, weil:
- Asynchrones I/O passt gut zu WebSocket + vielen gleichzeitigen Verbindungen
- Kein neues Ökosystem wenn Java nur in der Mod genutzt wird
- Schnelle Iteration

---

## 9. Implementierungsphasen

### Phase 1 — Nametag-Badge (DONE: separate task)
Nur Mod-Code. Kein Backend. Spieler mit Mod erkennen sich auf demselben Server.
→ Spec: `tasks/visual/nametag_badge.md`

### Phase 2 — Backend Grundgerüst + Auth
- VPS aufsetzen, Caddy + PostgreSQL installieren
- Node.js-Projekt initialisieren, Tabellen anlegen
- `POST /auth/verify` implementieren (Mojang-Verify + JWT)
- Mod: `MojangAuthFlow`, `ApiClient`, `TokenStore`
- Ziel: Mod kann sich einloggen und einen JWT bekommen

### Phase 3 — Freundesliste
- Backend: `/friends/*`-Endpoints
- Mod: `FriendService`, `FriendListWidget` in `SocialScreen`
- Freundschaftsanfragen senden und annehmen

### Phase 4 — Echtzeit-Chat
- Backend: WebSocket-Server, `/messages/*`-Endpoints
- Mod: `WsClient`, `ChatService`, `ChatWidget`
- `NotificationManager`-Integration für neue Nachrichten

### Phase 5 — Daily Screenshot + Streaks
- Backend: Screenshot-Upload, Streak-Berechnung (Cronjob), `/screenshots/*`, `/streaks/*`
- Mod: `ScreenshotShareService`, `DailyShareWidget`, `StreakBadge`
- Integration in `ScreenshotGalleryScreen`

---

## 10. Offene Fragen & Entscheidungen

| Frage | Optionen | Empfehlung |
|---|---|---|
| Screenshot-Ablauf | 24h, 48h, 7 Tage | 48h (Snapchat-Feeling, kein Dauerarchiv) |
| Streak-Reset-Zeit | Midnight UTC, Midnight lokal | UTC (einheitlich, einfacher im Backend) |
| Online-Status | Echtzeit via WS-Heartbeat, oder nur "zuletzt gesehen" | "Zuletzt gesehen" für Anfang |
| Max. Screenshot-Größe | 2 MB, 5 MB, 10 MB | 5 MB (Minecraft-Screenshots sind unkomprimiert klein) |
| Freundeslimit | unbegrenzt, 50, 100 | 50 für Anfang |
| Domain | mandatory.gg, mandatory.mod | offen |
| Datenschutz/DSGVO | Nutzer-Daten in EU hosten | Hetzner Nürnberg = EU ✓ |
| Screenshot Moderation | Kein Filter, Hash-Blacklist, AI-Moderation | Hash-Blacklist (pHash) für bekannte CSAM |

---

## 11. Sicherheitsüberlegungen

- **JWT-Secret** nie in Client-Code: Der JWT wird nur server-seitig signiert und verifiziert. Der Client speichert ihn als Bearer-Token.
- **Rate Limiting**: Alle Endpoints rate-limiten (z.B. 10 Anfragen/s pro IP, 1 Screenshot-Upload/5min pro User).
- **Screenshot-Inhalte**: pHash-Blacklist-Check beim Upload-Endpoint (vor dem Speichern).
- **Freundschafts-Validierung**: Nachrichten und Screenshot-Shares nur zwischen bestehenden Freundschaften möglich.
- **UUID-Fälschung**: Mojang-Auth macht UUID-Fälschung unmöglich — der Verify-Step bestätigt, dass der Client wirklich der Besitzer des Accounts ist.
- **HTTPS only**: Caddy erzwingt TLS, kein HTTP.

---

## 12. Abhängigkeiten von der bestehenden Mod

| Bestehende Klasse | Wird genutzt für |
|---|---|
| `NotificationManager` | In-Game-Toasts für neue Nachrichten, Freundschaftsanfragen |
| `ScreenshotGalleryScreen` | "Teilen"-Button in Screenshot-Galerie |
| `ModConfig` | JWT speichern (neues Feld `social.token`) |
| `MainMenuScreen` | Neuer "Social"-Button im Hauptmenü der Mod |
| `ModuleCategory` | Evtl. neue Kategorie `SOCIAL` für Social-bezogene Module |

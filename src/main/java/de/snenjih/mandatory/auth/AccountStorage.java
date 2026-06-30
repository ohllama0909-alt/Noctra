package de.snenjih.mandatory.auth;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class AccountStorage {

    private static final Gson GSON    = new GsonBuilder().setPrettyPrinting().create();
    private static final int  VERSION = 1;

    private static AccountStorage INSTANCE;

    private final Path          filePath;
    private final List<AccountEntry> accounts = new ArrayList<>();
    private       UUID          activeUuid;

    private AccountStorage(Path filePath) {
        this.filePath = filePath;
    }

    public static AccountStorage load() {
        Path dir  = FabricLoader.getInstance().getGameDir().resolve("mandatory");
        Path file = dir.resolve("accounts.json");
        try { Files.createDirectories(dir); } catch (IOException ignored) {}
        AccountStorage storage = new AccountStorage(file);
        storage.loadFromDisk();
        INSTANCE = storage;
        return storage;
    }

    public static AccountStorage getInstance() { return INSTANCE; }

    // ---- Persistence --------------------------------------------------------

    private void loadFromDisk() {
        if (!Files.exists(filePath)) return;
        try (Reader r = Files.newBufferedReader(filePath)) {
            JsonElement root = JsonParser.parseReader(r);
            if (!root.isJsonObject()) return;
            JsonObject obj = root.getAsJsonObject();

            if (obj.has("active") && obj.get("active").isJsonPrimitive()) {
                try { activeUuid = UUID.fromString(obj.get("active").getAsString()); }
                catch (Exception ignored) {}
            }

            JsonArray arr = obj.has("accounts") ? obj.getAsJsonArray("accounts") : new JsonArray();
            for (JsonElement el : arr) {
                if (!el.isJsonObject()) continue;
                JsonObject a = el.getAsJsonObject();
                try {
                    UUID       uuid    = UUID.fromString(a.get("uuid").getAsString());
                    String     name    = a.get("username").getAsString();
                    String     skin    = a.has("skin_url")  ? a.get("skin_url").getAsString()  : "";
                    JsonObject session = a.has("session")   ? a.getAsJsonObject("session")      : null;
                    accounts.add(new AccountEntry(uuid, name, skin, session));
                } catch (Exception ignored) {}
            }
        } catch (IOException | JsonSyntaxException e) {
            System.err.println("[Mandatory] Failed to load accounts: " + e.getMessage());
        }
    }

    public void save() {
        JsonObject root = new JsonObject();
        root.addProperty("_version", VERSION);
        if (activeUuid != null)
            root.addProperty("active", activeUuid.toString());
        else
            root.add("active", JsonNull.INSTANCE);

        JsonArray arr = new JsonArray();
        for (AccountEntry e : accounts) {
            JsonObject a = new JsonObject();
            a.addProperty("uuid",     e.uuid().toString());
            a.addProperty("username", e.username());
            a.addProperty("skin_url", e.skinUrl());
            if (e.sessionJson() != null) a.add("session", e.sessionJson());
            arr.add(a);
        }
        root.add("accounts", arr);

        try (Writer w = Files.newBufferedWriter(filePath)) {
            GSON.toJson(root, w);
        } catch (IOException e) {
            System.err.println("[Mandatory] Failed to save accounts: " + e.getMessage());
        }
    }

    // ---- Query --------------------------------------------------------------

    public List<AccountEntry> getAll()      { return List.copyOf(accounts); }
    public UUID               getActiveUuid() { return activeUuid; }

    public AccountEntry getActive() {
        if (activeUuid != null) {
            for (AccountEntry e : accounts)
                if (e.uuid().equals(activeUuid)) return e;
        }
        return accounts.isEmpty() ? null : accounts.get(0);
    }

    public AccountEntry findByUuid(UUID uuid) {
        for (AccountEntry e : accounts)
            if (e.uuid().equals(uuid)) return e;
        return null;
    }

    // ---- Mutation -----------------------------------------------------------

    public void setActive(UUID uuid) { activeUuid = uuid; }

    /** Adds the entry, replacing any existing entry with the same UUID. */
    public void addOrReplace(AccountEntry entry) {
        accounts.removeIf(e -> e.uuid().equals(entry.uuid()));
        accounts.add(entry);
        if (activeUuid == null) activeUuid = entry.uuid();
    }

    public void remove(UUID uuid) {
        accounts.removeIf(e -> e.uuid().equals(uuid));
        if (uuid.equals(activeUuid))
            activeUuid = accounts.isEmpty() ? null : accounts.get(0).uuid();
    }
}

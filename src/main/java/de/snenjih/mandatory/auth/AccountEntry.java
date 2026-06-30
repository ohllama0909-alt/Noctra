package de.snenjih.mandatory.auth;

import com.google.gson.JsonObject;

import java.util.UUID;

public final class AccountEntry {

    private final UUID   uuid;
    private       String username;
    private       String skinUrl;
    private       JsonObject sessionJson;

    public AccountEntry(UUID uuid, String username, String skinUrl, JsonObject sessionJson) {
        this.uuid        = uuid;
        this.username    = username;
        this.skinUrl     = skinUrl != null ? skinUrl : "";
        this.sessionJson = sessionJson;
    }

    public UUID       uuid()        { return uuid; }
    public String     username()    { return username; }
    public String     skinUrl()     { return skinUrl; }
    public JsonObject sessionJson() { return sessionJson; }

    public void setUsername(String username)       { this.username    = username; }
    public void setSkinUrl(String url)             { this.skinUrl     = url != null ? url : ""; }
    public void setSessionJson(JsonObject session) { this.sessionJson = session; }
}

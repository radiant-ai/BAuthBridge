package fun.milkyway.bauthbridge.common;

import java.util.UUID;

public class AuthorizationMessage implements Message {
    private Action action;
    private UUID playerUUID;
    public enum Action {
        LOGIN,
        REGISTER,
        LOGOUT
    }
    public AuthorizationMessage(UUID playerUUID, Action action) {
        this.playerUUID = playerUUID;
        this.action = action;
    }
    public AuthorizationMessage(String message) throws IllegalArgumentException {
        String[] tokens = message.split("\\|");
        if (tokens.length > 1) {
            this.action = Action.valueOf(tokens[0]);
            this.playerUUID = UUID.fromString(tokens[1]);
        }
    }
    @Override
    public String asString() {
        return action.name()+"|"+playerUUID.toString();
    }
    public UUID getPlayerUUID() {
        return playerUUID;
    }
    public Action getAction() {
        return action;
    }
}

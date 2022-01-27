package fun.milkyway.bauthbridge.paper.listeners;

import fun.milkyway.bauthbridge.common.AuthorizationMessage;
import fun.milkyway.bauthbridge.common.Message;
import fun.milkyway.bauthbridge.paper.managers.MessageManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import ru.baronessdev.paid.auth.api.events.*;

public class AuthorizationListener implements Listener {

    private final MessageManager messageManager;

    public AuthorizationListener(MessageManager messageManager) {
        this.messageManager = messageManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerLogin(AuthPlayerLoginEvent event) {
        Player player = event.getPlayer();
        Message message = new AuthorizationMessage(player.getUniqueId(), AuthorizationMessage.Action.LOGIN);
        messageManager.sendMessage(message, 2);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerLogin(AuthPlayerSessionSavedEvent event) {
        Player player = event.getPlayer();
        Message message = new AuthorizationMessage(player.getUniqueId(), AuthorizationMessage.Action.LOGIN);
        messageManager.sendMessage(message, 2);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerRegister(AuthPlayerRegisterEvent event) {
        Player player = event.getPlayer();
        Message message = new AuthorizationMessage(player.getUniqueId(), AuthorizationMessage.Action.REGISTER);
        messageManager.sendMessage(message, 2);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerRegister(AuthPlayerPreRegisterEvent event) {
        Player player = event.getPlayer();
        Message message = new AuthorizationMessage(player.getUniqueId(), AuthorizationMessage.Action.PREREGISTER);
        messageManager.sendMessage(message, 2);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerLogin(AuthPlayerPreLoginEvent event) {
        Player player = event.getPlayer();
        Message message = new AuthorizationMessage(player.getUniqueId(), AuthorizationMessage.Action.PRELOGIN);
        messageManager.sendMessage(message, 2);
    }
}

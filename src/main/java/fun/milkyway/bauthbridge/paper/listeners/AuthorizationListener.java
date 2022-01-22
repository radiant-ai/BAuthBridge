package fun.milkyway.bauthbridge.paper.listeners;

import fun.milkyway.bauthbridge.common.AuthorizationMessage;
import fun.milkyway.bauthbridge.common.Message;
import fun.milkyway.bauthbridge.paper.managers.MessageManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import ru.baronessdev.paid.auth.api.events.AuthPlayerLoginEvent;

public class AuthorizationListener implements Listener {
    private MessageManager messageManager;
    public AuthorizationListener(MessageManager messageManager) {
        this.messageManager = messageManager;
    }
    @EventHandler
    public void onPlayerLogin(AuthPlayerLoginEvent event) {
        Player player = event.getPlayer();
        Message message = new AuthorizationMessage(player.getUniqueId(), AuthorizationMessage.Action.LOGIN);
        messageManager.sendMessage(message);
    }
}

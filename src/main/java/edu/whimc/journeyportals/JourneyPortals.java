package edu.whimc.journeyportals;

import edu.whimc.portals.Portal;
import java.util.Objects;
import java.util.stream.Collectors;
import net.whimxiqal.journey.api.JourneyApi;
import net.whimxiqal.journey.api.JourneyApiProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class JourneyPortals extends JavaPlugin {

  public static JourneyPortals instance;

  @Override
  public void onEnable() {
    instance = this;
    JourneyApi journey = JourneyApiProvider.get();
    journey.registerTunnels(player -> Portal.getPortals().stream()
        .filter(portal -> portal.getDestination() != null)
        .filter(portal -> portal.getWorld() != null)
        .filter(portal -> portal.getDestination().getLocation().getWorld() != null)
        .filter(portal -> portal.getPermission() == null || player.hasPermission(portal.getPermission().getName()))
        .map(portal -> {
          try {
            return PortalTunnel.from(portal);
          } catch (Exception e) {
            return null;
          }
        })
        .filter(Objects::nonNull)
        .collect(Collectors.toList()));
  }
}

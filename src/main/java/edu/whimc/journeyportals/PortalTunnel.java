package edu.whimc.journeyportals;

import edu.whimc.portals.Portal;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import net.whimxiqal.journey.api.Cell;
import net.whimxiqal.journey.api.Tunnel;
import net.whimxiqal.journey.bukkit.api.JourneyBukkitApi;
import net.whimxiqal.journey.bukkit.api.JourneyBukkitApiProvider;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;

public class PortalTunnel implements Tunnel {

  public final static int COST = 2;
  private final String portalName;
  private final Cell origin;
  private final Cell destination;

  private PortalTunnel(String name, Cell origin, Cell destination) {
    this.portalName = name;
    this.origin = origin;
    this.destination = destination;
  }

  /**
   * Static constructor, to create a port directly from a WHIMC portal.
   *
   * @param portal the portal
   * @return the generated port
   */
  public static PortalTunnel from(Portal portal) {
    if (portal.getWorld() == null) {
      throw new IllegalStateException("Error with portal: " + portal.getName()
          + "A Portal Tunnel may only be created with portals that have a world.");
    }
    if (portal.getDestination() == null) {
      throw new IllegalStateException("Error with portal: " + portal.getName()
          + "A Portal Tunnel may only be created with portals that have a destination.");
    }
    if (portal.getDestination().getLocation().getWorld() == null) {
      throw new IllegalStateException("Error with portal: " + portal.getName()
          + ". A Portal Tunnel may only be created with portals"
          + " that have a world associated with its destination.");
    }

    Cell origin = getOriginOf(portal);
    if (origin == null) {
      throw new IllegalStateException("Error with portal: " + portal.getName()
          + ". A reachable central location could not be identified.");
    }

    Cell destination = getDestinationOf(portal);

    return new PortalTunnel(portal.getName(), origin, destination);
  }

  @Nullable
  private static Cell getOriginOf(Portal portal) {
    // TODO all this logic can be much better
    CompletableFuture<Cell> future = new CompletableFuture<>();
    Bukkit.getScheduler().runTask(JourneyPortals.instance, () -> {
      JourneyBukkitApi journeyBukkit = JourneyBukkitApiProvider.get();
      // Start by trying to use the center of the portal.
      int locX = (portal.getPos1().getBlockX() + portal.getPos2().getBlockX()) / 2;  // center of portal
      int locY = Math.min(portal.getPos1().getBlockY(), portal.getPos2().getBlockY());  // bottom of portal
      int locZ = (portal.getPos1().getBlockZ() + portal.getPos2().getBlockZ()) / 2;
      while (!portal.getWorld().getBlockAt(locX, locY, locZ).getBlockData().getMaterial().isAir()
          || !portal.getWorld().getBlockAt(locX, locY + 1, locZ).getBlockData().getMaterial().isAir()) {
        locY++;
        if (locY > Math.max(portal.getPos1().getBlockY(), portal.getPos2().getBlockY())) {
          // There is no y value that works for the center of this portal.
          // Try every other point and see what sticks (this does not repeat)
          for (locX = portal.getPos1().getBlockX(); locX <= portal.getPos2().getBlockX(); locX++) {
            for (locY = portal.getPos1().getBlockY(); locY < portal.getPos2().getBlockY(); locY++) {
              for (locZ = portal.getPos1().getBlockZ(); locZ <= portal.getPos2().getBlockZ(); locZ++) {
                Material atFeet = portal.getWorld().getBlockAt(locX, locY, locZ).getBlockData().getMaterial();
                Material atHead = portal.getWorld().getBlockAt(locX, locY + 1, locZ).getBlockData().getMaterial();
                if (atFeet.isAir() && atHead.isAir()) {
                  future.complete(new Cell(locX, locY, locZ, journeyBukkit.toDomainId(portal.getWorld())));
                }
              }
            }
          }
          // Nothing at all found
          return;
        }
      }
      // We found one at the center of the portal!
      future.complete(new Cell(locX, locY, locZ, journeyBukkit.toDomainId(portal.getWorld())));
    });
    try {
      return future.get();
    } catch (InterruptedException | ExecutionException e) {
      JourneyPortals.instance.getLogger().severe("A future for creating a PortalTunnel failed");
      e.printStackTrace();
      return null;
    }
  }

  private static Cell getDestinationOf(Portal portal) {
    return JourneyBukkitApiProvider.get().toCell(portal.getDestination().getLocation());
  }

  @Override
  public String toString() {
    return "PortalTunnel{'" + portalName + "'}";
  }

  @Override
  public Cell origin() {
    return origin;
  }

  @Override
  public Cell destination() {
    return destination;
  }

  @Override
  public int cost() {
    return COST;
  }

  @Override
  public boolean completedWith(Cell location) {
    Portal portal = Portal.getPortal(this.portalName);
    return Math.min(portal.getPos1().getBlockX(), portal.getPos2().getBlockX()) <= location.blockX()
        && location.blockX() <= Math.max(portal.getPos1().getBlockX(), portal.getPos2().getBlockX())
        && Math.min(portal.getPos1().getBlockY(), portal.getPos2().getBlockY()) <= location.blockY()
        && location.blockY() <= Math.max(portal.getPos1().getBlockY(), portal.getPos2().getBlockY())
        && Math.min(portal.getPos1().getBlockZ(), portal.getPos2().getBlockZ()) <= location.blockZ()
        && location.blockZ() <= Math.max(portal.getPos1().getBlockZ(), portal.getPos2().getBlockZ());
  }

}

package simpleserver.message;

import simpleserver.Color;
import simpleserver.Coordinate.Dimension;
import simpleserver.Player;

public class DimensionChat extends AbstractChat {
  Dimension dimension;

  public DimensionChat(Player sender, Dimension dim) {
    super(sender);
    dimension = dim;
    chatRoom = dimension.toString();
  }

  @Override
  protected boolean sendToPlayer(Player reciever) {
    return reciever.getDimension().equals(dimension) || reciever.equals(sender);
  }

  @Override
  public void noRecieverFound() {
    sender.addTMessage(Color.RED, "Nobody is in this dimension to hear you");
  }
}

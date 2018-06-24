package civ.core.display.menu.button;

import java.awt.Graphics2D;
import civ.core.display.GUI;
import civ.core.event.Events;
import civ.core.input.MouseHandler;

public class UnitMenuButton extends Button {

  public UnitMenuButton(Events e, int bIndex) {
    super(GUI.getWindowHeight(), GUI.getHexRadius(), GUI.getHexRadius() * 4, bIndex);
    this.e = e;
  }

  public void drawButton(Graphics2D g) {
    // g.setColor(Color.WHITE);
    // g.fill3DRect(xPos, yPos, buttonSizeX, buttonSizeY, isClickable);
    g.drawImage(e.getImage(), xPos, yPos, buttonSizeX, buttonSizeY, null);
  }

  public void onPress() {
    if (MouseHandler.pressedMouse) {
      if (buttonBounds.intersects(MouseHandler.mX, MouseHandler.mY, BUTTON_CLICK_BUFFER,
          BUTTON_CLICK_BUFFER)) {
        MouseHandler.pressedMouse = false;
        performEvent();
      }
    }
  }

}

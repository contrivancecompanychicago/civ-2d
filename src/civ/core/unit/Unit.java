package civ.core.unit;

import java.util.ArrayList;
import java.util.List;
import civ.core.data.Layout;
import civ.core.data.Point;
import civ.core.data.hex.Hex;
import civ.core.data.hex.HexCoordinate;
import civ.core.data.hex.PathHex;
import civ.core.data.map.HexMap;
import civ.core.display.GUI;
import civ.core.display.menu.Menu;
import civ.core.instance.IUnit;
import civ.core.map.civilization.BaseCivilization;

public class Unit extends IUnit {
  protected Menu actionMenu;

  protected void init() {
  }

  public Unit(String name, BaseCivilization civOwner, HexCoordinate curPos,
      double movementPotential, int productionCost) {
    this.name = name;
    this.civOwner = civOwner;
    this.curPos = curPos;
    this.movement = movementPotential;
    this.movementPotential = movementPotential;
    this.movementTemp = movementPotential;
    this.productionCost = productionCost;
  }

  public Unit(String name, BaseCivilization civOwner, HexCoordinate curPos,
      double movementPotential, double strength, int productionCost, boolean isMilitary,
      boolean isSpawned) {
    this(name, civOwner, curPos, movementPotential, productionCost);
    this.strength = strength;
    this.isMilitary = isMilitary;
    this.isSpawned = isSpawned;
  }

  public void addToMapAndCiv(HexMap map, List<BaseCivilization> civs) {
    // Get the map hex for units
    Hex hex = map.getHex(curPos);

    // Set the units in the hexes
    hex.addNewUnit(this, isMilitary);

    // Update the hexes in the map
    map.setHex(curPos, hex);

    // Add units to the civ
    BaseCivilization c1 = civs.get(0);
    c1.addUnit(this);
    civs.set(0, c1);

    init();
  }

  public void deleteFromMapAndCiv(HexMap map, List<BaseCivilization> civs) {
    // Get the map hex for units
    Hex hex = map.getHex(curPos);

    // Set the units in the hexes
    hex.replaceUnit(null, isMilitary);

    // Update the hexes in the map
    map.setHex(curPos, hex);

    // Add units to the civ
    BaseCivilization c1 = civs.get(0);
    c1.deleteUnit(this);
    civs.set(0, c1);
  }

  public void deleteBySelling() {

  }
  
  public void nextTurn() {
    resetMovementTemp();
    resetMovementPotential();
  }

  public void moveUnit(GUI ui, HexMap map, List<BaseCivilization> civs, Hex focusHex, int scrollX, int scrollY) {
    Hex fromHex = map.getHex(focusHex);
    if (!fromHex.canSetCivilian() || !fromHex.canSetMilitary()) {
      int mouseX = 0;//MouseHandler.movedMX;
      int mouseY = 0;//MouseHandler.movedMY;

      HexCoordinate h = Layout.pixelToHex(new Point(mouseX - scrollX, mouseY - scrollY));
      Hex toHex = map.getHex(h);

      Unit cu = fromHex.getCivilianUnit();
      Unit mu = fromHex.getMilitaryUnit();

      Unit ctu = toHex.getCivilianUnit();
      Unit mtu = toHex.getMilitaryUnit();

      double pathTotal = 0.0D;
      for (PathHex ph : ui.getUnitPath()) {
        if (ph.getPassable() || ph.getCanSwitch()) {
          Hex mapHex = map.getHex(ph);
          pathTotal += mapHex.getMovementTotal();
        }
      }

      // Civ units and military units cannot occupy the same hex (for now)
      if (cu != null && ctu == null && mu == null && mtu != null) { // swapping units -- civ to mil
        if (sameOwner(cu, mtu)) {
          swapUnitOnMap(map, civs, fromHex, toHex, cu, mtu, pathTotal);
        }
      } else if (cu == null && ctu != null && mu != null && mtu == null) { // swapping units -- mil
                                                                           // to civ
        if (sameOwner(mu, ctu)) {
          swapUnitOnMap(map, civs, fromHex, toHex, mu, ctu, pathTotal);
        }
      } else if ((cu != null || mu != null) && ctu == null && mtu == null) { // Move civ or mil
                                                                             // units to empty hex
        moveUnitOnMap(map, civs, fromHex, toHex, cu != null ? cu : mu, pathTotal);
      }
      ui.setFocusedUnitPath(null);
    }
  }

  private void moveUnitOnMap(HexMap map, List<BaseCivilization> civs, Hex fromHex, Hex toHex, Unit u, double totalHexCost) {
    HexCoordinate newLocation = toHex.getPosition();
    u.decreaseMovement(totalHexCost);

    Unit tempUnit = u;
    tempUnit.setPosition(newLocation);

    fromHex.resetUnits();
    toHex.replaceUnit(u, tempUnit.getIsMilitary());

    // Move the unit on the map
    map.setHex(toHex);
    map.setHex(fromHex);

    // Add units to the civ
    BaseCivilization c1 = civs.get(0);
    c1.replaceUnit(u, tempUnit);
    civs.set(0, c1);
  }

  private void swapUnitOnMap(HexMap map, List<BaseCivilization> civs, Hex fromHex, Hex toHex, Unit currentFromUnit, Unit currentToUnit,
      double totalHexCost) {
    fromHex.resetUnits();
    toHex.resetUnits();

    currentToUnit.decreaseMovement(totalHexCost);
    currentFromUnit.decreaseMovement(totalHexCost);

    Unit tempToUnit = currentToUnit;
    Unit tempFromUnit = currentFromUnit;
    HexCoordinate unitFrom = currentFromUnit.getPosition();
    HexCoordinate unitTo = currentToUnit.getPosition();

    tempToUnit.setPosition(unitFrom);
    tempFromUnit.setPosition(unitTo);

    fromHex.replaceUnit(tempToUnit, currentToUnit.getIsMilitary());
    toHex.replaceUnit(tempFromUnit, currentFromUnit.getIsMilitary());

    // Move the units on the map
    map.setHex(toHex);
    map.setHex(fromHex);

    // Add units to the civ
    BaseCivilization c1 = civs.get(0);
    c1.replaceUnit(currentFromUnit, tempFromUnit);
    c1.replaceUnit(currentToUnit, tempToUnit);
    civs.set(0, c1);
  }

  private boolean sameOwner(Unit fromU, Unit toU) {
    return fromU.getOwner().sameCivilization(toU.getOwner().getID());
  }

  public List<PathHex> validUnitMove(GUI ui, HexMap map, List<HexCoordinate> path) {
    List<PathHex> finalPath = new ArrayList<PathHex>();
    Hex currentHex = map.getHex(ui.getFocusHex());

    boolean unitBlocking = false;
    double currentPathCost = 0D;
    for (int i = path.size(); --i >= 0;) {
      HexCoordinate h = path.get(i);
      Hex mapHex = map.getHex(h);

      double hexCost = currentHex.getMovementTotal();
      boolean unitMovementRemaining = this.ableToMove(hexCost);
      boolean done = false;

      currentPathCost += hexCost;

      if (unitMovementRemaining && !unitBlocking) {
        for (Unit u : mapHex.getUnits()) { // Check for a unit blocking the path
          if (u != null) {
            boolean canSwitch = false;
            unitBlocking = (u.getOwner() == this.getOwner() && u.ableToMove(currentPathCost));
            finalPath.add(new PathHex(h, !unitBlocking, canSwitch));
            done = true;
          }
          finalPath.add(new PathHex(h, !unitBlocking && !done));
        }
      }
    }
    this.setMovementTempForMultiMove();
    return finalPath;
  }

  public HexCoordinate getPosition() {
    return curPos;
  }

  public boolean getSpawned() {
    return isSpawned;
  }

  public boolean getIsMilitary() {
    return isMilitary;
  }

  public double getStrength() {
    return strength;
  }

  public String getName() {
    return name;
  }

  public double getTotalMovement() {
    return movement;
  }

  public double getMovementPotential() {
    return movementPotential;
  }

  public BaseCivilization getOwner() {
    return civOwner;
  }

  public int getHealth() {
    return health;
  }

  public int getProductionCost() {
    return productionCost;
  }

  public boolean isBeingMoved() {
    return isMoving;
  }

  public void toggleBeingMoved() {
    this.isMoving = !this.isMoving;
  }

  public void setBeingMoved(boolean moved) {
    this.isMoving = moved;
  }

  public boolean isBeingAttacked() {
    return isAttacking;
  }

  public void toggleBeingAttacked() {
    this.isMoving = !this.isMoving;
  }

  public void setBeingAttacked(boolean attack) {
    this.isMoving = attack;
  }

  public Menu getMenu() {
    return this.actionMenu;
  }

  public void setIsSpawned() {
    isSpawned = true;
  }

  public void setPosition(HexCoordinate h) {
    this.curPos = h;
  }

  public boolean ableToMove(double hexCost) {
    return ((movementTemp -= hexCost) >= 0D) && (movementPotential - hexCost >= 0D);
  }

  public void decreaseMovement(double hexCost) {
    this.movementPotential -= hexCost;
  }

  public void resetMovementTemp() {
    this.movementTemp = movement;
  }

  public void resetMovementPotential() {
    this.movementPotential = movement;
  }

  public void setMovementTempForMultiMove() {
    this.movementTemp =
        this.movementPotential != this.movement ? this.movementPotential : this.movement;
  }

  public boolean isInZOC(HexCoordinate h) {
    return curPos != null ? HexMap.rangesIntersect(h, curPos, 1) : false;
  }
}

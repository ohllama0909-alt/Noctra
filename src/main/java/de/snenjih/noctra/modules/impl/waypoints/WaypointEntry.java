package de.snenjih.noctra.modules.impl.waypoints;

public final class WaypointEntry {

    private final String name;
    private final int    x;
    private final int    y;
    private final int    z;
    private final int    colorArgb;
    private final String colorHex;
    private final String dimension;

    // Mutable, updated each tick
    public double cachedDistance = 0.0;
    public double cachedAngle    = 0.0;

    public WaypointEntry(String name, int x, int y, int z, int colorArgb, String colorHex, String dimension) {
        this.name      = name;
        this.x         = x;
        this.y         = y;
        this.z         = z;
        this.colorArgb = colorArgb;
        this.colorHex  = colorHex;
        this.dimension = dimension;
    }

    public String name()      { return name; }
    public int    x()         { return x; }
    public int    y()         { return y; }
    public int    z()         { return z; }
    public int    colorArgb() { return colorArgb; }
    public String colorHex()  { return colorHex; }
    public String dimension() { return dimension; }
}

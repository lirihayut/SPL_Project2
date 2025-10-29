package bgu.spl.mics.application.objects;

public class CloudPoint {
    private double x;
    private double y;

    public CloudPoint(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    @Override
    public String toString() {
        return "CloudPoint(x: " + x + ", y:" + y + " )";
    }
}

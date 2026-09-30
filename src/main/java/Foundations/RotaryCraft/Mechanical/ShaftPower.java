package Foundations.RotaryCraft.Mechanical;

/** A shaft signal, not an energy buffer. Speed is rad/s; torque is Nm. */
public record ShaftPower(int omega, int torque) {
    public static final ShaftPower STOPPED = new ShaftPower(0, 0);

    public ShaftPower {
        if (omega < 0 || torque < 0) {
            throw new IllegalArgumentException("Shaft speed and torque must be nonnegative");
        }
    }

    public long watts() {
        return (long) omega * torque;
    }

    /** Ideal legacy gearbox arithmetic; wear, lubricant and failure belong to the machine. */
    public ShaftPower gear(int ratio, boolean reduction, int speedLimit, int torqueLimit) {
        if (ratio != 2 && ratio != 4 && ratio != 8 && ratio != 16) {
            throw new IllegalArgumentException("Gear ratio must be 2, 4, 8 or 16");
        }
        if (speedLimit < 0 || torqueLimit < 0) {
            throw new IllegalArgumentException("Shaft limits must be nonnegative");
        }
        int speed = reduction ? omega / ratio : (int) Math.min((long) omega * ratio, speedLimit);
        int force = reduction ? (int) Math.min((long) torque * ratio, torqueLimit) : torque / ratio;
        return new ShaftPower(speed, force);
    }
}

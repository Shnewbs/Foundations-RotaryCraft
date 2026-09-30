package Foundations.RotaryCraft.Mechanical;

/** Legacy DC engine speed/torque, advanced once per server tick. */
public record DcEngineState(int speed, int torque) {
    public static final DcEngineState STOPPED = new DcEngineState(0, 0);
    public static final int MAX_SPEED = 256;
    public static final int MAX_TORQUE = 4;
    // Legacy compound assignment truncates 4 * log2(257) to 32 rad/s per tick.
    public static final int ACCELERATION = (int) (4 * Math.log(MAX_SPEED + 1) / Math.log(2));
    public DcEngineState {
        if (speed < 0 || speed > MAX_SPEED || torque < 0 || torque > MAX_TORQUE || (speed == 0 && torque != 0))
            throw new IllegalArgumentException("Invalid DC engine state");
    }
    public DcEngineState tick(boolean powered) {
        if (powered) return new DcEngineState(Math.min(MAX_SPEED, speed + ACCELERATION), MAX_TORQUE);
        int next = Math.max(0, speed - speed / 256 - 1);
        return next == 0 ? STOPPED : new DcEngineState(next, torque);
    }
    public static DcEngineState restore(int speed, int torque) {
        int boundedSpeed = Math.clamp(speed, 0, MAX_SPEED);
        return new DcEngineState(boundedSpeed, boundedSpeed == 0 ? 0 : Math.clamp(torque, 0, MAX_TORQUE));
    }
    public ShaftPower power() { return new ShaftPower(speed, torque); }
}

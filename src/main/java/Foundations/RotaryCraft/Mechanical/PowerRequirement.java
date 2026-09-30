package Foundations.RotaryCraft.Mechanical;

/** Legacy machines require all three thresholds, not just total power. */
public record PowerRequirement(int minimumTorque, int minimumSpeed, long minimumWatts) {
    public PowerRequirement {
        if (minimumTorque < 0 || minimumSpeed < 0 || minimumWatts < 0) {
            throw new IllegalArgumentException("Power requirements must be nonnegative");
        }
    }

    public boolean isSatisfiedBy(ShaftPower signal) {
        return signal.torque() >= minimumTorque
                && signal.omega() >= minimumSpeed
                && signal.watts() >= minimumWatts;
    }
}

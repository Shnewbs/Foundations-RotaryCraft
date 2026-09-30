package Foundations.RotaryCraft.Electrical;

import Foundations.RotaryCraft.Mechanical.ShaftPower;

/** Ideal ElectriCraft signal conversions; network resistance and overloads are separate. */
public record ElectricalPower(int voltage, int current) {
    // Legacy ElectriCraft WireNetwork.TORQUE_PER_AMP.
    public static final int TORQUE_PER_AMP = 8;

    public ElectricalPower {
        if (voltage < 0 || current < 0) {
            throw new IllegalArgumentException("Voltage and current must be nonnegative");
        }
    }

    public long watts() {
        return (long) voltage * current;
    }

    public static ElectricalPower fromShaft(ShaftPower shaft) {
        return new ElectricalPower(Math.multiplyExact(shaft.omega(), TORQUE_PER_AMP),
                shaft.torque() / TORQUE_PER_AMP);
    }

    public ShaftPower toShaft() {
        return new ShaftPower(voltage / TORQUE_PER_AMP,
                Math.multiplyExact(current, TORQUE_PER_AMP));
    }
}

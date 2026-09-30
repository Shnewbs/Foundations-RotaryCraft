package Foundations.RotaryCraft.Mechanical;

import Foundations.RotaryCraft.Electrical.ElectricalPower;

/** Dependency-free regression checks, run by Gradle check. */
public final class MechanicalPowerTest {
    public static void main(String[] args) {
        ShaftPower input = new ShaftPower(1024, 64);
        equal(65536L, input.watts(), "shaft power");
        for (int ratio : new int[] {2, 4, 8, 16}) {
            ShaftPower reduced = input.gear(ratio, true, Integer.MAX_VALUE, Integer.MAX_VALUE);
            equal(input.watts(), reduced.watts(), "ideal reduction conserves power");
            equal(input, reduced.gear(ratio, false, Integer.MAX_VALUE, Integer.MAX_VALUE), "round trip");
        }
        equal(4611686014132420609L, new ShaftPower(Integer.MAX_VALUE, Integer.MAX_VALUE).watts(), "long product");
        ShaftPower clipped = new ShaftPower(Integer.MAX_VALUE, Integer.MAX_VALUE)
                .gear(16, true, Integer.MAX_VALUE, 1000);
        equal(1000, clipped.torque(), "torque limit prevents overflow");
        equal(1000, input.gear(16, false, 1000, Integer.MAX_VALUE).omega(), "speed limit");
        equal(0L, new ShaftPower(1, 1).gear(16, true, Integer.MAX_VALUE, Integer.MAX_VALUE).watts(), "integer rounding");
        PowerRequirement grindstone = new PowerRequirement(256, 1, 16384);
        yes(grindstone.isSatisfiedBy(new ShaftPower(64, 256)), "exact grindstone boundary");
        yes(!grindstone.isSatisfiedBy(new ShaftPower(256, 64)), "equal watts cannot replace torque");
        yes(!new PowerRequirement(1, 256, 16384).isSatisfiedBy(new ShaftPower(64, 256)), "speed threshold");
        yes(!grindstone.isSatisfiedBy(new ShaftPower(1, 256)), "power threshold");
        invalid(() -> new ShaftPower(-1, 1));
        invalid(() -> input.gear(3, true, 1000, 1000));
        invalid(() -> input.gear(2, true, -1, 1000));
        invalid(() -> new PowerRequirement(0, 0, -1));
        ElectricalPower electrical = ElectricalPower.fromShaft(input);
        equal(8192, electrical.voltage(), "legacy voltage");
        equal(8, electrical.current(), "legacy current");
        equal(input.watts(), electrical.watts(), "ideal electrical conversion");
        equal(input, electrical.toShaft(), "electrical round trip");
        for (int torque = 0; torque < 32; torque++) {
            ShaftPower shaft = new ShaftPower(100, torque);
            yes(ElectricalPower.fromShaft(shaft).watts() <= shaft.watts(), "generator never creates power");
        }
        for (int volts = 0; volts < 32; volts++) {
            ElectricalPower signal = new ElectricalPower(volts, 10);
            yes(signal.toShaft().watts() <= signal.watts(), "motor never creates power");
        }
        invalid(() -> new ElectricalPower(-1, 0));
        overflow(() -> ElectricalPower.fromShaft(new ShaftPower(Integer.MAX_VALUE, 8)));
        overflow(() -> new ElectricalPower(8, Integer.MAX_VALUE).toShaft());
        System.out.println("Mechanical power regression checks passed");
    }
    private static void equal(Object expected, Object actual, String message) {
        if (!expected.equals(actual)) throw new AssertionError(message + ": " + expected + " != " + actual);
    }
    private static void yes(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
    private static void overflow(Runnable action) {
        try { action.run(); } catch (ArithmeticException expected) { return; }
        throw new AssertionError("Overflow was accepted");
    }
    private static void invalid(Runnable action) {
        try { action.run(); } catch (IllegalArgumentException expected) { return; }
        throw new AssertionError("Invalid input was accepted");
    }
}

package Foundations.RotaryCraft.Power;

import net.neoforged.neoforge.energy.IEnergyStorage;

public final class EnergyTransfer {
    private EnergyTransfer() {
    }

    public static int transfer(IEnergyStorage source, IEnergyStorage destination, int maximum) {
        if (maximum <= 0 || !source.canExtract() || !destination.canReceive()) {
            return 0;
        }

        int available = source.extractEnergy(maximum, true);
        int accepted = destination.receiveEnergy(available, true);
        if (accepted <= 0) {
            return 0;
        }

        int extracted = source.extractEnergy(accepted, false);
        int received = destination.receiveEnergy(extracted, false);
        if (received < extracted) {
            source.receiveEnergy(extracted - received, false);
        }
        return received;
    }
}

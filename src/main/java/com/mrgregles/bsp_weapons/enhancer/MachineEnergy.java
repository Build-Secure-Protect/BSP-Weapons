package com.mrgregles.bsp_weapons.enhancer;

import net.minecraftforge.energy.EnergyStorage;

/** A machine's Forge Energy store (D75): takes charge from any cable or charger, gives none out, and is spent only by the machine's own operations. */
public class MachineEnergy extends EnergyStorage {
    private Runnable onChange = () -> { };

    public MachineEnergy(int capacity, int maxReceive) {
        super(capacity, maxReceive, 0, 0);
    }

    public MachineEnergy(int capacity, int maxReceive, int stored) {
        super(capacity, maxReceive, 0, stored);
    }

    public void onChange(Runnable r) {
        this.onChange = r;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        int taken = super.receiveEnergy(maxReceive, simulate);
        if (taken > 0 && !simulate) onChange.run();
        return taken;
    }

    /** Burns a set charge for an operation; the caller has checked it is there. */
    public void spend(int amount) {
        energy = Math.max(0, energy - Math.max(0, amount));
        onChange.run();
    }

    public void set(int amount) {
        energy = Math.max(0, Math.min(capacity, amount));
    }
}

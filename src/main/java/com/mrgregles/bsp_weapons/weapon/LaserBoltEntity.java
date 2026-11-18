package com.mrgregles.bsp_weapons.weapon;

import com.mrgregles.bsp_weapons.compat.RegionGuard;
import com.mrgregles.bsp_weapons.registry.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

/**
 * The lance an energy weapon fires (D64, D65): flies straight at a set speed, no gravity, fades after its range, hurts
 * the first thing it meets and stops at a block. Damage, piercing and range come from the weapon's sheet at the shot;
 * the colour is synced for the renderer. Players are hurt only where Region Guard allows PvP for both ends of the shot.
 */
public class LaserBoltEntity extends Projectile {
    private static final EntityDataAccessor<Integer> COLOUR = SynchedEntityData.defineId(LaserBoltEntity.class, EntityDataSerializers.INT);
    private double damage = 4, piercing = 0, range = 24, travelled;

    public LaserBoltEntity(EntityType<? extends LaserBoltEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    public LaserBoltEntity(Level level, LivingEntity shooter, double damage, double piercing, double range, int colour) {
        this(ModEntities.LASER_BOLT.get(), level);
        setOwner(shooter);
        this.damage = damage;
        this.piercing = piercing;
        this.range = range;
        entityData.set(COLOUR, colour);
        Vec3 eye = shooter.getEyePosition();
        setPos(eye.x, eye.y - 0.1, eye.z);
    }

    /** Sets the flight and faces along it, so the first frame drawn is already right. */
    public void fly(Vec3 velocity) {
        setDeltaMovement(velocity);
        updateRotation();
        yRotO = getYRot();
        xRotO = getXRot();
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(COLOUR, 0x19D3B0);
    }

    public int colour() {
        return entityData.get(COLOUR);
    }

    public double damage() {
        return damage;
    }

    public double piercing() {
        return piercing;
    }

    public double range() {
        return range;
    }

    public double travelled() {
        return travelled;
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 move = getDeltaMovement();
        HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hit.getType() != HitResult.Type.MISS && !net.minecraftforge.event.ForgeEventFactory.onProjectileImpact(this, hit)) {
            onHit(hit);
            if (isRemoved()) return;
        }
        Vec3 next = position().add(move);
        setPos(next.x, next.y, next.z);
        travelled += move.length();
        if (level().isClientSide) ring(position(), move.normalize(), 0.25 + Math.min(1.2, travelled * 0.15), 8);
        updateRotation(); // yaw = atan2(x, z), pitch = atan2(y, h): the convention the renderer and the spawn packet share
        if (travelled >= range || tickCount > 20 * 10) discard();
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return super.canHitEntity(target) && !(target instanceof LaserBoltEntity) && target != getOwner();
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        super.onHitEntity(hit);
        if (level().isClientSide) return;
        Entity target = hit.getEntity();
        Entity owner = getOwner();
        if (target instanceof LivingEntity living) {
            if (target instanceof Player) {
                // a player is hurt only by a shooter who is still here and allowed to, where either of them stands
                if (!(owner instanceof Player shooter) || !(level() instanceof ServerLevel sl)
                        || !RegionGuard.pvpAllowed(sl, shooter.blockPosition(), target.blockPosition(), shooter)) {
                    discard();
                    return;
                }
            }
            var source = Damage.laser(level(), this, owner);
            float amount = Damage.amountFor(living, damage, piercing);
            living.hurt(source, amount); // armour takes no wear from a laser (hurtArmor is not reachable from here); noted in the audit
        }
        level().broadcastEntityEvent(this, (byte) 3);
        discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult hit) {
        super.onHitBlock(hit);
        if (!level().isClientSide) {
            level().broadcastEntityEvent(this, (byte) 3);
            discard();
        }
    }

    /** A ring of pixel motes around a point in the plane across {@code axis} (D64); the client only. */
    private void ring(Vec3 at, Vec3 axis, double radius, int count) {
        Vec3 right = Math.abs(axis.y) > 0.9 ? new Vec3(1, 0, 0) : axis.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 up = right.cross(axis).normalize();
        int c = colour();
        var dust = new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(((c >> 16) & 255) / 255f, ((c >> 8) & 255) / 255f, (c & 255) / 255f), 0.6f);
        for (int i = 0; i < count; i++) {
            double a = i * Math.PI * 2 / count;
            Vec3 r = right.scale(Math.cos(a) * radius).add(up.scale(Math.sin(a) * radius));
            level().addParticle(dust, at.x + r.x, at.y + r.y, at.z + r.z, r.x * 0.05, r.y * 0.05, r.z * 0.05);
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 3) ring(position(), getDeltaMovement().lengthSqr() < 1e-6 ? new Vec3(0, 0, 1) : getDeltaMovement().normalize(), 0.5, 12);
        else super.handleEntityEvent(id);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putDouble("Damage", damage);
        tag.putDouble("Piercing", piercing);
        tag.putDouble("Range", range);
        tag.putDouble("Travelled", travelled);
        tag.putInt("Colour", colour());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        damage = tag.getDouble("Damage");
        piercing = tag.getDouble("Piercing");
        range = tag.getDouble("Range");
        travelled = tag.getDouble("Travelled");
        entityData.set(COLOUR, tag.getInt("Colour"));
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public boolean isPickable() {
        return false;
    }
}

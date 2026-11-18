package com.mrgregles.bsp_weapons.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.mrgregles.bsp_weapons.weapon.LaserBoltEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/**
 * The lance (D64): a thick short bar of pixels along its flight, drawn full bright with the lightning render type (plain
 * colour, additive, no texture), four pixels square and sixteen long in block sixteenths. The ring behind it and the hit
 * are particles spawned by the entity's client tick. No buffer is kept across calls.
 */
public class LaserBoltRenderer extends EntityRenderer<LaserBoltEntity> {
    public LaserBoltRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(LaserBoltEntity bolt, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        // angles from the velocity itself (constant in flight), so the bar always lies along the flight whatever the entity's rotation fields say
        var dir = bolt.getDeltaMovement();
        float flightYaw = (float) Math.toDegrees(Mth.atan2(dir.x, dir.z)), flightPitch = (float) Math.toDegrees(Mth.atan2(dir.y, Math.sqrt(dir.x * dir.x + dir.z * dir.z)));
        pose.mulPose(Axis.YP.rotationDegrees(flightYaw - 90));
        pose.mulPose(Axis.ZP.rotationDegrees(flightPitch));
        int c = bolt.colour();
        float r = ((c >> 16) & 255) / 255f, g = ((c >> 8) & 255) / 255f, b = (c & 255) / 255f;
        VertexConsumer v = buffers.getBuffer(RenderType.lightning());
        Matrix4f m = pose.last().pose();
        box(v, m, -0.5f, -0.125f, -0.125f, 0.5f, 0.125f, 0.125f, r, g, b, 0.9f);           // the lance, a block long
        box(v, m, -0.3f, -0.06f, -0.06f, 0.55f, 0.06f, 0.06f, 1f, 1f, 1f, 0.9f);           // its bright core
        box(v, m, -0.6f, -0.2f, -0.2f, -0.45f, 0.2f, 0.2f, r, g, b, 0.5f);                 // the pixel at its tail
        pose.popPose();
        super.render(bolt, yaw, partial, pose, buffers, light);
    }

    private static void box(VertexConsumer v, Matrix4f m, float x0, float y0, float z0, float x1, float y1, float z1, float r, float g, float b, float a) {
        quad(v, m, x0, y0, z0, x0, y1, z0, x0, y1, z1, x0, y0, z1, r, g, b, a);
        quad(v, m, x1, y0, z1, x1, y1, z1, x1, y1, z0, x1, y0, z0, r, g, b, a);
        quad(v, m, x0, y0, z1, x1, y0, z1, x1, y0, z0, x0, y0, z0, r, g, b, a);
        quad(v, m, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1, r, g, b, a);
        quad(v, m, x0, y0, z0, x1, y0, z0, x1, y1, z0, x0, y1, z0, r, g, b, a);
        quad(v, m, x0, y1, z1, x1, y1, z1, x1, y0, z1, x0, y0, z1, r, g, b, a);
    }

    private static void quad(VertexConsumer v, Matrix4f m, float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz, float dx, float dy, float dz, float r, float g, float b, float a) {
        v.vertex(m, ax, ay, az).color(r, g, b, a).endVertex();
        v.vertex(m, bx, by, bz).color(r, g, b, a).endVertex();
        v.vertex(m, cx, cy, cz).color(r, g, b, a).endVertex();
        v.vertex(m, dx, dy, dz).color(r, g, b, a).endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(LaserBoltEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}

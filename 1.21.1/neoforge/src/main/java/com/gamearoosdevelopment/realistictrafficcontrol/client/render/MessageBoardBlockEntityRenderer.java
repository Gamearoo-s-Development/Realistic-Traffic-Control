package com.gamearoosdevelopment.realistictrafficcontrol.client.render;

import com.gamearoosdevelopment.realistictrafficcontrol.blocks.RTCProperties;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.MessageBoardBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import org.joml.Matrix4f;

/** Modern BER for the oversized portable message/arrow board. */
public final class MessageBoardBlockEntityRenderer implements BlockEntityRenderer<MessageBoardBlockEntity> {
    public MessageBoardBlockEntityRenderer(BlockEntityRendererProvider.Context context) { }
    @Override public boolean shouldRenderOffScreen(MessageBoardBlockEntity board) { return true; }
    @Override public int getViewDistance() { return 512; }

    @Override public void render(MessageBoardBlockEntity board, float partialTick, PoseStack pose,
            MultiBufferSource buffers, int light, int overlay) {
        int rotation = board.getBlockState().getValue(RTCProperties.ROTATION);
        pose.pushPose();
        pose.translate(.5, 0, .5);
        pose.mulPose(Axis.YP.rotationDegrees(rotation * -22.5F));
        pose.translate(-.5, 0, -.5);
        // debugFilledBox uses TRIANGLE_STRIP. This renderer emits independent
        // quads, so using it joined unrelated faces into the huge triangles
        // visible across the cabinet and trailer.
        VertexConsumer solid = buffers.getBuffer(RenderType.debugQuads());
        // Trailer chassis, draw bar, mast, stabilizers, wheels, and wheel hubs.
        box(pose, solid, -.6F, .13F, .34F, 1.6F, .25F, .66F, 0xD96A08);
        box(pose, solid, -1.10F, .15F, .44F, -.60F, .21F, .56F, 0xD96A08);
        box(pose, solid, .43F, .22F, .43F, .57F, 1.76F, .57F, 0xD96A08);
        box(pose, solid, -.68F, .05F, .45F, -.54F, .20F, .55F, 0xD96A08);
        box(pose, solid, 1.54F, .05F, .45F, 1.68F, .20F, .55F, 0xD96A08);
        box(pose, solid, -.72F, 1.72F, .32F, 1.72F, 2.98F, .68F, 0x151719);
        box(pose, solid, -.55F, 0, .23F, -.30F, .38F, .77F, 0x111111);
        box(pose, solid, 1.30F, 0, .23F, 1.55F, .38F, .77F, 0x111111);
        box(pose, solid, -.49F, .10F, .20F, -.36F, .28F, .23F, 0x303336);
        box(pose, solid, 1.36F, .10F, .20F, 1.49F, .28F, .23F, 0x303336);
        // Raised cabinet bezel and inset screen.
        box(pose, solid, -.76F, 1.68F, .27F, -.65F, 3.00F, .73F, 0x303336);
        box(pose, solid, 1.65F, 1.68F, .27F, 1.76F, 3.00F, .73F, 0x303336);
        box(pose, solid, -.65F, 2.87F, .27F, 1.65F, 3.00F, .73F, 0x303336);
        box(pose, solid, -.65F, 1.68F, .27F, 1.65F, 1.81F, .73F, 0x303336);
        box(pose, solid, -.65F, 1.81F, .283F, 1.65F, 2.87F, .30F, 0x050606);
        renderDisplay(board, pose, buffers);
        pose.popPose();
    }

    private void renderDisplay(MessageBoardBlockEntity board, PoseStack pose, MultiBufferSource buffers) {
        if (board.getMode() == MessageBoardBlockEntity.DisplayMode.OFF) return;
        int color = litColor(board);
        if (board.getMode() == MessageBoardBlockEntity.DisplayMode.TEXT) {
            Font font = Minecraft.getInstance().font;
            pose.pushPose();
            pose.translate(.5, 2.73, .281);
            pose.scale(-board.getTextScale() / 92F, -board.getTextScale() / 92F, 1);
            for (int i = 0; i < MessageBoardBlockEntity.MAX_LINES; i++) {
                String text = styled(board.getLine(i), board.getFontStyle());
                font.drawInBatch(text, -font.width(text) / 2F, i * 22, color, false, pose.last().pose(),
                        buffers, Font.DisplayMode.POLYGON_OFFSET, 0, LightTexture.FULL_BRIGHT);
            }
            pose.popPose();
        } else if (board.getMode() == MessageBoardBlockEntity.DisplayMode.ARROW_LEFT
                || board.getMode() == MessageBoardBlockEntity.DisplayMode.ARROW_RIGHT) {
            renderArrow(board, pose, buffers, color);
        } else if (board.getMode() == MessageBoardBlockEntity.DisplayMode.CAUTION) {
            renderCaution(pose, buffers, color);
        }
    }

    private static void renderArrow(MessageBoardBlockEntity board, PoseStack pose, MultiBufferSource buffers,
            int color) {
        VertexConsumer out = buffers.getBuffer(RenderType.debugQuads());
        Matrix4f matrix = pose.last().pose();
        float z = .271F;
        if (board.getMode() == MessageBoardBlockEntity.DisplayMode.ARROW_LEFT) {
            triangle(out, matrix, .58F, 1.94F, 1.52F, 2.34F, .58F, 2.74F, z, color);
            rectangle(out, matrix, -.48F, 2.22F, .78F, 2.46F, z, color);
        } else {
            triangle(out, matrix, .42F, 1.94F, -.52F, 2.34F, .42F, 2.74F, z, color);
            rectangle(out, matrix, .22F, 2.22F, 1.48F, 2.46F, z, color);
        }
    }

    private static void renderCaution(PoseStack pose, MultiBufferSource buffers, int color) {
        VertexConsumer out = buffers.getBuffer(RenderType.debugQuads());
        Matrix4f matrix = pose.last().pose();
        diamond(out, matrix, -.43F, 2.05F, .11F, .14F, .271F, color);
        diamond(out, matrix, 1.43F, 2.05F, .11F, .14F, .271F, color);
        diamond(out, matrix, -.43F, 2.63F, .11F, .14F, .271F, color);
        diamond(out, matrix, 1.43F, 2.63F, .11F, .14F, .271F, color);
        Font font = Minecraft.getInstance().font;
        pose.pushPose();
        pose.translate(.5, 2.46, .269);
        pose.scale(-1F / 40F, -1F / 40F, 1);
        String text = "CAUTION";
        font.drawInBatch(text, -font.width(text) / 2F, 0, color, false, pose.last().pose(),
                buffers, Font.DisplayMode.POLYGON_OFFSET, 0, LightTexture.FULL_BRIGHT);
        pose.popPose();
    }

    private static String styled(String text, MessageBoardBlockEntity.FontStyle style) {
        return switch (style) {
            case BOLD -> "\u00a7l" + text;
            case ITALIC -> "\u00a7o" + text;
            case BOLD_ITALIC -> "\u00a7l\u00a7o" + text;
            default -> text;
        };
    }
    private static int litColor(MessageBoardBlockEntity board) {
        int c = board.getColor(); float b = board.getBrightness();
        return 0xFF000000 | ((int) (((c >> 16) & 255) * b) << 16)
                | ((int) (((c >> 8) & 255) * b) << 8) | (int) ((c & 255) * b);
    }
    private static void rectangle(VertexConsumer out, Matrix4f matrix, float x1, float y1, float x2,
            float y2, float z, int color) {
        triangle(out, matrix, x1, y1, x1, y2, x2, y2, z, color);
        triangle(out, matrix, x1, y1, x2, y2, x2, y1, z, color);
    }
    private static void triangle(VertexConsumer out, Matrix4f matrix, float x1, float y1, float x2,
            float y2, float x3, float y3, float z, int color) {
        vertex(out, matrix, x1, y1, z, color);
        vertex(out, matrix, x2, y2, z, color);
        vertex(out, matrix, x3, y3, z, color);
        vertex(out, matrix, x3, y3, z, color);
    }
    private static void diamond(VertexConsumer out, Matrix4f matrix, float x, float y, float halfWidth,
            float halfHeight, float z, int color) {
        vertex(out, matrix, x, y + halfHeight, z, color);
        vertex(out, matrix, x + halfWidth, y, z, color);
        vertex(out, matrix, x, y - halfHeight, z, color);
        vertex(out, matrix, x - halfWidth, y, z, color);
    }
    private static void box(PoseStack pose, VertexConsumer out, float x1, float y1, float z1,
            float x2, float y2, float z2, int color) {
        Matrix4f m = pose.last().pose();
        quad(out,m,x1,y1,z1,x1,y2,z1,x2,y2,z1,x2,y1,z1,color);
        quad(out,m,x2,y1,z2,x2,y2,z2,x1,y2,z2,x1,y1,z2,color);
        quad(out,m,x1,y1,z2,x1,y2,z2,x1,y2,z1,x1,y1,z1,color);
        quad(out,m,x2,y1,z1,x2,y2,z1,x2,y2,z2,x2,y1,z2,color);
        quad(out,m,x1,y2,z1,x1,y2,z2,x2,y2,z2,x2,y2,z1,color);
        quad(out,m,x1,y1,z2,x1,y1,z1,x2,y1,z1,x2,y1,z2,color);
    }
    private static void quad(VertexConsumer out, Matrix4f m, float x1,float y1,float z1,float x2,float y2,float z2,
            float x3,float y3,float z3,float x4,float y4,float z4,int c) {
        vertex(out,m,x1,y1,z1,c); vertex(out,m,x2,y2,z2,c); vertex(out,m,x3,y3,z3,c); vertex(out,m,x4,y4,z4,c);
    }
    private static void vertex(VertexConsumer out, Matrix4f m, float x,float y,float z,int c) {
        out.addVertex(m,x,y,z).setColor((c>>16)&255,(c>>8)&255,c&255,255);
    }
}

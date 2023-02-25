/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.utils.render.buffered;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.wynntils.utils.render.Texture;
import java.util.OptionalDouble;
import java.util.function.Function;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

public class CustomRenderType extends RenderType {
    // Copied from RenderType.LINE_STRIP and changed the line width from the default
    // to 3
    public static final RenderType LOOTRUN_LINE = RenderType.create(
            "wynntils_lootrun_line",
            DefaultVertexFormat.POSITION_COLOR_NORMAL,
            Mode.LINE_STRIP,
            256,
            false,
            false,
            CompositeState.builder()
                    .setShaderState(RENDERTYPE_LINES_SHADER)
                    .setLineState(new LineStateShard(OptionalDouble.of(3)))
                    .setLayeringState(VIEW_OFFSET_Z_LAYERING)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setOutputState(ITEM_ENTITY_TARGET)
                    .setWriteMaskState(COLOR_DEPTH_WRITE)
                    .setCullState(NO_CULL)
                    .createCompositeState(false));

    public static final RenderType LOOTRUN_QUAD = RenderType.create(
            "wynntils_lootrun_quad",
            DefaultVertexFormat.POSITION_COLOR_TEX,
            Mode.QUADS,
            256,
            false,
            false,
            CompositeState.builder()
                    .setShaderState(POSITION_COLOR_TEX_SHADER)
                    .setCullState(NO_CULL)
                    .setTextureState(new TextureStateShard(Texture.LOOTRUN_LINE.resource(), false, false))
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setWriteMaskState(COLOR_DEPTH_WRITE)
                    .createCompositeState(false));

    public static final RenderType POSITION_COLOR_TRIANGLE_STRIP = RenderType.create(
            "wynntils_position_color_triangle_strip",
            DefaultVertexFormat.POSITION_COLOR,
            Mode.TRIANGLE_STRIP,
            256,
            false,
            false,
            CompositeState.builder()
                    .setShaderState(POSITION_COLOR_SHADER)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setWriteMaskState(RenderStateShard.COLOR_DEPTH_WRITE)
                    .createCompositeState(false));

    public static final RenderType POSITION_COLOR_QUAD = RenderType.create(
            "wynntils_position_color_quad",
            DefaultVertexFormat.POSITION_COLOR,
            Mode.QUADS,
            256,
            false,
            false,
            CompositeState.builder()
                    .setShaderState(POSITION_COLOR_SHADER)
                    .setTransparencyState(CustomRenderStateShard.SEMI_TRANSPARENT_TRANSPARENCY)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(false));

    private static final Function<ResourceLocation, RenderType> POSITION_TEXTURE_QUAD =
            Util.memoize(resource -> RenderType.create(
                    "wynntils_position_texture_quad",
                    DefaultVertexFormat.POSITION_TEX,
                    Mode.QUADS,
                    256,
                    false,
                    false,
                    CompositeState.builder()
                            .setShaderState(POSITION_TEX_SHADER)
                            .setTextureState(new TextureStateShard(resource, false, false))
                            .setTransparencyState(CustomRenderStateShard.SEMI_TRANSPARENT_TRANSPARENCY)
                            .createCompositeState(false)));

    private static final Function<ResourceLocation, RenderType> POSITION_COLOR_TEXTURE_QUAD =
            Util.memoize(resource -> RenderType.create(
                    "wynntils_position_color_texture_quad",
                    DefaultVertexFormat.POSITION_COLOR_TEX,
                    Mode.QUADS,
                    256,
                    false,
                    false,
                    CompositeState.builder()
                            .setShaderState(POSITION_COLOR_TEX_SHADER)
                            .setTextureState(new TextureStateShard(resource, false, false))
                            .setTransparencyState(CustomRenderStateShard.SEMI_TRANSPARENT_TRANSPARENCY)
                            .setWriteMaskState(COLOR_WRITE)
                            .createCompositeState(false)));

    public static RenderType getPositionColorTextureQuad(ResourceLocation resource) {
        return POSITION_COLOR_TEXTURE_QUAD.apply(resource);
    }

    public static RenderType getPositionTextureQuad(ResourceLocation resource) {
        return POSITION_TEXTURE_QUAD.apply(resource);
    }

    public CustomRenderType(
            String pName,
            VertexFormat pFormat,
            Mode pMode,
            int pBufferSize,
            boolean pAffectsCrumbling,
            boolean pSortOnUpload,
            Runnable pSetupState,
            Runnable pClearState) {
        super(pName, pFormat, pMode, pBufferSize, pAffectsCrumbling, pSortOnUpload, pSetupState, pClearState);
    }
}

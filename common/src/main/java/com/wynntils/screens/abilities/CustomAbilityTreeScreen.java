/*
 * Copyright © Wynntils 2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.screens.abilities;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Models;
import com.wynntils.core.text.StyledText;
import com.wynntils.models.abilitytree.type.AbilityTreeConnectionNode;
import com.wynntils.models.abilitytree.type.AbilityTreeConnectionType;
import com.wynntils.models.abilitytree.type.AbilityTreeInfo;
import com.wynntils.models.abilitytree.type.AbilityTreeLocation;
import com.wynntils.models.abilitytree.type.AbilityTreeSkillNode;
import com.wynntils.screens.abilities.widgets.AbilityNodeConnectionWidget;
import com.wynntils.screens.abilities.widgets.AbilityNodeWidget;
import com.wynntils.screens.abilities.widgets.AbilityTreePageSelectorButton;
import com.wynntils.screens.base.WynntilsScreen;
import com.wynntils.utils.MathUtils;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.RenderUtils;
import com.wynntils.utils.render.Texture;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import com.wynntils.utils.type.Pair;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;

public class CustomAbilityTreeScreen extends WynntilsScreen {
    // This percentage of the screen's height is used to render this screen
    private static final float SCREEN_HEIGHT_PERCENT = 0.5f;

    private static final int NODE_AREA_OFFSET_X = 85;
    private static final int NODE_AREA_OFFSET_Y = 25;

    private static final int NODE_AREA_WIDTH = 155;
    private static final int NODE_AREA_HEIGHT = 99;

    private static final int UP_ARROW_X = 250;
    private static final int UP_ARROW_Y = 117;

    private static final int DOWN_ARROW_X = 278;
    private static final int DOWN_ARROW_Y = 117;

    private final AbilityTreeInfo abilityTreeInfo;

    private final List<AbilityNodeWidget> nodeWidgets = new ArrayList<>();
    private final Map<AbilityTreeLocation, AbilityNodeConnectionWidget> connectionWidgets = new LinkedHashMap();

    // This scale is used to scale the whole screen to fit the screen size
    private float textureScale = 1f;
    private int currentPage;
    private TreeParseState treeParseState = TreeParseState.PARSING;

    public CustomAbilityTreeScreen() {
        super(Component.literal("Ability Tree"));

        abilityTreeInfo = Models.AbilityTree.getAbilityTree(Models.Character.getClassType());
        setCurrentPage(0);
    }

    // region Init

    @Override
    protected void doInit() {
        textureScale = height * SCREEN_HEIGHT_PERCENT / Texture.ABILITY_TREE_BACKGROUND.height();

        this.addRenderableWidget(new AbilityTreePageSelectorButton(
                UP_ARROW_X,
                UP_ARROW_Y,
                Texture.ABILITY_TREE_UP_ARROW.width(),
                Texture.ABILITY_TREE_UP_ARROW.height(),
                this,
                true));

        this.addRenderableWidget(new AbilityTreePageSelectorButton(
                DOWN_ARROW_X,
                DOWN_ARROW_Y,
                Texture.ABILITY_TREE_DOWN_ARROW.width(),
                Texture.ABILITY_TREE_DOWN_ARROW.height(),
                this,
                false));
    }

    // endregion

    // region Rendering

    @Override
    public void doRender(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(poseStack);

        poseStack.pushPose();
        // Make the drawing origin the start of the texture, centered on the screen
        poseStack.translate(
                (this.width - Texture.ABILITY_TREE_BACKGROUND.width() * textureScale) / 2,
                (this.height - Texture.ABILITY_TREE_BACKGROUND.height() * textureScale) / 2,
                0);

        final int backgroundWidth = Texture.ABILITY_TREE_BACKGROUND.width();
        final int backgroundHeight = Texture.ABILITY_TREE_BACKGROUND.height();
        RenderUtils.drawScalingTexturedRect(
                poseStack,
                Texture.ABILITY_TREE_BACKGROUND.resource(),
                0,
                0,
                0,
                backgroundWidth * textureScale,
                backgroundHeight * textureScale,
                backgroundWidth,
                backgroundHeight);

        if (treeParseState == TreeParseState.PARSED) {
            renderNodes(poseStack, mouseX, mouseY, partialTick);

            renderWidgets(poseStack, mouseX, mouseY, partialTick);
        } else {
            FontRenderer.getInstance()
                    .renderAlignedTextInBox(
                            poseStack,
                            StyledText.fromComponent(treeParseState.getText()),
                            0,
                            backgroundWidth * textureScale,
                            0,
                            backgroundHeight * textureScale,
                            0,
                            CommonColors.WHITE,
                            HorizontalAlignment.CENTER,
                            VerticalAlignment.MIDDLE,
                            TextShadow.OUTLINE,
                            1f);
        }

        poseStack.popPose();
    }

    private void renderNodes(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        poseStack.pushPose();

        // Set the node area offset
        poseStack.translate(NODE_AREA_OFFSET_X * textureScale, NODE_AREA_OFFSET_Y * textureScale, 0);

        // Apply texture scale
        poseStack.scale(textureScale, textureScale, 1);

        nodeWidgets.forEach(widget -> widget.render(poseStack, mouseX, mouseY, partialTick));
        connectionWidgets.values().forEach(widget -> widget.render(poseStack, mouseX, mouseY, partialTick));

        poseStack.popPose();
    }

    private void renderWidgets(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        poseStack.pushPose();

        // Apply texture scale
        poseStack.scale(textureScale, textureScale, 1);

        renderables.forEach(widget -> widget.render(poseStack, mouseX, mouseY, partialTick));

        poseStack.popPose();
    }

    // endregion

    // region Mouse Handlers

    @Override
    public boolean doMouseClicked(double mouseX, double mouseY, int button) {
        if (treeParseState != TreeParseState.PARSED) return false;

        // Translate the mouse position to match what is rendered on the screen
        mouseX -= (this.width - Texture.ABILITY_TREE_BACKGROUND.width() * textureScale) / 2;
        mouseY -= (this.height - Texture.ABILITY_TREE_BACKGROUND.height() * textureScale) / 2;

        // Actual texture positions are not scaled, only the rendering is
        mouseX /= textureScale;
        mouseY /= textureScale;

        for (GuiEventListener child : this.children()) {
            if (child.isMouseOver(mouseX, mouseY)) {
                if (child.mouseClicked(mouseX, mouseY, button)) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (treeParseState != TreeParseState.PARSED) return false;

        setCurrentPage((int) (currentPage - Math.signum(delta)));
        return true;
    }

    // endregion

    // region Page Management

    public void setCurrentPage(int page) {
        currentPage = MathUtils.clamp(page, 0, Models.AbilityTree.ABILITY_TREE_PAGES - 1);

        reconstructWidgets();
    }

    public int getCurrentPage() {
        return currentPage;
    }

    // endregion

    // region Node Widget Building

    public void updateAbilityTree() {
        setTreeParseState(TreeParseState.PARSED);

        reconstructWidgets();
    }

    private void reconstructWidgets() {
        nodeWidgets.clear();
        connectionWidgets.clear();

        abilityTreeInfo.nodes().stream()
                .filter(node -> node.location().page() == currentPage + 1)
                .forEach(node -> {
                    Pair<Integer, Integer> renderLocation = getRenderLocation(node.location());

                    nodeWidgets.add(new AbilityNodeWidget(
                            renderLocation.a() - AbilityNodeWidget.SIZE / 2,
                            renderLocation.b() - AbilityNodeWidget.SIZE / 2,
                            AbilityNodeWidget.SIZE,
                            AbilityNodeWidget.SIZE,
                            node));
                });

        List<AbilityTreeSkillNode> multiPageConnectionNodesFromLastPage = abilityTreeInfo.nodes().stream()
                .filter(node -> node.location().page() == currentPage)
                .filter(node -> nodeWidgets.stream()
                        .map(AbilityNodeWidget::getNode)
                        .map(AbilityTreeSkillNode::id)
                        .anyMatch(node.connections()::contains))
                .toList();

        // We do this "backwards":
        // First find connection nodes from last page, then find the nodes they connect to on this page
        for (AbilityTreeSkillNode connectionNode : multiPageConnectionNodesFromLastPage) {
            List<AbilityTreeSkillNode> multiPageConnections = nodeWidgets.stream()
                    .map(AbilityNodeWidget::getNode)
                    .filter(node -> connectionNode.connections().contains(node.id()))
                    .toList();

            for (AbilityTreeSkillNode currentNode : multiPageConnections) {
                final int col = currentNode.location().col();
                final int row = currentNode.location().row();

                // Multi page connections are basically the same as vertical connections,
                // when the receiving node is the one rendered. But this has to be handled first.
                addConnectionsVertically(currentNode, connectionNode, col, 0, row);
            }
        }

        for (AbilityNodeWidget nodeWidget : nodeWidgets) {
            final AbilityTreeSkillNode currentNode = nodeWidget.getNode();
            final int col = currentNode.location().col();
            final int row = currentNode.location().row();

            for (Integer connection : currentNode.connections()) {
                Optional<AbilityTreeSkillNode> connectionOptional = abilityTreeInfo.nodes().stream()
                        .filter(node -> node.id() == connection)
                        .findFirst();

                if (connectionOptional.isEmpty()) {
                    WynntilsMod.warn("Unable to find connection node for " + connection);
                    continue;
                }

                AbilityTreeSkillNode connectionNode = connectionOptional.get();

                final int connectionCol = connectionNode.location().col();
                final int connectionRow = connectionNode.location().row();

                // Only horizontal connections are needed for the same column
                if (row == connectionRow) {
                    addConnectionsHorizontally(currentNode, connectionNode, col, row, connectionCol);
                    continue;
                }

                // Only vertical connections are needed for the same row
                if (col == connectionCol) {
                    addConnectionsVertically(currentNode, connectionNode, col, row, connectionRow);
                    continue;
                }

                // Handle complex connections here

                // Firstly, we add horizontal connections, if the turn is not enough
                if (Math.abs(col - connectionCol) > 1) {
                    addConnectionsHorizontally(currentNode, connectionNode, col, row, connectionCol);
                }

                // Then we add the turn
                addTurnConnection(currentNode, connectionNode, col, row, connectionCol);

                // Finally, we add vertical connections, if the turn is not enough
                if (Math.abs(row - connectionRow) > 1) {
                    addConnectionsVertically(currentNode, connectionNode, connectionCol, row, connectionRow);
                }
            }
        }
    }

    private Pair<Integer, Integer> getRenderLocation(AbilityTreeLocation location) {
        float horizontalChunkWidth = NODE_AREA_WIDTH / AbilityTreeLocation.MAX_COLS;
        float verticalChunkHeight = NODE_AREA_HEIGHT / AbilityTreeLocation.MAX_ROWS;

        return Pair.of((int) (location.col() * horizontalChunkWidth + horizontalChunkWidth / 2f), (int)
                (location.row() * verticalChunkHeight + verticalChunkHeight / 2f));
    }

    // endregion

    // region Connection Logic

    private void addTurnConnection(
            AbilityTreeSkillNode currentNode,
            AbilityTreeSkillNode connectionNode,
            int currentCol,
            int currentRow,
            int connectionCol) {
        AbilityTreeLocation location =
                new AbilityTreeLocation(currentNode.location().page(), currentRow, connectionCol);
        Pair<Integer, Integer> renderLocation = getRenderLocation(location);

        AbilityTreeConnectionNode node;

        if (currentCol < connectionCol) {
            node = new AbilityTreeConnectionNode(
                    AbilityTreeConnectionType.DOWN_RIGHT_TURN,
                    new AbilityTreeSkillNode[] {null, null, connectionNode, currentNode});
        } else {
            node = new AbilityTreeConnectionNode(
                    AbilityTreeConnectionType.DOWN_LEFT_TURN,
                    new AbilityTreeSkillNode[] {null, currentNode, connectionNode, null});
        }

        AbilityNodeConnectionWidget oldWidget = connectionWidgets.get(location);
        AbilityTreeConnectionNode merged = node.merge(oldWidget != null ? oldWidget.getNode() : null);

        connectionWidgets.put(
                location,
                new AbilityNodeConnectionWidget(
                        renderLocation.a() - AbilityNodeConnectionWidget.SIZE / 2,
                        renderLocation.b() - AbilityNodeConnectionWidget.SIZE / 2,
                        AbilityNodeConnectionWidget.SIZE,
                        AbilityNodeConnectionWidget.SIZE,
                        merged));
    }

    private void addConnectionsHorizontally(
            AbilityTreeSkillNode currentNode,
            AbilityTreeSkillNode connectionNode,
            int currentCol,
            int currentRow,
            int targetCol) {
        AbilityTreeSkillNode startNode = currentCol < targetCol ? currentNode : connectionNode;
        AbilityTreeSkillNode endNode = currentCol < targetCol ? connectionNode : currentNode;

        int startCol = Math.min(currentCol, targetCol) + 1;
        int endCol = Math.max(currentCol, targetCol) - 1;

        for (int i = startCol; i <= endCol; i++) {
            AbilityTreeLocation location =
                    new AbilityTreeLocation(currentNode.location().page(), currentRow, i);
            Pair<Integer, Integer> renderLocation = getRenderLocation(location);

            AbilityTreeConnectionNode node = new AbilityTreeConnectionNode(
                    AbilityTreeConnectionType.HORIZONTAL, new AbilityTreeSkillNode[] {null, endNode, null, startNode});

            AbilityNodeConnectionWidget oldWidget = connectionWidgets.get(location);
            AbilityTreeConnectionNode merged = node.merge(oldWidget != null ? oldWidget.getNode() : null);

            connectionWidgets.put(
                    location,
                    new AbilityNodeConnectionWidget(
                            renderLocation.a() - AbilityNodeConnectionWidget.SIZE / 2,
                            renderLocation.b() - AbilityNodeConnectionWidget.SIZE / 2,
                            AbilityNodeConnectionWidget.SIZE,
                            AbilityNodeConnectionWidget.SIZE,
                            merged));
        }
    }

    private void addConnectionsVertically(
            AbilityTreeSkillNode currentNode,
            AbilityTreeSkillNode connectionNode,
            int currentCol,
            int currentRow,
            int targetRow) {
        AbilityTreeSkillNode startNode;
        AbilityTreeSkillNode endNode;
        int startRow;
        int endRow;

        if (currentNode.location().page() > connectionNode.location().page()) {
            startNode = connectionNode;
            endNode = currentNode;

            startRow = 0;
            endRow = targetRow - 1;
        } else if (currentNode.location().page() < connectionNode.location().page()) {
            startNode = currentNode;
            endNode = connectionNode;

            startRow = currentRow + 1;
            endRow = AbilityTreeLocation.MAX_ROWS - 1;
        } else {
            startNode = currentRow < targetRow ? currentNode : connectionNode;
            endNode = currentRow < targetRow ? connectionNode : currentNode;

            startRow = Math.min(currentRow, targetRow) + 1;
            endRow = Math.max(currentRow, targetRow) - 1;
        }

        for (int i = startRow; i <= endRow; i++) {
            AbilityTreeLocation location =
                    new AbilityTreeLocation(currentNode.location().page(), i, currentCol);
            Pair<Integer, Integer> renderLocation = getRenderLocation(location);

            AbilityTreeConnectionNode node = new AbilityTreeConnectionNode(
                    AbilityTreeConnectionType.VERTICAL, new AbilityTreeSkillNode[] {startNode, null, endNode, null});

            AbilityNodeConnectionWidget oldWidget = connectionWidgets.get(location);
            AbilityTreeConnectionNode merged = node.merge(oldWidget != null ? oldWidget.getNode() : null);

            connectionWidgets.put(
                    location,
                    new AbilityNodeConnectionWidget(
                            renderLocation.a() - AbilityNodeConnectionWidget.SIZE / 2,
                            renderLocation.b() - AbilityNodeConnectionWidget.SIZE / 2,
                            AbilityNodeConnectionWidget.SIZE,
                            AbilityNodeConnectionWidget.SIZE,
                            merged));
        }
    }

    // endregion

    // region Parse State Logic

    public void setTreeParseState(TreeParseState treeParseState) {
        this.treeParseState = treeParseState;
    }

    public enum TreeParseState {
        PARSING(Component.translatable("screens.wynntils.abilityTree.parsing")),
        PARSED(null),
        FAILED(Component.translatable("screens.wynntils.abilityTree.parseFailed"));

        private final Component text;

        TreeParseState(Component text) {
            this.text = text;
        }

        public Component getText() {
            return text;
        }
    }

    // endregion
}

/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.handlers.labels;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Handler;
import com.wynntils.core.text.StyledText;
import com.wynntils.handlers.labels.event.EntityLabelEvent;
import com.wynntils.handlers.labels.event.LabelIdentifiedEvent;
import com.wynntils.handlers.labels.event.LabelsRemovedEvent;
import com.wynntils.handlers.labels.event.TextDisplayChangedEvent;
import com.wynntils.handlers.labels.type.LabelInfo;
import com.wynntils.handlers.labels.type.LabelParser;
import com.wynntils.mc.event.RemoveEntitiesEvent;
import com.wynntils.mc.event.SetEntityDataEvent;
import com.wynntils.models.worlds.event.WorldStateEvent;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.mc.type.Location;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;

public class LabelHandler extends Handler {
    private final List<LabelParser> parsers = new ArrayList<>();

    private final Map<Integer, LabelInfo> liveLabels = new HashMap<>();

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onEntitySetData(SetEntityDataEvent event) {
        // Handle the events regarding the Wynncraft 2.1 TextDisplays
        handleTextDisplayEvents(event);

        // Handle the events regarding the entity labels, which are old (usually armor stands)
        handleEntityLabelEvents(event);
    }

    private void handleTextDisplayEvents(SetEntityDataEvent event) {
        Entity entity = McUtils.mc().level.getEntity(event.getId());
        if (!(entity instanceof Display.TextDisplay textDisplay)) return;

        for (SynchedEntityData.DataValue<?> packedItem : event.getPackedItems()) {
            if (packedItem.id() == Display.TextDisplay.DATA_TEXT_ID.id()) {
                Component oldComponent = textDisplay.getText();
                Component newComponent = (Component) packedItem.value();

                StyledText oldText = StyledText.fromComponent(oldComponent);
                StyledText newText = StyledText.fromComponent(newComponent);

                // Sometimes there is no actual change; ignore it then
                if (oldText.equals(newText)) continue;

                LabelInfo labelInfo = tryIdentifyLabel(newText, entity);
                if (labelInfo != null) {
                    liveLabels.put(entity.getId(), labelInfo);
                }

                TextDisplayChangedEvent.Text textChangedEvent =
                        new TextDisplayChangedEvent.Text(textDisplay, labelInfo, newText);
                WynntilsMod.postEvent(textChangedEvent);

                // If the event was cancelled, remove the name change data
                if (textChangedEvent.isCanceled()) {
                    event.removePackedItem(packedItem);
                    continue;
                }

                // If the event changed the name, update the data
                if (!textChangedEvent.getText().equals(newText)) {
                    SynchedEntityData.DataValue<Component> newTextData = new SynchedEntityData.DataValue<>(
                            Display.TextDisplay.DATA_TEXT_ID.id(),
                            (EntityDataSerializer<Component>) packedItem.serializer(),
                            textChangedEvent.getText().getComponent());
                    event.removePackedItem(packedItem);
                    event.addPackedItem(newTextData);
                }
            }
        }
    }

    private void handleEntityLabelEvents(SetEntityDataEvent event) {
        Entity entity = McUtils.mc().level.getEntity(event.getId());
        if (entity == null) return;

        for (SynchedEntityData.DataValue<?> packedItem : event.getPackedItems()) {
            if (packedItem.id() == Entity.DATA_CUSTOM_NAME_VISIBLE.id()) {
                WynntilsMod.postEvent(new EntityLabelEvent.Visibility(entity, (Boolean) packedItem.value()));
                continue;
            }

            if (packedItem.id() == Entity.DATA_CUSTOM_NAME.id()) {
                Optional<Component> value = (Optional<Component>) packedItem.value();
                if (value.isEmpty()) continue;

                Component oldNameComponent = entity.getCustomName();
                StyledText oldName =
                        oldNameComponent != null ? StyledText.fromComponent(oldNameComponent) : StyledText.EMPTY;
                StyledText newName = StyledText.fromComponent(value.get());

                // Sometimes there is no actual change; ignore it then
                if (newName.equals(oldName)) continue;

                EntityLabelEvent.Changed labelChangedEvent = new EntityLabelEvent.Changed(entity, newName);
                WynntilsMod.postEvent(labelChangedEvent);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onEntitiesRemoved(RemoveEntitiesEvent event) {
        List<LabelInfo> removedLabels = liveLabels.values().stream()
                .filter(label -> event.getEntityIds().contains(label.getEntity().getId()))
                .toList();

        removedLabels.forEach(label -> liveLabels.remove(label.getEntity().getId()));
        WynntilsMod.postEvent(new LabelsRemovedEvent(removedLabels));
    }

    @SubscribeEvent
    public void onWorldStateChange(WorldStateEvent event) {
        List<LabelInfo> oldLabels = new ArrayList<>(liveLabels.values());
        liveLabels.clear();
        WynntilsMod.postEvent(new LabelsRemovedEvent(oldLabels));
    }

    public void registerParser(LabelParser labelParser) {
        parsers.add(labelParser);
    }

    private LabelInfo tryIdentifyLabel(StyledText name, Entity entity) {
        for (LabelParser parser : parsers) {
            LabelInfo info = parser.getInfo(name, Location.containing(entity.position()), entity);

            if (info == null) continue;

            WynntilsMod.postEvent(new LabelIdentifiedEvent(info));
            return info;
        }

        return null;
    }
}

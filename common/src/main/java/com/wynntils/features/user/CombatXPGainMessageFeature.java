package com.wynntils.features.user;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.config.Config;
import com.wynntils.core.features.UserFeature;
import com.wynntils.core.features.properties.FeatureInfo;
import com.wynntils.mc.event.ClientTickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import com.wynntils.core.notifications.NotificationManager;
import com.wynntils.wynn.model.CharacterManager;
import com.wynntils.wynn.utils.WynnUtils;
import java.text.DecimalFormat;


@FeatureInfo
public class CombatXPGainMessageFeature extends UserFeature {

    @Config
    public boolean getCombatXPGainMessages = true;

    private static final DecimalFormat percentFormat = new DecimalFormat("##.#");
    private static float newTickXP = 0;
    private static float lastTickXP = 0;
    private static int trackedPercentage = 0;

    @SubscribeEvent
    public void onTick (ClientTickEvent.End event) {
        if(!WynnUtils.onWorld() || !getCombatXPGainMessages) { return; }
        CharacterManager.CharacterInfo data = WynnUtils.getCharacterInfo();
    
        newTickXP = data.getCurrentXp();
        WynntilsMod.info("NEW TICK XP IS " + newTickXP); //FIXME DEBUG

        if (newTickXP == lastTickXP) { return; }

        int neededXP = data.getXpPointsNeededToLevelUp();
        if (lastTickXP != 0) {
            trackedPercentage = (int) lastTickXP / neededXP;
        }
        else {
            trackedPercentage = (int) newTickXP / neededXP;
        }

        int gainedXP = Math.round(newTickXP) - Math.round(lastTickXP);

        float percentGained = (float) gainedXP / neededXP;
        float percentChange = trackedPercentage - percentGained;

        percentFormat.format(percentChange);

        String message = String.format("§a+%d XP (§b%s%%§a)", gainedXP, percentChange);

        NotificationManager.queueMessage(message);

        lastTickXP = newTickXP;
        WynntilsMod.info("LAST TICK XP IS " + lastTickXP); //FIXME DEBUG
    }
}

package com.thanpisitkub.mod.item;

import com.thanpisitkub.mod.power.PowerData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/** Right-click to gain / lose the thanpisitkub power. */
public class PowerCoreItem extends Item {

    public PowerCoreItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            boolean now = !PowerData.has(player);
            PowerData.set(player, now);
            player.displayClientMessage(Component.translatable(
                    now ? "message.thanpisitkub.power_on" : "message.thanpisitkub.power_off")
                    .withStyle(now ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.GRAY), true);
            level.playSound(null, player.blockPosition(),
                    now ? SoundEvents.BEACON_ACTIVATE : SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.0f, 1.2f);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.thanpisitkub.core.use").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.thanpisitkub.core.impact").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.thanpisitkub.core.divide").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.translatable("tooltip.thanpisitkub.core.cancel").withStyle(ChatFormatting.DARK_GRAY));
    }
}

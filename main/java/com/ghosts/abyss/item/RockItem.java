package com.ghosts.abyss.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class RockItem extends Item {

    public RockItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide()) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));

            ItemEntity rockEntity = new ItemEntity(level, player.getX(), player.getEyeY() - 0.2, player.getZ(), new ItemStack(this));

            Vec3 look = player.getLookAngle();
            rockEntity.setDeltaMovement(look.x * 1.5, look.y * 1.5 + 0.2, look.z * 1.5);
            rockEntity.setPickUpDelay(100);

            rockEntity.getPersistentData().putUUID("jreborn_rock_owner", player.getUUID());
            rockEntity.getPersistentData().putInt("jreborn_rock_timer", 20);

            level.addFreshEntity(rockEntity);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
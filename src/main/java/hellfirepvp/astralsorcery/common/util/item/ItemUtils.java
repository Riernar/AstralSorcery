/*******************************************************************************
 * HellFirePvP / Astral Sorcery 2019
 *
 * All rights reserved.
 * The source code is available on github: https://github.com/HellFirePvP/AstralSorcery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.astralsorcery.common.util.item;

import hellfirepvp.astralsorcery.common.util.fluid.CompatFluidActionResult;
import hellfirepvp.astralsorcery.common.util.fluid.CompatFluidStack;
import hellfirepvp.astralsorcery.common.util.fluid.CompatFluidUtil;
import hellfirepvp.astralsorcery.common.util.fluid.handler.CompatVoidFluidHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.Tag;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static hellfirepvp.astralsorcery.common.util.item.ItemComparator.Clause.*;

/**
 * This class is part of the Astral Sorcery Mod
 * The complete source code for this mod can be found on github.
 * Class: ItemUtils
 * Created by HellFirePvP
 * Date: 31.07.2016 / 17:51
 */
public class ItemUtils {

    public static final IItemHandler EMPTY_INVENTORY = new ItemHandlerEmpty();
    private static final Random rand = new Random();

    public static ItemEntity dropItem(World world, double x, double y, double z, ItemStack stack) {
        if (world.isRemote) return null;
        ItemEntity ei = new ItemEntity(world, x, y, z, stack);
        ei.setMotion(new Vec3d(0, 0, 0));
        world.addEntity(ei);
        ei.setDefaultPickupDelay();
        return ei;
    }

    public static ItemEntity dropItemNaturally(World world, double x, double y, double z, ItemStack stack) {
        if (world.isRemote) return null;
        ItemEntity ei = new ItemEntity(world, x, y, z, stack);
        applyRandomDropOffset(ei);
        world.addEntity(ei);
        ei.setDefaultPickupDelay();
        return ei;
    }

    private static void applyRandomDropOffset(ItemEntity item) {
        item.setMotion(rand.nextFloat() * 0.3F - 0.15D,
                rand.nextFloat() * 0.3F - 0.15D,
                rand.nextFloat() * 0.3F - 0.15D);
    }

    @Nonnull
    public static ItemStack createBlockStack(BlockState state) {
        return new ItemStack(state.getBlock());
    }

    @Nullable
    public static BlockState createBlockState(ItemStack stack) {
        Block b = Block.getBlockFromItem(stack.getItem());
        if (b == Blocks.AIR) return null;
        return b.getDefaultState();
    }

    @Nonnull
    public static List<ItemStack> getItemsOfTag(ResourceLocation key) {
        Tag<Item> tag = ItemTags.getCollection().get(key);
        return tag == null ? Collections.emptyList() : getItemsOfTag(tag);
    }

    @Nonnull
    public static List<ItemStack> getItemsOfTag(Tag<Item> itemTag) {
        return itemTag.getAllElements().stream().map(ItemStack::new).collect(Collectors.toList());
    }

    public static Collection<ItemStack> scanInventoryFor(IItemHandler handler, Item i) {
        List<ItemStack> out = new LinkedList<>();
        for (int j = 0; j < handler.getSlots(); j++) {
            ItemStack s = handler.getStackInSlot(j);
            if (!s.isEmpty() && s.getItem() == i)
                out.add(copyStackWithSize(s, s.getCount()));
        }
        return out;
    }

    public static Collection<ItemStack> scanInventoryForMatching(IItemHandler handler, ItemStack match, boolean strict) {
        return findItemsInInventory(handler, match, strict);
    }

    public static Collection<ItemStack> findItemsInPlayerInventory(PlayerEntity player, ItemStack match, boolean strict) {
        return findItemsInInventory(player.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY).orElse(EMPTY_INVENTORY), match, strict);
    }

    public static Collection<ItemStack> findItemsInInventory(IItemHandler handler, ItemStack match, boolean strict) {
        List<ItemStack> stacksOut = new LinkedList<>();
        for (int j = 0; j < handler.getSlots(); j++) {
            ItemStack s = handler.getStackInSlot(j);
            if (strict ?
                    ItemComparator.compare(s, match, ITEM, NBT_STRICT, CAPABILITIES_COMPATIBLE) :
                    ItemComparator.compare(s, match, ITEM)) {
                stacksOut.add(copyStackWithSize(s, s.getCount()));
            }
        }
        return stacksOut;
    }

    public static Map<Integer, ItemStack> findItemsIndexedInPlayerInventory(PlayerEntity player, Predicate<ItemStack> match) {
        return findItemsIndexedInInventory(player.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY).orElse(EMPTY_INVENTORY), match);
    }

    public static Map<Integer, ItemStack> findItemsIndexedInInventory(IItemHandler handler, ItemStack match, boolean strict) {
        return findItemsIndexedInInventory(handler,
                (s) -> strict ?
                        ItemComparator.compare(s, match, ITEM, NBT_STRICT, CAPABILITIES_COMPATIBLE) :
                        ItemComparator.compare(s, match, ITEM));
    }

    public static Map<Integer, ItemStack> findItemsIndexedInInventory(IItemHandler handler, Predicate<ItemStack> match) {
        Map<Integer, ItemStack> stacksOut = new HashMap<>();
        for (int j = 0; j < handler.getSlots(); j++) {
            ItemStack s = handler.getStackInSlot(j);
            if (match.test(s)) {
                stacksOut.put(j, copyStackWithSize(s, s.getCount()));
            }
        }
        return stacksOut;
    }

    public static boolean consumeFromPlayerInventory(PlayerEntity player, ItemStack requestingItemStack, ItemStack toConsume, boolean simulate) {
        int consumed = 0;
        ItemStack tryConsume = copyStackWithSize(toConsume, toConsume.getCount() - consumed);
        return tryConsume.isEmpty() || consumeFromInventory((IItemHandlerModifiable) player.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null), tryConsume, simulate);
    }

    public static boolean tryConsumeFromInventory(IItemHandler handler, ItemStack toConsume, boolean simulate) {
        return handler instanceof IItemHandlerModifiable && consumeFromInventory((IItemHandlerModifiable) handler, toConsume, simulate);
    }

    public static boolean consumeFromInventory(IItemHandlerModifiable handler, ItemStack toConsume, boolean simulate) {
        Map<Integer, ItemStack> contents = findItemsIndexedInInventory(handler, toConsume, false);
        if (contents.isEmpty()) return false;

        int cAmt = toConsume.getCount();
        for (int slot : contents.keySet()) {
            ItemStack inSlot = contents.get(slot);
            int toRemove = cAmt > inSlot.getCount() ? inSlot.getCount() : cAmt;
            cAmt -= toRemove;
            if (!simulate) {
                handler.setStackInSlot(slot, copyStackWithSize(inSlot, inSlot.getCount() - toRemove));
            }
            if (cAmt <= 0) {
                break;
            }
        }
        return cAmt <= 0;
    }

    public static void dropInventory(IItemHandler handle, World worldIn, BlockPos pos) {
        if (worldIn.isRemote) return;
        for (int i = 0; i < handle.getSlots(); i++) {
            ItemStack stack = handle.getStackInSlot(i);
            if (stack.isEmpty()) continue;
            dropItemNaturally(worldIn, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
        }
    }

    public static CompatFluidActionResult drainFluidFromItem(ItemStack stack, Fluid fluid, int mbAmount, boolean doDrain) {
        return drainFluidFromItem(stack, new CompatFluidStack(fluid, mbAmount), doDrain);
    }

    public static CompatFluidActionResult drainFluidFromItem(ItemStack stack, CompatFluidStack fluidStack, boolean doDrain) {
        return CompatFluidUtil.tryEmptyContainer(stack, CompatVoidFluidHandler.INSTANCE, fluidStack.getAmount(), null, doDrain);
    }

    /*public static void decrStackInInventory(ItemStack[] stacks, int slot) {
        if(slot < 0 || slot >= stacks.length) return;
        ItemStack st = stacks[slot];
        if(st == null) return;
        st.getCount()--;
        if(st.getCount() <= 0) {
            stacks[slot] = null;
        }
    }*/

    public static void decrStackInInventory(ItemStackHandler handler, int slot) {
        if (slot < 0 || slot >= handler.getSlots()) return;
        ItemStack st = handler.getStackInSlot(slot);
        if (st.isEmpty()) return;
        st.setCount(st.getCount() - 1);
        if (st.getCount() <= 0) {
            handler.setStackInSlot(slot, ItemStack.EMPTY);
        }
    }

    public static boolean tryPlaceItemInInventory(@Nonnull ItemStack stack, IItemHandler handler) {
        return tryPlaceItemInInventory(stack, handler, 0, handler.getSlots());
    }

    public static boolean tryPlaceItemInInventory(@Nonnull ItemStack stack, IItemHandler handler, int start, int end) {
        ItemStack toAdd = stack.copy();
        if (!hasInventorySpace(toAdd, handler, start, end)) return false;
        int max = stack.getMaxStackSize();

        for (int i = start; i < end; i++) {
            ItemStack in = handler.getStackInSlot(i);
            if (in.isEmpty()) {
                int added = Math.min(stack.getCount(), max);
                stack.setCount(stack.getCount() - added);
                handler.insertItem(i, copyStackWithSize(stack, added), false);
                return true;
            } else {
                if (ItemComparator.compare(stack, in, ITEM, NBT_STRICT, CAPABILITIES_COMPATIBLE)) {

                    int space = max - in.getCount();
                    int added = Math.min(stack.getCount(), space);
                    stack.setCount(stack.getCount() - added);
                    handler.getStackInSlot(i).setCount(handler.getStackInSlot(i).getCount() + added);
                    if (stack.getCount() <= 0)
                        return true;
                }
            }
        }
        return stack.getCount() == 0;
    }

    public static boolean hasInventorySpace(@Nonnull ItemStack stack, IItemHandler handler, int rangeMin, int rangeMax) {
        int size = stack.getCount();
        int max = stack.getMaxStackSize();
        for (int i = rangeMin; i < rangeMax && size > 0; i++) {
            ItemStack in = handler.getStackInSlot(i);
            if (in.isEmpty()) {
                size -= max;
            } else {
                if (ItemComparator.compare(stack, in, ITEM, NBT_STRICT, CAPABILITIES_COMPATIBLE)) {
                    int space = max - in.getCount();
                    size -= space;
                }
            }
        }
        return size <= 0;
    }

    public static ItemStack copyStackWithSize(@Nonnull ItemStack stack, int amount) {
        if (stack.isEmpty() || amount <= 0) return ItemStack.EMPTY;
        ItemStack s = stack.copy();
        s.setCount(amount);
        return s;
    }

    private static class ItemHandlerEmpty implements IItemHandler {

        @Override
        public int getSlots() {
            return 0;
        }

        @Nonnull
        @Override
        public ItemStack getStackInSlot(int slot) {
            return ItemStack.EMPTY;
        }

        @Nonnull
        @Override
        public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
            return stack;
        }

        @Nonnull
        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            return false;
        }
    }

}

/*******************************************************************************
 * HellFirePvP / Astral Sorcery 2019
 *
 * All rights reserved.
 * The source code is available on github: https://github.com/HellFirePvP/AstralSorcery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.astralsorcery.common.network.packet.server;

import hellfirepvp.astralsorcery.common.network.base.ASPacket;
import hellfirepvp.astralsorcery.common.util.data.ByteBufUtils;
import hellfirepvp.astralsorcery.common.util.data.Vector3;
import hellfirepvp.astralsorcery.common.util.fluid.CompatFluidStack;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.LogicalSidedProvider;
import net.minecraftforge.fml.network.NetworkEvent;

import javax.annotation.Nonnull;

/**
 * This class is part of the Astral Sorcery Mod
 * The complete source code for this mod can be found on github.
 * Class: PktPlayLiquidInteraction
 * Created by HellFirePvP
 * Date: 01.06.2019 / 23:46
 */
public class PktPlayLiquidInteraction extends ASPacket<PktPlayLiquidInteraction> {

    private CompatFluidStack comp1, comp2;
    private Vector3 pos;

    public PktPlayLiquidInteraction() {}

    public PktPlayLiquidInteraction(CompatFluidStack comp1, CompatFluidStack comp2, Vector3 pos) {
        this.comp1 = comp1;
        this.comp2 = comp2;
        this.pos = pos;
    }

    @Nonnull
    @Override
    public Encoder<PktPlayLiquidInteraction> encoder() {
        return (packet, buffer) -> {
            ByteBufUtils.writeFluidStack(buffer, packet.comp1);
            ByteBufUtils.writeFluidStack(buffer, packet.comp2);
            ByteBufUtils.writeVector(buffer, packet.pos);
        };
    }

    @Nonnull
    @Override
    public Decoder<PktPlayLiquidInteraction> decoder() {
        return buffer ->
                new PktPlayLiquidInteraction(
                        ByteBufUtils.readFluidStack(buffer),
                        ByteBufUtils.readFluidStack(buffer),
                        ByteBufUtils.readVector(buffer));
    }

    @Nonnull
    @Override
    public Handler<PktPlayLiquidInteraction> handler() {
        return new Handler<PktPlayLiquidInteraction>() {
            @Override
            @OnlyIn(Dist.CLIENT)
            public void handleClient(PktPlayLiquidInteraction packet, NetworkEvent.Context context) {
                World mcWorld = LogicalSidedProvider.CLIENTWORLD.get(LogicalSide.CLIENT);

                context.enqueueWork(() -> {

                });
            }

            @Override
            public void handle(PktPlayLiquidInteraction packet, NetworkEvent.Context context, LogicalSide side) {}
        };
    }
}

/*
 * This file is part of the L2J Mobius project.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package ai.sevensigns;

import org.l2jmobius.gameserver.instancemanager.MapRegionManager;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.player.TeleportWhereType;
import org.l2jmobius.gameserver.model.sevensigns.SevenSigns;

import ai.AbstractNpcAI;

/**
 * @author Mobius
 */
public class SevenSignsDungeonMob extends AbstractNpcAI
{
	private SevenSignsDungeonMob()
	{
	}

	@Override
	public String onAggro(Npc npc, Player player, boolean isPet)
	{
		final int playerCabal = SevenSigns.getInstance().getPlayerCabal(player.getObjectId());
		final int winningCabal = SevenSigns.getInstance().getCabalHighestScore();
		if (SevenSigns.getInstance().isSealValidationPeriod() && (playerCabal != winningCabal))
		{
			player.sendMessage("You have been teleported to the nearest town because you do not belong to the winning cabal.");
			player.teleToLocation(MapRegionManager.getInstance().getTeleportToLocation(player, TeleportWhereType.TOWN));
		}
		return super.onAggro(npc, player, isPet);
	}

	public static void main(String[] args)
	{
		new SevenSignsDungeonMob();
	}
}

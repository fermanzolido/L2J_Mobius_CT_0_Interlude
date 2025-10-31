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
package handlers.effecthandlers;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.model.Seed;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.instance.Monster;
import org.l2jmobius.gameserver.model.conditions.Condition;
import org.l2jmobius.gameserver.model.effects.AbstractEffect;
import org.l2jmobius.gameserver.model.quest.QuestSound;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * Sow effect implementation.
 * @author Adry_85, l3x
 */
public class Sow extends AbstractEffect
{
	public Sow(Condition attachCond, Condition applyCond, StatSet set, StatSet params)
	{
		super(attachCond, applyCond, set, params);
	}
	
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill)
	{
		if (!effector.isPlayer() || !effected.isMonster())
		{
			return;
		}
		
		final Player player = effector.asPlayer();
		final Monster target = effected.asMonster();
		if (target.isDead() || (!target.getTemplate().canBeSown()) || target.isSeeded() || (target.getSeederId() != player.getObjectId()))
		{
			return;
		}
		
		// Consuming used seed
		final Seed seed = target.getSeed();
		if (!player.destroyItemByItemId(null, seed.getSeedId(), 1, target, false))
		{
			return;
		}
		
		final SystemMessage sm;
		if (calcSuccess(player, target, seed))
		{
			player.sendPacket(QuestSound.ITEMSOUND_QUEST_ITEMGET.getPacket());
			target.setSeeded(player);
			sm = new SystemMessage(SystemMessageId.THE_SEED_WAS_SUCCESSFULLY_SOWN);
		}
		else
		{
			sm = new SystemMessage(SystemMessageId.THE_SEED_WAS_NOT_SOWN);
		}
		
		if (player.isInParty())
		{
			player.getParty().broadcastPacket(sm);
		}
		else
		{
			player.sendPacket(sm);
		}
		
		if (target.getMostHated() == player)
		{
			target.stopHating(player);
			target.setTarget(null);
		}
		target.getAI().setIntention(Intention.IDLE);
	}
	
	private static boolean calcSuccess(Creature creature, Creature target, Seed seed)
	{
		final int levelTarget = target.getLevel();
		int basicSuccess = seed.isAlternative() ? 20 : 90;
		
		// Penalty for level difference between target and seed.
		final int seedLevelDiff = Math.abs(levelTarget - seed.getLevel());
		if (seedLevelDiff > 5)
		{
			basicSuccess -= 5 * (seedLevelDiff - 5);
		}
		
		// Penalty for level difference between player and target.
		final int playerLevelDiff = Math.abs(creature.getLevel() - levelTarget);
		if (playerLevelDiff > 5)
		{
			basicSuccess -= 5 * (playerLevelDiff - 5);
		}
		
		// Final chance is capped between 1% and 100%.
		basicSuccess = Math.max(basicSuccess, 1);
		return Rnd.get(100) < basicSuccess;
	}
}

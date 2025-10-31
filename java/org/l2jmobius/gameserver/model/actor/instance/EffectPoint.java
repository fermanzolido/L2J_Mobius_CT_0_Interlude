/*
 * Copyright (c) 2013 L2jMobius
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.l2jmobius.gameserver.model.actor.instance;

import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.ai.CreatureAI;
import org.l2jmobius.gameserver.ai.AbstractAI.CtrlIntention;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;

public class EffectPoint extends Npc
{
	private final Player _owner;
	private final Skill _skill;
	
	/**
	 * Creates an effect point.
	 * @param template the effect point NPC template
	 * @param owner the owner
	 */
	public EffectPoint(NpcTemplate template, Creature owner)
	{
		this(template, owner, null);
	}

	public EffectPoint(NpcTemplate template, Creature owner, Skill skill)
	{
		super(template);
		setInstanceType(InstanceType.EffectPoint);
		setInvul(false);
		_owner = owner == null ? null : owner.asPlayer();
		if (owner != null)
		{
			setInstanceId(owner.getInstanceId());
		}
		_skill = skill;
		if (_skill != null)
		{
			getAI().setIntention(CtrlIntention.AI_INTENTION_CAST, _skill, this);
		}
	}

	@Override
	public CreatureAI getAI()
	{
		CreatureAI ai = _ai;
		if (ai == null)
		{
			synchronized (this)
			{
				ai = _ai;
				if (ai == null)
				{
					_ai = ai = new CreatureAI(this);
				}
			}
		}
		return ai;
	}
	
	@Override
	public Player asPlayer()
	{
		return _owner;
	}
	
	/**
	 * this is called when a player interacts with this NPC
	 * @param player
	 */
	@Override
	public void onAction(Player player, boolean interact)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	@Override
	public void onActionShift(Player player)
	{
		if (player == null)
		{
			return;
		}
		
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
}
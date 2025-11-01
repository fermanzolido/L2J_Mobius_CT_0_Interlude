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
package handlers.admincommandhandlers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.managers.QuestManager;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.quest.State;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExShowQuestMark;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.QuestList;

/**
 * @author Korvin, Zoey76
 */
public class AdminShowQuests implements IAdminCommandHandler
{
	private static final Logger LOGGER = Logger.getLogger(AdminShowQuests.class.getName());
	
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_charquestmenu",
		"admin_setcharquest"
	};
	
	private static final String[] _states =
	{
		"CREATED",
		"STARTED",
		"COMPLETED"
	};
	
	@Override
	public boolean onCommand(String command, Player activeChar)
	{
		Player target = null;
		try
		{
			final String[] cmdParams = command.split(" ");
			final WorldObject targetObject = activeChar.getTarget();
			if (cmdParams.length > 1)
			{
				target = World.getInstance().getPlayer(cmdParams[1]);
			}
			else if ((targetObject != null) && targetObject.isPlayer())
			{
				target = (Player) targetObject;
			}

			if (target == null)
			{
				activeChar.sendPacket(SystemMessageId.INVALID_TARGET);
				return false;
			}

			final String[] params = new String[4];
			if (command.startsWith("admin_charquestmenu"))
			{
				if (cmdParams.length > 2)
				{
					if (cmdParams[2].equals("0"))
					{
						params[0] = "var";
						params[1] = "Start";
					}
					else if (cmdParams[2].equals("1"))
					{
						params[0] = "var";
						params[1] = "Started";
					}
					else if (cmdParams[2].equals("2"))
					{
						params[0] = "var";
						params[1] = "Completed";
					}
					else if (cmdParams[2].equals("3"))
					{
						params[0] = "full";
					}
					else if (cmdParams[2].contains("_"))
					{
						params[0] = "name";
						params[1] = cmdParams[2];
					}

					if ((cmdParams.length > 3) && cmdParams[3].equals("custom"))
					{
						params[0] = "custom";
						params[1] = cmdParams[2];
					}
				}
				
				if (params[0] != null)
				{
					showQuestMenu(target, activeChar, params);
				}
				else
				{
					showFirstQuestMenu(target, activeChar);
				}
			}
			else if (command.startsWith("admin_setcharquest"))
			{
				if (cmdParams.length > 4)
				{
					params[0] = cmdParams[2];
					params[1] = cmdParams[3];
					params[2] = cmdParams[4];
					if (cmdParams.length == 6)
					{
						params[3] = cmdParams[5];
					}
					setQuestVar(target, activeChar, params);
				}
				else
				{
					activeChar.sendMessage("Usage: //setcharquest <player> <questname> <varname> <value> [<is repeatable>]");
					return false;
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Wrong usage. //charquestmenu <player_name> [quest_id|quest_name]");
		}
		return true;
	}
	
	private void showFirstQuestMenu(Player target, Player actor)
	{
		final StringBuilder replyMSG = new StringBuilder("<html><body><table width=270><tr><td width=45><button value=\"Main\" action=\"bypass admin_admin\" width=45 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td><td width=180><center>Player: " + target.getName() + "</center></td><td width=45><button value=\"Back\" action=\"bypass admin_admin6\" width=45 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr></table>");
		final NpcHtmlMessage adminReply = new NpcHtmlMessage();
		final int ID = target.getObjectId();
		replyMSG.append("Quest Menu for <font color=\"LEVEL\">" + target.getName() + "</font> (ID:" + ID + ")<br><center>");
		replyMSG.append("<table width=250><tr><td><button value=\"CREATED\" action=\"bypass -h admin_charquestmenu " + target.getName() + " 0\" width=95 height=21 back=\"bigbutton_over\" fore=\"bigbutton\"></td></tr>");
		replyMSG.append("<tr><td><button value=\"STARTED\" action=\"bypass -h admin_charquestmenu " + target.getName() + " 1\" width=95 height=21 back=\"bigbutton_over\" fore=\"bigbutton\"></td></tr>");
		replyMSG.append("<tr><td><button value=\"COMPLETED\" action=\"bypass -h admin_charquestmenu " + target.getName() + " 2\" width=95 height=21 back=\"bigbutton_over\" fore=\"bigbutton\"></td></tr>");
		replyMSG.append("<tr><td><br><button value=\"All\" action=\"bypass -h admin_charquestmenu " + target.getName() + " 3\" width=95 height=21 back=\"bigbutton_over\" fore=\"bigbutton\"></td></tr>");
		replyMSG.append("<tr><td><br><br>Manual Edit by Quest number:<br></td></tr>");
		replyMSG.append("<tr><td><edit var=\"qn\" width=65 height=12><br><button value=\"Edit\" action=\"bypass -h admin_charquestmenu " + target.getName() + " $qn custom\" width=65 height=21 back=\"L2UI_ch3.smallbutton2_over\" fore=\"L2UI_ch3.smallbutton2\"></td></tr>");
		replyMSG.append("</table></center></body></html>");
		adminReply.setHtml(replyMSG.toString());
		actor.sendPacket(adminReply);
	}
	
	private void showQuestMenu(Player target, Player actor, String[] params)
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			ResultSet rs;
			PreparedStatement req;
			final int ID = target.getObjectId();
			final StringBuilder replyMSG = new StringBuilder("<html><body>");
			final NpcHtmlMessage adminReply = new NpcHtmlMessage();
			
			switch (params[0])
			{
				case "full":
				{
					showFullQuestList(target, replyMSG);
					break;
				}
				case "name":
				{
					showQuestDetails(target, replyMSG, params[1]);
					break;
				}
				case "var":
				{
					showQuestsByState(target, replyMSG, params[1]);
					break;
				}
				case "custom":
				{
					showCustomQuestMenu(target, replyMSG, Integer.parseInt(params[1]));
					break;
				}
			}
			
			adminReply.setHtml(replyMSG.toString());
			actor.sendPacket(adminReply);
		}
		catch (Exception e)
		{
			actor.sendMessage("There was an error.");
			LOGGER.warning(AdminShowQuests.class.getSimpleName() + ": " + e.getMessage());
		}
	}
	
	private void showFullQuestList(Player target, StringBuilder replyMSG) throws SQLException
	{
		replyMSG.append("<table width=250><tr><td>Full Quest List for <font color=\"LEVEL\">" + target.getName() + "</font> (ID:" + target.getObjectId() + ")</td></tr>");
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT DISTINCT name FROM character_quests WHERE charId=? AND var='<state>' ORDER by name"))
		{
			ps.setInt(1, target.getObjectId());
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					replyMSG.append("<tr><td><a action=\"bypass -h admin_charquestmenu " + target.getName() + " " + rs.getString(1) + "\">" + rs.getString(1) + "</a></td></tr>");
				}
			}
		}
		replyMSG.append("</table></body></html>");
	}

	private void showQuestDetails(Player target, StringBuilder replyMSG, String questName) throws SQLException
	{
		final QuestState qs = target.getQuestState(questName);
		final String state = (qs != null) ? _states[qs.getState()] : "CREATED";
		replyMSG.append("Character: <font color=\"LEVEL\">" + target.getName() + "</font><br>Quest: <font color=\"LEVEL\">" + questName + "</font><br>State: <font color=\"LEVEL\">" + state + "</font><br><br>");
		replyMSG.append("<center><table width=250><tr><td width=70>Var</td><td width=40>Value</td><td>New Value</td><td>&nbsp;</td></tr>");
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT var,value FROM character_quests WHERE charId=? and name=?"))
		{
			ps.setInt(1, target.getObjectId());
			ps.setString(2, questName);
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					final String varName = rs.getString(1);
					if (varName.equals("<state>"))
					{
						continue;
					}
					replyMSG.append("<tr><td>" + varName + "</td><td>" + rs.getString(2) + "</td><td><edit var=\"var" + varName + "\" width=80 height=15></td><td><button value=\"Set\" action=\"bypass -h admin_setcharquest " + target.getName() + " " + questName + " " + varName + " $var" + varName + "\" width=30 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td><td><button value=\"Del\" action=\"bypass -h admin_setcharquest " + target.getName() + " " + questName + " " + varName + " delete\" width=30 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr>");
				}
			}
		}
		replyMSG.append("</table><br><br><table width=250><tr><td>Repeatable quest:</td><td>Unrepeatable quest:</td></tr>");
		replyMSG.append("<tr><td><button value=\"Quest Complete\" action=\"bypass -h admin_setcharquest " + target.getName() + " " + questName + " state COMPLETED 1\" width=95 height=21 back=\"bigbutton_over\" fore=\"bigbutton\"></td>");
		replyMSG.append("<td><button value=\"Quest Complete\" action=\"bypass -h admin_setcharquest " + target.getName() + " " + questName + " state COMPLETED 0\" width=95 height=21 back=\"bigbutton_over\" fore=\"bigbutton\"></td></tr>");
		replyMSG.append("</table><br><br><font color=\"ff0000\">Delete Quest from DB:</font><br><button value=\"Quest Delete\" action=\"bypass -h admin_setcharquest " + target.getName() + " " + questName + " state DELETE\" width=95 height=21 back=\"bigbutton_over\" fore=\"bigbutton\">");
		replyMSG.append("</center></body></html>");
	}

	private void showQuestsByState(Player target, StringBuilder replyMSG, String state) throws SQLException
	{
		replyMSG.append("Character: <font color=\"LEVEL\">" + target.getName() + "</font><br>Quests with state: <font color=\"LEVEL\">" + state + "</font><br>");
		replyMSG.append("<table width=250>");
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT DISTINCT name FROM character_quests WHERE charId=? and var='<state>' and value=?"))
		{
			ps.setInt(1, target.getObjectId());
			ps.setString(2, state);
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					replyMSG.append("<tr><td><a action=\"bypass -h admin_charquestmenu " + target.getName() + " " + rs.getString(1) + "\">" + rs.getString(1) + "</a></td></tr>");
				}
			}
		}
		replyMSG.append("</table></body></html>");
	}

	private void showCustomQuestMenu(Player target, StringBuilder replyMSG, int questId) throws SQLException
	{
		final Quest quest = QuestManager.getInstance().getQuest(questId);
		if (quest == null)
		{
			replyMSG.append("<center><font color=\"ee0000\">Quest with number </font><font color=\"LEVEL\">" + questId + "</font><font color=\"ee0000\"> doesn't exist!</font></center></body></html>");
			return;
		}

		final String questName = quest.getName();
		final QuestState qs = target.getQuestState(questName);
		if (qs != null)
		{
			showQuestDetails(target, replyMSG, questName);
		}
		else
		{
			final String state = "N/A";
			replyMSG.append("Character: <font color=\"LEVEL\">" + target.getName() + "</font><br>Quest: <font color=\"LEVEL\">" + questName + "</font><br>State: <font color=\"LEVEL\">" + state + "</font><br><br>");
			replyMSG.append("<center>Start this Quest for player:<br>");
			replyMSG.append("<button value=\"Create Quest\" action=\"bypass -h admin_setcharquest " + target.getName() + " " + questId + " state CREATE\" width=95 height=21 back=\"bigbutton_over\" fore=\"bigbutton\"><br><br>");
			replyMSG.append("<font color=\"ee0000\">Only for Unrepeateble quests:</font><br>");
			replyMSG.append("<button value=\"Create & Complete\" action=\"bypass -h admin_setcharquest " + target.getName() + " " + questId + " state CC\" width=95 height=21 back=\"bigbutton_over\" fore=\"bigbutton\"><br><br>");
			replyMSG.append("</center></body></html>");
		}
	}

	private void setQuestVar(Player target, Player actor, String[] params)
	{
		QuestState qs = target.getQuestState(params[0]);
		final String[] outval = new String[3];
		if (params[1].equals("state"))
		{
			switch (params[2])
			{
				case "COMPLETED":
				{
					qs.exitQuest(Boolean.parseBoolean(params[3]));
					break;
				}
				case "DELETE":
				{
					Quest.deleteQuestInDb(qs, true);
					qs.exitQuest(true);
					target.sendPacket(new QuestList(target));
					target.sendPacket(new ExShowQuestMark(qs.getQuest()));
					break;
				}
				case "CREATE":
				{
					qs = QuestManager.getInstance().getQuest(Integer.parseInt(params[0])).newQuestState(target);
					qs.setState(State.STARTED);
					qs.setCond(1);
					target.sendPacket(new QuestList(target));
					target.sendPacket(new ExShowQuestMark(qs.getQuest().getId()));
					params[0] = qs.getQuest().getName();
					break;
				}
				case "CC":
				{
					qs = QuestManager.getInstance().getQuest(Integer.parseInt(params[0])).newQuestState(target);
					qs.exitQuest(false);
					target.sendPacket(new QuestList(target));
					target.sendPacket(new ExShowQuestMark(qs.getQuest().getId()));
					params[0] = qs.getQuest().getName();
					break;
				}
			}
		}
		else
		{
			if (params[2].equals("delete"))
			{
				qs.unset(params[1]);
			}
			else
			{
				qs.set(params[1], params[2]);
			}
			
			target.sendPacket(new QuestList(target));
			target.sendPacket(new ExShowQuestMark(qs.getQuest().getId()));
		}
		
		actor.sendMessage("");
		final String[] outparams = new String[3];
		outparams[0] = "name";
		outparams[1] = params[0];
		showQuestMenu(target, actor, outparams);
	}
	
	@Override
	public String[] getCommandList()
	{
		return ADMIN_COMMANDS;
	}
}

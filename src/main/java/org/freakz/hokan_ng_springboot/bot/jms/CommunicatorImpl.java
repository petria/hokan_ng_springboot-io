package org.freakz.hokan_ng_springboot.bot.jms;

import lombok.extern.slf4j.Slf4j;
import org.freakz.hokan_ng_springboot.bot.enums.HokanModule;
import org.freakz.hokan_ng_springboot.bot.events.IrcMessageEvent;
import org.freakz.hokan_ng_springboot.bot.events.ServiceRequest;
import org.freakz.hokan_ng_springboot.bot.events.ServiceRequestType;
import org.freakz.hokan_ng_springboot.bot.jms.api.JmsSender;
import org.freakz.hokan_ng_springboot.bot.jpa.entity.Alias;
import org.freakz.hokan_ng_springboot.bot.jpa.entity.UserChannel;
import org.freakz.hokan_ng_springboot.bot.jpa.service.AliasService;
import org.freakz.hokan_ng_springboot.bot.util.CommandArgs;
import org.freakz.hokan_ng_springboot.bot.util.StringStuff;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Created by Petri Airio on 9.4.2015.
 */
@Service
@Slf4j
public class CommunicatorImpl implements EngineCommunicator, ServiceCommunicator {

  @Autowired
  private AliasService aliasService;

  @Autowired
  private JmsSender jmsSender;

  private boolean resolveAlias(IrcMessageEvent event) {
    String line = event.getMessage();
    List<Alias> aliases = aliasService.findAll();
    for (Alias alias : aliases) {
      if (line.startsWith(alias.getAlias())) {
        String message = event.getMessage();
        String aliasMessage = message.replaceFirst(alias.getAlias(), alias.getCommand());
        event.setMessage(aliasMessage);
        return true;
      }
    }
    return false;
  }

  private boolean isLastCommandRepeatAlias(IrcMessageEvent event, UserChannel userChannel) {
    CommandArgs args = new CommandArgs(event.getMessage());
    if (args.getCmd().equals("!")) {
      String lastCommand = userChannel.getLastCommand();
      if (lastCommand != null && lastCommand.length() > 0) {
        CommandArgs lastCommandArgs = new CommandArgs(lastCommand);
        String aliasMessage;
        if (args.hasArgs()) {
          String message = event.getMessage();
          aliasMessage = message.replaceFirst("!", lastCommandArgs.getCmd());
          event.setMessage(aliasMessage);
        } else {
          String message = event.getMessage();
          aliasMessage = message.replaceFirst("!", lastCommand);
          event.setMessage(aliasMessage);
        }
        event.setOutputPrefix(String.format("%s :: ", aliasMessage));
        return true;
      } else {
        log.debug("No valid lastCommand: {}", lastCommand);
        return false;
      }
    }
    return false;
  }

  @Override
  public String sendToEngine(IrcMessageEvent event, UserChannel userChannel) {
    if (event.getMessage().length() > 0) {
      try {
        boolean repeatAlias = isLastCommandRepeatAlias(event, userChannel);
        boolean aliased = resolveAlias(event);
        String message = event.getMessage();
        boolean between = StringStuff.isInBetween(message, "&&", ' ');
        log.info("Aliased: {} - RepeatAlias: {} - between = {}", aliased, repeatAlias, between);
        if (!message.startsWith("!alias") && between) {
          String[] split = message.split("\\&\\&");
          for (String splitted : split) {
            IrcMessageEvent splitEvent = (IrcMessageEvent) event.clone();
            String trimmed = splitted.trim();
            splitEvent.setOutputPrefix(String.format("%s :: ", trimmed));
            splitEvent.setMessage(trimmed);
            if (trimmed.startsWith("!")) {
              log.debug("Sending to engine: {}", trimmed);
              jmsSender.send(HokanModule.HokanEngine.getQueueName(), "EVENT", splitEvent, false);
            } else {
              log.debug("Not a command: {}", trimmed);
            }
          }
        } else {
          if (event.getMessage().startsWith("!")) {
            log.debug("Sending to engine: {}", message);
            jmsSender.send(HokanModule.HokanEngine.getQueueName(), "EVENT", event, false);
          } else {
            log.debug("Not a command: {}", message);
          }
        }
      } catch (Exception e) {
        log.error("error", e);
      }
      return "Sent!";
    }
    return "not a command";
  }


  @Override
  public void sendServiceRequest(IrcMessageEvent ircEvent, ServiceRequestType requestType) {
    ServiceRequest request = new ServiceRequest(requestType, ircEvent, new CommandArgs(ircEvent.getMessage()), (Object[]) null);
    try {
      jmsSender.send(HokanModule.HokanServices.getQueueName(), "SERVICE_REQUEST", request, false);
    } catch (Exception e) {
      log.error("error", e);
    }
  }

}

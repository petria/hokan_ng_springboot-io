package org.freakz.hokan_ng_springboot.bot.jms;

import org.freakz.hokan_ng_springboot.bot.events.IrcMessageEvent;
import org.freakz.hokan_ng_springboot.bot.events.ServiceRequestType;

/**
 * Created by Petri Airio on 27.8.2015.
 */
public interface ServiceCommunicator {

    void sendServiceRequest(IrcMessageEvent ircEvent, ServiceRequestType requestType);

}

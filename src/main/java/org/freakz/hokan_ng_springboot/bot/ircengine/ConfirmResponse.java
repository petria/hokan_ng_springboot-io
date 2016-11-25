package org.freakz.hokan_ng_springboot.bot.ircengine;

import org.freakz.hokan_ng_springboot.bot.common.events.EngineResponse;
import org.freakz.hokan_ng_springboot.bot.common.util.StringStuff;
import org.joda.time.DateTime;

import java.io.Serializable;

/**
 * Created by Petri Airio on 26.8.2015.
 * -
 */
public class ConfirmResponse implements Serializable {

    private DateTime created;
    private EngineResponse response;

    public ConfirmResponse(EngineResponse response) {
        this.response = response;
        this.created = new DateTime().now();
    }

    public DateTime getCreated() {
        return created;
    }

    public void setCreated(DateTime created) {
        this.created = created;
    }

    public EngineResponse getResponse() {
        return response;
    }

    public void setResponse(EngineResponse response) {
        this.response = response;
    }

    @Override
    public String toString() {
        return String.format("%s: %s", StringStuff.formatNiceDate(created.toDate(), false),
                response.getIrcMessageEvent().getSender());
    }
}

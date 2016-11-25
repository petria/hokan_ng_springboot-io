package org.freakz.hokan_ng_springboot.bot.jms;

import lombok.extern.slf4j.Slf4j;
import org.freakz.hokan_ng_springboot.bot.common.events.EngineResponse;
import org.freakz.hokan_ng_springboot.bot.common.events.NotifyRequest;
import org.freakz.hokan_ng_springboot.bot.common.jms.JmsEnvelope;
import org.freakz.hokan_ng_springboot.bot.common.jms.SpringJmsReceiver;
import org.freakz.hokan_ng_springboot.bot.common.service.ConnectionManagerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created by petria on 5.2.2015.
 * -
 */
@Component
@Slf4j
public class IoJmsReceiver extends SpringJmsReceiver {

    private final ConnectionManagerService connectionManagerService;

    @Autowired
    public IoJmsReceiver(ConnectionManagerService connectionManagerService) {
        this.connectionManagerService = connectionManagerService;
    }

    @Override
    public String getDestinationName() {
        return "HokanNGIoQueue";
    }

    @Override
    public void handleJmsEnvelope(JmsEnvelope envelope) throws Exception {
        if (envelope.getMessageIn().getPayLoadObject("ENGINE_RESPONSE") != null) {
            handleEngineReply(envelope);
        } else if (envelope.getMessageIn().getPayLoadObject("TV_NOTIFY_REQUEST") != null) {
            handleNotify(envelope, "TV_NOTIFY_REQUEST");
        } else if (envelope.getMessageIn().getPayLoadObject("STATS_NOTIFY_REQUEST") != null) {
            handleNotify(envelope, "STATS_NOTIFY_REQUEST");
        } else if (envelope.getMessageIn().getPayLoadObject("URLS_NOTIFY_REQUEST") != null) {
            handleNotify(envelope, "URLS_NOTIFY_REQUEST");
        }
    }

    private void handleNotify(JmsEnvelope envelope, String payload) {
        NotifyRequest notifyRequest = (NotifyRequest) envelope.getMessageIn().getPayLoadObject(payload);
        log.debug("handling NotifyRequest: {}", notifyRequest);
        connectionManagerService.handleNotifyRequest(notifyRequest);
    }

    private void handleEngineReply(JmsEnvelope envelope) {
        EngineResponse response = (EngineResponse) envelope.getMessageIn().getPayLoadObject("ENGINE_RESPONSE");
        connectionManagerService.handleEngineResponse(response);
    }

}

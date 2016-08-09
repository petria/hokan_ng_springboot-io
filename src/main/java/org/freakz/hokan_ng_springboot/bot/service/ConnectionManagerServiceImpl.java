package org.freakz.hokan_ng_springboot.bot.service;

import lombok.extern.slf4j.Slf4j;
import org.freakz.hokan_ng_springboot.bot.events.EngineResponse;
import org.freakz.hokan_ng_springboot.bot.events.NotifyRequest;
import org.freakz.hokan_ng_springboot.bot.exception.HokanException;
import org.freakz.hokan_ng_springboot.bot.exception.HokanServiceException;
import org.freakz.hokan_ng_springboot.bot.ircengine.HokanCore;
import org.freakz.hokan_ng_springboot.bot.ircengine.connector.AsyncConnector;
import org.freakz.hokan_ng_springboot.bot.ircengine.connector.Connector;
import org.freakz.hokan_ng_springboot.bot.ircengine.connector.EngineConnector;
import org.freakz.hokan_ng_springboot.bot.jpa.entity.*;
import org.freakz.hokan_ng_springboot.bot.jpa.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by AirioP on 17.2.2015.
 */
@Service
@Slf4j
public class ConnectionManagerServiceImpl implements ConnectionManagerService, EngineConnector {

  @Autowired
  private ApplicationContext context;

  @Autowired
  private ChannelService channelService;

  @Autowired
  private IrcServerConfigService ircServerConfigService;

  @Autowired
  private LoggedInUserService loggedInUserService;

  @Autowired
  private NetworkService networkService;

  @Autowired
  private PropertyService propertyService;

  @Autowired
  private UserRepositoryService userService;

  private String botNick;

  private Map<String, HokanCore> connectedEngines = new HashMap<>();
  private Map<String, Connector> connectors = new HashMap<>();
  private Map<String, IrcServerConfig> configuredServers;


  private void invalidateLoggedInUsers() {
    loggedInUserService.invalidateAll();
    userService.setAllLoggedIn(0);
  }


  @PostConstruct
  public void postInit() throws HokanException {

    updateServerMap();
    resetChannelStates();
    invalidateLoggedInUsers();

    for (IrcServerConfig server : this.configuredServers.values()) {
      log.debug(">> connecting server: " + server.getServer());
      if (server.getIrcServerConfigState() == IrcServerConfigState.CONNECTED) {
        try {
          connect(server.getNetwork());
        } catch (HokanException e) {
          log.error("Couldn't get engine online: " + server.getNetwork(), e);
        }
      }
    }
  }

  private void resetChannelStates() {
    channelService.resetChannelStates();
  }

  private void updateServerMap() {
    List<IrcServerConfig> servers = ircServerConfigService.findAll();
    configuredServers = new HashMap<>();
    for (IrcServerConfig server : servers) {
      configuredServers.put(server.getNetwork().getName(), server);
    }
  }

  private boolean botNickOk() {
    try {
      this.botNick = propertyService.findFirstByPropertyName(PropertyName.PROP_SYS_BOT_NICK).getValue();
      if (this.botNick == null) {
        this.botNick = "HokanNG2";
      }
    } catch (Exception e) {
      log.error("Error did occur {}", e);
      return false;
    }
    return botNick.length() > 0;
  }

  public HokanCore getConnectedEngine(Network network) {
    return this.connectedEngines.get(network.getName());
  }

  public void connect(Network network) throws HokanServiceException {

    if (!botNickOk()) {
      throw new HokanServiceException("PropertyName.PROP_SYS_BOT_NICK not configured correctly: " + this.botNick);
    }
    updateServerMap();
    HokanCore engine = getConnectedEngine(network);
    if (engine != null) {
      throw new HokanServiceException("Engine already connected to network: " + engine);
    }

    IrcServerConfig configuredServer = configuredServers.get(network.getName());
    if (configuredServer == null) {
      throw new HokanServiceException("IrcServerConfig not found for network: " + network);
    }
    configuredServer.setIrcServerConfigState(IrcServerConfigState.CONNECTED);
    this.ircServerConfigService.save(configuredServer);

    Connector connector;
    connector = this.connectors.get(configuredServer.getNetwork().getName());
    if (connector == null) {
      connector = context.getBean(AsyncConnector.class);
      this.connectors.put(configuredServer.getNetwork().getName(), connector);
      connector.connect(this.botNick, this, configuredServer);
    } else {
      throw new HokanServiceException("Going online attempt already going: " + configuredServer.getNetwork());
    }

  }

  @Override
  public void joinChannels(String network) throws HokanServiceException {

  }

  @Override
  public void connect(String networkName) throws HokanServiceException {
    Network network = networkService.getNetwork(networkName);
    if (network != null) {
      connect(network);
    } else {
      throw new HokanServiceException("Can't connect to {}, Network not configured: " + networkName);
    }
  }

  @Override
  public void disconnect(String network) throws HokanServiceException {

  }

  @Override
  public void disconnectAll() {

  }

  @Override
  public void updateServers() {

  }

  //	----- EngineConnector

  @Override
  public void engineConnectorTooManyConnectAttempts(Connector connector, IrcServerConfig configuredServer) {
    this.connectors.remove(configuredServer.getNetwork().getName());
    log.info("Too many connection attempts:" + connector);
  }

  @Override
  public void engineConnectorTooManyConnections(Connector connector, IrcServerConfig configuredServer) {
    log.info("Too many connections:" + connector);
  }

  @Override
  public void engineConnectorGotOnline(Connector connector, HokanCore engine) throws HokanException {

    IrcServerConfig config = engine.getIrcServerConfig();
    config.setIrcServerConfigState(IrcServerConfigState.CONNECTED);
    this.ircServerConfigService.save(config);
    engine.setIrcServerConfig(config);

    Network network = config.getNetwork();
    if (network.getFirstConnected() == null) {
      network.setFirstConnected(new Date());
    }
    network.addToConnectCount(1);

    engine.startOutputQueue();

    this.connectors.remove(network.getName());
    this.connectedEngines.put(network.getName(), engine);
    this.networkService.save(network);

    joinChannels(engine, network);

  }

  private void joinChannels(HokanCore engine, Network network) {
    List<Channel> channels = this.channelService.findAll();
    if (channels != null) {
      for (Channel channelToJoin : channels) {
        if (channelToJoin.getNetwork().getName().equals(network.getName())) {
          if (channelToJoin.getChannelStartupState() == ChannelStartupState.JOIN) {
            log.info("--> joining to {}", channelToJoin.getChannelName());
            engine.joinChannel(channelToJoin.getChannelName());
          }
        }
      }
    } else {
      log.info("NO channels to join: {} -> {}", engine, network);
    }
  }

  @Override
  public void engineConnectorDisconnected(HokanCore hokanCore) {
    log.info("Engine disconnected: " + hokanCore);
    try {
      this.connectedEngines.remove(hokanCore.getIrcServerConfig().getNetwork().getName());
      connect(hokanCore.getIrcServerConfig().getNetwork().getName());
    } catch (HokanServiceException e) {
      log.error("Couldn't re-connect after disconnect timeout!", e);
    }
  }

  @Override
  public void engineConnectorPingTimeout(HokanCore hokanCore) {
    log.info("Engine ping timeout: {}", hokanCore);
    try {
      this.connectedEngines.remove(hokanCore.getIrcServerConfig().getNetwork().getName());
      connect(hokanCore.getIrcServerConfig().getNetwork().getName());
    } catch (HokanServiceException e) {
      log.error("Couldn't re-connect after ping timeout!", e);
    }
  }

  @Override
  public void engineConnectorExcessFlood(HokanCore hokanCore) {
    log.info("Engine Excess Flood: {}", hokanCore);
    try {
      this.connectedEngines.remove(hokanCore.getIrcServerConfig().getNetwork().getName());
      connect(hokanCore.getIrcServerConfig().getNetwork().getName());
    } catch (HokanServiceException e) {
      log.error("Couldn't re-connect after Excess Flood!", e);
    }

  }

  @Override
  public void handleEngineResponse(EngineResponse response) {
//    log.debug("Handle: {}", response);
    HokanCore core = this.connectedEngines.get(response.getIrcMessageEvent().getNetwork());
    if (core != null) {
      core.handleEngineResponse(response);
    } else {
      log.warn("No active HokanCore found!");
    }
  }

  @Override
  public void handleTvNotifyRequest(NotifyRequest notifyRequest) {
    Channel channel = channelService.findOne(notifyRequest.getTargetChannelId());
    if (channel == null) {
      log.warn("Can't notify, no channel with id: {}", notifyRequest.getTargetChannelId());
      return;
    }
    HokanCore core = getConnectedEngine(channel.getNetwork());
    if (core == null) {
      log.warn("Can't notify, no HokanCore for channel: {}", channel);
      return;
    }
    core.handleSendMessage(channel.getChannelName(), notifyRequest.getNotifyMessage());
    log.debug("Tv Notify sent to: {}", channel.getChannelName());
  }

  @Override
  public void handleStatsNotifyRequest(NotifyRequest notifyRequest) {
    Channel channel = channelService.findOne(notifyRequest.getTargetChannelId());
    if (channel == null) {
      log.warn("Can't notify, no channel with id: {}", notifyRequest.getTargetChannelId());
      return;
    }
    HokanCore core = getConnectedEngine(channel.getNetwork());
    if (core == null) {
      log.warn("Can't notify, no HokanCore for channel: {}", channel);
      return;
    }
    core.handleSendMessage(channel.getChannelName(), notifyRequest.getNotifyMessage());
    log.debug("Stats Notify sent to: {}", channel.getChannelName());

  }

  @Override
  public void handleNotifyRequest(NotifyRequest notifyRequest) {
    Channel channel = channelService.findOne(notifyRequest.getTargetChannelId());
    if (channel == null) {
      log.warn("Can't notify, no channel with id: {}", notifyRequest.getTargetChannelId());
      return;
    }
    HokanCore core = getConnectedEngine(channel.getNetwork());
    if (core == null) {
      log.warn("Can't notify, no HokanCore for channel: {}", channel);
      return;
    }
    core.handleSendMessage(channel.getChannelName(), notifyRequest.getNotifyMessage());
    log.debug("NotifyRequest sent to: {}", channel.getChannelName());
  }

}

package org.freakz.hokan_ng_springboot.bot.io.ircengine;

import lombok.extern.slf4j.Slf4j;
import org.freakz.hokan_ng_springboot.bot.common.core.HokanCoreService;
import org.freakz.hokan_ng_springboot.bot.common.events.EngineMethodCall;
import org.freakz.hokan_ng_springboot.bot.common.events.EngineResponse;
import org.freakz.hokan_ng_springboot.bot.common.events.IrcEvent;
import org.freakz.hokan_ng_springboot.bot.common.events.IrcEventFactory;
import org.freakz.hokan_ng_springboot.bot.common.events.IrcMessageEvent;
import org.freakz.hokan_ng_springboot.bot.common.events.ServiceRequestType;
import org.freakz.hokan_ng_springboot.bot.common.exception.HokanException;
import org.freakz.hokan_ng_springboot.bot.common.jpa.entity.Channel;
import org.freakz.hokan_ng_springboot.bot.common.jpa.entity.ChannelState;
import org.freakz.hokan_ng_springboot.bot.common.jpa.entity.ChannelStats;
import org.freakz.hokan_ng_springboot.bot.common.jpa.entity.IrcLog;
import org.freakz.hokan_ng_springboot.bot.common.jpa.entity.IrcServerConfig;
import org.freakz.hokan_ng_springboot.bot.common.jpa.entity.JoinedUser;
import org.freakz.hokan_ng_springboot.bot.common.jpa.entity.Network;
import org.freakz.hokan_ng_springboot.bot.common.jpa.entity.PropertyName;
import org.freakz.hokan_ng_springboot.bot.common.jpa.entity.SearchReplace;
import org.freakz.hokan_ng_springboot.bot.common.jpa.entity.User;
import org.freakz.hokan_ng_springboot.bot.common.jpa.entity.UserChannel;
import org.freakz.hokan_ng_springboot.bot.common.jpa.entity.UserFlag;
import org.freakz.hokan_ng_springboot.bot.common.jpa.service.ChannelPropertyService;
import org.freakz.hokan_ng_springboot.bot.common.jpa.service.ChannelService;
import org.freakz.hokan_ng_springboot.bot.common.jpa.service.ChannelStatsService;
import org.freakz.hokan_ng_springboot.bot.common.jpa.service.IrcLogService;
import org.freakz.hokan_ng_springboot.bot.common.jpa.service.JoinedUserService;
import org.freakz.hokan_ng_springboot.bot.common.jpa.service.NetworkService;
import org.freakz.hokan_ng_springboot.bot.common.jpa.service.PropertyService;
import org.freakz.hokan_ng_springboot.bot.common.jpa.service.SearchReplaceService;
import org.freakz.hokan_ng_springboot.bot.common.jpa.service.UserChannelService;
import org.freakz.hokan_ng_springboot.bot.common.jpa.service.UserService;
import org.freakz.hokan_ng_springboot.bot.common.service.AccessControlService;
import org.freakz.hokan_ng_springboot.bot.common.util.CommandArgs;
import org.freakz.hokan_ng_springboot.bot.common.util.IRCUtility;
import org.freakz.hokan_ng_springboot.bot.common.util.StringStuff;
import org.freakz.hokan_ng_springboot.bot.io.ircengine.connector.EngineConnector;
import org.freakz.hokan_ng_springboot.bot.io.jms.EngineCommunicator;
import org.freakz.hokan_ng_springboot.bot.io.jms.ServiceCommunicator;
import org.jibble.pircbot.PircBot;
import org.jibble.pircbot.PircBotUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Created by AirioP on 17.2.2015.
 * -
 */
@Component
@Scope("prototype")
@Slf4j
public class HokanCore extends PircBot implements HokanCoreService {

    @Autowired
    private AccessControlService accessControlService;

    @Autowired
    private ChannelService channelService;

    @Autowired
    private ChannelStatsService channelStatsService;

    @Autowired
    private ChannelPropertyService channelPropertyService;

    @Autowired
    private EngineCommunicator engineCommunicator;

    @Autowired
    private IrcLogService ircLogService;

    @Autowired
    private JoinedUserService joinedUsersService;

    @Autowired
    private NetworkService networkService;

    @Autowired
    private PropertyService propertyService;

    @Autowired
    private UserChannelService userChannelService;

    @Autowired
    private ServiceCommunicator serviceCommunicator;

    @Autowired
    private SearchReplaceService searchReplaceService;

    @Autowired
    private UserService userService;

    @Autowired
    private OutputQueue outputQueue;

    private EngineConnector engineConnector;

    private IrcServerConfig ircServerConfig;

    private Map<String, String> serverProperties = new HashMap<>();

    private Map<String, List<String>> whoQueries = new HashMap<>();

    private Map<String, ConfirmResponse> confirmResponseMap = new HashMap<>();

    public void init(String botName, IrcServerConfig ircServerConfig) {
        this.ircServerConfig = ircServerConfig;
        setVerbose(true);
        setName(botName);
        setVersion("Hokan NG");
        setLogin("hokan");
        setMessageDelay(1100);
    }

    public IrcServerConfig getIrcServerConfig() {
        return this.ircServerConfig;
    }

    public void setIrcServerConfig(IrcServerConfig ircServerConfig) {
        this.ircServerConfig = ircServerConfig;
    }

    public void startOutputQueue() {
        this.outputQueue.init(this, getIrcServerConfig().isThrottleInUse());
    }

    @Override
    public void dispose() {
        outputQueue.stop();
        List<Runnable> runnableList = executor.shutdownNow();
        log.info("Runnable list  size: {}", runnableList.size());
        super.dispose();
    }

    @Autowired
    public void setEngineConnector(EngineConnector engineConnector) {
        this.engineConnector = engineConnector;
    }

    public void log(String message) {
        if (!message.contains("PING") && !message.contains("PONG")) {
            log.info(message);
        }
    }

    @Override
    protected void onUnknown(String line) {
        log.info("UNKNOWN: {}", line);
        if (line.contains("Ping timeout")) {
            this.engineConnector.engineConnectorPingTimeout(this);
        } else if (line.toLowerCase().contains("excess flood")) {
            this.engineConnector.engineConnectorExcessFlood(this);
        }
    }

    public void sendWhoQuery(String channel) {
        log.info("Sending WHO query to: " + channel);
        List<String> whoReplies = new ArrayList<>();
        whoQueries.put(channel.toLowerCase(), whoReplies);
        sendRawLineViaQueue("WHO " + channel);
    }

    @Override
    protected void onUserList(String channel, PircBotUser[] pircBotUsers) {
        sendWhoQuery(channel);
    }

    public Network getNetwork() {
        return networkService.getNetwork(getIrcServerConfig().getNetwork().getName());
    }

    public Channel getChannel(String channelName) {
        Channel channel;
        channel = channelService.findByNetworkAndChannelName(getNetwork(), channelName);

        if (channel == null) {
            channel = channelService.createChannel(getNetwork(), channelName);
        }
        return channel;
    }

    public Channel getChannel(IrcEvent ircEvent) {
        return getChannel(ircEvent.getChannel());
    }

    public ChannelStats getChannelStats(Channel channel) {
        ChannelStats channelStats = channelStatsService.findFirstByChannel(channel);
        if (channelStats == null) {
            channelStats = new ChannelStats();
            channelStats.setChannel(channel);
        }
        return channelStats;
    }

    public UserChannel getUserChannel(User user, Channel channel, IrcLog ircLog) {
        UserChannel userChannel = userChannelService.getUserChannel(user, channel);
        if (userChannel == null) {
            userChannel = userChannelService.createUserChannel(user, channel, ircLog);
        }
        return userChannel;
    }

    public User getUser(IrcEvent ircEvent) {
        User user;
        User maskUser = this.userService.getUserByMask(ircEvent.getMask());
        if (maskUser != null) {
            user = maskUser;
        } else {
            user = this.userService.findFirstByNick(ircEvent.getSender());
            if (user == null) {
                user = new User(ircEvent.getSender());
                user = userService.save(user);
            }
        }
        user.setRealMask(StringStuff.quoteRegExp(ircEvent.getMask()));
        this.userService.save(user);
        return user;
    }

    private void handleWhoList(String channelName, List<String> whoReplies) throws HokanException {
        Channel channel = getChannel(channelName);
        ChannelStats channelStats = getChannelStats(channel);
        if (whoReplies.size() > channelStats.getMaxUserCount()) {
            channelStats.setMaxUserCount(whoReplies.size());
            channelStats.setMaxUserCountDate(new Date());
        }
        channelStats.setLastActive(new Date());
        channelStatsService.save(channelStats);

        this.joinedUsersService.clearJoinedUsers(channel);
        for (String whoLine : whoReplies) {
            String[] split = whoLine.split(" ");
            String nick = split[5];
            String mask = split[5] + "!" + split[2] + "@" + split[3];
            String userModes = split[6];
            String fullName = StringStuff.joinStringArray(split, 8);
            User user = this.userService.findFirstByNick(nick);
            if (user == null) {
                user = new User();
                user.setNick(split[5]);
                user.setMask(StringStuff.quoteRegExp(mask));
                user.setPassword("not_set");
                user.setFullName(fullName);
            }
            user.setRealMask(StringStuff.quoteRegExp(mask));
            user = this.userService.save(user);

//      getUserChannel(user, channel);
/*      UserChannel userChannel = userChannelService.getUserChannel(user, channel);
      if (userChannel == null) {
        userChannelService.createUserChannel(user, channel);
      }*/
            this.joinedUsersService.createJoinedUser(channel, user, userModes);
        }
    }

    @Override
    protected void onTopic(String channelName, String topic, String setBy, long date, boolean changed) {
        IrcMessageEvent ircEvent = (IrcMessageEvent) IrcEventFactory.createIrcMessageEvent(getName(), getNetwork().getName(), channelName, setBy, "topic", "topic", topic);
        ircEvent.setTimestamp(date);
        ChannelStats channelStats = getChannelStats(getChannel(channelName));
        channelStats.setTopicSetBy(setBy);
        channelStats.setTopicSetDate(new Date());
        channelStatsService.save(channelStats);
        serviceCommunicator.sendServiceRequest(ircEvent, ServiceRequestType.CHANNEL_TOPIC_SET_REQUEST);
        log.info("Topic '{}' set by {}", topic, channelStats.getTopicSetBy());
    }

    @Override
    protected void onNickChange(String oldNick, String login, String hostname, String newNick) {
        log.debug("Nick changed, refreshing channels with who query!");
        for (String channel : getChannels()) {
            sendWhoQuery(channel);
        }
    }

    @Override
    protected void onKick(String channelName, String kickerNick, String kickerLogin, String kickerHostname, String recipientNick, String reason) {
        log.info("{} kicked from {}", recipientNick, channelName);
        sendWhoQuery(channelName);
        IrcEvent ircEvent = IrcEventFactory.createIrcEvent(getName(), getNetwork().getName(), channelName, kickerNick, kickerLogin, kickerHostname);
        if (recipientNick.equalsIgnoreCase(getNick())) {
            Channel channel = getChannel(ircEvent);
            channel.setChannelState(ChannelState.KICKED_OUT);
            channelService.save(channel);
        }
    }

    @Override
    protected void onPart(String channelName, String sender, String login, String hostname, String message) {
        log.info("{} parted channel: {}", sender, channelName);
        sendWhoQuery(channelName);

        IrcEvent ircEvent = IrcEventFactory.createIrcEvent(getName(), getNetwork().getName(), channelName, sender, login, hostname);

        if (sender.equalsIgnoreCase(getNick())) {
            Channel channel = getChannel(ircEvent);
            channel.setChannelState(ChannelState.PARTED);
            channelService.save(channel);
        }
    }

    @Override
    protected void onJoin(String channelName, String sender, String login, String hostname) {
        log.info("{} joined channel: {}", sender, channelName);
        sendTopicQuery(channelName);
        IrcEvent ircEvent = IrcEventFactory.createIrcEvent(getName(), getNetwork().getName(), channelName, sender, login, hostname);
        Channel channel = getChannel(ircEvent);
        ChannelStats channelStats = getChannelStats(channel);
        User user = getUser(ircEvent);
//    UserChannel userChannel = getUserChannel(user, channel);

        if (sender.equalsIgnoreCase(getNick())) {
            // Bot joining
            Network nw = getNetwork();
            nw.addToChannelsJoined(1);
            this.networkService.save(nw);
            channel.setChannelState(ChannelState.JOINED);
            if (channelStats.getFirstJoined() == null) {
                Date d = new Date();
                channelStats.setLastWriter(getName());
                channelStats.setMaxUserCount(1);
                channelStats.setFirstJoined(d);
                channelStats.setLastActive(d);
                channelStats.setMaxUserCountDate(d);
                channelStats.setWriterSpreeOwner(getName());
            }

        } else {
            boolean doJoin = channelPropertyService.getChannelPropertyAsBoolean(channel, PropertyName.PROP_CHANNEL_DO_JOIN_MESSAGE, false);
/* TODO
       String message = userChannel.getJoinComment();
        if (message != null && message.length() > 0) {
          handleSendMessage(channel.getChannelName(), String.format("%s -> %s", sender, message));
        }
      }*/
        }
        int oldC = channelStats.getMaxUserCount();
        int newC = getUsers(channel.getChannelName()).length;
        if (newC > oldC) {
            log.info("Got new channel users record: " + newC + " > " + oldC);
            channelStats.setMaxUserCount(newC);
            channelStats.setMaxUserCountDate(new Date());
        }

        channelService.save(channel);
        channelStatsService.save(channelStats);
    }

    private void sendTopicQuery(String channelName) {
        sendRawLineViaQueue("TOPIC " + channelName);
    }

    @Override
    protected void onOp(String channel, String sourceNick, String sourceLogin, String sourceHostname, String recipient) {
        sendWhoQuery(channel);
    }

    @Override
    protected void onDeop(String channel, String sourceNick, String sourceLogin, String sourceHostname, String recipient) {
        sendWhoQuery(channel);
    }

    @Override
    protected void onVoice(String channel, String sourceNick, String sourceLogin, String sourceHostname, String recipient) {
        sendWhoQuery(channel);
    }

    @Override
    protected void onDeVoice(String channel, String sourceNick, String sourceLogin, String sourceHostname, String recipient) {
        sendWhoQuery(channel);
    }

    @Override
    protected void onServerResponse(int code, String line) {
        if (code == RPL_WHOREPLY) {
            String[] split = line.split(" ");
            if (split.length >= 6) {
                String channel = split[1];
                List<String> whoReplies = whoQueries.get(channel.toLowerCase());
                if (whoReplies == null) {
                    log.debug("was null?? --> {}", whoQueries);
                    whoReplies = new ArrayList<>();
                    whoQueries.put(channel.toLowerCase(), whoReplies);
                }
                whoReplies.add(line);

            } else {
                log.info("SKIPPED WHO REPLY: {}", line);
            }

        } else if (code == RPL_ENDOFWHO) {
            String[] split = line.split(" ");
            String channel = split[1];
            List<String> whoReplies = this.whoQueries.remove(channel.toLowerCase());
            try {
                handleWhoList(channel, whoReplies);
            } catch (HokanException e) {
                log.error("Core error", e);
            }
            log.info("Handled {} WHO lines!", whoReplies.size());

        }
        if (code == 5) {
            String[] split = line.split(" ");
            for (String str : split) {
                if (str.contains("=")) {
                    String[] keyValue = str.split("=");
                    this.serverProperties.put(keyValue[0], keyValue[1]);
                    log.info("--> {}: {}", keyValue[0], keyValue[1]);
                }
            }
        }
    }

    private boolean isBotOp(Channel channel) {
        for (JoinedUser user : joinedUsersService.findJoinedUsers(channel)) {
            if (user.getUser().getNick().equalsIgnoreCase(getName())) {
                return user.isOp();
            }
        }
        return false;
    }

    @Override
    protected void onDisconnect() {
        engineConnector.engineConnectorDisconnected(this);
    }

    @Override
    protected void onPrivateMessage(String sender, String login, String hostname, String message, byte[] original) {
        IrcLog ircLog = ircLogService.addIrcLog(new Date(), sender, getName(), message);
        int confirmLong = propertyService.getPropertyAsInt(PropertyName.PROP_SYS_CONFIRM_LONG_MESSAGES, -1);
        if (confirmLong > 0) {
            if (handleConfirmMessages(sender, message)) {
                log.info("Confirm message handled!");
                return;
            }
        }

        IrcMessageEvent ircEvent = (IrcMessageEvent) IrcEventFactory.createIrcMessageEvent(getName(), getNetwork().getName(), "@privmsg", sender, login, hostname, message);
        ircEvent.setOriginal(original);
        ircEvent.setPrivate(true);

        Network nw = getNetwork();
        nw.addToLinesReceived(1);
        this.networkService.save(nw);

        User user = getUser(ircEvent);

        serviceCommunicator.sendServiceRequest(ircEvent, ServiceRequestType.CATCH_URLS_REQUEST);
        if (accessControlService.isAdminUser(user)) {
            handleBuiltInCommands(ircEvent);
        }

        boolean ignore = accessControlService.hasUserFlag(user, UserFlag.IGNORE_ON_CHANNEL);
        if (ignore) {
            log.debug("Ignoring: {}", user);
        } else {
            Channel channel = getChannel(ircEvent);
            UserChannel userChannel = getUserChannel(user, channel, ircLog);
            String result = engineCommunicator.sendToEngine(ircEvent, userChannel);
        }

    }

    @Override
    protected void onMessage(String channel, String sender, String login, String hostname, String message,
                             byte[] original) {
        IrcLog ircLog = this.ircLogService.addIrcLog(new Date(), sender, channel, message);
        String toMe = String.format("%s: ", getName());
        boolean isToMe = false;
        if (message.startsWith(toMe)) {
            message = message.replaceFirst(toMe, "");
            isToMe = true;
        }
        IrcMessageEvent ircEvent = (IrcMessageEvent) IrcEventFactory.createIrcMessageEvent(getName(), getNetwork().getName(), channel, sender, login, hostname, message);
        ircEvent.setOriginal(original);
        ircEvent.setToMe(isToMe);

        Network nw = getNetwork();
        nw.addToLinesReceived(1);
        this.networkService.save(nw);

        User user = getUser(ircEvent);
        Channel ch = getChannel(ircEvent);
        ircEvent.setBotOp(isBotOp(ch));

        ChannelStats channelStats = channelStatsService.findFirstByChannel(ch);
        if (channelStats == null) {
            channelStats = new ChannelStats();
            channelStats.setChannel(ch);
        }


        String lastWriter = channelStats.getLastWriter();
        if (lastWriter != null && lastWriter.equalsIgnoreCase(sender)) {
            int spree = channelStats.getLastWriterSpree();
            spree++;
            channelStats.setLastWriterSpree(spree);
            if (spree > channelStats.getWriterSpreeRecord()) {
                channelStats.setWriterSpreeRecord(spree);
                channelStats.setWriterSpreeOwner(sender);
            }
        } else {
            channelStats.setLastWriterSpree(1);
        }

        channelStats.setLastActive(new Date());
//        channelStats.setLastMessage(ircEvent.getMessage()); TODO ?
        channelStats.setLastWriter(ircEvent.getSender());
        channelStats.addToLinesReceived(1);

        channelStatsService.save(channelStats);

        UserChannel userChannel = userChannelService.getUserChannel(user, ch);
        if (userChannel == null) {
            userChannel = new UserChannel(user, ch);
        }
        userChannel.setLastIrcLogID(ircLog.getId() + "");
        userChannel.setLastMessageTime(new Date());
        userChannelService.save(userChannel);

        boolean wlt = channelPropertyService.getChannelPropertyAsBoolean(ch, PropertyName.PROP_CHANNEL_DO_WHOLELINE_TRICKERS, false);
        if (wlt || ircEvent.isToMe()) {
            WholeLineTrickers wholeLineTrickers = new WholeLineTrickers(this);
            wholeLineTrickers.checkWholeLineTrickers(ircEvent);
        }
        serviceCommunicator.sendServiceRequest(ircEvent, ServiceRequestType.CATCH_URLS_REQUEST);

        if (accessControlService.isAdminUser(user)) {
            handleBuiltInCommands(ircEvent);
        }
        this.channelService.save(ch);

        boolean ignore = accessControlService.hasUserFlag(user, UserFlag.IGNORE_ON_CHANNEL);
        if (ignore) {
            log.debug("Ignoring: {}", user);
        } else {
            String result = engineCommunicator.sendToEngine(ircEvent, userChannel);
        }
    }

    private void handleBuiltInCommands(IrcMessageEvent ircEvent) {
        String message = ircEvent.getMessage();
        CommandArgs args = new CommandArgs(ircEvent.getMessage());
        if (message.startsWith("!qset ")) {

            boolean ok = outputQueue.setQueueValues(args.getArgs());
            handleSendMessage(ircEvent.getChannel(), "!qset: " + ok);
            String info = String.format("Throttle[%s]: sleepTime %d - maxLines - %d - fullLineLength %d - fullLineSleepTime %d - throttleBaseSleepTime %d",
                    outputQueue.isUsingThrottle() + "",
                    outputQueue.defSleepTime, outputQueue.defMaxLines,
                    outputQueue.defFullLineLength, outputQueue.defFullLineSleepTime,
                    outputQueue.defThrottleBaseSleepTime);

            handleSendMessage(ircEvent.getChannel(), info);
        } else if (message.equals("!clist")) {
            if (confirmResponseMap.size() > 0) {
                String cList = "";
                for (ConfirmResponse confirmResponse : confirmResponseMap.values()) {
                    cList += "  " + confirmResponse.toString();
                }
                handleSendMessage(ircEvent.getChannel(), "Confirmation list: " + cList);
            } else {
                handleSendMessage(ircEvent.getChannel(), "Confirmation list is empty!");
            }
        } else if (message.equals("!cclear")) {

            confirmResponseMap.clear();
            handleSendMessage(ircEvent.getChannel(), "Confirmation list cleared!");

        } else if (message.equals("!qclear")) {

            outputQueue.clearOutQueue();
            handleSendMessage(ircEvent.getChannel(), "OutQueue cleared!");

        } else if (message.startsWith("!qthrottle")) {

            outputQueue.setThrottle(StringStuff.parseBooleanString(args.getArgs()));
            handleSendMessage(ircEvent.getChannel(), String.format("Throttle[%s]", outputQueue.isUsingThrottle() + ""));

        } else if (message.equals("!qinfo")) {
            String info = String.format("Throttle[%s]: sleepTime %d - maxLines - %d - fullLineLength %d - fullLineSleepTime %d - throttleBaseSleepTime %d",
                    outputQueue.isUsingThrottle() + "",
                    outputQueue.defSleepTime, outputQueue.defMaxLines,
                    outputQueue.defFullLineLength, outputQueue.defFullLineSleepTime,
                    outputQueue.defThrottleBaseSleepTime);

            handleSendMessage(ircEvent.getChannel(), info);

        } else if (message.equals("!methodmap")) {
            log.info("Re-building method map!");
            buildMethodMap();
        }
    }

    private Map<String, Method> methodMap = null;

    private void buildMethodMap() {
        Class clazz = HokanCore.class;
        Method[] methods = clazz.getMethods();
        this.methodMap = new HashMap<>();
        for (Method method : methods) {
            methodMap.put(method.getName(), method);
        }
        log.info("Built method map, size {}", methodMap.size());

    }

    private Method getEngineMethod(String name) { //}, int args) {
        if (this.methodMap == null) {
            buildMethodMap();
        }
        List<Method> matches = new ArrayList<>();
        for (Method method : methodMap.values()) {
            if (method.getName().equals(name)) { // && method.getGenericParameterTypes().length == args) {
                matches.add(method);
            }
        }
        if (matches.size() == 1) {
            return matches.get(0);
        } else if (matches.size() > 1) {
            log.info("ffufu"); // TODO
            return matches.get(0);
        }
        return null;
    }


    private boolean handleConfirmMessages(String sender, String message) {
        ConfirmResponse confirmResponse = confirmResponseMap.get(message.trim());
        if (confirmResponse != null) {
            doHandleEngineResponse(confirmResponse.getResponse());
            confirmResponseMap.remove(confirmResponse);
            return true;
        }
        return false;
    }

    private void doHandleEngineResponse(EngineResponse response) {

        if (response.getException() != null) {
            String error = " failed: " + response.getException().getMessage();
            String message = response.getIrcMessageEvent().getSender() + ": " + error;
            String target = response.getIrcMessageEvent().getChannel();
            sendMessage(target, message);
            return;
        }

        handleSendMessage(response);

        for (EngineMethodCall methodCall : response.getEngineMethodCalls()) {
            String methodName = methodCall.getMethodName();
            String[] methodArgs = methodCall.getMethodArgs();

            log.info("Executing engine method : " + methodName);
            log.info("Engine method args      : " + StringStuff.arrayToString(methodArgs, ", "));
            Method method = getEngineMethod(methodName);
            if (method != null) {
                String[] args = new String[method.getParameterTypes().length];
                int i = 0;
                for (String arg : methodArgs) {
                    args[i] = arg;
                    i++;
                }
                log.info("Using method args       : " + StringStuff.arrayToString(args, ", "));
                try {
                    log.info("Invoking method         : {}", method);
                    Object result = method.invoke(this, (Object[]) args);
                    log.info("Invoke   result         : {}", result);
                } catch (Exception e) {
                    log.error("Couldn't do engine method!", e);
                }
            } else {
                log.error("Couldn't find method for: " + methodName);
            }
        }
    }

    // @Override
    @SuppressWarnings({"ThrowableResultOfMethodCallIgnored"})
    public void handleEngineResponse(EngineResponse response) {
//    log.debug("Handle: {}", response);
        int confirmLong = propertyService.getPropertyAsInt(PropertyName.PROP_SYS_CONFIRM_LONG_MESSAGES, -1);
        if (confirmLong > 0) {
            int lines = response.getResponseMessage().split("\n").length;
            if (lines > confirmLong) {
                ConfirmResponse confirmResponse = new ConfirmResponse(response);
                String confirmKey = String.format("C%d", propertyService.getNextPid());
                confirmResponseMap.put(confirmKey, confirmResponse);
                String message = String.format("Your command caused too much output: %d / max %d - ", lines, confirmLong);
                message += String.format("To send it confirm with: %s", confirmKey);
                String raw = "PRIVMSG " + response.getIrcMessageEvent().getSender() + " :" + message;
                this.outputQueue.addLine(raw);
                return;
            }
        }
        doHandleEngineResponse(response);
        Channel channel = getChannel(response.getIrcMessageEvent());
        ChannelStats channelStats = getChannelStats(channel);
        channelStats.addToCommandsHandled(1);
        channelStatsService.save(channelStats);
    }

    protected void handleSendMessage(EngineResponse response) {
        String channel = response.getReplyTo();
        String message = response.getResponseMessage();
        if (message != null && message.trim().length() > 1) {
            boolean doSr = false;
            if (!response.isNoSearchReplace()) {
                if (!response.getIrcMessageEvent().isPrivate()) {
                    Channel ch = getChannel(response.getIrcMessageEvent().getChannel());
                    doSr = channelPropertyService.getChannelPropertyAsBoolean(ch, PropertyName.PROP_CHANNEL_DO_SEARCH_REPLACE, false);
                }
            }
            handleSendMessage(channel, message, doSr,
                    response.getIrcMessageEvent().getOutputPrefix(), response.getIrcMessageEvent().getOutputPostfix());
        }
    }

    private String handleSearchReplace(String message) {
        List<SearchReplace> searchReplaces = searchReplaceService.findAll();
        for (SearchReplace sr : searchReplaces) {
            try {
                message = Pattern.compile(sr.getSearch(), Pattern.CASE_INSENSITIVE).matcher(message).replaceAll(sr.getReplace());
            } catch (Exception e) {
                message += " || SR error: " + sr.getId() + " || ";
                break;
            }
        }
        return message;
    }

    public void handleSendMessage(String channel, String message) {
        handleSendMessage(channel, message, false, null, null);
    }

    public void handleSendMessage(String channel, String message, boolean doSr, String prefix, String postfix) {

        if (doSr) {
            message = handleSearchReplace(message);
        }

        Channel ch = null;
        if (channel.startsWith("#")) {
            ch = getChannel(channel);
        }
        if (prefix == null) {
            prefix = "";
        }
        if (postfix == null) {
            postfix = "";
        }
        boolean bbMode = channelPropertyService.getChannelPropertyAsBoolean(ch, PropertyName.PROP_CHANNEL_BB_MODE, false);
        if (bbMode) {
            int rnd = 1 + (int) (Math.random() * 100);
            if (rnd > 75) {
                prefix = "yo, " + prefix;
            }
            int rnd2 = 1 + (int) (Math.random() * 100);
            if (rnd2 > 75) {
                postfix = postfix + ", bitch";
            }
        }

        Network nw = getNetwork();
        ChannelStats stats = getChannelStats(ch);

        String[] lines = message.split("\n");
        for (String line : lines) {
            String[] split = IRCUtility.breakUpMessageByIRCLineLength(channel, line);
            for (String l : split) {
                String msg = prefix + l + postfix;
                String raw = "PRIVMSG " + channel + " :" + msg;
                this.outputQueue.addLine(raw);
                this.ircLogService.addIrcLog(new Date(), getNick(), channel, msg);
                if (ch != null) {
                    stats.addToLinesSent(1);
                }
                nw.addToLinesSent(1);
            }
        }
        if (stats != null && ch != null) {
            this.channelStatsService.save(stats);
        }
        this.networkService.save(nw);
    }

}



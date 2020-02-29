package io.bitchat.im.server.processor.msg;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSON;
import io.bitchat.core.id.IdFactory;
import io.bitchat.core.id.SnowflakeIdFactory;
import io.bitchat.im.ImServiceName;
import io.bitchat.im.message.MessageCategory;
import io.bitchat.im.message.MessageType;
import io.bitchat.im.message.P2pMessage;
import io.bitchat.im.server.service.message.DefaultMessageWriter;
import io.bitchat.im.server.service.message.MessageWriter;
import io.bitchat.im.server.session.DefaultSessionIdKeeper;
import io.bitchat.im.server.session.ImSession;
import io.bitchat.im.server.session.ImSessionManager;
import io.bitchat.lang.constants.ResultCode;
import io.bitchat.packet.Payload;
import io.bitchat.packet.factory.PayloadFactory;
import io.bitchat.packet.processor.AbstractRequestProcessor;
import io.bitchat.packet.processor.Processor;
import io.bitchat.server.SessionIdKeeper;
import io.bitchat.server.channel.ChannelType;
import io.bitchat.server.session.Session;
import io.bitchat.server.session.SessionFacade;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author houyi
 */
@Slf4j
@Processor(name = ImServiceName.SEND_P2P_MSG)
public class SendP2PMsgProcessor extends AbstractRequestProcessor {

    private IdFactory idFactory;
    private MessageWriter messageWriter;
    private SessionIdKeeper sessionIdKeeper;
    private SessionFacade sessionFacade;

    public SendP2PMsgProcessor() {
        this.idFactory = SnowflakeIdFactory.getInstance();
        this.messageWriter = DefaultMessageWriter.getInstance();
        this.sessionIdKeeper = DefaultSessionIdKeeper.getInstance();
        this.sessionFacade = new SessionFacade(ImSessionManager.getInstance());
    }

    @Override
    public Payload doProcess(ChannelHandlerContext ctx, Map<String, Object> params) {
        // transfer map to bean
        SendP2PMsgRequest request = BeanUtil.mapToBean(params, SendP2PMsgRequest.class, false);

        Channel fromChannel = ctx.channel();
        ImSession session = (ImSession) sessionFacade.getSession(fromChannel);
        Long userId = session.userId();
        String userName = session.getUserName();
        Long partnerId = request.getPartnerId();
        ChannelType channelType = ChannelType.getChannelType(request.getChannelType() != null ? request.getChannelType() : 0);
        Long msgId = idFactory.nextId();
        List<Session> partnerSessions = sessionFacade.getSessionsByUserIdAndChannelType(partnerId, channelType);
        boolean success = true;
        // partner is not online
        if (CollectionUtil.isEmpty(partnerSessions)) {
            success = false;
            // save offline msg for partner
            saveOfflineMsg(userId, request, msgId);
        } else {
            Map<String, Object> content = new HashMap<>();
            content.put("partnerId", userId);
            content.put("partnerName", userName);
            content.put("messageType", request.getMessageType());
            content.put("msg", request.getMsg());
            String contentJson = JSON.toJSONString(content);
            // transfer the msg to all partner endpoints
            for (Session partnerSession : partnerSessions) {
                sessionFacade.push(partnerSession, ImServiceName.TRANSFER_MSG, contentJson);
            }
        }
        Payload payload = success ?
                PayloadFactory.newSuccessPayload() :
                PayloadFactory.newErrorPayload(ResultCode.BIZ_FAIL.getCode(), "partner is not online");
        // save history msg
        saveHistoryMsg(userId, request, msgId);
        return payload;
    }


    private void saveOfflineMsg(Long userId, SendP2PMsgRequest request, Long msgId) {
        P2pMessage p2pMessage = getP2pMessage(userId, request, msgId);
        messageWriter.saveOfflineMsg(p2pMessage, request.getPartnerId());
    }

    private void saveHistoryMsg(Long userId, SendP2PMsgRequest request, Long msgId) {
        P2pMessage p2pMessage = getP2pMessage(userId, request, msgId);
        messageWriter.saveHistoryMsg(p2pMessage);
    }

    private P2pMessage getP2pMessage(Long userId, SendP2PMsgRequest request, Long msgId) {
        Long partnerId = request.getPartnerId();
        P2pMessage p2pMessage = new P2pMessage();
        p2pMessage.setMsgId(msgId);
        p2pMessage.setSessionId(sessionIdKeeper.p2pSessionId(userId, partnerId));
        p2pMessage.setCategory(MessageCategory.P2P.getType());
        p2pMessage.setType(MessageType.getEnum(request.getMessageType()));
        p2pMessage.setCreateTime(DateUtil.current(false));
        p2pMessage.setMsg(request.getMsg());
        p2pMessage.setUserId(userId);
        p2pMessage.setPartnerId(partnerId);
        p2pMessage.setMsg(request.getMsg());
        return p2pMessage;
    }

}

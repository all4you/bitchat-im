package io.bitchat.im.server.processor.user;

import cn.hutool.core.collection.CollectionUtil;
import io.bitchat.im.ImServiceName;
import io.bitchat.im.server.session.ImSession;
import io.bitchat.im.server.session.ImSessionManager;
import io.bitchat.im.user.User;
import io.bitchat.packet.Payload;
import io.bitchat.packet.factory.PayloadFactory;
import io.bitchat.packet.processor.AbstractRequestProcessor;
import io.bitchat.packet.processor.Processor;
import io.bitchat.server.session.Session;
import io.bitchat.server.session.SessionFacade;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author houyi
 */
@Slf4j
@Processor(name = ImServiceName.GET_ONLINE_USER)
public class GetOnlineFriendsProcessor extends AbstractRequestProcessor {

    private SessionFacade sessionFacade;

    public GetOnlineFriendsProcessor() {
        this.sessionFacade = new SessionFacade(ImSessionManager.getInstance());
    }

    @Override
    public Payload doProcess(ChannelHandlerContext ctx, Map<String, Object> params) {
        Session currentSession = sessionFacade.getSession(ctx.channel());
        List<Session> sessions = sessionFacade.getAllSessions();
        List<User> userList = sessions.stream()
                // 排除掉自身
                .filter(session -> !session.sessionId().equals(currentSession.sessionId()))
                // 排除掉其他类型的客户端
                .filter(session -> session.channelType() == currentSession.channelType())
                .map(session -> {
                    ImSession imSession = (ImSession) session;
                    return User.builder()
                            .channelType(imSession.channelType().getType())
                            .sessionId(imSession.sessionId())
                            .userId(imSession.userId())
                            .userName(imSession.getUserName())
                            .build();
                })
                .collect(Collectors.toList());
        Payload payload = PayloadFactory.newSuccessPayload();
        if (CollectionUtil.isNotEmpty(userList)) {
            payload.setResult(userList);
        }
        return payload;
    }


}

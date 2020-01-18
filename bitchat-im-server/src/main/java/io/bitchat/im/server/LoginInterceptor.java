package io.bitchat.im.server;

import io.bitchat.im.ImServiceName;
import io.bitchat.lang.constants.ResultCode;
import io.bitchat.lang.constants.ServiceName;
import io.bitchat.packet.Packet;
import io.bitchat.packet.Payload;
import io.bitchat.packet.factory.PayloadFactory;
import io.bitchat.packet.interceptor.Interceptor;
import io.bitchat.server.session.SessionHelper;
import io.netty.channel.Channel;

/**
 * @author houyi
 */
public class LoginInterceptor extends Interceptor {

    @Override
    public Payload preHandle(Channel channel, Packet packet) {
        boolean shouldCheckLogin = false;
        String serviceName = packet.getRequest().getServiceName();
        if (!(ImServiceName.REGISTER.equals(serviceName) || ImServiceName.LOGIN.equals(serviceName) || ServiceName.HEART_BEAT.equals(serviceName))) {
            shouldCheckLogin = true;
        }
        // if not logged in
        if (shouldCheckLogin && !SessionHelper.hasLogin(channel)) {
            return PayloadFactory.newErrorPayload(ResultCode.BIZ_FAIL.getCode(), "Not Logged in yet!");
        }
        // if already logged in
        if (ImServiceName.LOGIN.equals(serviceName) && SessionHelper.hasLogin(channel)) {
            return PayloadFactory.newErrorPayload(ResultCode.BIZ_FAIL.getCode(), "Already Logged in!");
        }
        return PayloadFactory.newSuccessPayload();
    }


}

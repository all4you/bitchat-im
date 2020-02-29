package io.bitchat.im.client.cmd.msg;

import com.alibaba.fastjson.JSON;
import io.bitchat.im.ImServiceName;
import io.bitchat.packet.processor.AbstractCommandProcessor;
import io.bitchat.packet.processor.Processor;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

/**
 * @author houyi
 */
@Slf4j
@Processor(name = ImServiceName.TRANSFER_MSG)
public class TransferredMsgProcessor extends AbstractCommandProcessor {

    @Override
    public void doProcess(ChannelHandlerContext ctx, String contentJson) {
        // transfer map to bean
        TransferMsgCmd pushMsgCmd = JSON.parseObject(contentJson, TransferMsgCmd.class);
        Long partnerId = pushMsgCmd.getPartnerId();
        String partnerName = pushMsgCmd.getPartnerName();
        String msg = pushMsgCmd.getMsg();
        System.out.println(String.format("%s(%d):\t%s", partnerName, partnerId, msg));
        System.out.println("bitchat> ");
    }


}

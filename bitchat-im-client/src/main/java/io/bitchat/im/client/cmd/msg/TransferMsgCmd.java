package io.bitchat.im.client.cmd.msg;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author houyi
 */
@Data
@NoArgsConstructor
public class TransferMsgCmd {

    private Long partnerId;

    private String partnerName;

    private Byte messageType;

    private String msg;

}

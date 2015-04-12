package edu.michigan.eecs588.Messenger;

import edu.michigan.eecs588.encryption.AESCrypto;
import edu.michigan.eecs588.encryption.Verifier;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.muc.MultiUserChat;

import java.util.Map;

/**
 * Created by sysofwan on 4/12/15.
 */
public class Messenger {

    private class SentMsgInfo {
        String message;
        int localCount;
        SentMsgInfo(String message, int localCount) {
            this.message = message;
            this.localCount = localCount;
        }
    }

    private final int MESSAGE_ROLL_COUNT = 10;

    AESCrypto encrypto;
    AESCrypto decrypto;
    MultiUserChat muc;
    MessageReceived cb;
    Map<String, Verifier> publicKeys;
    int currentGlobalCount;
    int currentLocalCount;
    Map<String, SentMsgInfo> localSentMessages;

    public Messenger(MultiUserChat muc, MessageReceived callback, final Map<String, Verifier> publicKeys) {
        this.muc = muc;
        this.cb = callback;
        this.publicKeys = publicKeys;


        muc.addMessageListener(new MessageListener() {
            @Override
            public void processMessage(Message message) {
                String encryptedMessage = message.getBody();
                // if it is our message
                if (localSentMessages.containsKey(encryptedMessage)) {
                    SentMsgInfo msgInfo = localSentMessages.get(encryptedMessage);
                    // if the message have the wrong encryption key, resend it
                    if (msgInfo.localCount/MESSAGE_ROLL_COUNT != currentGlobalCount/MESSAGE_ROLL_COUNT) {
                        sendMessage(msgInfo.message);
                        localSentMessages.remove(encryptedMessage);
                        return;
                    }
                    localSentMessages.remove(encryptedMessage);
                }

                String dcryptMsgStr = decrypto.decrypt(encryptedMessage);
                Verifier senderVerf = publicKeys.get(message.getFrom());
                if (dcryptMsgStr != null && senderVerf != null && senderVerf.verify(dcryptMsgStr)) {
                    message.setBody(getMessage(dcryptMsgStr));
                    // count for next message
                    currentGlobalCount++;
                    // roll key for next message
                    if (currentGlobalCount % MESSAGE_ROLL_COUNT == 0) {
                        decrypto.rollKey();
                    }
                    if (currentLocalCount < currentGlobalCount) {
                        currentLocalCount = currentGlobalCount;
                        if (currentLocalCount % MESSAGE_ROLL_COUNT == 0) {
                            encrypto.rollKey();
                        }
                    }
                    cb.onMessageReceived(message);
                }

            }
        });
    }

    public void sendMessage(String message) {

    }

    private String getSignature(String message) {
        return message.substring(message.length() - 32);
    }

    private String getMessage(String message) {
        return message.substring(0, message.length() - 32);
    }

    private String sign(String message) {

    }


}

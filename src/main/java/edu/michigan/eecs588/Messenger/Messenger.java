package edu.michigan.eecs588.Messenger;

import edu.michigan.eecs588.encryption.AESCrypto;
import edu.michigan.eecs588.encryption.Signer;
import edu.michigan.eecs588.encryption.Verifier;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.packet.Message;
import edu.michigan.eecs588.Messenger.MMessage;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jxmpp.util.XmppStringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by sysofwan on 4/12/15.
 */
public class Messenger implements MessengerInterface {

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
    Signer userSigner;
    int currentGlobalCount = 0;
    int currentLocalCount = 0;
    Map<String, SentMsgInfo> localSentMessages = new HashMap<>();

    public Messenger(MultiUserChat muc, MessageReceived callback, final Map<String, Verifier> publicKeys,
                     Signer userSigner, String secret) {
        this.muc = muc;
        this.cb = callback;
        this.publicKeys = publicKeys;
        this.userSigner = userSigner;
        encrypto = new AESCrypto(secret);
        decrypto = new AESCrypto(secret);


        this.muc.addMessageListener(new MessageListener() {
            @Override
            public void processMessage(Message message) {
                String encryptedMessage = message.getBody();
                // if it is our message
                if (localSentMessages.containsKey(encryptedMessage)) {
                    SentMsgInfo msgInfo = localSentMessages.get(encryptedMessage);
                    // if the message have the wrong encryption key, resend it
                    if (msgInfo.localCount/MESSAGE_ROLL_COUNT != currentGlobalCount/MESSAGE_ROLL_COUNT) {
                        try {
                            sendMessage(msgInfo.message);
                            localSentMessages.remove(encryptedMessage);
                        } catch (SmackException.NotConnectedException e) {
                            e.printStackTrace();
                        }
                        System.out.println("Don't f-ing return!");
                        return;
                    }
                    localSentMessages.remove(encryptedMessage);
                }

                String dcryptMsgStr = decrypto.decrypt(encryptedMessage);
                //System.out.println(dcryptMsgStr);
                String sign = getSignature(dcryptMsgStr);
                //System.out.println("sign: " +  sign);
                String messageNoSign = getMessage(dcryptMsgStr);
                Verifier senderVerf = Messenger.this.publicKeys.get(XmppStringUtils.parseResource(message.getFrom()));
                if (sign != null && dcryptMsgStr != null && senderVerf != null && messageNoSign != null &&
                        senderVerf.verify(messageNoSign, sign)) {
                    MMessage m = new MMessage(messageNoSign, message.getFrom());
                    cb.onMessageReceived(m);
                    rollGlobalKey();
                }

            }
        });
    }

    /* (non-Javadoc)
	 * @see edu.michigan.eecs588.Messenger.MessengerInterface#sendMessage(java.lang.String)
	 */
    @Override
	public void sendMessage(String message) throws SmackException.NotConnectedException {
        if (message != null && !message.equals("")) {
            String sign = userSigner.sign(message);
            String signedMessage = sign + "," + message;
            String encrypted = encrypto.encrypt(signedMessage);
            localSentMessages.put(encrypted, new SentMsgInfo(message, currentLocalCount));
            muc.sendMessage(encrypted);
            rollLocalKey();
        }
    }

    private void rollLocalKey() {
        currentLocalCount++;
        if (currentLocalCount % MESSAGE_ROLL_COUNT == 0) {
            encrypto.rollKey();
        }
    }

    private void rollGlobalKey() {
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
    }

    private String getSignature(String message) {
        String[] sign = message.split(",");
        return sign.length > 0 ? sign[0] : null;
    }

    private String getMessage(String message) {
        String[] sign = message.split(",");
        return sign.length > 1 ? sign[1] : null;
    }

}

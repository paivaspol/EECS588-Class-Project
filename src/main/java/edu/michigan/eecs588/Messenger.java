package edu.michigan.eecs588;

import edu.michigan.eecs588.encryption.AESCrypto;
import edu.michigan.eecs588.encryption.Verifier;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.muc.MultiUserChat;

import java.security.PublicKey;
import java.util.Map;

/**
 * Created by sysofwan on 4/12/15.
 */
public class Messenger {

    AESCrypto crypto;
    MultiUserChat muc;
    MessageReceived cb;
    Map<String, Verifier> publicKeys;

    public Messenger(MultiUserChat muc, MessageReceived callback, Map<String, Verifier> publicKeys) {
        this.muc = muc;
        this.cb = callback;
        this.publicKeys = publicKeys;

        muc.addMessageListener(new MessageListener() {
            @Override
            public void processMessage(Message message) {
                String signature = message.getFrom();
                signature.substring()
                String decrypted = crypto.decrypt(message.getBody());
                cb.onMessageReceived(decrypted);
            }
        });
    }


}

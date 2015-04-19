package edu.michigan.eecs588;

import edu.michigan.eecs588.Messenger.MessengerInterface;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;

import java.io.IOException;
import java.util.Scanner;

/**
 * Created by sens on 4/18/15.
 */
public class Participants
{
    private static final String INVITE = "$invite";
    private static final String CREATE = "$create";
    private static final String SETUP = "$setup";

    private static final String PRIVATE = "$private";

    public static void main(String[] args) throws SmackException, IOException, XMPPException, InterruptedException
    {
        for (int index = 1; index < 20; ++index)
        {
            final String filename = "user" + index + ".properties";
            Thread thread = new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    Client client;
                    try
                    {
                        client = new Client(filename);
                        Scanner in = new Scanner(System.in);
                        String input;

                        MessengerInterface m;
                        while (true)
                        {
                            client.getPrinter().print();
                            input = in.nextLine();
                            CommandType commandType = parseInput(input);
                            String[] splitted;
                            switch (commandType)
                            {
                                case CREATE:
                                    splitted = input.split(" ");
                                    client.createRoom(splitted[1]);
                                    break;

                                case INVITE:
                                    splitted = input.split(" ");
                                    client.inviteParticipant(splitted[1]);
                                    break;

                                case SETUP:
                                    client.setup();
                                    break;

                                case PRIVATE:
                                    splitted = input.split(" ");
                                    client.createPrivateChat(splitted[1]);
                                    break;

                                case MESSAGE:
                                    m = client.getMessenger();
                                    if (m != null)
                                    {
                                        m.sendMessage(input);
                                    }
                                    break;
                            }
                        }
                    }
                    catch (IOException | SmackException | XMPPException e)
                    {
                        e.printStackTrace();
                    }
                }
            });
            thread.join();
            thread.start();
        }
    }

    private static CommandType parseInput(String command) {
        if (command.startsWith(CREATE)) {
            return CommandType.CREATE;
        } else if (command.startsWith(INVITE)) {
            return CommandType.INVITE;
        } else if (command.startsWith(SETUP)) {
            return CommandType.SETUP;
        } else if (command.startsWith(PRIVATE)) {
            return CommandType.PRIVATE;
        }
        return CommandType.MESSAGE;
    }
}

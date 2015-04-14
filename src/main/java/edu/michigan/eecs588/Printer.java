package edu.michigan.eecs588;

public class Printer
{
    private Client client;

    public Printer(Client client)
    {
        this.client = client;
    }

    public void println(Object text)
    {
        System.out.println(System.currentTimeMillis() + ":" + client.getRoomName() + "> " + text);
    }

    public void print()
    {
        System.out.print(System.currentTimeMillis() + ":" + client.getRoomName() + "> ");
    }
}

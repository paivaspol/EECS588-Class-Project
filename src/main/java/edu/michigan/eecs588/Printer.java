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
        System.out.println(client.getRoomName() + "> " + text);
    }

    public void print()
    {
        System.out.print(client.getRoomName() + "> ");
    }
}

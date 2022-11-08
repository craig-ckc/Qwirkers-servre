package Server;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

import Game.Models.Player;
import Server.messages.Message;
import Server.messages.Receiver;
import Server.messages.client.Leave;

public class Client {
    private Socket client;

    // I/O streams for communicating with client.
    private ObjectInputStream input;
    private ObjectOutputStream output;

    // Using a thread-safe queue to handle multiple threads adding to the same
    // queue (potentially) and a single thread de-queueing and sending messages
    // across the network/internet.
    private BlockingQueue<Message> outgoingMessages = new LinkedBlockingDeque<>();

    private Receiver receiver;

    private ReadThread readThread;
    private WriteThread writeThread;

    // Details about the current connection's client.
    public int ClientID;
    public String session = "";
    public Player player;

    public int turn;

    public Client(Socket client, int ClientID, String session) {
        this.client = client;
        this.ClientID = ClientID;

        this.session = session;

        this.turn = -1;

        // Start read loop thread. 
        readThread = new ReadThread(); 
        readThread.start();
    }

    public void setReceiver(Receiver receiver) {
        this.receiver = receiver;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public Player player() {
        return player;
    }

    public void send(Message message) {
        try {
            outgoingMessages.put(message);
        } catch (InterruptedException e) {
            
        }
    }

    private class ReadThread extends Thread {
        @Override
        public void run() {
            try {
                output = new ObjectOutputStream(client.getOutputStream());
                output.flush(); 
                input = new ObjectInputStream(client.getInputStream());

                writeThread = new WriteThread();
                writeThread.start();

                Message msg;
                do {
                    Object obj = input.readObject();

                    msg = (Message) obj;

                    if (receiver != null) receiver.received(msg);

                } while (msg.getClass() != Leave.class);

                client.close();

            } catch (Exception e) {
                System.out.println(e);
            } finally {
                Games.leave(Client.this);

                readThread = null;

                if (writeThread != null) writeThread.interrupt();
            }
        }
    } 

    private class WriteThread extends Thread {
        @Override
        public void run() {
            try {
                while (!isInterrupted()) {
                    Message msg = outgoingMessages.take();

                    output.writeObject(msg);
                    output.flush();

                }

            } catch (Exception e) {

            } finally {
                writeThread = null;
            }
        }
    }
}

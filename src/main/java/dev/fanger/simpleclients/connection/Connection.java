package dev.fanger.simpleclients.connection;

import dev.fanger.simpleclients.logging.Level;
import dev.fanger.simpleclients.logging.Logger;
import dev.fanger.simpleclients.server.data.payload.Payload;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.UUID;

public class Connection {

    private Socket socket;
    private ObjectInputStream inputStream;
    private ObjectOutputStream outputStream;
    private UUID id = UUID.randomUUID();
    private boolean clientShouldBeDestroyed;

    public Connection(Socket socket){
        this.socket = socket;

        try {
            outputStream = new ObjectOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            Logger.log(Level.ERROR, e);
            shutDownClient();
        }

        try {
            inputStream = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            Logger.log(Level.ERROR, e);
            shutDownClient();
        }
    }

    synchronized public void sendData(Payload payload){
        if(!clientShouldBeDestroyed) {
            try {
                outputStream.writeObject(payload);
                outputStream.flush();
            } catch (IOException e) {
                Logger.log(Level.DEBUG, e);
                shutDownClient();
            }
        }
    }

    public Payload retrieveData(){
        if(!clientShouldBeDestroyed) {
            try {
                Payload readObject = (Payload) inputStream.readObject();
                return readObject;
            } catch (Exception e) {
                Logger.log(Level.DEBUG, e);
                shutDownClient();
            }
        }

        return null;
    }

    public boolean clientShouldBeDestroyed() {
        return clientShouldBeDestroyed;
    }

    public void shutDownClient() {
        if(inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException e) {
                Logger.log(Level.ERROR, e);
            }
        }

        if(outputStream != null) {
            try {
                outputStream.close();
            } catch (IOException e) {
                Logger.log(Level.ERROR, e);
            }
        }

        try {
            socket.close();
        } catch (IOException e) {
            Logger.log(Level.ERROR, e);
        }

        clientShouldBeDestroyed = true;
    }

    public UUID getId() {
        return id;
    }

    public static Connection newConnection(String hostname, int port) {
        Socket clientSocket = new Socket();

        try {
            clientSocket.connect(new InetSocketAddress(hostname, port));
            Connection connection = new Connection(clientSocket);
            return connection;
        } catch (IOException e) {
            Logger.log(Level.ERROR, e);
        }

        return null;
    }

}

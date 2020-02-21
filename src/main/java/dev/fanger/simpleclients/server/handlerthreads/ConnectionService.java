package dev.fanger.simpleclients.server.handlerthreads;

import dev.fanger.simpleclients.connection.Connection;
import dev.fanger.simpleclients.logging.Level;
import dev.fanger.simpleclients.logging.Logger;
import dev.fanger.simpleclients.server.cloud.CloudManager;
import dev.fanger.simpleclients.server.data.task.Task;
import dev.fanger.simpleclients.server.handlerthreads.datahelper.DataReceiveHelper;
import dev.fanger.simpleclients.server.handlerthreads.datahelper.DataReceiveHelperServer;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ConnectionService implements Runnable {

    private int port;
    private boolean continueRunning = true;

    private ServerSocket serverSocket;

    private ConcurrentHashMap<UUID, Connection> clients;
    private ConcurrentHashMap<UUID, DataReceiveHelperServer> dataReceiveHelpers;
    private ConcurrentHashMap<String, Task> tasks;
    private CloudManager cloudManager;

    public ConnectionService(int port,
                             ConcurrentHashMap<UUID, Connection> clients,
                             ConcurrentHashMap<UUID, DataReceiveHelperServer> dataReceiveHelpers,
                             ConcurrentHashMap<String, Task> tasks,
                             CloudManager cloudManager){
        this.port = port;
        this.clients = clients;
        this.dataReceiveHelpers = dataReceiveHelpers;
        this.tasks = tasks;
        this.cloudManager = cloudManager;
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(port);

            while(continueRunning){
                Socket newClientSocket = serverSocket.accept();

                // Setup new connection with newly accepted client
                Connection newClientConnection = new Connection(newClientSocket);
                Logger.log(Level.INFO, "Accepted a new client: " + newClientConnection.getId());
                clients.put(newClientConnection.getId(), newClientConnection);

                // Setup data helper for new connection
                DataReceiveHelperServer dataReceiveHelper = new DataReceiveHelperServer(newClientConnection, tasks, cloudManager);
                Thread dataReceiveHelperThread = new Thread(dataReceiveHelper);
                dataReceiveHelperThread.start();
                dataReceiveHelpers.put(newClientConnection.getId(), dataReceiveHelper);

                // Add watcher to shut down and remove clients after they've completed running
                Thread connectionWatcherThread = new Thread(new ConnectionWatcher(newClientConnection.getId(), dataReceiveHelperThread));
                connectionWatcherThread.start();

                // Log status of total clients so far
                Logger.log(Level.DEBUG, "Current client list size: " + clients.keySet().size());
            }
        } catch (IOException e) {
            Logger.log(Level.ERROR, e);
        }

        Logger.log(Level.INFO, "shutdown");
    }

    /**
     * Shuts down the server connection service
     */
    public void shutdown() {
        this.continueRunning = false;

        for(DataReceiveHelperServer dataReceiveHelper : dataReceiveHelpers.values()) {
            dataReceiveHelper.shutdown();
        }

        for(Task task : tasks.values()) {
            task.shutDownTaskExecutors();
        }

        try {
            serverSocket.close();
        } catch (IOException e) {
            Logger.log(Level.ERROR, e);
        }
    }

    /**
     * Used to guarantee the shutdown of connections and data helper threads
     * This is used in an additional thread that waits until the {@link #dataReceiveHelperThread} completes
     *  but using {@link Thread[#join()]}
     */
    private class ConnectionWatcher implements Runnable {

        private UUID connectionId;
        private Thread dataReceiveHelperThread;

        public ConnectionWatcher(UUID connectionId,
                                 Thread dataReceiveHelperThread) {
            this.connectionId = connectionId;
            this.dataReceiveHelperThread = dataReceiveHelperThread;
        }

        @Override
        public void run() {
            try {
                // Wait for the connection to disconnect
                dataReceiveHelperThread.join();
            } catch (InterruptedException e) {
                Logger.log(Level.DEBUG, e);
            } finally {
                if(connectionId != null) {
                    Logger.log(Level.INFO, "Disconnected client: " + connectionId);

                    if (clients != null && clients.containsKey(connectionId)) {
                        if(clients.get(connectionId) != null) {
                            clients.get(connectionId).shutDownConnection();
                        }
                        clients.remove(connectionId);
                    }

                    if(dataReceiveHelpers != null && dataReceiveHelpers.containsKey(connectionId)) {
                        dataReceiveHelpers.remove(connectionId);
                    }
                }
            }
        }

    }

}

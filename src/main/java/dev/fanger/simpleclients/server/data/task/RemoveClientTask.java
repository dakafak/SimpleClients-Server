package dev.fanger.simpleclients.server.data.task;

import dev.fanger.simpleclients.connection.Connection;
import dev.fanger.simpleclients.server.data.payload.Payload;
import dev.fanger.simpleclients.server.handlerthreads.datahelper.ConnectionReceiveDataHelper;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RemoveClientTask extends Task {

	private ConcurrentHashMap<UUID, Connection> clients;
	private ConcurrentHashMap<UUID, ConnectionReceiveDataHelper> connectionReceiveDataHelpers;

	public RemoveClientTask(ConcurrentHashMap<UUID, Connection> clients,
							ConcurrentHashMap<UUID, ConnectionReceiveDataHelper> connectionReceiveDataHelpers) {
		this.clients = clients;
		this.connectionReceiveDataHelpers = connectionReceiveDataHelpers;
	}

	@Override
	public void executeTask(Connection connection, Payload payload) {
		UUID connectionId = connection.getId();
		connection.shutDownClient();
		System.out.println("Disconnected client: " + connection.getId());

		clients.remove(connectionId);
		connectionReceiveDataHelpers.remove(connectionId);
		System.out.println("Current client list size: " + clients.keySet().size());
	}
}

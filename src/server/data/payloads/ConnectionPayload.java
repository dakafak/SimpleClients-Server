package server.data.payloads;

import client.ClientId;
import server.data.Payload;
import server.data.PayloadValidator;
import server.data.datatypes.ConnectionData;

public abstract class ConnectionPayload extends Payload<ConnectionData> {

    public ConnectionPayload(ConnectionData data, PayloadValidator<ConnectionData> payloadValidator) {
        super(data, payloadValidator);
    }

    public abstract ClientId getClientId();

}

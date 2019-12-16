package com.cloudogu.scm.review.events;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChannelTest {

  @Mock
  private ClientFactory clientFactory;

  @InjectMocks
  private Channel channel;

  @Test
  void shouldNotifyClient() {
    Client client = register("1-2-3");
    Message message = broadcast("1-2-4");
    verify(client).send(message);
  }

  @Test
  void shouldNotNotifySender() {
    Client client = register("1-2-3");
    Message message = broadcast("1-2-3");
    verify(client, never()).send(message);
  }

  @Test
  void shouldRemoveClosedClients() {
    Client closedClient = register("1-2-3");
    when(closedClient.isClosed()).thenReturn(true);
    Client activeClient = register("1-2-4");

    channel.removeClosedClients();

    Message message = broadcast("1-2-5");
    verify(closedClient, never()).send(message);
    verify(activeClient).send(message);
  }

  @Test
  void shouldNotifyAllClientsForMessagesWithoutSenderId() {
    Client one = register("1-2-3");
    Client two = register("1-2-4");
    Client three = register(null);

    Message message = broadcast(null);

    verify(one).send(message);
    verify(two).send(message);
    verify(three).send(message);
  }

  private Message broadcast(String sender) {
    Message message = new Message(Message.Type.PULL_REQUEST, "ka");
    channel.broadcast(sender, message);
    return message;
  }

  private Client register(String sessionId) {
    Registration registration = new Registration(null, null, sessionId);
    Client client = mock(Client.class);
    lenient().when(client.getSessionId()).thenReturn(sessionId);
    when(clientFactory.create(registration)).thenReturn(client);
    channel.register(registration);
    return client;
  }

}

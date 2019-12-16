package com.cloudogu.scm.review.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;
import sonia.scm.security.SessionId;

public class Channel {

  private static final Logger LOG = LoggerFactory.getLogger(Channel.class);

  private final List<Client> clients = new ArrayList<>();
  private final ReadWriteLock lock = new ReentrantReadWriteLock();

  private final ClientFactory clientFactory;

  Channel(ClientFactory clientFactory) {
    this.clientFactory = clientFactory;
  }

  public void register(Registration registration) {
    Client client = clientFactory.create(registration);

    LOG.info("registered new client {}", client.getSessionId());
    lock.writeLock().lock();
    try {
      clients.add(client);
    } finally {
      lock.writeLock().unlock();
    }
  }

  public void broadcast(SessionId senderId, Message message) {
    lock.readLock().lock();
    try {
      clients.stream()
        .filter(isNotSender(senderId))
        .forEach(client -> client.send(message));
    } finally {
      lock.readLock().unlock();
    }
  }

  private Predicate<? super Client> isNotSender(SessionId senderId) {
    return client -> {
      if (senderId != null) {
        return !senderId.equals(client.getSessionId());
      }
      return true;
    };
  }

  void removeClosedClients() {
    lock.writeLock().lock();
    try {
      clients.removeIf(Client::isClosed);
    } finally {
      lock.writeLock().unlock();
    }
  }

}


/*
 * MIT License
 *
 * Copyright (c) 2020-present Cloudogu GmbH and Contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.cloudogu.scm.review.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Iterator;
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
  private final Clock clock;

  Channel(ClientFactory clientFactory, Clock clock) {
    this.clientFactory = clientFactory;
    this.clock = clock;
  }

  public void register(Registration registration) {
    Client client = clientFactory.create(registration);

    LOG.trace("registered new client {}", client.getSessionId());
    lock.writeLock().lock();
    try {
      clients.add(client);
    } finally {
      lock.writeLock().unlock();
    }
  }

  public void broadcast(SessionId senderId, Message message) {
    LOG.trace("broadcast message to clients");
    lock.readLock().lock();
    try {
      clients.stream()
        .filter(isNotSender(senderId))
        .forEach(client -> client.send(message));
    } finally {
      lock.readLock().unlock();
    }
  }

  private Predicate<Client> isNotSender(SessionId senderId) {
    return client -> {
      if (senderId != null) {
        return !senderId.equals(client.getSessionId());
      }
      return true;
    };
  }

  void removeClosedOrTimeoutClients() {
    Instant timeoutLimit = Instant.now(clock).minus(1, ChronoUnit.HOURS);
    lock.writeLock().lock();
    try {
      int removeCounter = 0;
      Iterator<Client> it = clients.iterator();
      while (it.hasNext()) {
        Client client = it.next();
        if (client.isClosed()) {
          LOG.trace("remove closed client with session {}", client.getSessionId());
          it.remove();
          removeCounter++;
        } else if (client.getLastUsed().isBefore(timeoutLimit)) {
          client.close();
          LOG.trace("remove client with session {}, because it has reached the timeout", client.getSessionId());
          it.remove();
          removeCounter++;
        }
      }
      LOG.trace("removed {} closed clients from channel", removeCounter);
    } finally {
      lock.writeLock().unlock();
    }
  }

}


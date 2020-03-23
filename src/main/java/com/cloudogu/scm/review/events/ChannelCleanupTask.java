package com.cloudogu.scm.review.events;

import javax.inject.Inject;

public class ChannelCleanupTask implements Runnable {

  private final ChannelRegistry registry;

  @Inject
  public ChannelCleanupTask(ChannelRegistry registry) {
    this.registry = registry;
  }

  @Override
  public void run() {
    registry.removeClosedClients();
  }
}

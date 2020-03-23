package com.cloudogu.scm.review.events;

import sonia.scm.plugin.Extension;
import sonia.scm.schedule.Scheduler;

import javax.inject.Inject;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

@Extension
public class EventContextListener implements ServletContextListener {

  private final Scheduler scheduler;

  @Inject
  public EventContextListener(Scheduler scheduler) {
    this.scheduler = scheduler;
  }

  @Override
  public void contextInitialized(ServletContextEvent sce) {
    // remove all closed or timed out clients every 30 seconds
    this.scheduler.schedule("0/30 * * * * ?", ChannelCleanupTask.class);
  }

  @Override
  public void contextDestroyed(ServletContextEvent sce) {

  }
}

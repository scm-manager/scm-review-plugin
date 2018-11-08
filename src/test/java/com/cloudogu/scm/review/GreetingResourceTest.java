package com.cloudogu.scm.review;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GreetingResourceTest {

  @Test
  void testGreeting() {
    GreetingResource resource = new GreetingResource();
    assertEquals("hello trillian", resource.greet("trillian"));
  }

}

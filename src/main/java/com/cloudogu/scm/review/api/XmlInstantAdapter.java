package com.cloudogu.scm.review.api;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.time.Instant;

public class XmlInstantAdapter extends XmlAdapter<Long, Instant> {
  @Override
  public Long marshal(Instant v) {
    return v == null ? null : v.toEpochMilli();
  }

  @Override
  public Instant unmarshal(Long v) {
    return v == null ? null : Instant.ofEpochMilli(v);
  }
}

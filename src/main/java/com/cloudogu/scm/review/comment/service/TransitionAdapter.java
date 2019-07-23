package com.cloudogu.scm.review.comment.service;

import javax.xml.bind.annotation.adapters.XmlAdapter;

public class TransitionAdapter extends XmlAdapter<String, Transition> {
  @Override
  public Transition unmarshal(String s) throws Exception {
    String[] split = s.split(":");
    return (Transition) Enum.valueOf((Class<Enum>)Class.forName(split[0]), split[1]);
  }

  @Override
  public String marshal(Transition transition) {
    return transition.getClass().getName() + ":" + transition.name();
  }
}

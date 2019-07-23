package com.cloudogu.scm.review.comment.service;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

@XmlJavaTypeAdapter(TransitionAdapter.class)
public interface Transition {
  String name();
}

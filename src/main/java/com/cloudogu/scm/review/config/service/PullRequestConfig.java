package com.cloudogu.scm.review.config.service;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "config")
public class PullRequestConfig {

  private boolean enabled = false;
  @XmlElement(name = "protected-branch-patterns")
  private List<String> protectedBranchPatterns = new ArrayList<>();

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public List<String> getProtectedBranchPatterns() {
    return protectedBranchPatterns;
  }

  public void setProtectedBranchPatterns(List<String> protectedBranchPatterns) {
    this.protectedBranchPatterns = protectedBranchPatterns;
  }
}

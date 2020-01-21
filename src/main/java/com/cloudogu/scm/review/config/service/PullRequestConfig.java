package com.cloudogu.scm.review.config.service;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "config")
public class PullRequestConfig {

  private boolean enabled = false;
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

package com.cloudogu.scm.review.config.service;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.HashSet;
import java.util.Set;

@XmlRootElement(name = "config")
public class RepositoryPullRequestConfig {

  private boolean enabled = false;
  private Set<String> protectedBranchPatterns = new HashSet<>();

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public Set<String> getProtectedBranchPatterns() {
    return protectedBranchPatterns;
  }

  public void setProtectedBranchPatterns(Set<String> protectedBranchPatterns) {
    this.protectedBranchPatterns = protectedBranchPatterns;
  }
}

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
package com.cloudogu.scm.review.comment.service;

import java.time.Instant;

public class Reply extends BasicComment {

  private boolean systemReply = false;

  public boolean isSystemReply() {
    return systemReply;
  }

  public void setSystemReply(boolean systemReply) {
    this.systemReply = systemReply;
  }

  public static Reply createReply(String id, String text, String author) {
    Reply comment = new Reply();
    comment.setId(id);
    comment.setComment(text);
    comment.setAuthor(author);
    comment.setDate(Instant.now());
    return comment;
  }

  public static Reply createNewReply(String text) {
    Reply comment = new Reply();
    comment.setComment(text);
    comment.setDate(Instant.now());
    return comment;
  }

  @Override
  public Reply clone() {
    return (Reply) super.clone();
  }
}

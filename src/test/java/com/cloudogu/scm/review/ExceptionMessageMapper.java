package com.cloudogu.scm.review;

import org.apache.shiro.authz.UnauthorizedException;
import org.jboss.resteasy.mock.MockHttpResponse;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import java.io.UnsupportedEncodingException;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static org.assertj.core.api.Assertions.assertThat;

@Provider
public class ExceptionMessageMapper implements ExceptionMapper<Exception> {
  @Override
  public Response toResponse(Exception exception) {
    return Response
      .status(exception instanceof UnauthorizedException? SC_FORBIDDEN: SC_BAD_REQUEST)
      .entity(exception.getClass().getName() + "\n" + exception.getMessage())
      .build();
  }

  public static ExceptionAssert assertExceptionFrom(MockHttpResponse response) throws UnsupportedEncodingException {
    return new ExceptionAssert(response);
  }

  public static class ExceptionAssert {
    private final String actualExceptionClass;
    private final String actualMessage;

    private ExceptionAssert(MockHttpResponse response) throws UnsupportedEncodingException {
      assertThat(response.getStatus()).isEqualTo(SC_BAD_REQUEST);
      String contentAsString = response.getContentAsString();
      actualExceptionClass = contentAsString.substring(0, contentAsString.indexOf('\n'));
      actualMessage = contentAsString.substring(contentAsString.indexOf('\n') + 1);
    }

    public ExceptionAssert isOffClass(Class<? extends Exception> expectedExceptionClass) {
      assertThat(actualExceptionClass)
        .overridingErrorMessage("Expected exception of type %s, but got %s", expectedExceptionClass.getName(), actualExceptionClass)
        .startsWith(expectedExceptionClass.getName());
      return this;
    }

    public ExceptionAssert hasMessageMatching(String pattern) {
      assertThat(actualMessage).matches(pattern);
      return this;
    }
  }
}

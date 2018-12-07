package com.cloudogu.scm.review.comment.dto;

import com.cloudogu.scm.review.comment.service.Comment;
import de.otto.edison.hal.Links;
import org.mapstruct.AfterMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import sonia.scm.api.v2.resources.BaseMapper;

import java.net.URI;

import static de.otto.edison.hal.Links.linkingTo;

@Mapper
public abstract class CommentMapper extends BaseMapper<Comment, CommentDto> {


  public abstract Comment map(CommentDto commentDto) ;

  @AfterMapping
  void appendLinks(@MappingTarget CommentDto target, @Context URI location) {
    Links.Builder linksBuilder = linkingTo()
      .self(location.toString());
    target.add(linksBuilder.build());
  }
}

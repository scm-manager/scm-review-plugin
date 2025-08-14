/*
 * Copyright (c) 2020 - present Cloudogu GmbH
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License as published by the Free
 * Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */

package com.cloudogu.scm.review.update;

import com.google.inject.Inject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import sonia.scm.migration.UpdateException;
import sonia.scm.migration.UpdateStep;
import sonia.scm.plugin.Extension;
import sonia.scm.repository.RepositoryLocationResolver;
import sonia.scm.version.Version;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Extension
public class AddTypeToCommentUpdateStep implements UpdateStep {

  private RepositoryLocationResolver.RepositoryLocationResolverInstance<Path> locationResolver;

  @Inject
  public AddTypeToCommentUpdateStep(RepositoryLocationResolver locationResolver) {
    this.locationResolver = locationResolver.forClass(Path.class);
  }

  @Override
  public void doUpdate() {
    locationResolver.forAllLocations((repositoryId, path) -> {
      Path storePath = path.resolve("store").resolve("data").resolve("pullRequestComment");
      if (Files.isDirectory(storePath)) {
        try {
          updateStorePath(storePath);
        } catch (IOException e) {
          throw new UpdateException("could not update path " + storePath, e);
        }
      }
    });
  }

  private void updateStorePath(Path storePath) throws IOException {
    try (Stream<Path> files = Files.list(storePath)) {
      files.forEach(storeFile -> {
        try {
          updateStoreFile(storeFile);
        } catch (ParserConfigurationException | IOException | SAXException | TransformerException e) {
          throw new UpdateException("could not parse comment file " + storeFile + " in path " + storePath, e);
        }
      });
    }
  }

  private void updateStoreFile(Path storeFile) throws ParserConfigurationException, IOException, SAXException, TransformerException {
    Document document = createDocumentBuilder().parse(storeFile.toFile());

    List<Node> nodes = extractCommentNodes(document);
    nodes.stream()
      .filter(this::isNotTextOnlyNode)
      .filter(this::hasNoType)
      .forEach(node -> addCommentType(document, node));
    writeChangedNodes(storeFile, document, nodes);
  }

  private boolean isNotTextOnlyNode(Node commentNode) {
    return "pull-request-comments".equals(commentNode.getParentNode().getNodeName());
  }

  private boolean hasNoType(Node commentNode) {
    NodeList childNodes = commentNode.getChildNodes();
    for (int i = 0; i < childNodes.getLength(); ++i) {
      if ("type".equals(childNodes.item(i).getNodeName())) {
        return false;
      }
    }
    return true;
  }

  private void writeChangedNodes(Path storeFile, Document document, List<Node> nodes) throws TransformerException {
    if (!nodes.isEmpty()) {
      Transformer transformer = createTransformer();
      transformer.transform(new DOMSource(document), new StreamResult(storeFile.toFile()));
    }
  }

  private Transformer createTransformer() throws TransformerConfigurationException {
    TransformerFactory factory = TransformerFactory.newInstance();
    factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    Transformer transformer = factory.newTransformer();
    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
    return factory.newTransformer();
  }

  private List<Node> extractCommentNodes(Document document) {
    NodeList commentNodeList = document.getDocumentElement().getElementsByTagName("comment");

    List<Node> commentNodes = new ArrayList<>();
    for (int i = 0; i < commentNodeList.getLength(); ++i) {
      commentNodes.add(commentNodeList.item(i));
    }
    return commentNodes;
  }

  private void addCommentType(Document document, Node commentNode) {
    Element newtypeNode = document.createElement("type");
    newtypeNode.setTextContent("COMMENT");
    commentNode.appendChild(newtypeNode);
  }

  private DocumentBuilder createDocumentBuilder() throws ParserConfigurationException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    return factory.newDocumentBuilder();
  }

  @Override
  public Version getTargetVersion() {
    return Version.parse("2.0.1");
  }

  @Override
  public String getAffectedDataType() {
    return "sonia.scm.pullrequest.comment.data.xml";
  }
}

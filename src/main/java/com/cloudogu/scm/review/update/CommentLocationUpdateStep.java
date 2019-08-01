package com.cloudogu.scm.review.update;

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

import javax.inject.Inject;
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
public class CommentLocationUpdateStep implements UpdateStep {

  private RepositoryLocationResolver.RepositoryLocationResolverInstance<Path> locationResolver;

  @Inject
  public CommentLocationUpdateStep(RepositoryLocationResolver locationResolver) {
    this.locationResolver = locationResolver.forClass(Path.class);
  }

  @Override
  @SuppressWarnings("squid:S3725") // we do not care about file performance in update steps
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

    List<Node> nodes = extractChangeIdNodes(document);
    nodes.forEach(node -> updateLocation(document, node));
    writeChangedNodes(storeFile, document, nodes);
  }

  private DocumentBuilder createDocumentBuilder() throws ParserConfigurationException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    return factory.newDocumentBuilder();
  }

  private List<Node> extractChangeIdNodes(Document document) {
    NodeList changeIdNodeList = document.getDocumentElement().getElementsByTagName("changeId");

    List<Node> changeIdNodes = new ArrayList<>();
    for (int i = 0; i < changeIdNodeList.getLength(); ++i) {
      changeIdNodes.add(changeIdNodeList.item(i));
    }
    return changeIdNodes;
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

  private void updateLocation(Document document, Node changeIdNode) {
    String textContent = changeIdNode.getTextContent();
    Node locationNode = changeIdNode.getParentNode();
    locationNode.removeChild(changeIdNode);
    switch (textContent.charAt(0)) {
      case 'I':
        appendLineNumberNode(document, textContent, locationNode, "newLineNumber");
        break;
      case 'D':
        appendLineNumberNode(document, textContent, locationNode, "oldLineNumber");
        break;
      case 'N':
        appendLineNumberNode(document, textContent, locationNode, "newLineNumber");
        appendLineNumberNode(document, textContent, locationNode, "oldLineNumber");
        break;
      default:
        throw new IllegalStateException("unknown type for change id: " + textContent);
    }
  }

  private void appendLineNumberNode(Document document, String textContent, Node locationNode, String newLineNumber) {
    Element newLineNumberNode = document.createElement(newLineNumber);
    newLineNumberNode.setTextContent(textContent.substring(1));
    locationNode.appendChild(newLineNumberNode);
  }

  @Override
  public Version getTargetVersion() {
    return Version.parse("2.0.2");
  }

  @Override
  public String getAffectedDataType() {
    return "sonia.scm.pullrequest.comment.data.xml";
  }
}

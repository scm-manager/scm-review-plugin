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

package com.cloudogu.scm.review.workflow;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * XML representation of {@link AppliedRule#configuration}.
 */
@XmlAccessorType(value = XmlAccessType.FIELD)
@Getter
@AllArgsConstructor
@NoArgsConstructor
class XmlConfiguration {
  Class<?> configurationType;
  @XmlAnyElement
  Element configuration;

  public static class RuleConfigurationXmlAdapter extends XmlAdapter<XmlConfiguration, Object> {

    private static final LoadingCache<Class<?>, JAXBContext> cache = CacheBuilder.newBuilder()
      .maximumSize(1000)
      .build(
        new CacheLoader<Class<?>, JAXBContext>() {
          @Override
          public JAXBContext load(Class<?> c) throws JAXBException {
            return JAXBContext.newInstance(c);
          }
        });

    @Override
    public Object unmarshal(XmlConfiguration internal) throws Exception {
      JAXBContext jaxbContext = cache.get(internal.configurationType);
      return jaxbContext.createUnmarshaller().unmarshal(internal.configuration);
    }

    @Override
    public XmlConfiguration marshal(Object configuration) throws Exception {
      if (configuration == null) {
        return null;
      }
      Class<?> configurationType = configuration.getClass();

      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
      factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
      factory.setAttribute(XMLConstants.FEATURE_SECURE_PROCESSING, true);

      Document document = factory.newDocumentBuilder().newDocument();

      JAXBContext jaxbContext = cache.get(configuration.getClass());
      jaxbContext.createMarshaller().marshal(configuration, document);

      Element configurationAsElement = document.getDocumentElement();

      return new XmlConfiguration(configurationType, configurationAsElement);
    }
  }
}

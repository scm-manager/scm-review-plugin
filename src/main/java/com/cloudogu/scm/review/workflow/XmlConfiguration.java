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

package com.cloudogu.scm.review.workflow;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAnyElement;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.XMLConstants;
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

/** ========================================================================= *
 * Copyright (C)  2016 IBM Corporation ( http://www.ibm.com/ )                *
 *                            All rights reserved.                            *
 *                                                                            *
 *  @author     Stephan H. Wissel (stw) <st.wissel@sg.ibm.com>                *
 *                                       @notessensei                         *
 * @version     1.0                                                           *
 * ========================================================================== *
 *                                                                            *
 * Licensed under the  Apache License, Version 2.0  (the "License").  You may *
 * not use this file except in compliance with the License.  You may obtain a *
 * copy of the License at <http://www.apache.org/licenses/LICENSE-2.0>.       *
 *                                                                            *
 * Unless  required  by applicable  law or  agreed  to  in writing,  software *
 * distributed under the License is distributed on an  "AS IS" BASIS, WITHOUT *
 * WARRANTIES OR  CONDITIONS OF ANY KIND, either express or implied.  See the *
 * License for the  specific language  governing permissions  and limitations *
 * under the License.                                                         *
 *                                                                            *
 * ========================================================================== *
 */
package net.wissel.blogrender;

import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartDocument;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

/**
 * @author stw
 *
 */
public class RSSFeedWriter {

  private final BlogIndex blogindex;
  private final Config config;

  public RSSFeedWriter(Config config, BlogIndex blogindex) {
          this.blogindex = blogindex;
          this.config = config;
  }

  public void write(final OutputStream out) throws Exception {

          // create a XMLOutputFactory
          XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();

          // create XMLEventWriter
          XMLEventWriter eventWriter = outputFactory
                          .createXMLEventWriter(out);

          // create a EventFactory

          XMLEventFactory eventFactory = XMLEventFactory.newInstance();
          XMLEvent end = eventFactory.createDTD("\n");

          // create and write Start Tag

          StartDocument startDocument = eventFactory.createStartDocument();

          eventWriter.add(startDocument);

          // create open tag
          eventWriter.add(end);

          StartElement rssStart = eventFactory.createStartElement("", "", "rss");
          eventWriter.add(rssStart);
          eventWriter.add(eventFactory.createAttribute("version", "2.0"));
          eventWriter.add(end);

          eventWriter.add(eventFactory.createStartElement("", "", "channel"));
          eventWriter.add(end);

          // Write the different nodes

          createNode(eventWriter, "title", this.config.rssTitle);

          createNode(eventWriter, "link", this.config.rssLink);

          createNode(eventWriter, "description", this.config.rssDescription);

          createNode(eventWriter, "language", this.config.language);

          createNode(eventWriter, "copyright", this.config.copyRight);

          createNode(eventWriter, "pubdate", this.getCurrentDateForFeed());

          for (BlogEntry entry : this.blogindex.topArticles) {
                  eventWriter.add(eventFactory.createStartElement("", "", "item"));
                  eventWriter.add(end);
                  createNode(eventWriter, "title", entry.getTitle());
                  createNode(eventWriter, "description", entry.getMainBody());
                  createNode(eventWriter, "link", entry.getNewURL());
                  createNode(eventWriter, "author", entry.getAuthor());
                  createNode(eventWriter, "guid", entry.getUNID());
                  createNode(eventWriter, "pubDate", entry.getPublishDateString());
                  eventWriter.add(end);
                  eventWriter.add(eventFactory.createEndElement("", "", "item"));
                  eventWriter.add(end);

          }

          eventWriter.add(end);
          eventWriter.add(eventFactory.createEndElement("", "", "channel"));
          eventWriter.add(end);
          eventWriter.add(eventFactory.createEndElement("", "", "rss"));

          eventWriter.add(end);

          eventWriter.add(eventFactory.createEndDocument());

          eventWriter.close();
  }

  private void createNode(XMLEventWriter eventWriter, String name,

  String value) throws XMLStreamException {
          XMLEventFactory eventFactory = XMLEventFactory.newInstance();
          XMLEvent end = eventFactory.createDTD("\n");
          XMLEvent tab = eventFactory.createDTD("\t");
          // create Start node
          StartElement sElement = eventFactory.createStartElement("", "", name);
          eventWriter.add(tab);
          eventWriter.add(sElement);
          // create Content
          Characters characters = eventFactory.createCharacters(value);
          eventWriter.add(characters);
          // create End node
          EndElement eElement = eventFactory.createEndElement("", "", name);
          eventWriter.add(eElement);
          eventWriter.add(end);
  }
  
  private String getCurrentDateForFeed() {
    Calendar cal = new GregorianCalendar();
    Date creationDate = cal.getTime();
    SimpleDateFormat date_format = new SimpleDateFormat(
                    "EEE', 'dd' 'MMM' 'yyyy' 'HH:mm:ss' 'Z", Locale.US);
    String pubdate = date_format.format(creationDate);
    return pubdate;
  }
  
}

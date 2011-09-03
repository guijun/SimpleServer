/*
 * Copyright (c) 2010 SimpleServer authors (see CONTRIBUTORS)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package simpleserver.config.xml;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import simpleserver.Coordinate;
import simpleserver.config.AbstractConfig;

import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import com.sun.org.apache.xml.internal.serialize.XMLSerializer;

public class GlobalConfig extends AbstractConfig {
  public Config config;
  public static final Lock lock = new ReentrantLock();

  public GlobalConfig() {
    super("config.xml");
  }

  @Override
  public void load() {
    lock.lock();
    try {
      copyDefaults();
    } catch (IOException e1) {
      System.out.println("Can't load stuff");
    }

    XMLReader xml;
    try {
      xml = XMLReaderFactory.createXMLReader();
    } catch (SAXException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      lock.unlock();
      return;
    }
    XMLTagResolver handler;
    try {
      handler = new XMLTagResolver();
    } catch (SAXException e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
      return;
    }
    xml.setContentHandler(handler);
    xml.setErrorHandler(handler);
    try {
      xml.setFeature("http://xml.org/sax/features/validation", true);
      xml.setEntityResolver(handler);
      xml.parse(new InputSource(new FileReader(getFile())));
    } catch (Exception e) {
      e.printStackTrace();
      lock.unlock();
      return;
    }
    processTags(handler.root());
    lock.unlock();
  }

  private void processTags(Config root) {
    config = root;
  }

  private void copyDefaults() throws IOException {
    File xml = new File("simpleserver", "permissions.xml");
    if (!xml.exists()) {
      copyFile(getResourceStream(), xml);
    }
  }

  private void copyFile(InputStream source, File dest) throws IOException {
    FileOutputStream stream = new FileOutputStream(dest);
    byte[] buffer = new byte[1024];
    int len;
    while ((len = source.read(buffer)) > 0) {
      stream.write(buffer, 0, len);
    }
    stream.close();
    source.close();
  }

  @Override
  public void save() {
    lock.lock();
    FileOutputStream fos;
    try {
      fos = new FileOutputStream(new File("test.xml"));
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      lock.unlock();
      return;
    }
    OutputFormat of = new OutputFormat("XML", "ISO-8859-1", true);
    of.setIndent(1);
    of.setIndenting(true);
    of.setDoctype(null, "http://somewhere/config.dtd ");
    XMLSerializer serializer = new XMLSerializer(fos, of);
    ContentHandler hd;
    try {
      hd = serializer.asContentHandler();

      hd.startDocument();
      config.save(hd, serializer);
      hd.endDocument();
      fos.close();
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      lock.unlock();
    }
  }

  public static final void main(String[] args) {
    getMemory();
    GlobalConfig conf = new GlobalConfig();
    conf.load();
    long start = new Date().getTime();
    conf.load();
    long end = new Date().getTime();
    System.out.println("\nLoading time: " + (end - start) + " ms");
    getMemory();
    conf.save();
    start = new Date().getTime();
    System.out.println("\nSaving time: " + (start - end) + " ms");

    int n = 10000;
    List<Area> areas = null;
    for (int i = 0; i < n; i++) {
      areas = conf.config.areas.get(new Coordinate(9, 11, 13));
    }
    start = new Date().getTime();
    System.out.println("\nQuery time: " + (start - end) + " ms");
    System.out.println("That is " + ((start - end) / (float) n) + " ms per query!!!");
    System.out.println("\nActive areas at given coordinate:");
    for (Area area : areas) {
      System.out.println(area.name);
    }

    System.out.println();
    conf = null;
    areas = null;
    getMemory();
  }

  private static final void getMemory() {
    Runtime runtime = Runtime.getRuntime();
    runtime.gc();
    long total = runtime.totalMemory() / 1000;
    long free = runtime.freeMemory() / 1000;
    System.out.println(String.format("Total memory: %d MB  free: %d MB  used: %d KB", total / 1000, free / 1000, (total - free)));
  }
}
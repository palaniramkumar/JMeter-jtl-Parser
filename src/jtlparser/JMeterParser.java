/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package jtlparser;

/**
 *
 * @author Ramkumar
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.SAXParserFactory;


import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Parser for JMeter.
 *
 * @author Kohsuke Kawaguchi
 */
public class JMeterParser  {
  String scenarioName;
  InfluxWritter influx;
  List<HttpSample> httpSamples;
  JMeterParser(String scenarioName){
      this.scenarioName = scenarioName;
      this.influx = new InfluxWritter("127.0.0.1", scenarioName);
      httpSamples = new LinkedList<HttpSample>();
  }

  void parse(File reportFile) throws Exception
  {
    // JMeter stores either CSV or XML in .JTL files.
    final boolean isXml = isXmlFile(reportFile);
    
    if (isXml) {
       parseXml(reportFile);
    } else {
       parseCsv(reportFile);
    }
    influx.insertRecord(httpSamples);
  }
  
  /**
   * Utility method that checks if the provided file has XML content.
   * 
   * This implementation looks for the first non-empty file. If an XML prolog appears there, this method returns <code>true</code>, otherwise <code>false</code> is returned.
   * 
   * @param file File from which the content is to e analyzed. Cannot be null.
   * @return <code>true</code> if the file content has been determined to be XML, otherwise <code>false</code>.
   */
  public static boolean isXmlFile(File file) throws IOException {
    BufferedReader reader = null;
    try {
      reader = new BufferedReader(new FileReader(file));
      String firstLine;
      while ((firstLine = reader.readLine()) != null ) {
        if (firstLine.trim().length() == 0) continue; // skip empty lines.
        return firstLine != null && firstLine.toLowerCase().trim().startsWith("<?xml ");
      }
      return false;
    } finally {
      if (reader != null) {
        reader.close();
      }
    }
  }
  
  /**
   * A delegate for {@link #parse(File)} that can process XML data.
   */
  void parseXml(File reportFile) throws Exception 
  {
    final SAXParserFactory factory = SAXParserFactory.newInstance();
    factory.setValidating(false);
    factory.setNamespaceAware(false);
    
    
    factory.newSAXParser().parse(reportFile, new DefaultHandler() {
      HttpSample currentSample;
      int counter = 0;

      /**
       * Performance XML log format is in http://jakarta.apache.org/jmeter/usermanual/listeners.html
       *
       * There are two different tags which delimit jmeter samples: 
       * - httpSample for http samples 
       * - sample for non http samples
       *
       * There are also two different XML formats which we have to handle: 
       * v2.0 = "label", "timeStamp", "time", "success"
       * v2.1 = "lb", "ts", "t", "s"
       */
      @Override
      public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if (!"httpSample".equalsIgnoreCase(qName) && !"sample".equalsIgnoreCase(qName)) {
          return;
        }
        
        final HttpSample sample = new HttpSample();
        
        final String dateValue;
        if (attributes.getValue("ts") != null) {
          dateValue = attributes.getValue("ts");
        } else {
          dateValue = attributes.getValue("timeStamp");
        }
        sample.setDate( new Date(Long.valueOf(dateValue)) );
        
        final String durationValue;
        if (attributes.getValue("t") != null) {
          durationValue = attributes.getValue("t");
        } else {
          durationValue = attributes.getValue("time"); 
        }
        sample.setDuration(Long.valueOf(durationValue));
        
        final String successfulValue;
        if (attributes.getValue("s") != null) {
          successfulValue = attributes.getValue("s");
        } else {
          successfulValue = attributes.getValue("success");
        }
        sample.setSuccessful(Boolean.parseBoolean(successfulValue));
        
        final String uriValue;
        if (attributes.getValue("lb") != null) {
          uriValue = attributes.getValue("lb");
        } else {
          uriValue = attributes.getValue("label");
        }
        sample.setUri(uriValue);
        
        final String httpCodeValue;
        if (attributes.getValue("rc") != null && attributes.getValue("rc").length() <= 3) {
          httpCodeValue = attributes.getValue("rc");
        } else {
          httpCodeValue = "0";
        }
        sample.setHttpCode(httpCodeValue);
        
        final String sizeInKbValue;
        if (attributes.getValue("by") != null) {
          sizeInKbValue = attributes.getValue("by");
        } else {
          sizeInKbValue = "0";
        }
        sample.setSizeInKb(Double.valueOf(sizeInKbValue) / 1024d);
        
        if (counter == 0) {
          currentSample = sample;
        }
          counter++;
      }

      @Override
      public void endElement(String uri, String localName, String qName) {
        if ("httpSample".equalsIgnoreCase(qName) || "sample".equalsIgnoreCase(qName)) {
          if (counter == 1) {
            try {
                //influx.insertRecord(currentSample);
                //System.out.println("URI: "+currentSample.getUri());
                //System.out.println("Code: "+currentSample.getHttpCode());
                //System.out.println("Duration: "+currentSample.getDuration());
                //System.out.println("Size: "+currentSample.getSizeInKb());
                httpSamples.add(currentSample);
            } catch (Exception e) {
              e.printStackTrace();
            }
          }
          //System.out.println(currentSample.getDuration());
          //influx.insertRecord(currentSample);
          Progress.total_record_parsed++;
          System.out.println("Processing Record "+Progress.total_record_parsed);
          counter--;
        }
      }
    });
    
  
  }
  
  /**
   * A delegate for {@link #parse(File)} that can process CSV data.
   */
  void parseCsv(File reportFile) throws Exception {
    // TODO The arguments in this constructor should be configurable.
    final JMeterCsvParser delegate = new JMeterCsvParser("timestamp,elapsed,URL,responseCode,success", ",", false);
    delegate.parse(reportFile);
  }

}

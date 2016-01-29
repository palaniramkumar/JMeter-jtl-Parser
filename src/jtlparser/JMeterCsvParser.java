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
import java.util.Date;
import java.util.HashSet;
import java.util.Set;



public class JMeterCsvParser  {

  public final boolean skipFirstLine;
  public final String delimiter;
  public int timestampIdx = -1;
  public int elapsedIdx = -1;
  public int responseCodeIdx = -1;
  public int successIdx = -1;
  public int urlIdx = -1;
  public final String pattern;


  public JMeterCsvParser( String pattern, String delimiter, Boolean skipFirstLine) throws Exception {

    this.skipFirstLine = skipFirstLine;
    this.delimiter = delimiter;
    this.pattern = pattern;
    String[] fields = pattern.split(delimiter);
    for (int i = 0; i < fields.length; i++) {
      String field = fields[i];
      if ("timestamp".equals(field)) {
        timestampIdx = i;
      } else if ("elapsed".equals(field)) {
        elapsedIdx = i;
      } else if ("responseCode".equals(field)) {
        responseCodeIdx = i;
      } else if ("success".equals(field)) {
        successIdx = i;
      } else if ("URL".equals(field)) {
        urlIdx = i;
      }
    }
    if (timestampIdx < 0 || elapsedIdx < 0 || responseCodeIdx < 0
        || successIdx < 0 || urlIdx < 0) {
      throw new Exception("Missing required column");
    }
  }

    public boolean doCheckPattern(String pattern) {
      if (pattern == null || pattern.isEmpty()) {
        return false;
      }
      Set<String> missing = new HashSet<String>();
      validatePresent(missing, pattern, "timestamp");
      validatePresent(missing, pattern, "elapsed");
      validatePresent(missing, pattern, "responseCode");
      validatePresent(missing, pattern, "success");
      validatePresent(missing, pattern, "URL");
      if (missing.isEmpty()) {
        return true;
      } else {
        StringBuilder builder = new StringBuilder();
        for (String field : missing) {
          builder.append(field + ", ");
        }
        builder.setLength(builder.length() - 2);
        return false;
      }
    }

    private void validatePresent(Set<String> missing, String pattern,
        String string) {
      if (!pattern.contains(string)) {
        missing.add(string);
      }
    }
  

  // This may be unnecessary. I tried many things getting the pattern to show up
  // correctly in the UI and this was one of them.
  public String getDefaultPattern() {
    return "timestamp,elapsed,responseCode,threadName,success,failureMessage,grpThreads,allThreads,URL,Latency,SampleCount,ErrorCount";
  }


  void parse(File reportFile) throws Exception {
     
    final BufferedReader reader = new BufferedReader(new FileReader(reportFile));
    try {
      String line = reader.readLine();
      if (line != null && skipFirstLine) {
        line = reader.readLine();
      }
      while (line != null) {
        final HttpSample sample = getSample(line);
        if (sample != null) {
          try {
              System.out.println(sample.getUri());
          } catch (Exception e) {
            throw new RuntimeException("Error parsing file '"+ reportFile +"': Unable to add sample for line " + line, e);
          }
        }
        line = reader.readLine();
      }

    } finally {
      if (reader != null) {
        reader.close();
      }
    }
  }

  /**
   * Parses a single HttpSample instance from a single CSV line.
   * 
   * @param line
   *          file line with the provided pattern (cannot be null).
   * @return An sample instance (never null).
   */
  private HttpSample getSample(String line) {
    final HttpSample sample = new HttpSample();
    final String commasNotInsideQuotes = ",(?=([^\"]*\"[^\"]*\")*[^\"]*$)";
    final String[] values = line.split(commasNotInsideQuotes);
    sample.setDate(new Date(Long.valueOf(values[timestampIdx])));
    sample.setDuration(Long.valueOf(values[elapsedIdx]));
    sample.setHttpCode(values[responseCodeIdx]);
    sample.setSuccessful(Boolean.valueOf(values[successIdx]));
    sample.setUri(values[urlIdx]);
    return sample;
  }
}
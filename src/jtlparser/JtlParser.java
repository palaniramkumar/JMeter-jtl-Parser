/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
    https://github.com/jenkinsci/performance-plugin/blob/master/src/main/java/hudson/plugins/performance/
 */

package jtlparser;

import java.io.File;

/**
 *
 * @author Ramkumar
 */
public class JtlParser {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception{
        // TODO code application logic here
        JMeterParser parser = new JMeterParser();
        parser.parse(new File("/Users/Ramkumar/Desktop/demo.xml"));
    }
    
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jtlparser;

import java.util.List;
import java.util.concurrent.TimeUnit;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;

/**
 *
 * @author nsadmin
 */
public class InfluxWritter {

    String host;
    InfluxDB influxDB;
    String scenarioName;

    InfluxWritter(String host, String scenarioName) {
        this.host = host;
        this.influxDB = InfluxDBFactory.connect("http://" + host + ":8086", "root", "root");
        this.scenarioName = scenarioName;

    }

    boolean insertRecord(List<HttpSample> samples) {
        BatchPoints batchPoints = BatchPoints
                .database("jmeterJTL")
                .tag("async", "true")
                .retentionPolicy("default")
                .consistency(InfluxDB.ConsistencyLevel.ALL)
                .build();
        final int BATCH_SIZE = 1000;
        samples.stream().map((sample) -> {
            Progress.total_record_inserted++;
            return sample;
        }).forEach((sample) -> {
            if (Progress.total_record_inserted % BATCH_SIZE != 0) {
                Point point = Point.measurement(scenarioName)
                        .time(sample.getDate().getTime(), TimeUnit.MILLISECONDS)
                        .field("duration", sample.getDuration())
                        .field("response_code", sample.getHttpCode())
                        .field("response_size", sample.getSizeInKb())
                        .field("sample_name", sample.getUri())
                        .build();
                batchPoints.point(point);
            } else {
                System.out.println("Inserting Record " + Progress.total_record_inserted);
                influxDB.write(batchPoints);
                batchPoints.getPoints().clear();
            }

        });
        if (!batchPoints.getPoints().isEmpty()) {
            influxDB.write(batchPoints);
            System.out.println("Inserting Record " + Progress.total_record_inserted);

        }
        return true;
    }
}

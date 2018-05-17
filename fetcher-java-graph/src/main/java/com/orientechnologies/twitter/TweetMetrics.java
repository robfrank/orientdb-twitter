package com.orientechnologies.twitter;

import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;
import lombok.extern.log4j.Log4j2;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

/**
 * Created by frank on 16/03/2016.
 */
@Log4j2
public class TweetMetrics {

    public static final MetricRegistry METRICS = new MetricRegistry();

    public static void configureMetrics() {
        //logger reporter
        final Slf4jReporter logReporter = Slf4jReporter.forRegistry(METRICS)
                .outputTo(LoggerFactory.getLogger("com.orientdb"))
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build();
        logReporter.start(1, TimeUnit.MINUTES);

        //graphite reporter
        boolean graphitEnabled = Boolean.valueOf(System.getProperty("tw2odb.graphite", "false"));

        if (graphitEnabled) {
            enableGraphiteReporter();
        }
    }

    static void enableGraphiteReporter() {
        String graphiteHost = System.getProperty("tw2odb.graphiteHost", "graphite");
        int graphitePort = Integer.valueOf(System.getProperty("tw2odb.graphitePort", "2003"));

        log.info("enabling graphite reporter:: {}:{}", graphiteHost, graphitePort);
        final Graphite graphite = new Graphite(new InetSocketAddress(graphiteHost, graphitePort));
        final GraphiteReporter reporter = GraphiteReporter.forRegistry(TweetMetrics.METRICS)
                .prefixedWith("twitter-fetcher")
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .filter(MetricFilter.ALL)
                .build(graphite);
        reporter.start(1, TimeUnit.MINUTES);

    }

}

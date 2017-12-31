package com.orientechnologies.twitter;

import com.codahale.metrics.Meter;
import com.google.common.base.Splitter;
import io.reactivex.flowables.ConnectableFlowable;
import io.reactivex.schedulers.Schedulers;
import lombok.extern.log4j.Log4j2;
import twitter4j.Status;

import java.util.List;

import static com.codahale.metrics.MetricRegistry.name;
import static com.orientechnologies.twitter.TweetMetrics.METRICS;

/**
 * Created by frank on 13/03/2016.
 */
@Log4j2
public class TweetToOrientDB {

    private final TweetPersister repository;
    private final List<String> keywords;
    private final List<String> languages;
    private final Meter fetched;
    private final Meter dropped;

    public TweetToOrientDB(TweetPersister repository, String keywords, String languages) {
        this.repository = repository;

        this.keywords = Splitter.on(',')
                .omitEmptyStrings()
                .trimResults()
                .splitToList(keywords);

        this.languages = Splitter.on(',')
                .omitEmptyStrings()
                .trimResults()
                .splitToList(languages);

        fetched = METRICS.meter(name("fetcher", "documents", "fetched"));
        dropped = METRICS.meter(name("fetcher", "documents", "dropped"));
    }

    public void start() {

        log.info("start fetching from twitter stream");

        final ConnectableFlowable<Status> tweetsFlowable = TweetFlowable.tweetFlowable(keywords, languages)
                .retry()
                .publish();

        tweetsFlowable.observeOn(Schedulers.computation()).forEach(s -> fetched.mark());

        tweetsFlowable.observeOn(Schedulers.io()).forEach(status -> repository.persists(status));

        tweetsFlowable.onBackpressureDrop(drop -> dropped.mark());

        tweetsFlowable.connect();
    }

    public void stop() {

    }

}

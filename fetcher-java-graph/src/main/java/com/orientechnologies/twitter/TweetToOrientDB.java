package com.orientechnologies.twitter;

import com.codahale.metrics.Meter;
import com.google.common.base.Splitter;
import lombok.extern.log4j.Log4j2;
import rx.observables.ConnectableObservable;
import rx.schedulers.Schedulers;
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

        fetched = METRICS.meter(name(getClass(), "fetched"));
        dropped = METRICS.meter(name(getClass(), "dropped"));
    }


    public void start() {

        log.info("start fetching from twitter stream");

        ConnectableObservable<Status> tweetsObservable = TweetObservable.tweetObservable(keywords, languages)
                .onBackpressureDrop()
                .retry()
                .publish();

        tweetsObservable.observeOn(Schedulers.computation()).forEach(status -> fetched.mark());

        tweetsObservable.onBackpressureDrop(drop -> dropped.mark());

        tweetsObservable.observeOn(Schedulers.io()).forEach(status -> repository.persists(status));

        tweetsObservable.connect();

    }


    public void stop() {

    }

}

package com.orientechnologies.twitter;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import lombok.extern.log4j.Log4j2;
import twitter4j.FilterQuery;
import twitter4j.Status;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;

import java.util.List;

@Log4j2
public final class TweetFlowable {

    public static Flowable<Status> tweetFlowable(final List<String> keywords, final List<String> languages) {


        return Flowable.create(emitter -> {

                    final TwitterStream twitterStream = new TwitterStreamFactory().getInstance()

                            .onStatus(status -> emitter.onNext(status))

                            .onException(e -> emitter.onError(e));

                    twitterStream.onRateLimitReached(rl -> log.info("rate exceeded:: {} ", rl));

                    twitterStream.onRateLimitStatus(rs -> log.info("rate status:: {} ", rs));


                    if (keywords.isEmpty()) {
                        log.info("no keywords provided, sample the stream");
                        twitterStream.sample();
                    } else {
                        log.info("filter stream for languages:: {} - and keywords:: {} ", languages, keywords);

                        FilterQuery query = new FilterQuery()
                                .language(languages.toArray(new String[]{}))
                                .track(keywords.toArray(new String[]{}));

                        twitterStream.filter(query);
                    }

                }


                , BackpressureStrategy.DROP);


    }

}
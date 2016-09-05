package com.orientechnologies.twitter;

import lombok.extern.log4j.Log4j2;
import rx.Observable;
import twitter4j.FilterQuery;
import twitter4j.Status;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;

import java.util.List;

@Log4j2
public final class TweetObservable {

    public static Observable<Status> tweetObservable(final List<String> keywords, final List<String> languages) {

        return Observable.create(subscriber -> {

            final TwitterStream twitterStream = new TwitterStreamFactory().getInstance()

                    .onStatus(status -> subscriber.onNext(status))

                    .onException(e -> subscriber.onError(e));

            twitterStream.onRateLimitReached(rl -> log.info("rate exceeded:: {} ", rl));

            twitterStream.onRateLimitStatus(rs -> log.info("rate status:: {} ", rs));

            if (keywords.isEmpty()) {
                log.info("no keywords provided, sample the stream");
                twitterStream.sample();
            } else {
                log.info("filter stream for languages:: {} - and keywords:: {} ", languages, keywords);

                FilterQuery query = new FilterQuery()
                        .language(languages.toArray(new String[] {}))
                        .track(keywords.toArray(new String[] {}));

                twitterStream.filter(query);
            }
        });


    }
}
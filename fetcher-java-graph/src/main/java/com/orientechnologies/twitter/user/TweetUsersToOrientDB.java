package com.orientechnologies.twitter.user;

import com.orientechnologies.orient.core.command.OCommandResultListener;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.sql.query.OSQLAsynchQuery;
import com.orientechnologies.twitter.TweetPersister;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientBaseGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;
import lombok.extern.log4j.Log4j2;
import twitter4j.RateLimitStatus;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.api.FriendsFollowersResources;

import java.util.concurrent.TimeUnit;

/**
 * Created by frank on 12/03/2017.
 */
@Log4j2
public class TweetUsersToOrientDB {

    private final String dbUrl;

    public TweetUsersToOrientDB() {
        dbUrl = System.getProperty("tw2odb.dbUrl", "remote:localhost/tweets");

    }

    public void start() {
        log.info("connect to: " + dbUrl);

        final OrientGraphFactory factory = new OrientGraphFactory(dbUrl, "admin", "admin")
                .setupPool(1, 10);

        final TweetPersister repository = new TweetPersister(factory);

        OCommandResultListener listener = new UserResultListener(repository, factory);


        while (true) {
            ODatabaseDocumentTx db = new ODatabaseDocumentTx(dbUrl).open("admin", "admin");

            try {
                db.query(new OSQLAsynchQuery<>("SELECT FROM User where fetched = false LIMIT 5", listener));
            } catch (Exception e) {
                log.error("error while retrieving");
            } finally {
                db.close();
            }
        }

    }

    public void stop() {

    }


    private static class UserResultListener implements OCommandResultListener {

        private final TweetPersister persister;
        private final OrientGraphFactory factory;
        private final FriendsFollowersResources followersResources;

        public UserResultListener(TweetPersister persister, OrientGraphFactory factory) {
            this.persister = persister;
            this.factory = factory;

            followersResources = TwitterFactory.getSingleton().friendsFollowers();
        }

        @Override
        public boolean result(Object iRecord) {

            OrientBaseGraph graph = factory.getTx();

            try {
                final OrientVertex user = graph.getVertex(iRecord);
                user.setProperty("fetched", true);
                user.save();

                log.info("getting followers and friends for user {} ", user);

                if (user == null) return false;

                Long userId = user.getProperty("userId");

                try {
                    followersResources.getFollowersList(userId, -1).stream()
                            .forEach(follower -> {
                                        log.info("follower: " + follower.getScreenName());
                                        Vertex vertex = persister.storeUser(graph, follower);
                                        vertex.addEdge("Follows", user);
                                        graph.commit();
                                    }
                            );
                    followersResources.getFriendsList(userId, -1).stream()
                            .forEach(follower -> {
                                        log.info("friend: " + follower.getScreenName());
                                        Vertex vertex = persister.storeUser(graph, follower);
                                        vertex.addEdge("Follows", user);
                                        graph.commit();
                                    }
                            );
                } catch (TwitterException e) {
                    handleRetryAfter(e);
                }


                log.info("done for user {} ", userId);
            } catch (Exception e) {
                log.error("something went wrong ", e);
            } finally {

                graph.shutdown();
            }


            return true;
        }

        public void handleRetryAfter(TwitterException e) {
            if (e.getStatusCode() == 500 || e.exceededRateLimitation()) {
                RateLimitStatus limitStatus = e.getRateLimitStatus();

                log.warn("rate limit execed, wait for {} seconds", limitStatus.getSecondsUntilReset());
                try {
                    TimeUnit.SECONDS.sleep(limitStatus.getSecondsUntilReset());
                } catch (InterruptedException e1) {
                    //nooop
                }

            } else {
                log.error(e);
            }
        }

        @Override
        public void end() {

        }

        @Override
        public Object getResult() {
            return null;
        }
    }

}

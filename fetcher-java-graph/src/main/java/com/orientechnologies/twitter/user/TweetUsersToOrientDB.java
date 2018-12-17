package com.orientechnologies.twitter.user;

import com.orientechnologies.orient.core.command.OCommandResultListener;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.OCommandSQL;
import com.orientechnologies.orient.core.sql.query.OSQLAsynchQuery;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;
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

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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

                List<ODocument> result = db.query(new OSQLSynchQuery<>("SELECT count(*) AS users FROM User where fetched = false"));

                log.info("users to be fetched:: {} ", result.get(0).field("users").toString());

                result = db.query(new OSQLSynchQuery<>("SELECT count(*) AS users FROM User where fetched = true"));

                log.info("users fetched:: {} ", result.get(0).field("users").toString());

                db.query(new OSQLAsynchQuery<>("SELECT FROM User where fetched = false LIMIT 5", listener));
            } catch (Exception e) {
                log.error("error while retrieving");
            } finally {
                db.activateOnCurrentThread();
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
        public boolean result(Object record) {

            OrientBaseGraph graph = factory.getTx();

            try {
                final OrientVertex user = graph.getVertex(record);

                if (user == null) return false;

                graph.command(new OCommandSQL("UPDATE " + user.getIdentity().toString() + " SET fetched = true")).execute();

                log.info("getting followers and friends for user {} ", user);

                Long userId = user.getProperty("userId");

                try {
                    final String followers = followersResources.getFollowersList(userId, -1, 200).stream()
                            .map(follower -> {
                                        Vertex followerVertex = persister.storeUser(graph, follower);
                                        graph.commit();
//                                        followerVertex.addEdge("Follows", user);
                                        graph.addEdge("class:Follows", followerVertex, user, null);

                                        graph.commit();
                                        return follower.getScreenName();
                                    }
                            ).collect(Collectors.joining(", "));

                    log.info("followers of user {} :: {} ", user, followers);
                } catch (TwitterException e) {
                    handleRetryAfter(e);
                }

                try {
                    final String friends = followersResources.getFriendsList(userId, -1, 200).stream()
                            .map(friend -> {
                                        Vertex vertex = persister.storeUser(graph, friend);
                                        graph.commit();

//                                        user.addEdge("Follows", vertex);
                                        graph.addEdge("class:Follows", vertex, user, null);

                                        graph.commit();
                                        return friend.getScreenName();
                                    }
                            ).collect(Collectors.joining(","));

                    log.info("friends of user {} :: {} ", user, friends);

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

                log.warn("rate limit exceeded, wait for {} seconds", limitStatus.getSecondsUntilReset());
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

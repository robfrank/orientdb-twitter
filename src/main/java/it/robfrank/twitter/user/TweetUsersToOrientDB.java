package it.robfrank.twitter.user;

import com.orientechnologies.orient.core.db.OPartitionedDatabasePool;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientBaseGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;
import it.robfrank.twitter.TweetPersister;
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

    private final FriendsFollowersResources followersResources;

    public TweetUsersToOrientDB() {
        dbUrl = System.getProperty("tw2odb.dbUrl", "plocal:./tweets");
        followersResources = TwitterFactory.getSingleton().friendsFollowers();


    }

    public void start() {
        log.info("connect to: " + dbUrl);

        final OrientGraphFactory factory = new OrientGraphFactory(dbUrl, "admin", "admin")
                .setupPool(1, 10);

        final TweetPersister persister = new TweetPersister(factory);

        final OPartitionedDatabasePool pool = new OPartitionedDatabasePool(dbUrl, "admin", "admin");

        while (true) {
            ODatabaseDocumentTx db = pool.acquire();

            try {

                List<ODocument> results = db.query(new OSQLSynchQuery<>("SELECT count(*) AS users FROM User where fetched = false"));

                log.info("users to be fetched:: {} ", results.get(0).field("users").toString());

                results = db.query(new OSQLSynchQuery<>("SELECT count(*) AS users FROM User where fetched = true"));

                log.info("users fetched:: {} ", results.get(0).field("users").toString());

                final List<ODocument> users = db.query(new OSQLSynchQuery<ODocument>("SELECT FROM User where fetched = false LIMIT 5"));

                users.stream()
                        .forEach(uer -> result(uer, factory, persister));


            } catch (Exception e) {
                log.error("error while retrieving", e);
            } finally {
                db.activateOnCurrentThread();
                db.close();
            }
        }
//        pool.close();
    }

    public void stop() {

    }


    public boolean result(ODocument record, OrientGraphFactory factory, TweetPersister persister) {

        OrientBaseGraph graph = factory.getTx();

        try {
            final OrientVertex user = graph.getVertex(record);

            if (user == null) return false;

            user.setProperty("fetched", true, OType.BOOLEAN);

            user.save();

            log.info("getting followers and friends for user {} ", user);

            Long userId = user.getProperty("userId");

            try {
                final String followers = followersResources.getFollowersList(userId, -1, 200).stream()
                        .map(follower -> {
                                    Vertex followerVertex = persister.storeUser(graph, follower);
                                    graph.commit();

                                    final Iterable<ODocument> execute = graph.command(new OSQLSynchQuery<>("select from " + followerVertex.getId() + " where out('Follows').@rid contains " + user.getId()))
                                            .execute();

                                    if (!execute.iterator().hasNext()) {
                                        graph.addEdge("class:Follows", followerVertex, user, null);
                                    }
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
                                    final Iterable<ODocument> execute = graph.command(new OSQLSynchQuery<>("select from " + vertex.getId() + " where out('Follows').@rid contains " + user.getId()))
                                            .execute();

                                    if (!execute.iterator().hasNext()) {
                                        graph.addEdge("class:Follows", vertex, user, null);
                                    }
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

    private void handleRetryAfter(TwitterException e) {
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


}

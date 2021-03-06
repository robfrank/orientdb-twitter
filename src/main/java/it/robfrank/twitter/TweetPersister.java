package it.robfrank.twitter;

import com.codahale.metrics.Meter;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientBaseGraph;
import com.tinkerpop.blueprints.impls.orient.OrientEdge;
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;
import lombok.extern.log4j.Log4j2;
import twitter4j.Status;
import twitter4j.User;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.codahale.metrics.MetricRegistry.name;
import static it.robfrank.twitter.TweetMetrics.METRICS;
import static java.util.Arrays.asList;

/**
 * Created by frank on 13/03/2016.
 */
@Log4j2
public class TweetPersister {

    private final OrientGraphFactory factory;
    private final Meter persisted;
    private final Meter duplicated;
    private final Meter persistedUsers;

    public TweetPersister(OrientGraphFactory factory) {

        this.factory = factory;

        persisted = METRICS.meter(name("persister", "documents", "persisted"));
        persistedUsers = METRICS.meter(name("persister", "users", "persisted"));
        duplicated = METRICS.meter(name("persister", "documents", "duplicated"));
    }

    public void persists(Status status) {
        OrientBaseGraph graph = factory.getTx();
        try {

            Vertex prevTweet = graph.getVertexByKey("Tweet.tweetId", status.getId());

            if (prevTweet != null) {
                duplicated.mark();
                return;
            }

            Vertex user = storeUser(status, graph);

            Vertex tweet = storeTweet(status, graph);

            userPostsTweet(user, tweet, graph);

            storeSource(status, tweet, graph);

            storeHashtags(status, tweet, graph);

            connectIfRetweet(status, tweet, graph);

            connectIfReply(status, tweet, graph);

            storeMentions(status, tweet, graph);

            graph.commit();
            persisted.mark();
        } catch (Throwable e) {
            graph.rollback();
            log.error("something bad happened while storing:: " + status.getId() + "-" + status.getUser().getId(), e);
        } finally {
            graph.shutdown();
        }

    }

    private void storeMentions(Status status, Vertex tweet, OrientBaseGraph graph) {

        Arrays.stream(status.getUserMentionEntities())
                .forEach(ume -> {
                    Vertex mentioned = graph.getVertexByKey("User.userId", ume.getId());

                    if (mentioned != null) {
                        graph.addEdge("class:Mentions", tweet, mentioned, null);
                    } else {
                        OrientVertex user = graph.addVertex("class:User")
                                .setProperties(new ArrayList<Object>() {{
                                    add("userId");
                                    add(ume.getId());
                                    add("screenName");
                                    add(ume.getScreenName());

                                }}.toArray());

                        graph.addEdge("class:Mentions", tweet, user, null);

                    }
                });

    }

    private void connectIfReply(Status status, Vertex tweet, OrientBaseGraph graph) {
        if (status.getInReplyToStatusId() != -1) {
            Vertex reply = graph.getVertexByKey("Tweet.tweetId", status.getInReplyToStatusId());
            if (reply != null) {
                graph.addEdge("class:ReplyTo", tweet, reply, null);
            }
        }
    }

    private void connectIfRetweet(Status status, Vertex tweet, OrientBaseGraph graph) {
        if (status.isRetweet()) {
            Vertex retweeted = graph.getVertexByKey("Tweet.tweetId", status.getRetweetedStatus().getId());
            if (retweeted != null) {
                graph.addEdge("class:Retweets", tweet, retweeted, null);
            }
        }
    }

    private void storeHashtags(Status status, Vertex tweet, OrientBaseGraph graph) {

        Arrays.stream(status.getHashtagEntities())
                .forEach(hashtag -> {
                    final String label = hashtag.getText().toLowerCase();
                    Vertex prevTag = graph.getVertexByKey("Hashtag.label", label);

                    if (prevTag != null) {
                        graph.addEdge("class:Tags", prevTag, tweet, null);

                    } else {

                        OrientVertex tag = graph.addVertex("class:Hashtag", "label", label);
                        graph.addEdge("class:Tags", tag, tweet, null);
                    }
                });
    }

    private void storeSource(Status status, Vertex tweet, OrientBaseGraph graph) {

        if (status.getSource() != null) {

            String source = cleanUpHtml(status.getSource())
                    .replace("twitter for", "")
                    .replace("twitter", "");

            Vertex prevSource = graph.getVertexByKey("Source.name", source);

            if (prevSource != null) {
                graph.addEdge("class:Using", tweet, prevSource, null);
                return;
            }
            OrientVertex newSource = graph.addVertex("class:Source", "name", source);

            graph.addEdge("class:Using", tweet, newSource, null);

        }
    }

    private String cleanUpHtml(String html) {
        return html.replaceAll("\\<.*?>", "").toLowerCase();
    }

    public Vertex storeTweet(Status status, OrientBaseGraph graph) {

        OrientVertex orientVertex =
                (OrientVertex) Optional.ofNullable(graph.getVertexByKey("Tweet.tweetId", status.getId()))
                        .orElse(graph.addVertex("class:Tweet"));

        List<Object> props = new ArrayList<Object>() {{
            add("text");
            add(status.getText());
            add("tweetId");
            add(Long.valueOf(status.getId()));
            add("createdAt");
            add(status.getCreatedAt());
            add("lang");
            add(status.getLang());
            add("source");
            add(cleanUpHtml(status.getSource()));
            add("userScreenName");
            add(status.getUser().getScreenName());

            Optional.ofNullable(status.getUser().getTimeZone())
                    .ifPresent(sn -> {
                        add("userTimeZone");
                        add(sn);
                    });


            add("userLang");
            add(status.getUser().getLang());

            Optional.ofNullable(status.getInReplyToScreenName())
                    .ifPresent(sn -> {
                        add("inReplyToScreenName");
                        add(sn);
                    });

            add("isRetweet");
            add(Boolean.valueOf(status.isRetweet()));
            add("isRetweeted");
            add(Boolean.valueOf(status.isRetweeted()));

            Optional.ofNullable(status.getGeoLocation())
                    .ifPresent(geo -> {
                        add("geo");
                        add(new ODocument("OPoint")
                                .field("coordinates", asList(geo.getLongitude(), geo.getLatitude())));
                    });
        }};

        orientVertex.setProperties(props.toArray()).save();

        return orientVertex;
    }

    public Vertex storeUser(Status status, OrientBaseGraph graph) {


        final User userData = status.getUser();

        return storeUser(graph, userData);
    }

    public Vertex storeUser(OrientBaseGraph graph, User userData) {
        OrientVertex user =
                (OrientVertex) Optional.ofNullable(graph.getVertexByKey("User.userId", userData.getId()))
                        .orElse(graph.addVertex("class:User"));

        List<Object> props = new ArrayList<Object>() {{
            add("userId");
            add(userData.getId());
            add("screenName");
            add(userData.getScreenName());
            add("fetched");
            add(Optional.ofNullable(user.getProperty("fetched")).orElse(Boolean.FALSE));
            Optional.ofNullable(userData.getDescription())
                    .ifPresent(desc -> {
                        add("description");
                        add(desc);
                    });
            Optional.ofNullable(userData.getLocation())
                    .ifPresent(loc -> {
                        add("location");
                        add(loc);
                    });
        }};


        user.setProperties(props.toArray()).save();
        persistedUsers.mark();
        return user;
    }

    public OrientEdge userPostsTweet(Vertex user, Vertex tweet, OrientBaseGraph graph) {

        OrientEdge posts = graph.addEdge("class:Posts", user, tweet, null);

        return posts;

    }

}

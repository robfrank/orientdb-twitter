package com.orientechnologies.twitter;

import com.orientechnologies.orient.core.command.OCommandRequest;
import com.orientechnologies.orient.core.sql.OCommandSQL;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;
import org.junit.Ignore;
import org.junit.Test;
import twitter4j.FilterQuery;
import twitter4j.HashtagEntity;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.TwitterObjectFactory;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;

import java.util.concurrent.TimeUnit;

/**
 * Created by frank on 13/03/2016.
 */
public class Twitter4jTest {


    @Test @Ignore
    public void testName() throws Exception {
        TwitterFactory tf = new TwitterFactory();
        Twitter twitter = tf.getInstance();


    }


    @Test @Ignore
    public void testName2() throws Exception {


        TwitterStream twitterStream = new TwitterStreamFactory().getInstance();
        //    twitterStream.addListener(listener);

        twitterStream.onStatus(this::statusPrinter);

        FilterQuery qry = new FilterQuery();
        qry.language("it", "en")
                .track("devops", "programming", "codemotion", "cloud", "java", "nodejs",
                        "agile", "scrum", "bigdata", "spark", "scala", "akka",
                        "orientdb", "graphdb", "neo4j", "cassandra", "tdd", "nosql"
                );

        twitterStream.filter(qry);

        TimeUnit.SECONDS.sleep(10);

    }

    private void statusPrinter(Status status) {


        HashtagEntity[] hashtagEntities = status.getHashtagEntities();

        ;
        System.out.println("----------");
        //        System.out.println("user:: " + status.getUser());
        //        System.out.println("user location:: " + status.getUser().getLocation());
        //        System.out.println("lang:: " + status.getLang());
        //        System.out.println("place:: " + status.getPlace());
        //        System.out.println("geo:: " + status.getGeoLocation());
        //        System.out.println("text::" + status.getText());
        //        System.out.println("ht::" + Arrays.stream(status.getHashtagEntities())
        //                .map(ht -> ht.getText())
        //                .collect(Collectors.joining(", ")));
        System.out.println(TwitterObjectFactory.getRawJSON(status).toString());


        System.out.println("----------");

    }


    @Test @Ignore
    public void testRxJava() throws Exception {
        OrientGraphFactory factory = new OrientGraphFactory("plocal:./target/databases/tweets").setupPool(1, 10);

        //        createDb(factory);

        TweetPersister repository = new TweetPersister(factory);
        TweetToOrientDB tweetToOrientDB = new TweetToOrientDB(repository, "", "");

        tweetToOrientDB.start();

        TimeUnit.SECONDS.sleep(30);

        OrientGraph tx = factory.getTx();


        OCommandRequest command = tx.command(new OCommandSQL("select from tweet"));


        Iterable<Vertex> v = command.execute();

        v.forEach(t -> System.out.println(t));
        tx.shutdown();

        factory.close();
    }


}

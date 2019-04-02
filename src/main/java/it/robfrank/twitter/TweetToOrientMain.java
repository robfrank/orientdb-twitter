package it.robfrank.twitter;

import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Created by frank on 15/03/2016.
 */

@Log4j2
public class TweetToOrientMain {

    public static void main(String[] args) throws IOException, InterruptedException {
        System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager");

        waitForOthers();

        log.info("starting twitter fetcher");
        TweetMetrics.configureMetrics();

        final String dbUrl = System.getProperty("tw2odb.dbUrl", "plocal:./tweets");
        final String keywords = System.getProperty("tw2odb.keywords", "");
        final String languages = System.getProperty("tw2odb.langs", "");

        final Boolean createDb = Boolean.parseBoolean(System.getProperty("tw2odb.createDb", "true"));

        if (createDb)
            TwitterDbUtils.createDbIfNeeded(dbUrl);

        final OrientGraphFactory graphFactory = new OrientGraphFactory(dbUrl, "admin", "admin")
                .setupPool(1, 10);

        final TweetPersister persister = new TweetPersister(graphFactory);

        final TweetToOrientDB tweetToOrientDB = new TweetToOrientDB(persister, keywords, languages);

        tweetToOrientDB.start();

    }

    public static void waitForOthers() throws InterruptedException {
        int timeout = Integer.valueOf(System.getProperty("tw2odb.startUpTimeout", "5"));

        log.info("waiting other services for {} seconds ", timeout);

        TimeUnit.SECONDS.sleep(timeout);
    }

}

package com.orientechnologies.twitter.user;

import com.orientechnologies.orient.core.Orient;
import com.orientechnologies.orient.core.shutdown.OShutdownHandler;
import com.orientechnologies.twitter.TweetMetrics;
import com.orientechnologies.twitter.TweetToOrientMain;
import com.orientechnologies.twitter.live.TweetLiveStreamReader;
import lombok.extern.log4j.Log4j2;

/**
 * Created by frank on 20/12/2016.
 */
@Log4j2
public class TweetUsersFetcherMain {

    public static void main(String[] args) throws Exception {
        System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager");

        TweetMetrics.configureMetrics();

//        TweetToOrientMain.waitForOthers();

        TweetUsersToOrientDB usersToOrientDB = new TweetUsersToOrientDB();

        usersToOrientDB.start();



    }

}

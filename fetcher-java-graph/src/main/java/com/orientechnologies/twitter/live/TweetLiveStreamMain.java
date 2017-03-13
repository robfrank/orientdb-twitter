package com.orientechnologies.twitter.live;

import com.orientechnologies.orient.core.Orient;
import com.orientechnologies.orient.core.shutdown.OShutdownHandler;
import com.orientechnologies.twitter.TweetMetrics;
import com.orientechnologies.twitter.TweetToOrientMain;
import lombok.extern.log4j.Log4j2;

/**
 * Created by frank on 20/12/2016.
 */
@Log4j2
public class TweetLiveStreamMain {

  public static void main(String[] args) throws Exception {
    System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager");

    TweetMetrics.configureMetrics();

    TweetToOrientMain.waitForOthers();

    TweetLiveStreamReader liveStreamReader = new TweetLiveStreamReader();

    liveStreamReader.start();

//    Orient.instance().removeShutdownHook();

//    Runtime.getRuntime().addShutdownHook(new Thread(() -> liveStreamReader.stop()));

    Orient.instance().addShutdownHandler(new OShutdownHandler() {
      @Override
      public int getPriority() {
        return Integer.MAX_VALUE;
      }

      @Override
      public void shutdown() throws Exception {

        liveStreamReader.stop();
      }
    });

    Thread.currentThread().join();

//    liveStreamReader.stop();
  }

}

package com.orientechnologies.twitter;

import com.orientechnologies.orient.client.remote.OServerAdmin;
import com.orientechnologies.orient.core.command.script.OCommandScript;
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;
import com.tinkerpop.blueprints.impls.orient.OrientGraphNoTx;
import lombok.extern.log4j.Log4j2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

/**
 * Created by frank on 15/03/2016.
 */
@Log4j2
public class TwitterDbUtils {

  public static void createDbIfNeeded(String dbUrl) throws IOException {

    if (dbUrl.startsWith("remote:")) {
      OServerAdmin serverAdmin = new OServerAdmin(dbUrl);

      log.info("connecting to server {} ", serverAdmin.getURL());

      String dbServerUser = System.getProperty("tw2odb.dbServerUser", "root");
      String dbServerPasswd = System.getProperty("tw2odb.dbServerPasswd", "root");

      serverAdmin.connect(dbServerUser, dbServerPasswd);

      if (serverAdmin.existsDatabase("tweets", "plocal")) {
        log.info("database exists, nothing to do here");
      } else {

        log.info("create database tweets");
        serverAdmin.createDatabase("tweets", "graph", "plocal");

        OrientGraphFactory factory = new OrientGraphFactory(dbUrl, "admin", "admin")
            .setupPool(1, 10);

        createDb(factory);

        factory.close();

        log.info("db created on server:: {}", dbUrl);
      }
    } else {

      log.info("creating db:: " + dbUrl);

      OrientGraphFactory factory = new OrientGraphFactory(dbUrl, "admin", "admin")
          .setupPool(1, 10);

      createDb(factory);

      factory.close();

      log.info("db created:: {}", dbUrl);

    }

  }

  public static void createDb(OrientGraphFactory factory) {

    OrientGraphNoTx graph = factory.getNoTx();

    fromSQLFile(graph);

    graph.getRawGraph().getMetadata().getSchema().reload();

    graph.shutdown();

  }

  private static void fromSQLFile(OrientGraphNoTx graph) {

    log.info("loading SQL schema from script");
    InputStream stream = ClassLoader.getSystemResourceAsStream("tweets.osql");

    try {
      graph.command(new OCommandScript("sql", read(stream))).execute();
      log.info("schema loaded");
    } catch (IOException e) {
      log.error("unable to create database schema", e);
    }

  }

  public static String read(InputStream input) throws IOException {
    try (BufferedReader buffer = new BufferedReader(new InputStreamReader(input))) {
      return buffer.lines().collect(Collectors.joining("\n"));
    }
  }

}

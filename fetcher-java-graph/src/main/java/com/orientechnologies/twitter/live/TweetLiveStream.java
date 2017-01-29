package com.orientechnologies.twitter.live;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.db.record.ORecordOperation;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.OCommandSQL;
import com.orientechnologies.orient.core.sql.query.OLiveQuery;
import com.orientechnologies.orient.core.sql.query.OLiveResultListener;
import lombok.extern.log4j.Log4j2;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.util.stream.Collectors.toList;

/**
 * Created by frank on 20/12/2016.
 */
@Log4j2
public class TweetLiveStream {

    public static void main(String[] args) throws Exception {


        String queries = System.getProperty("tw2odb.queries", "SELECT FROM tweet WHERE text LUCENE 'cloud' ");


        ODatabaseDocumentTx db = new ODatabaseDocumentTx("remote:localhost/tweets")
                .open("admin", "admin");

        OLiveResultListener listener = new OLiveResultListener() {

            public void onLiveResult(int token, ORecordOperation iOp) {
                log.info(Thread.currentThread().getId() + " - token:: " + token + " content: " + iOp.record);
            }

            @Override
            public void onError(int iLiveToken) {
                log.error("ERROR iLiveToken = " + iLiveToken);
            }

            @Override
            public void onUnsubscribe(int iLiveToken) {
                log.info("UNSUBSCRIBE iLiveToken = " + iLiveToken);
            }


        };


        List<Integer> tokens = Arrays.stream(queries.split(","))
                .peek(q -> log.info("registering live query:: " + q))
                .map(q_ -> "LIVE " + q_)
                .map(q -> db.<List<ODocument>>query(new OLiveQuery<ODocument>(q, listener)).get(0))
                .map(d -> d.<Integer>field("token"))
                .collect(toList());

        TimeUnit.SECONDS.sleep(20);


        tokens.stream()
                .forEach(token -> db.command(new OCommandSQL("LIVE UNSUBSCRIBE " + token)).execute());

    }
}

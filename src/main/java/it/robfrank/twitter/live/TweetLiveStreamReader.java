package it.robfrank.twitter.live;

import com.codahale.metrics.Meter;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.db.record.ORecordOperation;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.OCommandSQL;
import com.orientechnologies.orient.core.sql.query.OLiveQuery;
import com.orientechnologies.orient.core.sql.query.OLiveResultListener;
import it.robfrank.twitter.TweetMetrics;
import lombok.extern.log4j.Log4j2;

import java.util.Arrays;
import java.util.List;

import static com.codahale.metrics.MetricRegistry.name;
import static java.util.stream.Collectors.toList;

/**
 * Created by frank on 12/03/2017.
 */
@Log4j2
public class TweetLiveStreamReader {

    private final String dbUrl;
    private final String queries;
    private List<Integer> tokens;

    public TweetLiveStreamReader() {
        dbUrl = System.getProperty("tw2odb.dbUrl", "remote:localhost/tweets");
        queries = System.getProperty("tw2odb.queries", "SELECT FROM Tweet WHERE text LUCENE 'cloud' ");

    }

    public void start() {
        log.info("connect to: " + dbUrl);
        final ODatabaseDocumentTx db = new ODatabaseDocumentTx(dbUrl)
                .open("admin", "admin");

        final OLiveResultListener listener = new MyOLiveResultListener(db.copy());

        tokens = Arrays.stream(queries.split(","))
                .peek(q -> log.info("registering live query:: {} ", q))
                .map(q -> "LIVE " + q)
                .map(q -> db.<List<ODocument>>query(new OLiveQuery<ODocument>(q, listener)).get(0))
                .map(d -> d.<Integer>field("token"))
                .peek(t -> log.info("registered token:: {}", t))
                .collect(toList());

    }

    public void stop() {
        final ODatabaseDocumentTx db = new ODatabaseDocumentTx(dbUrl)
                .open("admin", "admin");

        log.info("unsubscribe live queries");
        db.activateOnCurrentThread();
        tokens.stream()
                .peek(t -> log.info("unregistering token:: {}", t))
                .forEach(token -> db.command(new OCommandSQL("LIVE UNSUBSCRIBE " + token)).execute());

        db.close();
    }

    private static class MyOLiveResultListener implements OLiveResultListener {

        private final ODatabaseDocumentTx db;
        Meter fetched = TweetMetrics.METRICS.meter(name("live", "documents", "fetched"));

        public MyOLiveResultListener(ODatabaseDocumentTx db) {

            this.db = db;
        }

        public void onLiveResult(int token, ORecordOperation operation) {
            fetched.mark();

            db.activateOnCurrentThread();

            log.info("  token:: {} - content:: {} ", token, operation.record);
        }

        @Override
        public void onError(int token) {
            log.error("ERROR token = " + token);
        }

        @Override
        public void onUnsubscribe(int token) {
            log.info("UNSUBSCRIBE iLiveToken = " + token);
        }

    }

}

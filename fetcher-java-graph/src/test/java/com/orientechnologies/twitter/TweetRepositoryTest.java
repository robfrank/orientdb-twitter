package com.orientechnologies.twitter;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;
import com.tinkerpop.blueprints.impls.orient.OrientGraphNoTx;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import twitter4j.Status;
import twitter4j.TwitterObjectFactory;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by frank on 15/03/2016.
 */
public class TweetRepositoryTest {

    private TweetPersister repo;
    private OrientGraphFactory graphFactory;

    @Before
    public void setUp() throws Exception {
        graphFactory = new OrientGraphFactory("memory:tweets").setupPool(1, 10);
        TwitterDbUtils.createDb(graphFactory);
        repo = new TweetPersister(graphFactory);

    }

    @After
    public void tearDown() throws Exception {
        graphFactory.close();
        ODatabaseDocumentTx db = new ODatabaseDocumentTx(graphFactory.getDatabase().getURL());
        db.open("admin", "admin");
        db.drop();
    }


    @Test
    public void name() throws Exception {
        ODatabaseDocumentTx db = new ODatabaseDocumentTx(graphFactory.getDatabase().getURL());
        db.open("admin", "admin");

        List<ODocument> query = db.query(new OSQLSynchQuery<>("select count(*) from OUser"));

        System.out.println("" + query.get(0).field("count"));
    }

    @Test
    public void shouldMapStatusToDb() throws Exception {
        String jsonStatus = new String(Files.readAllBytes(Paths.get("./src/test/resources", "status_2.json")));

        Status status = TwitterObjectFactory.createStatus(jsonStatus);

        repo.persists(status);

        OrientGraphNoTx graph = graphFactory.getNoTx();

        assertThat(graph.countVertices("Tweet")).isEqualTo(1);
        assertThat(graph.countVertices("User")).isEqualTo(1);
        assertThat(graph.countVertices("Hashtag")).isEqualTo(1);
        assertThat(graph.countVertices("Source")).isEqualTo(1);

        assertThat(graph.countEdges("Posts")).isEqualTo(1);
        assertThat(graph.countEdges("Source")).isEqualTo(1);
        assertThat(graph.countEdges("Tags")).isEqualTo(1);
        assertThat(graph.countEdges("Using")).isEqualTo(1);

        graph.shutdown();

    }

    @Test
    public void shouldMapMentions() throws Exception {
        String jsonStatus = new String(Files.readAllBytes(Paths.get("./src/test/resources", "status_3.json")));

        Status status = TwitterObjectFactory.createStatus(jsonStatus);

        repo.persists(status);

        OrientGraphNoTx graph = graphFactory.getNoTx();

        assertThat(graph.countVertices("Tweet")).isEqualTo(1);

        //User mentioned isn't in the db, added bu storeMentions
        assertThat(graph.countVertices("User")).isEqualTo(2);

        assertThat(graph.countEdges("Mentions")).isEqualTo(1);

        graph.shutdown();

    }

    @Test
    public void shouldAvoidDuplicates() throws Exception {
        String jsonStatus = new String(Files.readAllBytes(Paths.get("./src/test/resources", "status_3.json")));

        Status status = TwitterObjectFactory.createStatus(jsonStatus);

        IntStream.range(0, 4)
                .forEach(i -> repo.persists(status));

        OrientGraphNoTx graph = graphFactory.getNoTx();

        assertThat(graph.countVertices("Tweet")).isEqualTo(1);

        //User mentioned isn't in the db, added bu storeMentions
        assertThat(graph.countVertices("User")).isEqualTo(2);

        assertThat(graph.countEdges("Mentions")).isEqualTo(1);

        graph.shutdown();

    }
}
package com.orientechnologies.twitter;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by frank on 13/07/2016.
 */
public class TwitterDbUtilsTest {

    @Rule
    public TestName testName = new TestName();

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();


    @Test
    public void shouldCreatePlocalDatabase() throws Exception {
        System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager");

        Configurator.setRootLevel(Level.INFO);


        String dbUrl = "plocal:" + folder.getRoot().getAbsolutePath();

        TwitterDbUtils.createDbIfNeeded(dbUrl);

        ODatabaseDocumentTx db = new ODatabaseDocumentTx(dbUrl).open("admin", "admin");

        assertThat(db.exists()).isTrue();

        assertThat(db.getMetadata().getSchema().getClass("Tweet")).isNotNull();


        db.drop();


    }
}
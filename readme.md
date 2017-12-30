### OrientDB Twitter demo

This is a set of projects used while presenting OrientDB capabilities, such as relationship management through graph data modelling and fulltext search index.

To run this application you need to provide Twitter's oauth tokens: https://dev.twitter.com/oauth/overview/application-owner-access-tokens

## Twitter fetcher 

The fetcher module is in charge to fetch data from the Twitter stream. 

**This project uses  [Lombok](https://projectlombok.org/). Setup your IDE .**

It uses [Twitter4j](http://twitter4j.org/) and [RxJava](https://github.com/ReactiveX/RxJava).

First, build it

```
mvn clean install
```

You can build a Docker container
```
mvn docker:build
```
# Launch with **plocal** (embedded) OrientDB

Plain java

```
java -Dtwitter4j.oauth.consumerKey=YOUR_CONSUMER_KEY\
    -Dtwitter4j.oauth.consumerSecret=YOUR_CONSUMET_SECRET \
    -Dtwitter4j.oauth.accessToken=YOUR_ACCES_TOKEN \
    -Dtwitter4j.oauth.accessTokenSecret=YOUR_ACCES_TOKEN_SECRET \
    -Dtw2odb.keywords="cloud,java,nosql,orientdb,graph" \
    -Dtw2odb.langs=en,it \
    -Dtw2odb.dbUrl=plocal:./tweets \
    -Dtw2odb.createDb=true \
    -jar target/twitter-fetcher.jar
```


# Launch with **remote** OrientDB server

Support for spatial indexes is needed to run the tweets fetcher and persister, so follow instructions on official documentation to install [spatial index support](http://orientdb.com/docs/2.2.x/Spatial-Index.html)
The process is able to create the database on remote server is provided with server's root credentials.

```
java -Dtwitter4j.oauth.consumerKey=YOUR_CONSUMER_KEY\
    -Dtwitter4j.oauth.consumerSecret=YOUR_CONSUMET_SECRET \
    -Dtwitter4j.oauth.accessToken=YOUR_ACCES_TOKEN \
    -Dtwitter4j.oauth.accessTokenSecret=YOUR_ACCES_TOKEN_SECRET \
    -Dtw2odb.keywords="cloud,java,nosql,orientdb,graph" \
    -Dtw2odb.langs=en,it \
    -Dtw2odb.dbUrl=remote:localhost/tweets \
    -Dtw2odb.createDb=true \
    -Dtw2odb.serverUsername=MY_SERVER_USERNAME \
    -Dtw2odb.serverPassword=MY_SERVER_PASSWORD \
    -jar target/twitter-fetcher.jar
```

After the first run, the database will be present, so switch off auto-creation setting **-Dtw2odb.createDb=false**  

Docker


```
docker run --name odb-fetcher -d --link orientdb:orientdb \
    orientdb/orientdb-twitter-fetcher-java-graph \
    java \
    -Dtwitter4j.oauth.consumerKey=YOUR_CONSUMER_KEY\
    -Dtwitter4j.oauth.consumerSecret=YOUR_CONSUMET_SECRET \
    -Dtwitter4j.oauth.accessToken=YOUR_ACCES_TOKEN \
    -Dtwitter4j.oauth.accessTokenSecret=YOUR_ACCES_TOKEN_SECRET \
    -Dtw2odb.keywords="cloud,java,nosql,orientdb,graph" 
    -Dtw2odb.keywords="openstack,paas,docker,container,neo4j,codemotionBLN,cloud,java,nosql,orientdb,graph" \
    -Dtw2odb.langs=en,it \
    -Dtw2odb.dbUrl=remote:orientdb/tweets \
    -Dtw2odb.createDb=true \
    -Dtw2odb.serverUsername=MY_SERVER_USERNAME \
    -Dtw2odb.serverPassword=MY_SERVER_PASSWORD \
    -jar orientdb-twitter-fetcher.jar

```


Properties

| name  | description|default | example| 
|---|---|---|---|
| tw2odb.username  | database's username|admin|YOUR_USERNAME |
| tw2odb.password  | database's password|admin|YOUR_PASSWORD |
| tw2odb.serverUsername  | server's username|root|YOUR_SERVER_ROOT_USERNAME |
| tw2odb.serverPassword  | server's password (provided at server startup)|empty|YOUR_SERVER_ROOT_PASSWORD |
| tw2odb.keywords  |comma separated list of keyword for filtering the stream|empty|cloud,java, |
| tw2odb.langs  |comma separated list of languages for filtering the stream|empty| en,it|
| tw2odb.dbUrl  |OrientDB url| plocal:./tweets|-Dtw2odb.dbUrl=plocal:/opt/orientdb/databases/tweets|
| tw2odb.createDb |create a new db| true|-Dtw2odb.createDb=true|

     








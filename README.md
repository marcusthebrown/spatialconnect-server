# spatialconnect-server

This server is the interface used to communicate with mobile clients
using the SpatialConnect libraries.  It's also the API that powers the
dashboard web application.

## Version
0.7

## Running with Docker

First you have to install [Docker](https://docs.docker.com/engine/installation/) for your local workstation.

### Building the spatialconnect-server container

You can build the spatialconnect-server Docker container like this

```
docker build -t boundlessgeo/spatialconnect-server .
```

Once you've built the container, you can push it to the Docker registry to
trigger a redeployment:

```
docker push boundlessgeo/spatialconnect-server
```

> Note that anytime the boundlessgeo/spatialconnect-server GitHub repo is
> updated, the container will be rebuilt and deployed to the appropriate
> environment.

### Local development environment setup

First startup the database container:

```
docker-compose up -d postgis
```

Then create the database and database user.  (You may need to wait a few seconds for the db container to fully start)

```
docker-compose run -e PGHOST=postgis -e PGUSER=postgres --rm postgis createuser spacon
docker-compose run -e PGHOST=postgis -e PGUSER=postgres --rm postgis createdb spacon -O spacon
```

Then add the required extensions to our database.

```
docker-compose run -e PGHOST=postgis -e PGUSER=postgres --rm postgis psql -d spacon -c "CREATE EXTENSION postgis;"
docker-compose run -e PGHOST=postgis -e PGUSER=postgres --rm postgis psql -d spacon -c "CREATE EXTENSION pgcrypto;"
```

Now run the migration.

> You will need to install [leiningen](http://leiningen.org/) to run the migration, if you haven't installed it yet.  On OSX, run `brew install leiningen`.

```
cd server/
lein migrate
```

Start the spatialconnect-server container (which also starts the mosquitto container)

```
cd ..
docker-compose up -d spatialconnect-server
# you can tail the logs to ensure everything worked as expected
docker-compose logs -f spatialconnect-server
```


To run the webapp for local development,

```
cd /path/to/spatialconnect-server/web
npm install
npm run start:local
```

The webpack-dev-server will host the app here http://localhost:8080 and rebuild
the JS when you make changes.

When you're done, you can shut all the containers down with

```
docker-compose stop
```

And if you want to remove all the containers, you can run

```
docker-compose rm -vf
```


To test the TLS configuration, you can use the `mosquitto_pub` command line
client.  Make sure you obtain a valid token by authenticating to the
spatialconnect-server container api first.  Also note that the password is
required even though it is not used.
```
mosquitto_pub -h <container hostname or ip> -p 8883 -t "test" -m "sample pub"  -u "valid jwt" -P "anypass" --cafile path/to/ca.crt --insecure -d
```


### deploying to cloud foundry

You can deploy Docker continers to Cloud Foundry using:

```
# use this to deploy mosquitto
cf push mosquitto -o boundlessgeo/mosquitto:devio

# use this to deploy spatialconnect-server
cf push spatialconnect-server -o boundlessgeo/spatialconnect-server:devio
```

Then you will need to bind services and set environment variables before
the service can run.  You can use the Pivotal Apps Manager web interface
or use the `cf` command line tool like this:

```
cf set-env spatialconnect-server MQTT_BROKER_URL=ssl://the.broker.hostname:8884
cf bind-service spatialconnect-server db-service-instance-name
```

> You may also need to give the server more memory with `cf scale
> spatialconnect-server -m 2G`

Once the server is setup, you'll need to run the migration which can be
done by running a command on the container:

```
cf push spatialconnect-server -c "lein migrate"
```


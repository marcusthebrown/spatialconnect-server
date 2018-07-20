(defproject spacon "0.10.0-SNAPSHOT"
  :description "SpatialConnect Server"
  :url "http://github.com/boundlessgeo/spatialconnect-server"
  :license {:name "Apache License, Version 2.0"
            :url "https://www.apache.org/licenses/LICENSE-2.0"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/core.async "0.4.474"]
                 [org.clojure/data.json "0.2.6"]
                 [io.pedestal/pedestal.service "0.5.4"]
                 [io.pedestal/pedestal.jetty "0.5.4"]
                 [ragtime "0.7.2"]
                 [yesql "0.5.3"]
                 [cljfmt "0.6.0"]
                 [org.postgresql/postgresql "42.2.4"]
                 [clojure.jdbc/clojure.jdbc-c3p0 "0.3.3"]
                 [listora/uuid "0.1.2"]
                 [ch.qos.logback/logback-classic "1.2.3"
                  :exclusions [org.slf4j/slf4j-api]]
                 [org.slf4j/jul-to-slf4j "1.7.25"]
                 [org.slf4j/jcl-over-slf4j "1.7.25"]
                 [org.slf4j/log4j-over-slf4j "1.7.25"]
                 [com.stuartsierra/component "0.3.2"]
                 [clojurewerkz/machine_head "1.0.0"]
                 [com.boundlessgeo/schema "0.17.1"]
                 ; todo: the protobuf dependency should be packaged into the schema artifact
                 [com.google.protobuf/protobuf-java "3.6.0"]
                 [buddy "2.0.0"]
                 [camel-snake-kebab "0.4.0"]
                 [org.clojars.diogok/cljts "0.5.2"]
                 [org.clojure/tools.nrepl "0.2.13"]
                 [overtone/at-at "1.2.0"]
                 [clj-http "3.9.0"]
                 [com.gfredericks/test.chuck "0.2.9"]
                 [com.draines/postal "2.0.2"]
                 [org.clojure/tools.logging "0.4.1"]
                 [funcool/cats "2.2.0"]
                 [org.apache.kafka/kafka-clients "1.1.1"]
                 [org.apache.kafka/kafka-streams "1.1.1"
                  :exclusions [org.slf4j/slf4j-log4j12]]
                 [clj-time "0.14.4"]
                 [ymilky/franzy "0.0.1"]
                 [clj-http "3.9.0"]]


  :repositories  [["osgeo" "https://download.osgeo.org/webdav/geotools/"]
                  ["boundlessgeo-releases" "https://repo.boundlessgeo.com/artifactory/release/"]
                  ["clojars" {:sign-releases false}]
                  ["confluent" {:url "https://packages.confluent.io/maven/"}]
                  ["project" "file:repo"]]
  :dev-dependencies [[lein-reload "1.0.0"]]

  :plugins [[lein-environ "1.1.0"]
            [lein-cljfmt "0.6.0"]
            ;[ragtime/ragtime.lein "0.3.6"]
            [lein-codox "0.10.4"]
            [lein-cloverage "1.0.9"]
            [lein-ancient "0.6.15"]]

  :aliases {"migrate" ["run" "-m" "spacon.db.conn/migrate"]
            "rollback" ["run" "-m" "spacon.db.conn/rollback"]
            "sampledata" ["run" "-m" "spacon.generate-data"]}

  :monkeypatch-clojure-test false
  :min-lein-version "2.0.0"
  :resource-paths ["config", "resources"]
  ;; If you use HTTP/2 or ALPN, use the java-agent to pull in the correct alpn-boot dependency
  ;:java-agents [[org.mortbay.jetty.alpn/jetty-alpn-agent "2.0.3"]]
  :profiles {:dev {:source-paths ["dev"]
                   :resource-paths ["config", "resources"]
                   :dependencies [[io.pedestal/pedestal.service-tools
                                   "0.5.4"]
                                  [jonase/eastwood "0.2.8" :exclusions
                                   [org.clojure/clojure]]
                                  [org.clojure/test.check "0.9.0"]]
                   :plugins [[test2junit "1.2.2"]
                             [lein-autoreload "0.1.1"]]}
             :uberjar {:aot :all
                       :dependencies [[org.clojure/test.check "0.9.0"]]}}
  :test2junit-output-dir "target/test-results"
  :test2junit-run-ant true
  :uberjar-name "spacon-server.jar"
  :main ^{:skip-aot true} spacon.server)

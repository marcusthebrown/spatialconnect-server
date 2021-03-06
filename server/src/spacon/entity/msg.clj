;; Copyright 2016-2017 Boundless, http://boundlessgeo.com
;;
;; Licensed under the Apache License, Version 2.0 (the "License");
;; you may not use this file except in compliance with the License.
;; You may obtain a copy of the License at
;;
;; http://www.apache.org/licenses/LICENSE-2.0
;;
;; Unless required by applicable law or agreed to in writing, software
;; distributed under the License is distributed on an "AS IS" BASIS,
;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;; See the License for the specific language governing permissions and
;; limitations under the License.

(ns spacon.entity.msg
  (:require [clojure.data.json :as json]
            [clojure.string :refer [blank?]]
            [clojure.walk :refer [keywordize-keys]]
            [clojure.tools.logging :as log])
  (:import (com.boundlessgeo.schema MessagePbf$Msg)))

(defn- bytes->map [proto]
  (try
    (let [msg (MessagePbf$Msg/parseFrom proto)
          p (.getPayload msg)
          payload (if (blank? p) {} (keywordize-keys (json/read-str p)))]
      {:context (.getContext msg)
       :correlationId (.getCorrelationId msg)
       :jwt (.getJwt msg)
       :to (.getTo msg)
       :action (.getAction msg)
       :payload payload})
    (catch Exception e
      (log/error e "Could not parse protobuf into map")
      nil)))

(defn- make-protobuf [context correlationId jwt to action payload]
  (-> (MessagePbf$Msg/newBuilder)
      (.setContext context)
      (.setTo to)
      (.setJwt jwt)
      (.setAction action)
      (.setPayload payload)
      (.setCorrelationId correlationId)
      (.build)))

(defrecord Msg [correlationId jwt to action payload])

(defn from-bytes
  "Deserializes the protobuf byte array into an ConnectMessage record"
  [b]
  (if-let [map (bytes->map b)]
    (map->Msg map)
    nil))

(defn message->bytes
  "Serializes an Msg record into a protobuf byte array"
  [message]
  (.toByteArray (make-protobuf
                 (or (get message :context) "")
                 (or (get message :correlationId) -1)
                 (or (get message :jwt) "")
                 (or (get message :to) "")
                 (or (get message :action) "v1/NO_ACTION")
                 (json/write-str (or (get message :payload) "{}")))))

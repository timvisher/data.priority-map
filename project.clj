(defproject data.priority-map "0.0.8-SNAPSHOT"
  :description "Priority Maps are maps from items to priorities"
  :url "https://github.com/clojure/data.priority-map"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :parent [org.clojure/pom.contrib "0.1.2"]
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.170"]]
  :plugins [[lein-cljsbuild "1.1.1"]]
  :source-paths ["src/main/clojure"]
  :cljsbuild
  {:builds [{:id "dev"
             :source-paths ["src/main/clojure"]
             :compiler {:output-to "out/main.js"
                        :output-dir "out"
                        :optimizations :simple
                        :pretty-print true}}]})

{:namespaces
 ({:source-url
   "https://github.com/clojure/data.priority-map/blob/12f77d6d35cd03e7f74ba6df8b21b0c081b756d0/src/main/clojure/clojure/data/priority_map.clj",
   :wiki-url
   "http://clojure.github.com/data.priority-map/clojure.data.priority-map-api.html",
   :name "clojure.data.priority-map",
   :author "Mark Engelberg",
   :doc
   "A priority map is very similar to a sorted map, but whereas a sorted map produces a\nsequence of the entries sorted by key, a priority map produces the entries sorted by value.\nIn addition to supporting all the functions a sorted map supports, a priority map\ncan also be thought of as a queue of [item priority] pairs.  To support usage as\na versatile priority queue, priority maps also support conj/peek/pop operations.\n\nThe standard way to construct a priority map is with priority-map:\nuser=> (def p (priority-map :a 2 :b 1 :c 3 :d 5 :e 4 :f 3))\n#'user/p\nuser=> p\n{:b 1, :a 2, :c 3, :f 3, :e 4, :d 5}\n\nSo :b has priority 1, :a has priority 2, and so on.\nNotice how the priority map prints in an order sorted by its priorities (i.e., the map's values)\n\nWe can use assoc to assign a priority to a new item:\nuser=> (assoc p :g 1)\n{:b 1, :g 1, :a 2, :c 3, :f 3, :e 4, :d 5}\n\nor to assign a new priority to an extant item:\nuser=> (assoc p :c 4)\n{:b 1, :a 2, :f 3, :c 4, :e 4, :d 5}\n\nWe can remove an item from the priority map:\nuser=> (dissoc p :e)\n{:b 1, :a 2, :c 3, :f 3, :d 5}\n\nAn alternative way to add to the priority map is to conj a [item priority] pair:\nuser=> (conj p [:g 0])\n{:g 0, :b 1, :a 2, :c 3, :f 3, :e 4, :d 5}\n\nor use into:\nuser=> (into p [[:g 0] [:h 1] [:i 2]])\n{:g 0, :b 1, :h 1, :a 2, :i 2, :c 3, :f 3, :e 4, :d 5}\n\nPriority maps are countable:\nuser=> (count p)\n6\n\nLike other maps, equivalence is based not on type, but on contents.\nIn other words, just as a sorted-map can be equal to a hash-map,\nso can a priority-map.\nuser=> (= p {:b 1, :a 2, :c 3, :f 3, :e 4, :d 5})\ntrue\n\nYou can test them for emptiness:\nuser=> (empty? (priority-map))\ntrue\nuser=> (empty? p)\nfalse\n\nYou can test whether an item is in the priority map:\nuser=> (contains? p :a)\ntrue\nuser=> (contains? p :g)\nfalse\n\nIt is easy to look up the priority of a given item, using any of the standard map mechanisms:\nuser=> (get p :a)\n2\nuser=> (get p :g 10)\n10\nuser=> (p :a)\n2\nuser=> (:a p)\n2\n\nPriority maps derive much of their utility by providing priority-based seq.\nNote that no guarantees are made about the order in which items of the same priority appear.\nuser=> (seq p)\n([:b 1] [:a 2] [:c 3] [:f 3] [:e 4] [:d 5])\nBecause no guarantees are made about the order of same-priority items, note that\nrseq might not be an exact reverse of the seq.  It is only guaranteed to be in\ndescending order.\nuser=> (rseq p)\n([:d 5] [:e 4] [:c 3] [:f 3] [:a 2] [:b 1])\n\nThis means first/rest/next/for/map/etc. all operate in priority order.\nuser=> (first p)\n[:b 1]\nuser=> (rest p)\n([:a 2] [:c 3] [:f 3] [:e 4] [:d 5])\n\nPriority maps support metadata:\nuser=> (meta (with-meta p {:extra :info}))\n{:extra :info}\n\nBut perhaps most importantly, priority maps can also function as priority queues.\npeek, like first, gives you the first [item priority] pair in the collection.\npop removes the first [item priority] from the collection.\n(Note that unlike rest, which returns a seq, pop returns a priority map).\n\nuser=> (peek p)\n[:b 1]\nuser=> (pop p)\n{:a 2, :c 3, :f 3, :e 4, :d 5}\n\nIt is also possible to use a custom comparator:\nuser=> (priority-map-by > :a 1 :b 2 :c 3)\n{:c 3, :b 2, :a 1}\n\nSometimes, it is desirable to have a map where the values contain more information\nthan just the priority.  For example, let's say you want a map like:\n{:a [2 :apple], :b [1 :banana], :c [3 :carrot]}\nand you want to sort the map by the numeric priority found in the pair.\n\nA common mistake is to try to solve this with a custom comparator:\n(priority-map \n  (fn [[priority1 _] [priority2 _]] (< priority1 priority2))\n  :a [2 :apple], :b [1 :banana], :c [3 :carrot])\n\nThis will not work!  In Clojure, like Java, all comparators must be total orders,\nmeaning that you can't have a tie unless the objects you are comparing are\nin fact equal.  The above comparator breaks that rule because\n[2 :apple] and [2 :apricot] tie, but are not equal.\n\nThe correct way to construct such a priority map is by specifying a keyfn, which is used\nto extract the true priority from the priority map's vals.  (Note: It might seem a little odd\nthat the priority-extraction function is called a *key*fn, even though it is applied to the\nmap's values.  This terminology is based on the docstring of clojure.core/sort-by, which\nuses `keyfn` for the function which extracts the sort order.) \n\nIn the above example,\n\nuser=> (priority-map-keyfn first :a [2 :apple], :b [1 :banana], :c [3 :carrot])\n{:b [1 :banana], :a [2 :apple], :c [3 :carrot]}\n\nYou can also combine a keyfn with a comparator that operates on the extracted priorities:\n\nuser=> (priority-map-keyfn-by \n          first >\n          :a [2 :apple], :b [1 :banana], :c [3 :carrot])\n{:c [3 :carrot], :a [2 :apple], :b [1 :banana]}\n\n \n\nAll of these operations are efficient.  Generally speaking, most operations\nare O(log n) where n is the number of distinct priorities.  Some operations\n(for example, straightforward lookup of an item's priority, or testing\nwhether a given item is in the priority map) are as efficient\nas Clojure's built-in map.\n\nThe key to this efficiency is that internally, not only does the priority map store\nan ordinary hash map of items to priority, but it also stores a sorted map that\nmaps priorities to sets of items with that priority.\n\nA typical textbook priority queue data structure supports at the ability to add\na [item priority] pair to the queue, and to pop/peek the next [item priority] pair.\nBut many real-world applications of priority queues require more features, such\nas the ability to test whether something is already in the queue, or to reassign\na priority.  For example, a standard formulation of Dijkstra's algorithm requires the\nability to reduce the priority number associated with a given item.  Once you\nthrow persistence into the mix with the desire to adjust priorities, the traditional\nstructures just don't work that well.\n\nThis particular blend of Clojure's built-in hash sets, hash maps, and sorted maps\nproved to be a great way to implement an especially flexible persistent priority queue.\n\nConnoisseurs of algorithms will note that this structure's peek operation is not O(1) as\nit would be if based upon a heap data structure, but I feel this is a small concession for\nthe blend of persistence, priority reassignment, and priority-sorted seq, which can be\nquite expensive to achieve with a heap (I did actually try this for comparison).  Furthermore,\nthis peek's logarithmic behavior is quite good (on my computer I can do a million\npeeks at a priority map with a million items in 750ms).  Also, consider that peek and pop\nusually follow one another, and even with a heap, pop is logarithmic.  So the net combination\nof peek and pop is not much different between this versatile formulation of a priority map and\na more limited heap-based one.  In a nutshell, peek, although not O(1), is unlikely to be the\nbottleneck in your program.\n\nAll in all, I hope you will find priority maps to be an easy-to-use and useful addition\nto Clojure's assortment of built-in maps (hash-map and sorted-map)."}),
 :vars
 ({:arglists ([priority->set-of-items item->priority _meta keyfn]),
   :name "->PersistentPriorityMap",
   :namespace "clojure.data.priority-map",
   :source-url
   "https://github.com/clojure/data.priority-map/blob/12f77d6d35cd03e7f74ba6df8b21b0c081b756d0/src/main/clojure/clojure/data/priority_map.clj#L204",
   :raw-source-url
   "https://github.com/clojure/data.priority-map/raw/12f77d6d35cd03e7f74ba6df8b21b0c081b756d0/src/main/clojure/clojure/data/priority_map.clj",
   :wiki-url
   "http://clojure.github.com/data.priority-map//clojure.data.priority-map-api.html#clojure.data.priority-map/->PersistentPriorityMap",
   :doc
   "Positional factory function for class clojure.data.priority_map.PersistentPriorityMap.",
   :var-type "function",
   :line 204,
   :file "src/main/clojure/clojure/data/priority_map.clj"}
  {:arglists ([& keyvals]),
   :name "priority-map",
   :namespace "clojure.data.priority-map",
   :source-url
   "https://github.com/clojure/data.priority-map/blob/12f77d6d35cd03e7f74ba6df8b21b0c081b756d0/src/main/clojure/clojure/data/priority_map.clj#L389",
   :raw-source-url
   "https://github.com/clojure/data.priority-map/raw/12f77d6d35cd03e7f74ba6df8b21b0c081b756d0/src/main/clojure/clojure/data/priority_map.clj",
   :wiki-url
   "http://clojure.github.com/data.priority-map//clojure.data.priority-map-api.html#clojure.data.priority-map/priority-map",
   :doc
   "Usage: (priority-map key val key val ...)\nReturns a new priority map with optional supplied mappings.\n(priority-map) returns an empty priority map.",
   :var-type "function",
   :line 389,
   :file "src/main/clojure/clojure/data/priority_map.clj"}
  {:arglists ([comparator & keyvals]),
   :name "priority-map-by",
   :namespace "clojure.data.priority-map",
   :source-url
   "https://github.com/clojure/data.priority-map/blob/12f77d6d35cd03e7f74ba6df8b21b0c081b756d0/src/main/clojure/clojure/data/priority_map.clj#L397",
   :raw-source-url
   "https://github.com/clojure/data.priority-map/raw/12f77d6d35cd03e7f74ba6df8b21b0c081b756d0/src/main/clojure/clojure/data/priority_map.clj",
   :wiki-url
   "http://clojure.github.com/data.priority-map//clojure.data.priority-map-api.html#clojure.data.priority-map/priority-map-by",
   :doc
   "Usage: (priority-map comparator key val key val ...)\nReturns a new priority map with custom comparator and optional supplied mappings.\n(priority-map-by comparator) yields an empty priority map with custom comparator.",
   :var-type "function",
   :line 397,
   :file "src/main/clojure/clojure/data/priority_map.clj"}
  {:arglists ([keyfn & keyvals]),
   :name "priority-map-keyfn",
   :namespace "clojure.data.priority-map",
   :source-url
   "https://github.com/clojure/data.priority-map/blob/12f77d6d35cd03e7f74ba6df8b21b0c081b756d0/src/main/clojure/clojure/data/priority_map.clj#L405",
   :raw-source-url
   "https://github.com/clojure/data.priority-map/raw/12f77d6d35cd03e7f74ba6df8b21b0c081b756d0/src/main/clojure/clojure/data/priority_map.clj",
   :wiki-url
   "http://clojure.github.com/data.priority-map//clojure.data.priority-map-api.html#clojure.data.priority-map/priority-map-keyfn",
   :doc
   "Usage: (priority-map-keyfn keyfn key val key val ...)\nReturns a new priority map with custom keyfn and optional supplied mappings.\nThe priority is determined by comparing (keyfn val). \n(priority-map-keyfn keyfn) yields an empty priority map with custom keyfn.",
   :var-type "function",
   :line 405,
   :file "src/main/clojure/clojure/data/priority_map.clj"}
  {:arglists ([keyfn comparator & keyvals]),
   :name "priority-map-keyfn-by",
   :namespace "clojure.data.priority-map",
   :source-url
   "https://github.com/clojure/data.priority-map/blob/12f77d6d35cd03e7f74ba6df8b21b0c081b756d0/src/main/clojure/clojure/data/priority_map.clj#L414",
   :raw-source-url
   "https://github.com/clojure/data.priority-map/raw/12f77d6d35cd03e7f74ba6df8b21b0c081b756d0/src/main/clojure/clojure/data/priority_map.clj",
   :wiki-url
   "http://clojure.github.com/data.priority-map//clojure.data.priority-map-api.html#clojure.data.priority-map/priority-map-keyfn-by",
   :doc
   "Usage: (priority-map-keyfn-by keyfn comparator key val key val ...)\nReturns a new priority map with custom keyfn, custom comparator, and optional supplied mappings.\nThe priority is determined by comparing (keyfn val).\n(priority-map-keyfn-by keyfn comparator) yields an empty priority map with custom keyfn and comparator.",
   :var-type "function",
   :line 414,
   :file "src/main/clojure/clojure/data/priority_map.clj"}
  {:file nil,
   :raw-source-url nil,
   :source-url nil,
   :wiki-url
   "http://clojure.github.com/data.priority-map//clojure.data.priority-map-api.html#clojure.data.priority-map/PersistentPriorityMap",
   :namespace "clojure.data.priority-map",
   :var-type "type",
   :name "PersistentPriorityMap"})}

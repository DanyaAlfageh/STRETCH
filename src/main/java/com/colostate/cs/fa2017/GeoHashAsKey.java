package com.colostate.cs.fa2017;

import org.apache.ignite.*;
import ch.hsr.geohash.GeoHash;
import org.apache.ignite.cache.CacheEntry;
import org.apache.ignite.cache.CacheMetrics;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.cache.CachePeekMode;
import org.apache.ignite.cache.affinity.Affinity;
import org.apache.ignite.cache.affinity.AffinityFunction;
import org.apache.ignite.cache.affinity.AffinityKey;
import org.apache.ignite.cache.affinity.AffinityKeyMapped;
import org.apache.ignite.cache.eviction.EvictableEntry;
import org.apache.ignite.cache.eviction.EvictionPolicy;
import org.apache.ignite.cache.eviction.lru.LruEvictionPolicy;
import org.apache.ignite.cluster.ClusterGroup;
import org.apache.ignite.cluster.ClusterMetrics;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.DataStorageConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.internal.processors.cache.GridCacheDefaultAffinityKeyMapper;
import org.apache.ignite.internal.processors.cache.persistence.evict.Random2LruPageEvictionTracker;
import org.apache.ignite.lang.IgniteRunnable;
import org.jetbrains.annotations.Nullable;


import java.io.*;
import java.lang.reflect.Array;
import java.util.*;

public class GeoHashAsKey {

    private static final String cacheName = "MyCache";



    public static void main(String[] args) {






        CacheConfiguration cacheCfg = new CacheConfiguration("MyCache");
        cacheCfg.setCacheMode(CacheMode.PARTITIONED);

        //Map<String, String> userAtt = new HashMap<String, String>(){{put("group","A");}};
        //userAtt.put("group", "A");


        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setCacheConfiguration(cacheCfg);
        cfg.setClientMode(true);

        //CacheConfiguration<GeoEntry, String>  cacheCfg1 = new CacheConfiguration(cacheName);
        //cacheCfg1.setEvictionPolicy(new BibekEvictionPolicy());

        // Changing total RAM size to be used by Ignite Node.
        DataStorageConfiguration storageCfg = new DataStorageConfiguration();

        // Setting the size of the default memory region to 40MB to achieve this.
        storageCfg.getDefaultDataRegionConfiguration().setMaxSize(
                50L * 1024* 1024 );

        cfg.setDataStorageConfiguration(storageCfg);

        // Start Ignite node.
        Ignite ignite = Ignition.start(cfg);
        //Ignite ignite = Ignition.start("/s/chopin/b/grad/bbkstha/Softwares/apache-ignite-fabric-2.5.0-bin/examples/config/example-data-regions.xml");

//        ClusterGroup cluster = ignite.cluster();
//
//        ClusterGroup clusterGroupA = cluster.forAttribute("group", "A");
//        ClusterGroup clusterGroupB = cluster.forAttribute("group", "B");
//        ClusterGroup clusterGroupC = cluster.forAttribute("group", "C");
//
//        ClusterNode groupAMaster = clusterGroupA.forAttribute("role", "master").node();
//        ClusterGroup groupAWorkers = clusterGroupB.forAttribute("role", "worker");
//        ClusterNode groupBMaster = clusterGroupA.forAttribute("role", "master").node();
//        ClusterGroup groupBWorkers = clusterGroupB.forAttribute("role", "worker");
//
//        ClusterGroup groupOfMasters = cluster.forAttribute("role", "master");
//
//        System.out.println("The number of masters: "+groupOfMasters.nodes().size());
//
//
//
//
//        Collection<ClusterNode> workers = clusterGroupA.nodes();
//        System.out.println("The Number of nodes in Group A is: "+workers.size());
//
////        UUID masterAID = groupAMaster.id();
////        UUID masterBID = groupBMaster.id();
////        UUID masterCID = groupBMaster.id();
//
//
//
//        IgniteMessaging groupMastersMessage = ignite.message(cluster.forClients());
//
//
//
//        IgniteMessaging messagingGroupA = ignite.message(clusterGroupA);
//        IgniteMessaging messagingGroupB = ignite.message(clusterGroupB);






        try (IgniteCache<Object, String> cache = ignite.getOrCreateCache(cacheName)) {
//                // Clear caches before running example.
                cache.clear();
//                Affinity<Object> affinity = ignite.affinity(cacheName);
//
//                List<Object> geoHashArray = new ArrayList<>();
//
//                PrintWriter writer1 = new PrintWriter("/s/chopin/b/grad/bbkstha/Desktop/GeospatialSample/observation/KeyToPartitionMap_RAffninty.txt", "UTF-8");
//                HashMap<Integer, ArrayList<GeoEntry>> partIdtoKey = new HashMap();
//
                FileInputStream fstream = new FileInputStream("/s/chopin/b/grad/bbkstha/Desktop/GeospatialSample/ds9.entity.csv"); //us/or/portland_metro.csv");
                BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
                String strLine;
                Integer counter = 0;
                Collection<String> keys = new ArrayList<>(12447);

                Affinity<Object> affinity = ignite.affinity(cacheName);


            int duplicate = 0;
                //Read File Line By Line
                while ((strLine = br.readLine()) != null){
                    // Print the content on the console
                    //System.out.println(strLine);

                    if (!strLine.startsWith("LON")){



                        String lat = strLine.split(",")[0];
                        String lon = strLine.split(",")[1];
                        GeoEntry geoEntry =  new GeoEntry(lon, lat, 2);

                        System.out.println("Geohash values: "+geoEntry.getGeoHash());

                        GridCacheDefaultAffinityKeyMapper cacheAffinityKeyMapper = new GridCacheDefaultAffinityKeyMapper();
                        Object affKey = cacheAffinityKeyMapper.affinityKey(geoEntry);
                        System.out.println("The corresponding aff key is: "+affKey);





                        //String geohashString = GeoHash.withCharacterPrecision(Double.parseDouble(lat), -Double.parseDouble(lon), 12).toBase32();
                        //System.out.println(strLine);
                       // System.out.println(geohashString);
                        //String affkey = geohashString.substring(0,6);

                        //Object newKey = new AffinityKey<>(geohashString,affkey);

                        //geoHashArray.add(newKey);



//                        if(cache.containsKey(newKey)){
//                           duplicate++;
//                        }



                        //keys.add(newKey.geoHash);

                        cache.put(geoEntry, strLine);

                        System.out.println("The corresponding partition ID for key is: "+affinity.partition(geoEntry));
                        System.out.println("The node is: "+affinity.mapPartitionToNode(affinity.partition(geoEntry)).id());

//                        String aKey =  affinity.affinityKey(newKey).toString();
//
//                        System.out.println("The geoHash is: "+aKey);
//
//
//
//
//                        System.out.println("The affKey for KEY: "+newKey+" is: "+aKey.toString());
//                        int partID = affinity.partition(newKey);
//                        System.out.println("The partitionID for KEY: "+newKey+" is: "+partID);
//
//                        if (partIdtoKey.containsKey(partID)) {
//                                ArrayList<GeoEntry> tmpArrayList = partIdtoKey.get(partID);
//                                tmpArrayList.add(newKey);
//                                partIdtoKey.put(partID, tmpArrayList);
//                        } else {
//                                ArrayList<GeoEntry> tmpArrayList = new ArrayList<>();
//                                tmpArrayList.add(newKey);
//                                partIdtoKey.put(partID, tmpArrayList);
//                        }
//
//                        System.out.println("The elements in partIdtoKey map with key "+partID+ "is: "+partIdtoKey.get(partID));

                        //visitUsingAffinityRun(geohashString.substring(0,7).hashCode());
                        //String value = cache.get(key);

                        counter++;
                         //System.out.println(counter+". The value of key:"+key+" is: "+value);
                    }
                }
                System.out.println("Counter: "+counter);
                //visitUsingMapKeysToNodes(counter, keys);

//                ClusterMetrics gA = clusterGroupA.metrics();
//                ClusterMetrics gB = clusterGroupB.metrics();
//            System.out.println("Heap memory used in A: "+gA.getHeapMemoryUsed());
//            System.out.println("Heap memory used in B: "+gB.getHeapMemoryUsed());

//                System.out.println("The size of hashmap is: "+partIdtoKey.size());
//                System.out.println("The counter is: "+counter);
//                ArrayList<Object> emptyList = new ArrayList<>(Arrays.asList("No-Key"));
//
//                int numberOfkeys=0;
//                for(int i =0 ; i<2048; i++) { //hardcoded partitionsize defined in xml
//                    //ArrayList<Object> values = (partIdtoKey.get(i)!=null) ? partIdtoKey.get(i) : emptyList;
//                    //System.out.print("The PartitionId is: "+i+ " and keys are: ");
//                    //System.out.println(i + "=>" + values.size());
//                    if(partIdtoKey.containsKey(i)){
//                        ArrayList<GeoEntry> values = partIdtoKey.get(i);
//                        numberOfkeys+=values.size();
//                        writer1.println("PartitionId: " + i + " maps: "+values.size()+"keys & || Keys are: ");
//
//                        for (int j = 0; j < values.size(); j++) {
//                            //System.out.print(values.get(j)+", ");
//                            writer1.print(values.get(j) + ", ");
//                            writer1.println("\n");
//                        }
//
//
//                        writer1.println("-----------------------------------------------------");
//                    }
//                }
//                writer1.close();
//
//
//
//
//
//
//
//
//
//                System.out.println("The number of duplicate keys: "+duplicate);
//                System.out.println("The numberOfkeys is: "+numberOfkeys);
//
//
//
//
//
//                // Building a list of all partitions numbers.
//                List<Integer> rndParts = new ArrayList<>(10);
//                for (int i = 0; i < affinity.partitions(); i++)
//                    rndParts.add(i);
//                // Getting partition to node mapping.
//                Map<Integer, ClusterNode> partPerNodes = affinity.mapPartitionsToNodes(rndParts);
//                System.out.println("The partition->node map gives: "+partPerNodes.size());
//
//                PrintWriter writer = new PrintWriter("/s/chopin/b/grad/bbkstha/Desktop/GeospatialSample/observation/PartitionToNodeMap_RAffinity.txt", "UTF-8");
//                for (Map.Entry<Integer, ClusterNode> entry : partPerNodes.entrySet()){
//                    writer.println("Partition Number: " + entry.getKey() + ", Node: " + entry.getValue()+"\n");
//                }
//                writer.close();
//
//                int primaryCacheSize = cache.size(CachePeekMode.PRIMARY);
//                System.out.println("The size of primary cache entities is: "+primaryCacheSize);
////
////                //for(int i =0; i<1024; i++)
////                    //System.out.println("Partition: "+i+". NumberofEntries: "+cache.sizeLong(i, CachePeekMode.PRIMARY));
////
////                PrintWriter writer1 = new PrintWriter("/s/chopin/b/grad/bbkstha/Desktop/GeospatialSample/observation/KeyToPartitionMap_RAffninty.txt", "UTF-8");
////                HashMap<Integer, ArrayList<Object>> partIdtoKey = new HashMap();
////
////                        for(Object eachGeoHash: geoHashArray){
////
////                            Integer partId = affinity.partition(eachGeoHash);
////                            if (partIdtoKey.containsKey(partId)) {
////                                ArrayList<Object> tmpArrayList = partIdtoKey.get(partId);
////                                tmpArrayList.add(eachGeoHash);
////                                partIdtoKey.put(partId, tmpArrayList);
////                            } else {
////                                ArrayList<Object> tmpArrayList = new ArrayList<>();
////                                tmpArrayList.add(eachGeoHash);
////                                partIdtoKey.put(partId, tmpArrayList);
////                            }
////                        }
////
////
//                int arraySize =  geoHashArray.size();
//                Random rand = new Random();
//                int randomInt = rand.nextInt();
//                int tmpIndex = randomInt > 0 ? randomInt % arraySize : randomInt *-1 % arraySize;
//                int index = tmpIndex > arraySize/2 ? tmpIndex /2 : tmpIndex;
//                for(int i=0; i< 3; i++) {
//                    cache.get(geoHashArray.get(i));
//                }
//
//                CacheMetrics stat = cache.metrics();
//
//
//
//
//                long getOffHeapCount = stat.getOffHeapGets();
//                long putOffHeapCount = stat.getOffHeapPuts();
//                long offheapHits = stat.getOffHeapHits();
//                long offheapMisses = stat.getOffHeapMisses();
//                float offheapHitPer = stat.getOffHeapHitPercentage();
//                float offheapMissPer = stat.getOffHeapMissPercentage();
//                float avgGetTime = stat.getAverageGetTime();
//                float avgPutTime = stat.getAveragePutTime();
//                long getOffHeapEvic = stat.getOffHeapEvictions();
//                long getCacheEvic = stat.getCacheEvictions();
//                long getRebalancePartitions = stat.getRebalancingPartitionsCount();
//
//                long getOffHeapAllocatedSize = stat.getOffHeapAllocatedSize();
//
//                long getHeapEntriesCount = stat.getHeapEntriesCount();
//                long getOffHeapPrimaryEntriesCount = stat.getOffHeapPrimaryEntriesCount();
//                long offheapCount = stat.getOffHeapEntriesCount();
//                long getOffHeapEntriesCount = stat.getOffHeapEntriesCount();
//
//
//
//                //local node metrices only.
//                long getCount = stat.getCacheGets();
//                long hits = stat.getCacheHits();
//                long misses = stat.getCacheMisses();
//                long backupcount = stat.getOffHeapBackupEntriesCount();
//                long putCount = stat.getCachePuts();
//                int getSize = stat.getSize();
//                int getKeySize = stat.getKeySize();
//
//
//
//
//                System.out.println("The stats are: ");
//
//
//                System.out.println("The getOffHeapCount is: "+getOffHeapCount);
//                System.out.println("The putOffHeapCount is: "+putOffHeapCount);
//                System.out.println("The offheapHits is: "+offheapHits);
//                System.out.println("The offheapMisses is: "+offheapMisses);
//                System.out.println("The offheapHitPer is: "+offheapHitPer);
//                System.out.println("The offheapMissPer is: "+offheapMissPer);
//
//                System.out.println("The hits is: "+hits);
//                System.out.println("The misses is: "+misses);
//                System.out.println("The backupcount is: "+backupcount);
//                System.out.println("The getCount is: "+getCount);
//                System.out.println("The putCount is: "+putCount);
//
//                System.out.println("The offheapCount is: "+offheapCount);
//                System.out.println("The getHeapEntriesCount is: "+getHeapEntriesCount);
//                System.out.println("The getOffHeapEntriesCount is: "+getOffHeapEntriesCount);
//                System.out.println("The getOffHeapPrimaryEntriesCount is: "+getOffHeapPrimaryEntriesCount);
//
//                System.out.println("The avgGetTime is: "+avgGetTime);
//                System.out.println("The avgPutTime is: "+avgPutTime);
//                System.out.println("The getOffHeapEvic is: "+getOffHeapEvic);
//                System.out.println("The getCacheEvic is: "+getCacheEvic);
//                System.out.println("The getRebalancePartitions is: "+getRebalancePartitions);
//
//                System.out.println("The getOffHeapAllocatedSize is: "+getOffHeapAllocatedSize);
//
//                //local cache mertrices only
//                System.out.println("------------------------");
//                System.out.println("THE LOCAL CACHE METRICES");
//                System.out.println("The getSize is: "+getSize);
//                System.out.println("The getKeySize is: "+getKeySize);
//
//
//                //br.close();
//               System.out.println("Finished.");
//
//
//               // ignite.close();

           // cache.destroy();
//
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
            e.printStackTrace();
        }


    }

    private static void visitUsingMapKeysToNodes(int KEY_CNT, Collection<String> keys) {
        final Ignite ignite = Ignition.ignite();




        // Map all keys to nodes.
        Map<ClusterNode, Collection<String>> mappings = ignite.<String>affinity(cacheName).mapKeysToNodes(keys);

        for (Map.Entry<ClusterNode, Collection<String>> mapping : mappings.entrySet()) {
            ClusterNode node = mapping.getKey();

            System.out.println("The cluster node id is: "+node.id());


            final Collection<String> mappedKeys = mapping.getValue();

            if (node != null) {
                // Bring computations to the nodes where the data resides (i.e. collocation).
                ignite.compute(ignite.cluster().forNode(node)).run(() -> {
                    IgniteCache<Integer, String> cache = ignite.cache(cacheName);

                    // Peek is a local memory lookup, however, value should never be 'null'
                    // as we are co-located with node that has a given key.
                    for (String key : mappedKeys)
                        System.out.println("Co-located using mapKeysToNodes [key= " + key);
                });
            }
        }
    }


    private static void visitUsingAffinityRun(final int key) {
        Ignite ignite = Ignition.ignite();

        final IgniteCache<Integer, String> cache = ignite.cache("SpatialDataCache");

//        for (int i = 0; i < KEY_CNT; i++) {
//            final int key = i;

            // This runnable will execute on the remote node where
            // data with the given key is located. Since it will be co-located
            // we can use local 'peek' operation safely.
            ignite.compute().affinityRun("SpatialDataCache", key, new IgniteRunnable() {
                @Override public void run() {
                    // Peek is a local memory lookup, however, value should never be 'null'
                    // as we are co-located with node that has a given key.
                    System.out.println("Co-located using affinityRun [key= " + key +
                            ", value=" + cache.localPeek(key) + ']');
                }
            });
        }


    public static class GeoEntry implements Serializable {

        private String geoHash;

        @AffinityKeyMapped
        private String subGeoHash;

        private GeoEntry(String lon, String lat, int upperRange) {

            this.geoHash = GeoHash.withCharacterPrecision(Double.parseDouble(lat), -Double.parseDouble(lon), 12).toBase32();
            this.subGeoHash = this.geoHash.substring(0,upperRange);

        }

        private String getGeoHash () {
            return this.geoHash;
        }

        private String getSubGeoHash() {
            return subGeoHash;
        }

        /** {@inheritDoc} */
        @Override public String toString() {
            return "GeoHash is: " + geoHash + " & subGeoHash is: "+subGeoHash;
        }

    }


    public static class BibekEvictionPolicy implements EvictionPolicy<Object, String>, Serializable {


        @Override
        public void onEntryAccessed(boolean rmv, EvictableEntry<Object, String> entry) {

        }
    }
}

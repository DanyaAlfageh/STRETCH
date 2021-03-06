package edu.colostate.cs.fa2017.stretch.query;

import edu.colostate.cs.fa2017.stretch.affinity.StretchAffinityFunction;
import edu.colostate.cs.fa2017.stretch.client.DataLoader;
import edu.colostate.cs.fa2017.stretch.util.GeoHashProcessor.Coordinates;
import edu.colostate.cs.fa2017.stretch.util.GeoHashProcessor.Point;
import edu.colostate.cs.fa2017.stretch.util.GeoHashProcessor.SpatialRange;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.cache.CachePeekMode;
import org.apache.ignite.cache.CacheRebalanceMode;
import org.apache.ignite.cache.affinity.Affinity;
import org.apache.ignite.cache.query.ScanQuery;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.DataStorageConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.lang.IgniteClosure;

import javax.cache.Cache;
import java.awt.*;
import java.awt.geom.Area;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.*;
import java.util.List;

public class PolygonQuery {

private static final String cacheName = "STRETCH-CACHE";
    public static int iteration = 1;

    public final static byte BITS_PER_CHAR = 5;
    public final static int MAX_PRECISION = 30;
    public final static int LATITUDE_RANGE = 90;
    public final static int LONGITUDE_RANGE = 180;


    public final static char[] charMap = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'b', 'c', 'd', 'e', 'f',
            'g', 'h', 'j', 'k', 'm', 'n', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z' };


    public final static HashMap<Character, Integer> charLookupTable = new HashMap<Character, Integer>();

    /**
     * Initialize HashMap for character to integer lookups.
     */
    static {
        for (int i = 0; i < charMap.length; ++i) {
            charLookupTable.put(charMap[i], i);
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException, IgniteCheckedException {


        if(args.length == 1){

            iteration = Integer.parseInt(args[0]);
        }


        IgniteConfiguration igniteConfiguration = new IgniteConfiguration();
        igniteConfiguration.setMetricsUpdateFrequency(2000);
        igniteConfiguration.setMetricsLogFrequency(60000);

        // Changing total RAM size to be used by Ignite Node.
        DataStorageConfiguration storageCfg = new DataStorageConfiguration();
        //cfg.setDataStorageConfiguration(storageCfg);
        storageCfg.setSystemRegionInitialSize(15L * 1024 * 1024);
        storageCfg.setSystemRegionMaxSize(45L * 1024 * 1024);
        storageCfg.setMetricsEnabled(true);

        // Applying the new configuration.
        igniteConfiguration.setDataStorageConfiguration(storageCfg);

        CacheConfiguration cacheConfiguration = new CacheConfiguration();
        cacheConfiguration.setManagementEnabled(true);
        cacheConfiguration.setStatisticsEnabled(true);
        cacheConfiguration.setName(cacheName);
        cacheConfiguration.setOnheapCacheEnabled(false);

        igniteConfiguration.setCacheConfiguration(cacheConfiguration);
        igniteConfiguration.setRebalanceThreadPoolSize(4);
        cacheConfiguration.setCacheMode(CacheMode.PARTITIONED);

        StretchAffinityFunction stretchAffinityFunction = new StretchAffinityFunction(false, 2048);
        cacheConfiguration.setAffinity(stretchAffinityFunction);
        cacheConfiguration.setRebalanceMode(CacheRebalanceMode.SYNC);
        cacheConfiguration.setStatisticsEnabled(true);
        //cacheConfiguration.setDataRegionName("default");

        Map<String, String> userAtt = new HashMap<String, String>() {{
            put("group","query");
            put("role", "client");
            put("donated","no");
            put("region-max", "100");
            put("split","no");

        }};
        igniteConfiguration.setUserAttributes(userAtt);
        igniteConfiguration.setClientMode(true);
        // Start Ignite node.
        Ignite ignite = Ignition.start(igniteConfiguration);
        ArrayList<Coordinates> queryPolygon = new ArrayList<>();
        queryPolygon.add(new Coordinates((float) 44.06, (float)-101.05));
        queryPolygon.add(new Coordinates((float) 44.06, (float)-112.06));
        queryPolygon.add(new Coordinates((float) 34.08, (float)-112.06));
        queryPolygon.add(new Coordinates((float) 34.08, (float)-101.05));
        queryPolygon.add(new Coordinates((float) 44.06, (float)-101.05));



        //Continent
        /*queryPolygon.add(new Coordinates((float) 25.00792604653878, (float)-118.30654310431964));
        queryPolygon.add(new Coordinates((float) 51.45535004292666, (float)-64.07802747931964));
        queryPolygon.add(new Coordinates((float) 63.03606146020872, (float)-135.7088188176205));
        queryPolygon.add(new Coordinates((float) 25.00792604653878, (float)-118.30654310431964));
*/




        long start = System.currentTimeMillis();

        Affinity affinity = ignite.affinity(cacheName);
        Map<ClusterNode, List<Integer>> intersectingPartitions = new HashMap<>();
        Map<ClusterNode, List<Integer>> enclosedPartitions = new HashMap<>();

        Map<String, Integer> keyToPartitionMap = getKeyToPartitionMap();

        for(Map.Entry<String, Integer> entry: keyToPartitionMap.entrySet()){

            ArrayList<Coordinates> geoHashToCoordinates = getCoordiantesArray(entry.getKey());
            ClusterNode node = affinity.mapPartitionToNode(entry.getValue());
            int checker = checkIntersection(queryPolygon, geoHashToCoordinates);
            if(checker == 1){
                /*System.out.println("Intersection found.");
                System.out.println("The geohash is: "+entry.getKey()+" and partition is: "+entry.getValue());
                System.out.println("The associated Node is "+node.id());*/

                if(intersectingPartitions.containsKey(node)){
                    //System.out.println("Adding");
                    List<Integer> tmpList = intersectingPartitions.get(node);
                    tmpList.add(entry.getValue());
                    intersectingPartitions.replace(node, tmpList);
                }
                else {
                    //System.out.println("New");
                    List<Integer> tmpList = new ArrayList<>();
                    tmpList.add(entry.getValue());
                    intersectingPartitions.put(node, tmpList);
                }
            }
            else if(checker == 0){
                //System.out.println("--------------------------");
                //System.out.println("Enclosure found"+entry.getKey());

                if(enclosedPartitions.containsKey(node)){
                    //System.out.println("Adding1");
                    List<Integer> tmpList = enclosedPartitions.get(node);
                    tmpList.add(entry.getValue());
                    enclosedPartitions.replace(node, tmpList);
                }
                else {
                    //System.out.println("New1");
                    List<Integer> tmpList = new ArrayList<>();
                    tmpList.add(entry.getValue());
                    enclosedPartitions.put(node, tmpList);
                }
            }
        }

        //System.out.println("The intersecting map size: "+intersectingPartitions.size());
        //System.out.println("The enclosed map size: "+enclosedPartitions.size());

        /*for(Map.Entry<ClusterNode, List<Integer>> entry : intersectingPartitions.entrySet()){
            for(int j=0; j< entry.getValue().size(); j++)
                System.out.println("The elementent in the list are: "+entry.getValue().get(j));
        }
        for(Map.Entry<ClusterNode, List<Integer>> entry : enclosedPartitions.entrySet()){
            for(int j=0; j< entry.getValue().size(); j++)
                System.out.println("The elementent in the enclo list are: "+entry.getValue().get(j));
        }*/


        Polygon queryPolygonToBeSent = new Polygon();
        for (Coordinates coords : queryPolygon) {
            edu.colostate.cs.fa2017.stretch.util.GeoHashProcessor.Point<Integer> point = coordinatesToXY(coords);
            queryPolygonToBeSent.addPoint(point.X(), point.Y());
        }

        Coordinates sampleCoordiantes = new Coordinates((float) 46.89900470407791, (float)-139.2253230941643);
        edu.colostate.cs.fa2017.stretch.util.GeoHashProcessor.Point<Integer> point = coordinatesToXY(sampleCoordiantes);
        //System.out.println("The point lies within the polygon: "+queryPolygonToBeSent.contains(point.X(), point.Y()));


        List<List<Cache.Entry>> allReturnedKV = new ArrayList<>();
        long totalElementsWithinPolygon = 0;

        System.out.println(iteration);
        //Running the query multiple times
        for(long i = 0; i < iteration; i++) {

            for (Map.Entry<ClusterNode, List<Integer>> ent : intersectingPartitions.entrySet()) {
                List<Integer> lst = ent.getValue();
                List<Cache.Entry> tmpList = ignite.compute(ignite.cluster().forNode(ent.getKey())).apply(
                        new IgniteClosure<List<Integer>, List<Cache.Entry>>() {
                            @Override
                            public List<Cache.Entry> apply(List<Integer> partitionList) {
                                IgniteCache<DataLoader.GeoEntry, String> localCache = ignite.cache(cacheName);
                                //System.out.println("Intersecting partition size:"+partitionList.size());
                                List<Cache.Entry> returningKVList = new ArrayList<>();
                                long sum = 0;
                                for (int index = 0; index < partitionList.size(); index++) {
                                    ScanQuery scanQuery = new ScanQuery();
                                    //System.out.println("Inside Scan query "+partitionList.get(index));
                                    scanQuery.setPartition(partitionList.get(index));
                                    // Execute the query.
                                    Iterator<Cache.Entry<DataLoader.GeoEntry, String>> iterator1 = localCache.query(scanQuery).iterator();
                                    //System.out.println("The iterator size: "+iterator1.hasNext());
                                    while (iterator1.hasNext()) {
                                        //System.out.println("Containment test.");
                                        Cache.Entry<DataLoader.GeoEntry, String> element = iterator1.next();
                                        Coordinates tmpCoordiantes = new Coordinates(Float.parseFloat(element.getValue().split(",")[0]), Float.parseFloat(element.getValue().split(",")[1]));
                                        edu.colostate.cs.fa2017.stretch.util.GeoHashProcessor.Point<Integer> point = coordinatesToXY(tmpCoordiantes);
                                        boolean isPointInside = queryPolygonToBeSent.contains(point.X(), point.Y());
                                        //System.out.println("The point lies within the polygon: "+isPointInside);

                                        if (isPointInside) {
                                            returningKVList.add(element);
                                            sum++;
                                            //System.out.println("The intermediate sum: "+sum);
                                        }
                                    }
                                }
                                System.out.println("The intersecting partitions sum: " + sum);
                                return returningKVList;
                            }
                        },
                        lst
                );
                totalElementsWithinPolygon+=tmpList.size();
                allReturnedKV.add(tmpList);
            }

            for (Map.Entry<ClusterNode, List<Integer>> ent1 : enclosedPartitions.entrySet()) {
                List<Integer> lst1 = ent1.getValue();
                List<Cache.Entry> tmpList= ignite.compute(ignite.cluster().forNode(ent1.getKey())).apply(
                        new IgniteClosure<List<Integer>, List<Cache.Entry>>() {
                            @Override
                            public List<Cache.Entry> apply(List<Integer> partitionList) {

                                Map<DataLoader.GeoEntry, String> returningKVMap = new HashMap<>();
                                List<Cache.Entry> returningList = new ArrayList<>();
                                IgniteCache<DataLoader.GeoEntry, String> localCache = ignite.cache(cacheName);
                                long sum = 0;
                                for (int i = 0; i < partitionList.size(); i++) {

                                    ScanQuery scanQuery = new ScanQuery();
                                    //System.out.println("Inside Scan query "+partitionList.get(index));
                                    scanQuery.setPartition(partitionList.get(i));
                                    // Execute the query.
                                    Iterator<Cache.Entry<DataLoader.GeoEntry, String>> iterator1 = localCache.query(scanQuery).iterator();
                                    while(iterator1.hasNext()){
                                        //Cache.Entry<DataLoader.GeoEntry, String> element = iterator1.next();
                                        returningList.add(iterator1.next());
                                    }
                                    //System.out.println(partitionList.get(i));
                                    sum += ignite.cache(cacheName).localSizeLong(partitionList.get(i), CachePeekMode.OFFHEAP);
                                    //System.out.println("The partition to key count:");
                                }
                                System.out.println("The enclosed partitions sum:" + sum);
                                return returningList;
                            }
                        },
                        lst1
                );
                totalElementsWithinPolygon+=tmpList.size();
                allReturnedKV.add(tmpList);
            }

        }

        System.out.println("The total Elements within the given polygon is: "+totalElementsWithinPolygon);
        long end = System.currentTimeMillis();


        System.out.println("The time taken is: "+(end-start)+" millisec");
    }

    public static ArrayList<Coordinates> getCoordiantesArray(String geoHash) {

        SpatialRange range1 = decodeHash(geoHash);

        Coordinates c1 = new Coordinates(range1.getLowerBoundForLatitude(), range1.getLowerBoundForLongitude());
        Coordinates c2 = new Coordinates(range1.getUpperBoundForLatitude(), range1.getLowerBoundForLongitude());
        Coordinates c3 = new Coordinates(range1.getUpperBoundForLatitude(), range1.getUpperBoundForLongitude());
        Coordinates c4 = new Coordinates(range1.getLowerBoundForLatitude(), range1.getUpperBoundForLongitude());

        ArrayList<Coordinates> cs1 = new ArrayList<Coordinates>();
        cs1.add(c1);cs1.add(c2);cs1.add(c3);cs1.add(c4);

        return cs1;
    }

    public static SpatialRange decodeHash(String geoHash) {
        ArrayList<Boolean> bits = getBits(geoHash);

        float[] longitude = decodeBits(bits, false);
        float[] latitude = decodeBits(bits, true);

        return new SpatialRange(latitude[0], latitude[1], longitude[0], longitude[1]);
    }

    private static ArrayList<Boolean> getBits(String hash) {
        hash = hash.toLowerCase();

        /* Create an array of bits, 5 bits per character: */
        ArrayList<Boolean> bits = new ArrayList<Boolean>(hash.length() * BITS_PER_CHAR);

        /* Loop through the hash string, setting appropriate bits. */
        for (int i = 0; i < hash.length(); ++i) {
            int charValue = charLookupTable.get(hash.charAt(i));

            /* Set bit from charValue, then shift over to the next bit. */
            for (int j = 0; j < BITS_PER_CHAR; ++j, charValue <<= 1) {
                bits.add((charValue & 0x10) == 0x10);
            }
        }
        return bits;
    }

    private static float[] decodeBits(ArrayList<Boolean> bits, boolean latitude) {
        float low, high, middle;
        int offset;

        if (latitude) {
            offset = 1;
            low = -90.0f;
            high = 90.0f;
        } else {
            offset = 0;
            low = -180.0f;
            high = 180.0f;
        }

        for (int i = offset; i < bits.size(); i += 2) {
            middle = (high + low) / 2;

            if (bits.get(i)) {
                low = middle;
            } else {
                high = middle;
            }
        }

        if (latitude) {
            return new float[] { low, high };
        } else {
            return new float[] { low, high };
        }
    }

    public static int checkIntersection(List<Coordinates> polygon, List<Coordinates> flank) {
        Polygon queryPolygon = new Polygon();
        for (Coordinates coords : polygon) {
            edu.colostate.cs.fa2017.stretch.util.GeoHashProcessor.Point<Integer> point = coordinatesToXY(coords);
            queryPolygon.addPoint(point.X(), point.Y());
        }


        Polygon geoHashToPolygon = new Polygon();
        for (Coordinates coords : flank) {
            Point<Integer> point = coordinatesToXY(coords);
            geoHashToPolygon.addPoint(point.X(), point.Y());
        }

        Area queryPolygonArea = new Area(queryPolygon);
        Area geoHashToPolygonArea = new Area(geoHashToPolygon);
        Area smallerPolygonArea = new Area(geoHashToPolygon);

        queryPolygonArea.intersect(geoHashToPolygonArea);

        if(queryPolygonArea.isEmpty()) {
            return -1;
        } else {
            if(queryPolygonArea.equals(smallerPolygonArea)){
             return 0;
            }else {
                return 1;
            }
         //return true;
        }
    }


    public static boolean checkEnclosure(List<Coordinates> polygon, List<Coordinates> flank) {
		Polygon geometry = new Polygon();
		for (Coordinates coords : polygon) {
			Point<Integer> point = coordinatesToXY(coords);
			geometry.addPoint(point.X(), point.Y());
		}

		Polygon geometrySmaller = new Polygon();
		for (Coordinates coords : flank) {
			Point<Integer> point = coordinatesToXY(coords);
			geometrySmaller.addPoint(point.X(), point.Y());
		}
		Area aPrev = new Area(geometrySmaller);
		Area a1 = new Area(geometry);
		Area a2 = new Area(geometrySmaller);

		a1.intersect(a2);

		if(a1.equals(aPrev)) {

			return true;
		} else {
			return false;
		}

	}

    public static Point<Integer> coordinatesToXY(Coordinates coords) {
        int width = 1 << MAX_PRECISION;
        float xDiff = coords.getLongitude() + 180;
        float yDiff = 90 - coords.getLatitude();
        int x = (int) (xDiff * width / 360);
        int y = (int) (yDiff * width / 180);
        return new Point<>(x, y);
    }


    public static Map<String, Integer> getKeyToPartitionMap(){
        Map<String, Integer> map = new HashMap<>();
        try
        {
            FileInputStream fis = new FileInputStream("/s/chopin/b/grad/bbkstha/Softwares/apache-ignite-2.7.0-bin/STRETCH/KeyToPartitionMap-X.ser");
            ObjectInputStream ois = new ObjectInputStream(fis);
            map = (HashMap<String, Integer>) ois.readObject();
            ois.close();
            fis.close();
        }catch(IOException ioe)
        {
            ioe.printStackTrace();

        }catch(ClassNotFoundException c)
        {
            System.out.println("Class not found");
            c.printStackTrace();
        }
        return map;
    }

    public static String[] getNeighbours(String geoHash) {
        String[] neighbors = new String[8];
        if (geoHash == null || geoHash.trim().length() == 0)
            throw new IllegalArgumentException("Invalid Geohash");
        geoHash = geoHash.trim();
        int precision = geoHash.length();
        SpatialRange boundingBox = decodeHash(geoHash);
        Coordinates centroid = boundingBox.getCenterPoint();
        float widthDiff = boundingBox.getUpperBoundForLongitude() - centroid.getLongitude();
        float heightDiff = boundingBox.getUpperBoundForLatitude() - centroid.getLatitude();
        neighbors[0] = encode(boundingBox.getUpperBoundForLatitude() + heightDiff,
                boundingBox.getLowerBoundForLongitude() - widthDiff, precision);
        neighbors[1] = encode(boundingBox.getUpperBoundForLatitude() + heightDiff, centroid.getLongitude(), precision);
        neighbors[2] = encode(boundingBox.getUpperBoundForLatitude() + heightDiff,
                boundingBox.getUpperBoundForLongitude() + widthDiff, precision);
        neighbors[3] = encode(centroid.getLatitude(), boundingBox.getLowerBoundForLongitude() - widthDiff, precision);
        neighbors[4] = encode(centroid.getLatitude(), boundingBox.getUpperBoundForLongitude() + widthDiff, precision);
        neighbors[5] = encode(boundingBox.getLowerBoundForLatitude() - heightDiff,
                boundingBox.getLowerBoundForLongitude() - widthDiff, precision);
        neighbors[6] = encode(boundingBox.getLowerBoundForLatitude() - heightDiff, centroid.getLongitude(), precision);
        neighbors[7] = encode(boundingBox.getLowerBoundForLatitude() - heightDiff,
                boundingBox.getUpperBoundForLongitude() + widthDiff, precision);
        return neighbors;
    }
    public static String encode(float latitude, float longitude, int precision) {
        while (latitude < -90f || latitude > 90f)
            latitude = latitude < -90f ? 180.0f + latitude : latitude > 90f ? -180f + latitude : latitude;
        while (longitude < -180f || longitude > 180f)
            longitude = longitude < -180f ? 360.0f + longitude : longitude > 180f ? -360f + longitude : longitude;
        /*
         * Set up 2-element arrays for longitude and latitude that we can flip
         * between while encoding
         */
        float[] high = new float[2];
        float[] low = new float[2];
        float[] value = new float[2];

        high[0] = LONGITUDE_RANGE;
        high[1] = LATITUDE_RANGE;
        low[0] = -LONGITUDE_RANGE;
        low[1] = -LATITUDE_RANGE;
        value[0] = longitude;
        value[1] = latitude;

        String hash = "";

        for (int p = 0; p < precision; ++p) {

            float middle = 0.0f;
            int charBits = 0;
            for (int b = 0; b < BITS_PER_CHAR; ++b) {
                int bit = (p * BITS_PER_CHAR) + b;

                charBits <<= 1;

                middle = (high[bit % 2] + low[bit % 2]) / 2;
                if (value[bit % 2] > middle) {
                    charBits |= 1;
                    low[bit % 2] = middle;
                } else {
                    high[bit % 2] = middle;
                }
            }

            hash += charMap[charBits];
        }

        return hash;
    }




}

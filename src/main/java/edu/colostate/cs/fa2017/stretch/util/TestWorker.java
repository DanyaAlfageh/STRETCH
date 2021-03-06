package edu.colostate.cs.fa2017.stretch.util;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;

import java.util.HashMap;
import java.util.Map;

public class TestWorker {

    public static void main(String[] args) {

        IgniteConfiguration cfg = new IgniteConfiguration();

        CacheConfiguration cacheCfg = new CacheConfiguration();

        cacheCfg.setName("TEST_CACHE");
        cacheCfg.setStatisticsEnabled(true);
        cfg.setCacheConfiguration(cacheCfg);
        Map<String, String> userAtt = new HashMap<String, String>() {{
            put("group", "TEST");
            put("role", "worker");
            put("donated", "no");
        }};
        cfg.setUserAttributes(userAtt);



        // Start Ignite node.
        Ignite ignite = Ignition.start(cfg);

        IgniteCache<Integer, Integer> cache = ignite.getOrCreateCache(cacheCfg);







    }
}

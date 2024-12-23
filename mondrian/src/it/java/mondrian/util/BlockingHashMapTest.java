/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 by Hitachi Vantara, LLC : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2029-07-20
 ******************************************************************************/


package mondrian.util;

import junit.framework.TestCase;
import mondrian.olap.Util;

import java.util.Random;
import java.util.concurrent.*;

/**
 * Testcase for {@link BlockingHashMap}.
 *
 * @author mcampbell
 */
public class BlockingHashMapTest extends TestCase {

    private final Random random = Util.createRandom(-1);
    private static final int SLEEP_TIME = 100;

    /**
     * Validates values put in the BlockingHashMap by one thread
     * can be correctly retrieved by another thread.
     * Also verifies get operations can happen concurrently, in
     * that total time to get all values synchronously would (on average) be
     * 50 milliseconds * 100 Getters, and the test will fail if duration
     * is greater than 2 seconds.
     *
     */
    public void testBlockingHashMap() throws InterruptedException {
        BlockingHashMap<Integer, Integer> map =
            new BlockingHashMap<Integer, Integer>(100);

        ExecutorService exec = Executors.newFixedThreadPool(20);
        try {
            for (int i = 0; i < 100; i++) {
                exec.submit(new Puter(i, i, map));
                exec.submit(new Getter(i, map));
            }
        } finally {
            exec.shutdown();
            boolean finished = exec.awaitTermination(
                2, TimeUnit.SECONDS);
            assertTrue(finished);
        }
    }


    private class Puter implements Runnable {
        private final Integer key;
        private final Integer value;
        private final BlockingHashMap<Integer, Integer> map;

        public Puter(
            Integer key, Integer value,
            BlockingHashMap<Integer, Integer> response)
        {
            this.key = key;
            this.value = value;
            this.map = response;
        }

        public void run() {
            try {
                Thread.sleep(random.nextInt(SLEEP_TIME));
                map.put(key, value);
                // System.out.println("putting key: " + key);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private class Getter implements Runnable {
        private final BlockingHashMap<Integer, Integer> map;
        private final Integer key;

        public Getter(Integer key, BlockingHashMap<Integer, Integer> map) {
            this.key = key;
            this.map = map;
        }

        public void run() {
            try {
                Thread.sleep(random.nextInt(SLEEP_TIME));
                Integer val = map.get(key);
                // System.out.println("getting key: " + key);
                assertEquals(key, val);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

// End BlockingHashMapTest.java

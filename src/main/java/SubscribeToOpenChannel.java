import com.satori.rtm.*;
import com.satori.rtm.model.*;
import com.sun.org.apache.xpath.internal.SourceTree;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class SubscribeToOpenChannel {
    static final String endpoint = "wss://open-data.api.satori.com";
    static final String appkey = "783ecdCcb8c5f9E66A56cBFeeeB672C3";
    static final String channel = "github-events";
    static long firstT = -1;
    static boolean myflag = true;
    static BlockingQueue<AnyJson> buffer1 = new LinkedBlockingQueue<AnyJson>();
    static BlockingQueue<snapshot> buffer2 = new LinkedBlockingQueue<snapshot>();
    static Map<String, Integer> DevsMap = new HashMap<String, Integer>();
    static Map<String, Integer> RepMap = new HashMap<String, Integer>();
    static JsonToSnapshot a = new JsonToSnapshot("1");
    static SnapshotToP b = new SnapshotToP("2");

    public static void main(String[] args) throws InterruptedException {
        final RtmClient client = new RtmClientBuilder(endpoint, appkey)
                .setListener(new RtmClientAdapter() {
                    @Override
                    public void onEnterConnected(RtmClient client) {
                        System.out.println("Connected to Satori RTM!");
                        firstT = System.currentTimeMillis();
                    }
                })
                .build();
        SubscriptionAdapter listener = new SubscriptionAdapter() {
            @Override
            public void onSubscriptionData(SubscriptionData data) {
                //System.out.println(System.currentTimeMillis());
                for (AnyJson json : data.getMessages()) {
                    buffer1.add(json);
                    //System.out.println(json.toString());
                }
                if (System.currentTimeMillis() - firstT > 20000) {
                    client.shutdown();
                    while (buffer1.size() != 0 || buffer2.size() != 0) {

                    }
                    int max = 0;
                    myflag = false;
                    System.out.println(DevsMap.size());
                    for (String id : DevsMap.keySet()) {
                        int a = DevsMap.get(id);
                        if (max < a) {
                            max = a;
                        }
                    }
                    System.out.println(max);
                }
            }
        };
        client.createSubscription(channel, SubscriptionMode.SIMPLE, listener);
        client.start();
        a.start();
        b.start();
    }

    static class JsonToSnapshot implements Runnable {
        private Thread t;
        private String threadName;

        JsonToSnapshot(String name) {
            threadName = name;
            System.out.println("Creating " + threadName);
        }

        public void run() {
            System.out.println("Running " + threadName);
            try {
                while (myflag || buffer1.size() != 0) {
                    AnyJson tmp = buffer1.take();
                    snapshot tmp2 = tmp.convertToType(snapshot.class);
                    buffer2.put(tmp2);
                    System.out.println("$");
                }
                System.out.println(myflag + " " + buffer1.size());
            } catch (InterruptedException e) {
                System.out.println("JsonToSnapshot Interrupted");
            }
            System.out.println("Thread " + threadName + " exiting.");
        }

        public Thread getT() {
            return t;
        }

        public void start() {
            System.out.println("Starting " + threadName);
            if (t == null) {
                t = new Thread(this, threadName);
                t.start();

            }
        }
    }

    static class SnapshotToP implements Runnable {
        private Thread t;
        private String threadName;

        SnapshotToP(String name) {
            threadName = name;
            System.out.println("Creating " + threadName);
        }

        public void run() {
            System.out.println("Running " + threadName);
            try {
                while (myflag || buffer2.size() != 0) {
                    snapshot tmp = buffer2.take();
                    if (DevsMap.containsKey(tmp.actor.id)) {
                        int val = DevsMap.get(tmp.actor.id) + 1;
                        DevsMap.put(tmp.actor.id, val);
                    } else {
                        DevsMap.put(tmp.actor.id, 1);
                    }
                    if (RepMap.containsKey(tmp.repo.id)) {
                        int val2 = RepMap.get(tmp.repo.id) + 1;
                        RepMap.put(tmp.repo.id, val2);
                    } else {
                        RepMap.put(tmp.repo.id, 1);
                    }
                    System.out.println("#");
                }
                System.out.println("#" + myflag + " " + buffer2.size());
            } catch (InterruptedException e) {
                System.out.println("Thread " + threadName + " interrupted.");
            }
            System.out.println("Thread " + threadName + " exiting.");
        }

        public Thread getT() {
            return t;
        }

        public void start() {
            System.out.println("Starting " + threadName);
            if (t == null) {
                t = new Thread(this, threadName);
                t.start();

            }
        }
    }

}

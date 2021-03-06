import com.satori.rtm.*;
import com.satori.rtm.model.*;
import com.sun.org.apache.xpath.internal.SourceTree;

import java.io.IOException;
import java.io.PrintWriter;
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
    static JsonToSnapshot a = new JsonToSnapshot("thread 1");
    static SnapshotToP b = new SnapshotToP("thread 2");
    static int events1=0;
    static int events2=0;
    public static void main(String[] args) throws InterruptedException {
        Scanner scanner=new Scanner(System.in);
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
                    events1++;
                    //System.out.println(json.toString());
                }
            }
        };
        int tmp=0;
        client.createSubscription(channel, SubscriptionMode.SIMPLE, listener);
        client.start();
        a.start();
        b.start();
        while (true) {
            myflag=false;
            scanner.next();
            int time=(int)((System.currentTimeMillis()-firstT)/1000) ;
            myflag=true;
            int max[] = {0,0,0,0,0,0,0,0,0,0,0};
            String mid[]={"","","","","","","","","","",""};
            System.out.println("Numbers Of Events:" +(events1)+'\n');
            System.out.println("\n in "+DevsMap.size()+" Devlopers :\n");
            for (String id : DevsMap.keySet()) {
                int a = DevsMap.get(id);
                max[10]=a;
                mid[10]=id;
                for(int i=0;i<11;i++) {
                    for(int j=0;j<10-i;j++)
                    {
                        if(max[j]<max[j+1])
                        {
                            int tm=max[j];
                            max[j]=max[j+1];
                            max[j+1]=tm;
                            String tm2=mid[j];
                            mid[j]=mid[j+1];
                            mid[j+1]=tm2;
                        }
                    }
                }
            }
            try{
                PrintWriter writer = new PrintWriter(String.format("Devs %d.txt",time ), "UTF-8");
                writer.println(DevsMap.size()+" Devlopers :\n");
                for(int i=0;i<10;i++)
                {
                    writer.println("id: "+mid[i]+" events: "+max[i]);
                    System.out.println("id: "+mid[i]+" events: "+max[i]);
                }
                writer.close();
            } catch (IOException e) {
                System.out.println("can not open or write file!");
            }

            int max2[] = {0,0,0,0,0,0,0,0,0,0,0};
            String mid2[]={"","","","","","","","","","",""};
            System.out.println("\nin "+RepMap.size()+" Repositories :\n");
            for (String id : RepMap.keySet()) {
                int a = RepMap.get(id);
                max2[10]=a;
                mid2[10]=id;
                for(int i=0;i<11;i++) {
                    for(int j=0;j<10-i;j++)
                    {
                        if(max2[j]<max2[j+1])
                        {
                            int tm=max2[j];
                            max2[j]=max2[j+1];
                            max2[j+1]=tm;
                            String tm2=mid2[j];
                            mid2[j]=mid2[j+1];
                            mid2[j+1]=tm2;
                        }
                    }
                }
            }
            try{
                PrintWriter writer = new PrintWriter(String.format("Reps%d.txt",time ), "UTF-8");
                writer.println("\n"+RepMap.size()+" Repositories :\n");
                for(int i=0;i<10;i++)
                {
                    writer.println("id: "+mid2[i]+" events: "+max2[i]);
                    System.out.println("id: "+mid2[i]+" events: "+max2[i]);
                }
                writer.close();
            } catch (IOException e) {
                System.out.println("can not open or write file!");
            }
            tmp++;
        }
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
                while (true) {
                    if(buffer1.isEmpty())
                        continue;;
                    AnyJson tmp = buffer1.take();
                    snapshot tmp2 = tmp.convertToType(snapshot.class);
                    buffer2.put(tmp2);
                }
            } catch (InterruptedException e) {
                System.out.println("JsonToSnapshot Interrupted");
            }
            System.out.println(threadName + " exiting.");
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
                while (true) {
                    if(buffer2.isEmpty() ||myflag)
                        continue;
                    snapshot tmp = buffer2.take();
                    events2++;
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
                }
            } catch (InterruptedException e) {
                System.out.println("Thread " + threadName + " interrupted.");
            }
            System.out.println(threadName + " exiting.");
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

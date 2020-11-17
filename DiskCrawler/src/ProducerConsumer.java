
import java.io.File;
import java.io.FileFilter;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ProducerConsumer
 * <p/>
 * Producer and consumer tasks in a desktop search application
 *
 * @author Brian Goetz and Tim Peierls
 */
public class ProducerConsumer {
    static class FileCrawler implements Runnable {
        private final BlockingQueue<File> fileQueue;
        private final FileFilter fileFilter;
        private final File root;
        private static final Object lock = new Object();
        //producer
        public FileCrawler(BlockingQueue<File> fileQueue,
                           final FileFilter fileFilter,
                           File root) {
            this.fileQueue = fileQueue;
            this.root = root;
            this.fileFilter = new FileFilter() {
                public boolean accept(File f) {
                    return f.isDirectory() || fileFilter.accept(f);
                }
            };
        }

        private boolean alreadyIndexed(File f) {
            return false;
        }

        public void run() {
            try {
                while(true){
                    synchronized (this){
                        if(fileQueue.size() == BOUND){
                            wait();
                        }

                        notify();
                        crawl(root);


                    }


                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        private void crawl(File root) throws InterruptedException {
            File[] entries = root.listFiles(fileFilter);
            if (entries != null) {
                for (File entry : entries)
                    if (entry.isDirectory())
                        crawl(entry);
                    else if (!alreadyIndexed(entry))
                        fileQueue.put(entry);
            }
        }
    }

    //consumer
    public static class Indexer implements Runnable {
        private final BlockingQueue<File> queue;
        private static volatile int counter;
        private static final Object lock = new Object();
        private int id;
        private static int currentTurn = 0;

        public Indexer(BlockingQueue<File> queue) {
            this.queue = queue;
            this.id = id;
        }
        public Indexer(BlockingQueue<File> queue, int id) {
            this.queue = queue;
            this.id = id;
        }

        public void run() {
            try {
                while (true){
                    synchronized (lock){
                        File file = queue.peek();
                        if(file == null){
                            lock.wait();
                        }
                        else{
                            lock.notify();
                            indexFile(queue.take());
                        }



                    }


                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        public void indexFile(File file) {
            // Index the file...
            System.out.println(file.getPath());
            System.out.println(counter);
            counter++;
        };

    }

    private static final int BOUND = 10;
    private static final int N_CONSUMERS = Runtime.getRuntime().availableProcessors();

    public static void startIndexing(File[] roots) {
        BlockingQueue<File> queue = new LinkedBlockingQueue<File>(BOUND);
        FileFilter filter = new FileFilter() {
            public boolean accept(File file) {
                return true;
            }
        };

        for (File root : roots)
            new Thread(new FileCrawler(queue, filter, root)).start();

        for (int i = 0; i < N_CONSUMERS; i++)
            new Thread(new Indexer(queue, i)).start();
    }

    public static void main(String[] args) {
        ;
        File[] directories = new File("C:\\Users\\Beau\\Desktop\\test10183").listFiles();
        startIndexing(directories);
    }
}
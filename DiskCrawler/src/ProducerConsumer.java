
import java.io.File;
import java.io.FileFilter;
import java.util.HashMap;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ProducerConsumer
 * <p/>
 * Producer and consumer tasks in a desktop search application
 *
 * @author Brian Goetz and Tim Peierls @betterAuthor Beau Cranston 000397019
 */
public class ProducerConsumer {
    /**
     * a static volitile boolean that determines if the program is finished crawling. It us volitile so that all ofthe consumer threads know right away that theres
     * nothing left to consume and therefore terminate
      */
    private static volatile boolean done = false;
    //a static volitile counter so that the counter can increment correctly despite multiple threads writing to it
    private static volatile int counter = 0;
    //A DEBUG TOGGLER NICE! You taught me this and i think it's a smart way of keeping things clean instead of commenting out outputs, so thank you
    private static boolean debug = false;
    private static ConcurrentHashMap<String, File> filesFound = new ConcurrentHashMap<>();
    static class FileCrawler implements Runnable {
        private final BlockingQueue<File> fileQueue;
        private final FileFilter fileFilter;
        private final File root;

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
    //checks if the file has already been indexed
        private boolean alreadyIndexed(File f) {
            if(filesFound.containsKey(f.getAbsolutePath())){
                return true;
            }
            else{
                return false;
            }

        }

        public void run() {
            try {
                //crawl now returns true when it is done, when the crawling is done, the done boolean should be true to notify the consumers that they can terminate
                done = crawl(root);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

        }

        /**
         * a recursive method to crawl through all of the files. It will take the file if it passes the filter check, and the file has already been indexed.
         * The crawl file returns true when the recursive call is done AKA the crawler has finished finding the files.
         * @param root
         * @return
         * @throws InterruptedException
         */
        private boolean crawl(File root) throws InterruptedException {
            File[] entries = root.listFiles(fileFilter);
            if (entries != null) {
                for (File entry : entries)
                    if (entry.isDirectory())
                        crawl(entry);
                    else if (!alreadyIndexed(entry)){
                        //add file to hashmap
                        if(debug == true){
                            System.out.println(entry.getAbsolutePath());
                        }

                        filesFound.put(entry.getName(), entry);
                        fileQueue.put(entry);
                    }


            }
            return true;
        }
    }

    static class Indexer implements Runnable {
        private final BlockingQueue<File> queue;
        //static final object so that all consumer threads have visibility of the lock and the lock cannot be modified
        private static final Object lock = new Object();
        public Indexer(BlockingQueue<File> queue) {
            this.queue = queue;
        }

        public void run() {
            try {
                //only loop while the disk crawler has not finished crawling
                while (!done){
                    //synchronize on the static final lcok object so that multuple consumers are not trying to access the queue at the same time;
                    synchronized (lock){
                        //if the crawler has not finished, take the top item in the queue
                        if(!done){
                            if(debug == true){
                                System.out.println("Take");
                            }
                            indexFile(queue.take());
                        }
                    }
                }
                if(debug == true){
                    System.out.println("Terminating");
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            //check if the thread is alive after execution, will return true but this is because it is just about to die off. Kind of useless code here.
            finally {
                if(debug == true){
                    System.out.println(Thread.currentThread().isAlive());
                }

            }
        }

        /**
         * prints the file path and the value of the counter and then increments the static volatile counter
         *
         */
        public void indexFile(File file) {
            // Index the file...
            //see the file that is being indexed
            System.out.println(file.getPath());
            //print the value of the counter
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
            new Thread(new Indexer(queue)).start();


    }

    public static void main(String[] args) {

        File[] directories = {new File("C:\\Users\\Beau\\Desktop\\test10183")};
        startIndexing(directories);
        //hold this thread up until the program finishes
        while(!done){

        }
        System.out.println("There are " + counter + " files in this directory!");

    }
}
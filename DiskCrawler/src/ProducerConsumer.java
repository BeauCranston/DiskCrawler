
import javafx.collections.ObservableList;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ProducerConsumer
 * <p/>
 * Producer and consumer tasks in a desktop search application
 *
 * @author Brian Goetz and Tim Peierls
 *
 * @betterAuthor Beau Cranston 000397019 , certify that this material is my
 *  * original work. No other person's work has been used without due
 *  * acknowledgement and I have not made my work available to anyone else.
 *
 */
public class ProducerConsumer {
    /**
     * a static volitile boolean that determines if the program is finished crawling. It us volitile so that all ofthe consumer threads know right away that theres
     * nothing left to consume and therefore terminate
      */
    private static volatile boolean producersDone = false;
    //a static volitile counter so that the counter can increment correctly despite multiple threads writing to it
    private static volatile int counter = 0;
    private static AtomicInteger currentCrawlCount = new AtomicInteger();
    //A DEBUG TOGGLER NICE! You taught me this and i think it's a smart way of keeping things clean instead of commenting out outputs, so thank you
    private static boolean debug = true;
    private static ConcurrentHashMap<String, File> filesFound = new ConcurrentHashMap<>();
    private static final int BOUND = 10;
    //the number of consumers to start with
    private static final int N_CONSUMERS = Runtime.getRuntime().availableProcessors();
    //instantiate thread pools
    private static ExecutorService producerService = Executors.newScheduledThreadPool(N_CONSUMERS);
    private static ExecutorService consumerService = Executors.newScheduledThreadPool(N_CONSUMERS);
    //string build to return the files paths ot the text area in the GUI
    public static StringBuilder matches;
    //volatile int to return to the gui for the files found count
    public static volatile int matchCount;
    //lock to make the startIndexing method wait until the consumer has finished indexing
    private static final Object lock = new Object();

    public ProducerConsumer(){
        matches = new StringBuilder();
        matchCount = 0;
    }


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

        /**
         * checks the global hashmap for the filepath hash to determine if the indexer has already gone over the file
         * @param f file to check
         * @return a boolean of whether or not the file has been indexed
         */
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
                //increment the current crawl count to set how many producers are currently running
                currentCrawlCount.getAndIncrement();
                //crawl on the file
                crawl(root);
                //decrement the current crawl count to set how many producers are currently running
                currentCrawlCount.getAndDecrement();
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
            //gets the children files
            File[] entries = root.listFiles(fileFilter);
            if (entries != null) {
                for (File entry : entries)
                    if (entry.isDirectory())
                        //fire a new thread for each child in the directory
                        producerService.submit(new FileCrawler(fileQueue ,fileFilter ,entry));
                    //if the file has not been indexed then get the file path and add it to the concurrenthashmap
                    else if (!alreadyIndexed(entry)){
                        //add file to hashmap
                        //debugWrite(entry.getAbsolutePath());
                        filesFound.put(entry.getAbsolutePath(), entry);
                        fileQueue.put(entry);
                    }
            }
            return true;
        }
    }

    static class Indexer implements Runnable {
        private final BlockingQueue<File> queue;
        //target file being passed fro mthe constructore
        private String targetFile;

        private static volatile boolean indexingDone = false;
        public Indexer(BlockingQueue<File> queue, String targetFile) {
            this.queue = queue;
            this.targetFile = targetFile;
        }

        public void run() {
            try {
                //while the crawlers are not finished running, keep indexing the files
                while (currentCrawlCount.get() > 0){
                    synchronized (lock){
                        //index the file from the queue
                        indexFile(queue.take());
                    }

                }
                //System.out.println("outta while");
                //shut down the consumer so that it will finish its remaining tasks
                consumerService.shutdown();
                //shut down the producer just in case it has remaining tasks
                producerService.shutdown();

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            //check if the thread is alive after execution, will return true but this is because it is just about to die off. Kind of useless code here.
            finally {
                //if all else fails then SHUT IT DOWNNNNNNNNN
                consumerService.shutdownNow();
                producerService.shutdownNow();
            }
        }

        /**
         * prints the file path and the value of the counter and then increments the static volatile counter
         *
         */
        public synchronized void indexFile(File file) {
            // Index the file...
            //see the file that is being indexed
            debugWrite(file.getPath());
            //check if the target file matches the new file being passed
            if(targetFile.matches(file.getName())){
                //if it is a match append the absolute path string to the string builder
                matches.append(file.getAbsolutePath() + "\n");
                //increment the match count
                matchCount++;
                debugWrite("Found File: " + file.getAbsolutePath());
            }
            //print the value of the counter
            debugWrite(counter);
            counter++;
        };

    }

    public void startIndexing(String pathName, String fileName) {
        File[] directories = {new File(pathName)};
        BlockingQueue<File> queue = new LinkedBlockingQueue<File>(BOUND);
        FileFilter filter = new FileFilter() {
            public boolean accept(File file) {
                return true;
            }
        };

        for (File root : directories)
            //fire a new task for the root directory so that the file crawler crawls on the root
            producerService.submit(new FileCrawler(queue, filter, root));

        for (int i = 0; i < N_CONSUMERS; i++)
            //fire nconsumer tasks
            consumerService.submit(new Indexer(queue,fileName));

        synchronized (lock){
            //lock this thread while the consumer service is running
            while(!consumerService.isShutdown()){
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            //once the consumer service has finished, see what we got
            System.out.println(getMatches());
            System.out.println(getMatchcount());
        }


    }

    /**
     * method for out put. only fro debugging purposes
     * @param msg
     * @param <T>
     */
    public static <T> void debugWrite(T msg){
        if(debug == true){
            System.out.println(msg);
        }

    }

    /**
     * gets the matches as a string and returns it
     * @return
     */
    public String getMatches(){
        return matches.toString();
    }

    /**
     * gets the match count
     * @return int
     */
    public int getMatchcount(){
        return matchCount;
    }


//    public static void main(String[] args) {
//
//        File[] directories = {new File("C:\\Users\\Beau\\Desktop\\test10183")};
//        startIndexing(directories, "Hamilton.txt");
//          C:\Users\Beau\Desktop\test10183
//        //hold this thread up until the program finishes
//    }
}
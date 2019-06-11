package pl.wizzdev.timestream.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pl.wizzdev.timestream.service.H5Service;

public class H5AddFileInBackground implements Runnable {

    private final Logger logger = LogManager.getLogger(getClass());
    private final H5Service h5Service = new H5Service();

    private Thread thread;
    private String threadName;
    private String filepath;

    public H5AddFileInBackground(String threadName, String filepath) {
        this.threadName = threadName;
        this.filepath = filepath;
        logger.debug("Creating new thread " + threadName);
    }

    public void run() {
        logger.debug("Running thread" + this.threadName);
        try {
            h5Service.sendH5FileToTimeStream(this.filepath);
        } catch (Exception exception) {
            logger.error("Unrecognized exception occurred " + exception.getMessage());
            exception.printStackTrace();
        }
        logger.debug("Thread " + this.threadName + " exiting.");
    }

    public void start() {
        logger.debug("Starting thread " + this.threadName);
        if (thread == null) {
            thread = new Thread(this, this.threadName);
            thread.start();
        }
    }
}

package org.example;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
public class Main {
    private  static Logger logger = LogManager.getLogger();
    public static void main(String[] args) {
        logger.info("info");
        logger.trace("trace");
        logger.warn("warn");
        logger.debug("Debug");
        logger.error("error");
        logger.fatal("fatal1");

    }
}
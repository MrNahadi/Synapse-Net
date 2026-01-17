package com.telecom.distributed.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main entry point for the Distributed Telecom Core System.
 * Starts the system and keeps it running for demonstration purposes.
 */
public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        logger.info("=".repeat(60));
        logger.info("DISTRIBUTED TELECOM CORE SYSTEM");
        logger.info("=".repeat(60));
        logger.info("Starting core system...");
        
        // Add shutdown hook for graceful shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down core system...");
        }));
        
        logger.info("Core system initialized successfully");
        logger.info("System is running. Press Ctrl+C to stop.");
        
        // Keep the application running
        try {
            while (true) {
                Thread.sleep(30000); // Log heartbeat every 30 seconds
                logger.info("Core system heartbeat - system is healthy");
            }
        } catch (InterruptedException e) {
            logger.info("Core system interrupted, shutting down...");
            Thread.currentThread().interrupt();
        }
    }
}

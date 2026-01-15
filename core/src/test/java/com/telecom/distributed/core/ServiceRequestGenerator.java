package com.telecom.distributed.core;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import com.telecom.distributed.core.model.ServiceId;
import com.telecom.distributed.core.model.ServiceRequest;
import com.telecom.distributed.core.model.ServiceType;

/**
 * QuickCheck generator for ServiceRequest objects.
 * Generates valid service requests with realistic parameters.
 */
public class ServiceRequestGenerator extends Generator<ServiceRequest> {
    
    private static final ServiceId[] SERVICE_IDS = {
        ServiceId.RPC_HANDLER,
        ServiceId.REPLICATION_SERVICE,
        ServiceId.MIGRATION_SERVICE,
        ServiceId.RECOVERY_SERVICE,
        ServiceId.TRANSACTION_COORDINATOR,
        ServiceId.LOAD_BALANCER_SERVICE,
        ServiceId.DEADLOCK_DETECTOR,
        ServiceId.ANALYTICS_SERVICE,
        ServiceId.DISTRIBUTED_MEMORY
    };
    
    private static final ServiceType[] SERVICE_TYPES = ServiceType.values();
    
    public ServiceRequestGenerator() {
        super(ServiceRequest.class);
    }
    
    @Override
    public ServiceRequest generate(SourceOfRandomness random, GenerationStatus status) {
        // Generate random but valid service request parameters
        ServiceId serviceId = random.choose(SERVICE_IDS);
        ServiceType serviceType = random.choose(SERVICE_TYPES);
        
        // CPU requirement: 0-100%
        double cpuRequirement = random.nextDouble(0.0, 100.0);
        
        // Memory requirement: 0-16GB
        double memoryRequirement = random.nextDouble(0.0, 16.0);
        
        // Transaction load: 0-500 tx/sec
        int transactionLoad = random.nextInt(0, 500);
        
        // Priority: 1-10 (inclusive)
        int priority = random.nextInt(1, 10);
        
        // Operation name
        String operation = random.choose(new String[]{
            "read", "write", "update", "delete", "query", "process"
        });
        
        // Payload: random bytes (0-1024 bytes)
        int payloadSize = random.nextInt(0, 1025);
        byte[] payload = new byte[payloadSize];
        random.nextBytes(payload);
        
        // Requires transaction: 50% chance
        boolean requiresTransaction = random.nextBoolean();
        
        return new ServiceRequest(
            serviceId, serviceType, cpuRequirement, memoryRequirement,
            transactionLoad, priority, operation, payload, requiresTransaction
        );
    }
}

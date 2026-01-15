package com.telecom.distributed.core;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import com.telecom.distributed.core.model.FailureType;

/**
 * QuickCheck generator for FailureType enum.
 */
public class FailureTypeGenerator extends Generator<FailureType> {
    
    private static final FailureType[] FAILURE_TYPES = FailureType.values();
    
    public FailureTypeGenerator() {
        super(FailureType.class);
    }
    
    @Override
    public FailureType generate(SourceOfRandomness random, GenerationStatus status) {
        return random.choose(FAILURE_TYPES);
    }
}

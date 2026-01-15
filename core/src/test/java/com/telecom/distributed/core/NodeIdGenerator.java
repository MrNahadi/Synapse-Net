package com.telecom.distributed.core;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import com.telecom.distributed.core.model.NodeId;

/**
 * QuickCheck generator for NodeId objects.
 */
public class NodeIdGenerator extends Generator<NodeId> {
    
    private static final NodeId[] NODE_IDS = {
        NodeId.EDGE1,
        NodeId.EDGE2,
        NodeId.CORE1,
        NodeId.CORE2,
        NodeId.CLOUD1
    };
    
    public NodeIdGenerator() {
        super(NodeId.class);
    }
    
    @Override
    public NodeId generate(SourceOfRandomness random, GenerationStatus status) {
        return random.choose(NODE_IDS);
    }
}

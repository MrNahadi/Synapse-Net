package com.telecom.distributed.core;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import com.telecom.distributed.core.model.NodeId;

import java.util.HashSet;
import java.util.Set;

/**
 * QuickCheck generator for Set<NodeId> objects.
 */
public class NodeIdSetGenerator extends Generator<Set<NodeId>> {
    
    private static final NodeId[] NODE_IDS = {
        NodeId.EDGE1,
        NodeId.EDGE2,
        NodeId.CORE1,
        NodeId.CORE2,
        NodeId.CLOUD1
    };
    
    @SuppressWarnings("unchecked")
    public NodeIdSetGenerator() {
        super((Class<Set<NodeId>>) (Class<?>) Set.class);
    }
    
    @Override
    public Set<NodeId> generate(SourceOfRandomness random, GenerationStatus status) {
        Set<NodeId> nodes = new HashSet<>();
        
        // Generate 0-3 failed nodes (to keep system operational)
        int count = random.nextInt(0, 4);
        
        for (int i = 0; i < count; i++) {
            nodes.add(random.choose(NODE_IDS));
        }
        
        return nodes;
    }
}

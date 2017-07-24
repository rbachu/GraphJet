package com.twitter.graphjet.bipartite.edgepool;


import com.twitter.graphjet.stats.StatsReceiver;

public class WithMetadataPowerLawDegreeEdgePool extends AbstractPowerLawDegreeEdgePool {
  /**
   * Reserves the needed memory for a {@link PowerLawDegreeEdgePool}, and initializes most of the
   * objects that would be needed for this graph. Note that memory would be allocated as needed,
   * and the amount of memory needed can change if the input parameters are violated.
   *
   * @param expectedNumNodes    is the expected number of nodes that will be added into this pool
   * @param expectedMaxDegree   is the expected maximum degree for a node in the pool
   * @param powerLawExponent    is the expected exponent of the power-law graph, i.e.
   *                            (# nodes with degree greater than 2^i) <= (n / powerLawExponent^i)
   */
  public WithMetadataPowerLawDegreeEdgePool(
    int expectedNumNodes,
    int expectedMaxDegree,
    double powerLawExponent,
    StatsReceiver statsReceiver) {

    super(expectedNumNodes, expectedMaxDegree, powerLawExponent, statsReceiver);

    WithMetadataRegularDegreeEdgePool[] edgePools = new WithMetadataRegularDegreeEdgePool[numPools];
    int[] poolDegrees = new int[numPools];
    readerAccessibleInfo =
      new ReaderAccessibleInfo(edgePools, poolDegrees, new int[expectedNumNodes]);
    for (int i = 0; i < numPools; i++) {
      initPool(i);
    }
    currentNumEdgesStored = 0;
  }

  private void initPool(int poolNumber) {
    int expectedNumNodesInPool =
      (int) Math.ceil(expectedNumNodes / Math.pow(powerLawExponent, poolNumber));
    int maxDegreeInPool = (int) Math.pow(2, poolNumber + 1);
    readerAccessibleInfo.edgePools[poolNumber] = new WithMetadataRegularDegreeEdgePool(
      expectedNumNodesInPool, maxDegreeInPool, statsReceiver.scope("poolNumber_" + poolNumber));
    readerAccessibleInfo.poolDegrees[poolNumber] = maxDegreeInPool;
  }

  @Override
  public void addEdge(int nodeA, int nodeB) {
    throw new UnsupportedOperationException("add a single edge one by one is not supported in "
      + "OptimizedEdgePool");
  }

  @Override
  public void addEdge(int nodeA, int nodeB, long metadata) {
    // First add the node if it doesn't exist
    int nextPoolForNodeA;
    if (nodeA >= readerAccessibleInfo.nodeDegrees.length) {
      expandArray(nodeA);
      nextPoolForNodeA = 0;
    } else {
      nextPoolForNodeA = getNextPoolForNode(nodeA);
      // Add a pool if needed
      if (nextPoolForNodeA >= numPools) {
        addPool();
      }
    }
    // Now add the edge
    readerAccessibleInfo.edgePools[nextPoolForNodeA].addEdge(nodeA, nodeB, metadata);
    // This is to guarantee that if a reader sees the updated degree later, they can find the edge
    currentNumEdgesStored += 2;
    // The order is important -- the updated degree is the ONLY way for a reader for going to the
    // new edge, so this needs to be the last update
    incrementNodeDegree(nodeA);
    currentNumEdgesStored--;

    numEdgesCounter.incr();
  }


  /**
   * Synchronization comment: this method works fine without needing synchronization between the
   * writer and the readers due to the wrapping of the arrays in ReaderAccessibleInfo.
   * See the publication safety comment in ReaderAccessibleInfo for details.
   */
  private void addPool() {
    int newNumPools = (int) Math.ceil(numPools * POOL_GROWTH_FACTOR);

    numPoolsCounter.incr(newNumPools - numPools);

    WithMetadataRegularDegreeEdgePool[] newEdgePools =
      new WithMetadataRegularDegreeEdgePool[newNumPools];
    int[] newPoolDegrees = new int[newNumPools];
    System.arraycopy(readerAccessibleInfo.edgePools, 0,
      newEdgePools, 0,
      readerAccessibleInfo.edgePools.length);
    System.arraycopy(readerAccessibleInfo.poolDegrees, 0,
      newPoolDegrees, 0,
      readerAccessibleInfo.poolDegrees.length);
    // This flushes all the reader-accessible data *together* to all threads: the readers are safe
    // as they reference the wrapper object since the data locations stay the same and also no one
    // can access the new pool yet since no node points to the new pool yet
    readerAccessibleInfo = new ReaderAccessibleInfo(
      newEdgePools,
      newPoolDegrees,
      readerAccessibleInfo.nodeDegrees);
    for (int i = numPools; i < newNumPools; i++) {
      initPool(i);
    }
    numPools = newNumPools;
    // Self-assignment to flush the numPools update across the memory barrier
    currentNumEdgesStored = currentNumEdgesStored;
  }
}

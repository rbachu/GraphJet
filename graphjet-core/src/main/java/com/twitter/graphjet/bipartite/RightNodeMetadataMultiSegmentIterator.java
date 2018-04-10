/**
 * Copyright 2018 Twitter. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.twitter.graphjet.bipartite;

import com.twitter.graphjet.bipartite.api.NodeMetadataEdgeIterator;
import com.twitter.graphjet.bipartite.segment.RightNodeMetadataLeftIndexedBipartiteGraphSegment;

import it.unimi.dsi.fastutil.ints.IntIterator;

public class RightNodeMetadataMultiSegmentIterator
  extends ReverseChronologicalMultiSegmentIterator<RightNodeMetadataLeftIndexedBipartiteGraphSegment>
  implements NodeMetadataEdgeIterator, ReusableNodeLongIterator {

  /**
   * This constructor is for easy reuse in the random iterator derived from this one.
   *
   * @param multiSegmentBipartiteGraph  is the underlying
   *                                    {@link RightNodeMetadataLeftIndexedBipartiteGraphSegment}
   * @param segmentEdgeAccessor         abstracts the left/right access in a common interface
   */
  public RightNodeMetadataMultiSegmentIterator(
    LeftIndexedMultiSegmentBipartiteGraph<RightNodeMetadataLeftIndexedBipartiteGraphSegment>
      multiSegmentBipartiteGraph,
    SegmentEdgeAccessor<RightNodeMetadataLeftIndexedBipartiteGraphSegment>
      segmentEdgeAccessor) {
    super(multiSegmentBipartiteGraph, segmentEdgeAccessor);
  }

  @Override
  public IntIterator getLeftNodeMetadata(byte nodeMetadataType) {
    throw new UnsupportedOperationException(
      "The getLeftNodeMetadata operation is currently not supported"
    );
  }

  @Override
  public IntIterator getRightNodeMetadata(byte nodeMetadataType) {
    return ((NodeMetadataEdgeIterator) currentSegmentIterator)
      .getRightNodeMetadata(nodeMetadataType);
  }

  @Override
  // Return 0 because RightNodeMetadataMultiSegmentIterator does not contain edge metadata.
  public long currentMetadata() {
    return 0L;
  }
}


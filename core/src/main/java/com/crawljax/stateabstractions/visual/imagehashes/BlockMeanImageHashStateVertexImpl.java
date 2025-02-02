package com.crawljax.stateabstractions.visual.imagehashes;

import com.crawljax.core.state.StateVertex;
import com.crawljax.core.state.StateVertexImpl;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import org.opencv.core.Mat;

/**
 * The state vertex class which represents a state in the browser. When iterating over the possible
 * candidate elements every time a candidate is returned its removed from the list so it is a one
 * time only access to the candidates.
 */
public class BlockMeanImageHashStateVertexImpl extends StateVertexImpl {

  private static final long serialVersionUID = 123400017983489L;
  public final Mat hashMat;
  final BlockMeanImageHash visHash;

  /**
   * Creates a current state without an url and the @strippedDom same as the @dom.
   *
   * @param name the name of the state
   * @param dom  the current DOM tree of the browser
   */
  @VisibleForTesting
  BlockMeanImageHashStateVertexImpl(int id, String name, String dom, BlockMeanImageHash visHash,
      Mat hashMat) {
    this(id, null, name, dom, dom, visHash, hashMat, -1);
  }

  /**
   * Defines a State.
   *
   * @param url         the current url of the state
   * @param name        the name of the state
   * @param dom         the current DOM tree of the browser
   * @param strippedDom the stripped dom by the OracleComparators
   * @param threshold
   */
  public BlockMeanImageHashStateVertexImpl(int id, String url, String name, String dom,
      String strippedDom,
      BlockMeanImageHash visHash,
      Mat hashMat, double threshold) {
    super(id, url, name, dom, strippedDom);
    this.visHash = visHash;
    this.hashMat = hashMat;
    if (threshold != -1) {
      this.visHash.maxThreshold = threshold;
    }
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(hashMat);
  }

  @Override
  public boolean equals(Object object) {
    if (object instanceof BlockMeanImageHashStateVertexImpl) {
      BlockMeanImageHashStateVertexImpl that = (BlockMeanImageHashStateVertexImpl) object;
      double distance = visHash.compare(this.hashMat, that.hashMat);
      return (distance >= visHash.minThreshold && distance <= visHash.maxThreshold)
          || (distance == 0.0);
    }
    return false;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("id", super.getId())
        .add("name", super.getName())
        .add("hash", visHash).toString();
  }

  @Override
  public boolean inThreshold(StateVertex vertexOfGraph) {
    // Only implemented when there is a threshold for near duplicates
    if (vertexOfGraph instanceof BlockMeanImageHashStateVertexImpl) {
      BlockMeanImageHashStateVertexImpl vertex = (BlockMeanImageHashStateVertexImpl) vertexOfGraph;
      double distance = visHash.compare(this.hashMat, vertex.hashMat);
      return distance >= visHash.minThreshold && distance <= visHash.maxThreshold;
    }
    return false;
  }

  @Override
  public double getDist(StateVertex vertexOfGraph) {
    if (vertexOfGraph instanceof BlockMeanImageHashStateVertexImpl) {
      BlockMeanImageHashStateVertexImpl vertex = (BlockMeanImageHashStateVertexImpl) vertexOfGraph;
      return visHash.compare(this.hashMat, vertex.hashMat);
    }
    return -1;
  }
}

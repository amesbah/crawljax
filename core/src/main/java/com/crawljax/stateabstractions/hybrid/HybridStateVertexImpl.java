package com.crawljax.stateabstractions.hybrid;

import com.crawljax.browser.EmbeddedBrowser;
import com.crawljax.browser.WebDriverBackedEmbeddedBrowser;
import com.crawljax.core.CandidateElement;
import com.crawljax.core.state.StateVertex;
import com.crawljax.core.state.StateVertexImpl;
import com.crawljax.fragmentation.Fragment;
import com.crawljax.fragmentation.FragmentManager;
import com.crawljax.stateabstractions.dom.apted.costmodel.StringUnitCostModel;
import com.crawljax.stateabstractions.dom.apted.distance.APTED;
import com.crawljax.stateabstractions.dom.apted.node.AptedNode;
import com.crawljax.stateabstractions.dom.apted.node.StringNodeData;
import com.crawljax.stateabstractions.dom.apted.util.AptedUtils;
import com.crawljax.stateabstractions.visual.OpenCVLoad;
import com.crawljax.util.DomUtils;
import com.crawljax.util.XPathHelper;
import com.crawljax.vips_selenium.VipsRectangle;
import com.crawljax.vips_selenium.VipsSelenium;
import com.crawljax.vips_selenium.VipsUtils;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.io.FilenameUtils;
import org.openqa.selenium.WebDriver;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.xpath.XPathExpressionException;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The state vertex class which represents a state in the browser. When iterating over the possible
 * candidate elements every time a candidate is returned its removed from the list so it is a one
 * time only access to the candidates.
 */
public class HybridStateVertexImpl extends StateVertexImpl{
	static {
		OpenCVLoad.load();
	}
	private static final long serialVersionUID = 123400017983489L;

	transient private ArrayList<Fragment> fragments;

	transient private Fragment rootFragment;

	transient private HashMap<Integer, Fragment> fragmentMap;

	public static boolean FAST_COMPARE = false;

	private double threshold = 0.0;
	transient private Document fragmentedDom = null;
	
	transient private BufferedImage image = null;
	
	private boolean fragmented = false;
	
	private boolean visualData = false;

	private int size = -1;

	public boolean isVisualData() {
		return visualData;
	}

	public void setVisualData(boolean visualData) {
		this.visualData = visualData;
	}

	/**
	 * Defines a State.
	 *
	 * @param id          id of the state in the SFG
	 * @param url         the current url of the state
	 * @param name        the name of the state
	 * @param dom         the current DOM tree of the browser
	 * @param strippedDom the stripped dom by the OracleComparators
	 * @param threshold   the threshold to be used
	 * @param visualData 
	 */
	public HybridStateVertexImpl(int id, String url, String name, String dom, String strippedDom,
			double threshold, boolean visualData) {
		super(id, url, name, dom, strippedDom);
		this.fragments =null;
		long start = System.currentTimeMillis();
		this.threshold = threshold;
		this.visualData = visualData;
		try {
			this.fragmentedDom = DomUtils.asDocument(strippedDom);
			boolean offline = false;
			VipsUtils.cleanDom(fragmentedDom, offline);
		} catch (IOException e) {
			System.out.println("Error creating document : state " +  id);
			e.printStackTrace();
		}
		long end = System.currentTimeMillis();
		LOG.info("Took {} ms to parse DOM", end-start);
	}

	public boolean isFragmented() {
		return fragmented;
	}
	
	public Document loadFragmentDom(Document dom, BufferedImage screenshot) {
		this.fragmentedDom = dom;
		boolean offline = true;
		VipsUtils.cleanDom(fragmentedDom, offline);
		VipsSelenium vips = new VipsSelenium(null, this.fragmentedDom, screenshot, 10, null, this.getName(), false, false);
//		VipsSeleniumParser parser = new VipsSeleniumParser(vips);
		List<VipsRectangle> rectangles = vips.startSegmentation();
		fragmented = true;
		this.image = screenshot;
		this.addFragments(rectangles, null);
		return fragmentedDom;
	}
	
	public Document fragmentDom(EmbeddedBrowser browser, BufferedImage screenshot, File screenshotFile) {
		if(!fragmented) {
			VipsSelenium vips = new VipsSelenium(browser.getWebDriver(),this.fragmentedDom, screenshot, 10, screenshotFile, this.getName(), true, ((WebDriverBackedEmbeddedBrowser)browser).isUSE_CDP());
//			VipsSeleniumParser parser = new VipsSeleniumParser(vips);
			List<VipsRectangle> rectangles = vips.startSegmentation();
			fragmented = true;
			this.image = screenshot;
			this.addFragments(rectangles, browser.getWebDriver());
		}
		
		return fragmentedDom;
	}
	
	@Override
	public Document getDocument() {
		return fragmentedDom;
	}
	
	
	@Override
	public int hashCode() {
		return Objects.hashCode(this.getStrippedDom());
	}

	public static boolean computeDistanceUsingChangedNodes(Document doc1, Document doc2, boolean visualData) {
		List<List<Node>> changedNodes  = getChangedNodes(doc1, doc2, visualData);
		List<Node> doc1Changed = changedNodes.get(0);
		List<Node> doc2Changed = changedNodes.get(1);
		boolean allHidden = true;
		for(Node changed : doc1Changed) {
			if(VipsUtils.isDisplayed(changed, null)) {
				allHidden = false;
				break;
			}
		}
		
		for(Node changed : doc2Changed) {
			if(VipsUtils.isDisplayed(changed, null)) {
				allHidden = false;
				break;
			}
		}
		
		return allHidden;
	}
	
	public static double computeDistance(Document doc1, Document doc2, boolean visualData) {
		AptedNode<StringNodeData> aptedDoc1 = AptedUtils.getAptedTree(doc1, visualData);
//		System.out.println(aptedDoc1);
		AptedNode<StringNodeData> aptedDoc2 = AptedUtils.getAptedTree(doc2, visualData);
//		System.out.println(aptedDoc2);
		APTED<StringUnitCostModel, StringNodeData> apted = new APTED<>(new StringUnitCostModel());
		 
		double structuralDistance = apted.computeEditDistance(aptedDoc1, aptedDoc2);
//		System.out.println(getChangedNodes(doc1, doc2));
		return structuralDistance; 
	}
	
	public BufferedImage getImage() {
		return image;
	}

	public void setImage(BufferedImage image) {
		this.image = image;
	}

	// Doc1 is the new state
	// Doc2 is the old state
	public static List<List<Node>> getChangedNodes(Document doc1, Document doc2, boolean visualData){
		
		List<Node> postOrder1 = Lists.newArrayList();
		populatePostorder(postOrder1, doc1.getElementsByTagName("body").item(0));
		
		List<Node> postOrder2 = Lists.newArrayList();
		populatePostorder(postOrder2, doc2.getElementsByTagName("body").item(0));
		
		List<Node> doc1Nodes = new LinkedList<>();
		List<Node> doc2Nodes = new LinkedList<>();
		Map<Node, Node> nodeMappings = Maps.newLinkedHashMap();
		
		
		AptedNode<StringNodeData> aptedDoc1 = AptedUtils.getAptedTree(doc1, visualData);
		
		AptedNode<StringNodeData> aptedDoc2 = AptedUtils.getAptedTree(doc2, visualData);
		APTED<StringUnitCostModel, StringNodeData> apted = new APTED<>(new StringUnitCostModel());
		double structuralDistance = apted.computeEditDistance(aptedDoc1, aptedDoc2);
		
		
		LinkedList<int[]> mappings = (LinkedList<int[]>) apted.computeEditMapping();
	    for (int[] mapping : mappings) {
	    		if (mapping[1] == 0) {
	    			
					doc1Nodes.add(postOrder1.get(mapping[0] - 1));
	    		} else if (mapping[0] == 0) {
	    			doc2Nodes.add(postOrder2.get(mapping[1] - 1));
	    		} else {
	    			Node oldNode = postOrder2.get(mapping[1] - 1);
	    			Node newNode = postOrder1.get(mapping[0] - 1);
	    			String oldS = AptedUtils.getNodeStringRepresentation(oldNode, visualData);
	    			String newS = AptedUtils.getNodeStringRepresentation(newNode, visualData);
	    			if(!oldS.equalsIgnoreCase(newS)){
	    				nodeMappings.put(oldNode, newNode);
	    				doc2Nodes.add(oldNode);
	    				doc1Nodes.add(newNode);
	    			}
	    		}
	    }
//	    String[] a = {"",""};
	    
	    List<List<Node>> changedNodes = new ArrayList<List<Node>>();
	    changedNodes.add(doc1Nodes);
	    changedNodes.add(doc2Nodes);

	    return changedNodes;
	}
	
	private static void populatePostorder(List<Node> postorderList, Node node) {
		if(node == null) {
			return;
		}
		ArrayList<Node> children = VipsUtils.getChildren(node);

		if(node.getNodeName().equalsIgnoreCase("select")) {
			if(children.size()>0) {
				postorderList.add(children.get(0));
			}
			postorderList.add(node);
			return;
		}
		
		for (Node item: children) {
			populatePostorder(postorderList, item);
		}
		postorderList.add(node);
	}
	
	public List<List<Node>> getDifference(StateVertex other){
		try {
			return getChangedNodes(this.fragmentedDom, other.getDocument(), visualData);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Use nearduplicate state to find dynamic fragments. 
	 * Can be used during state revisit as well.
	 * @param ndState
	 * @return
	 */
	public List<Fragment> assignDynamicFragments(StateVertex ndState) {
		if(!(ndState instanceof HybridStateVertexImpl)){	
			return new ArrayList<>();
		}
		List<Fragment> dynamicFragments = new ArrayList<Fragment>();
		try {
			List<Node> diffNodes = getDiffNodes(this.getDocument(), ((HybridStateVertexImpl)ndState).getDocument(), visualData);
			//		List<Node> diffNodes = FragmentManager.getDomDiffNodes(this, (HybridStateVertexImpl)ndState);
			LOG.debug("No of diff nodes found {}", diffNodes.size());
			for(Node node: diffNodes) {
				if(node==null)
					continue;
				LOG.debug("Diff node {}", XPathHelper.getXPathExpression(node));
				
				VipsUtils.setDynamic(node);
				Fragment fragment = getClosestFragment(node);
				LOG.debug("Closest fragment {} with xpath {}", fragment.getId(), XPathHelper.getXPathExpression(fragment.getFragmentParentNode()));
				if(fragment!=null) {
					if(!dynamicFragments.contains(fragment)) {
						dynamicFragments.add(fragment);
						fragment.setDynamic(true);
					}
				}
			}
			LOG.debug("No of dynamic fragments found {}", dynamicFragments.size());
		}catch(Exception ex) {
			LOG.error("Error assigning dynamic fragments {}", ex.getMessage());
		}
		return dynamicFragments;
	}

	/**
	 * Returns nodes of doc1 which are mapped to doc2 but have different tag or text value
	 * @param doc1
	 * @param doc2
	 * @param visualData 
	 * @return
	 */
	public static List<Node> getDiffNodes(Document doc1, Document doc2, boolean visualData) {		
		List<Node> postOrder1 = Lists.newArrayList();
		populatePostorder(postOrder1, doc1.getElementsByTagName("body").item(0));
		
		List<Node> postOrder2 = Lists.newArrayList();
		populatePostorder(postOrder2, doc2.getElementsByTagName("body").item(0));
		
		List<Node> doc1Nodes = new LinkedList<>();
		List<Node> doc2Nodes = new LinkedList<>();
		Map<Node, Node> nodeMappings = Maps.newLinkedHashMap();
		
		
		AptedNode<StringNodeData> aptedDoc1 = AptedUtils.getAptedTree(doc1, visualData);
		
		AptedNode<StringNodeData> aptedDoc2 = AptedUtils.getAptedTree(doc2, visualData);
		APTED<StringUnitCostModel, StringNodeData> apted = new APTED<>(new StringUnitCostModel());
		double structuralDistance = apted.computeEditDistance(aptedDoc1, aptedDoc2);
		
		
		LinkedList<int[]> mappings = (LinkedList<int[]>) apted.computeEditMapping();
	    for (int[] mapping : mappings) {
	    		if (mapping[1] == 0) {
	    			
//					doc1Nodes.add(postOrder1.get(mapping[0] - 1));
	    		} else if (mapping[0] == 0) {
//	    			doc2Nodes.add(postOrder2.get(mapping[1] - 1));
	    		} else {
	    			Node doc2Node = postOrder2.get(mapping[1] - 1);
	    			Node doc1Node = postOrder1.get(mapping[0] - 1);
	    			String doc2Tag = AptedUtils.getNodeStringRepresentation(doc2Node, visualData);
	    			String doc1Tag = AptedUtils.getNodeStringRepresentation(doc1Node, visualData);
	    			if(!doc2Tag.equalsIgnoreCase(doc1Tag)){
	    				doc2Nodes.add(doc2Node);
	    				doc1Nodes.add(doc1Node);
	    			}
	    			else {
	    				if(doc1Tag.equalsIgnoreCase("#text") && doc2Tag.equalsIgnoreCase("#text")) {
	    					if(!doc1Node.getTextContent().trim().equalsIgnoreCase(doc2Node.getTextContent().trim())) {
	    						doc1Nodes.add(doc1Node);
	    						doc2Nodes.add(doc2Node);
	    					}
	    				}
	    			}
	    			
    				nodeMappings.put(doc2Node, doc1Node);
	    		}
	    }
	    return doc1Nodes;
	}
	
	

	private int getSize() {
		if(size == -2) {
			return size;
		}
		if(size == -1) {
			try {
				size = DomUtils.getAllSubtreeNodes(getDocument().getElementsByTagName("body").item(0)).getLength();
			} catch (XPathExpressionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				size = -2;
			}
		}
		return size;
	}

	


	@Override
	public boolean equals(Object object) {
		HybridStateVertexImpl that = (HybridStateVertexImpl) object;
		if(this.getId() == that.getId()) {
			return true;
		}
		if(FAST_COMPARE) {
			if(this.getSize() < 0 || that.getSize() < 0) {
				LOG.error("Cant do fast compare: unable to get state size {} {}", this, that);
			}
			else if(this.getSize() != that.getSize()) {
				return false;
			}
		}
		try {
//			if(visualData) {
//				return computeDistanceUsingChangedNodes(this.getDocument(), that.getDocument(), visualData);
//			}
			double distance = computeDistance(this.getDocument(), that.getDocument(), visualData);
//			LOG.info("Distance  between {} {} is {}", this.getName(), that.getName(), distance);
			return distance <= threshold;
		}catch(Exception ex) {
			LOG.error("Error calculating distance between {} and {}", this.getName(), that.getName());
			ex.printStackTrace();
			return false;
		}
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("id", super.getId())
				.add("name", super.getName()).toString();
	}

	@Override
	public double getDist(StateVertex vertexOfGraph) {
		if (vertexOfGraph instanceof HybridStateVertexImpl) {
			HybridStateVertexImpl vertex = (HybridStateVertexImpl) vertexOfGraph;
			return computeDistance(this.getDocument(), vertex.getDocument(), visualData);
		}
		return -1;
	}

	@Override
	public void setDocument(Document dom) {
		this.fragmentedDom = dom;
	}

	public static double computeDistance_Oracle(Document doc1, Document doc2, boolean visualData) {
		AptedNode<StringNodeData> aptedDoc1 = AptedUtils.getAptedTree(doc1, visualData);
//		System.out.println(aptedDoc1);
		AptedNode<StringNodeData> aptedDoc2 = AptedUtils.getAptedTree(doc2, visualData);
//		System.out.println(aptedDoc2);
		APTED<StringUnitCostModel, StringNodeData> apted = new APTED<>(new StringUnitCostModel());
		 
		double structuralDistance = apted.computeEditDistance(aptedDoc1, aptedDoc2);
//		System.out.println(getChangedNodes(doc1, doc2));
		double toRemove = 0;
		List<List<Node>> changedNodes = getChangedNodes(doc1, doc2, visualData);
		List<Node> doc1Nodes = changedNodes.get(0);
		List<Node> doc2Nodes = changedNodes.get(1);
		
		List<Node> allNodes = new ArrayList<>();
		allNodes.addAll(doc1Nodes);
		allNodes.addAll(doc2Nodes);
		for(Node node: allNodes) {
			if(!VipsUtils.isDisplayed(node, null)) {
				toRemove += 1;
			}
		}
		return structuralDistance - toRemove; 
	}

	@Override
	public void setElementsFound(LinkedList<CandidateElement> elements){
		super.setElementsFound(elements);
		if(fragments!=null) {
			addCandidatesToFragments();
		}
	}

	@Override
	public ArrayList<Fragment> getFragments() {
		return fragments;
	}

	@Override
	public void setFragments(ArrayList<Fragment> fragments) {
		this.fragments = fragments;
	}

	@Override
	public void addFragments(List<VipsRectangle> rectangles, WebDriver driver) {
		if(rectangles == null)
			return;

		HashMap<Integer, VipsRectangle> rectangleMap  = new HashMap<>();
		for(VipsRectangle rectangle: rectangles) {
			rectangleMap.put(rectangle.getId(), rectangle);
		}
		fragmentMap  = new HashMap<>();
		for(VipsRectangle rectangle : rectangles) {
			//			String xpath = rectangle.getXpath();
			Fragment fragment = new Fragment(rectangle.getId(), rectangle.getNestedBlocks(), rectangle.getRect(), this);
			fragmentMap.put(rectangle.getId(), fragment);
			addFragment(fragment);
		}

		for(Integer rectKey : fragmentMap.keySet()) {
			Fragment current = fragmentMap.get(rectKey);
			int parentKey = rectangleMap.get(rectKey).getParentId();
			if(parentKey!= -1)
			{
				Fragment parentFragment = fragmentMap.get(parentKey);
				current.setParent(parentFragment);
				parentFragment.addChild(current);
			}

			else {
				this.rootFragment = current;
			}
		}


		try {
			setParentNode(rootFragment);
		}catch(Exception ex) {
			//			ex.printStackTrace();
			LOG.error("Could not set parent node for root fragment in : " + this.getName());
		}

		try {
			generateDomFragments(fragmentMap, driver);
		}catch(Exception ex) {
			LOG.error("Could not generate dom fragments {}", getName());
			ex.printStackTrace();
		}
		//		cleanFragments(driver);

		//		exportFragments();

		if(super.getCandidateElements() !=null) {
			addCandidatesToFragments();
		}
	}

	public void exportFragments(File screenshotsFolder, BufferedImage pageViewport) {

		if(screenshotsFolder.isDirectory()) {
			File fragFolder = new File(screenshotsFolder, FilenameUtils.getBaseName(this.getName()));
			fragFolder.mkdir();
			for(Fragment fragment : fragments) {
				if(!FragmentManager.usefulFragment(fragment)) {
					continue;
				}
				File subImageTarget = new File(fragFolder, "" + fragment.getId()+".png");
				VipsUtils.exportFragment(pageViewport, subImageTarget, fragment.getRect());
			}
		}


	}


	public int getNextFragmentId() {
		if(fragments == null)
			return -1;
		int maxId = 0;
		for(Fragment fragment: fragments) {
			if(fragment.getId()>maxId)
				maxId = fragment.getId();
		}
		return maxId+1;
	}


	private Fragment createDomFragment(Node fragmentParentNode, List<Node> nestedBlocks, Fragment parent, WebDriver driver){
		Fragment newFragment = new Fragment(-1, null, VipsUtils.getRectangle(fragmentParentNode, driver), this);
		newFragment.setFragmentParentNode(fragmentParentNode);
		if(!FragmentManager.usefulFragment(newFragment))
			return null;

		newFragment.setId(getNextFragmentId());
		newFragment.setNestedBlocks(nestedBlocks);

		newFragment.setParent(parent);
		parent.addChild(newFragment);

		newFragment.setDomParent(parent);
		parent.addDomChildren(newFragment);

		fragments.add(newFragment);

		return newFragment;
	}

	private List<Fragment> getDomFragments(Node rootNode, List<Node> nestedBlocks, HashMap<Integer, Fragment> fragmentMap, Fragment parent, WebDriver driver) {
		// TODO: If not useful then return

		Node lca = VipsUtils.getParentBox(nestedBlocks);

		Fragment created = null;
		List<Fragment> returnList = new ArrayList<Fragment>();
		int fragmentId = VipsUtils.getFragParent(rootNode);
		if(fragmentId >=0 ) {
			Fragment parentFragment = fragmentMap.get(fragmentId);
			if(parentFragment.getParent()!=null && parent!=null) {
				if( !parentFragment.getParent().equals(parent)) {
					LOG.debug("Problem with hierarchy {} : insert {}", fragmentId, parent.getId());
				}
				parentFragment.setDomParent(parent);
				parent.addDomChildren(parentFragment);
			}
				LOG.debug("{} Already a parent for fragment {}", XPathHelper.getSkeletonXpath(rootNode), fragmentId);
			if(nestedBlocks.size() <= 1) {
				LOG.debug("No DOM division needed for {}", fragmentId);
				return new ArrayList<Fragment>();
			}
			if(! FragmentManager.usefulFragment(parentFragment)){
				// No need to divide small fragments
				return new ArrayList<Fragment>();
			}

		}
		else {
			if(lca.isSameNode(rootNode)) {
				created = createDomFragment(rootNode, nestedBlocks, parent, driver);
				if(created!=null)
				{
					LOG.info("Created Dom fragment {}, child of {}, for {} ", created.getId(), parent.getId(), XPathHelper.getSkeletonXpath(rootNode));
					fragmentMap.put(created.getId(), created);
					returnList.add(created);
				}
			}
			else if(DomUtils.contains(rootNode, lca)) {
				// If root node contains the lca of nested blocks, then divide the lca
				LOG.debug("RootNode contains LCA of given nested blocks");
				return getDomFragments(lca, nestedBlocks, fragmentMap, parent, driver);
			}
		}

		if(created!=null) {
			//			LOG.info("New Fragment created, any dom fragments from children will be children to created {}", created.getId());
			parent = created;
		}

		List<Node> children = VipsUtils.getChildren(rootNode);
		for(Node child : children) {
			if(child.getNodeName().equalsIgnoreCase("#text"))
				continue;

			// If already a parent  then divide it
			if(VipsUtils.getFragParent(child)>=0) {
				Fragment childFragment = fragmentMap.get(VipsUtils.getFragParent(child));
				returnList.addAll(getDomFragments(child, childFragment.getNestedBlocks(), fragmentMap, parent, driver));
			}

			//or atleast one vips block inside it,
			else {
				List<Node> containedNodes = getContainedNodes(child, nestedBlocks);
				if(!containedNodes.isEmpty()) {
					returnList.addAll(getDomFragments(child, containedNodes, fragmentMap, parent, driver));
				}
				else {
					LOG.debug("Child has no vips blocks, ignoring it");
				}
			}
		}
		return returnList;
	}



	private List<Node> getContainedNodes(Node node, List<Node> nestedBlocks) {
		List<Node> returnList = new ArrayList<Node>();
		for(Node block: nestedBlocks) {
			if(DomUtils.contains(node, block)) {
				returnList.add(block);
			}
		}
		return returnList;
	}

	public List<Fragment> generateDomFragments(HashMap<Integer, Fragment> fragmentMap, WebDriver driver){
		List<Fragment> added = new ArrayList<>();
		Node rootNode = rootFragment.getFragmentParentNode();
		if(rootNode==null) {
			try {
				rootNode= getDocument().getFirstChild();
			} catch (Exception e) {
				LOG.error("Could not find root node for the state {}", this.getName());
				return added;
			};
		}

		added.addAll(getDomFragments(rootNode, rootFragment.getNestedBlocks(), fragmentMap, rootFragment, driver));

		return added;
	}


	public List<Fragment> cleanFragments(WebDriver driver) {
		// TO divide fragments by DOM
		List<Fragment> added = new ArrayList<>();
		CopyOnWriteArrayList<Fragment> copyFragments = new CopyOnWriteArrayList<>(fragments);
		for(Fragment fragment: copyFragments) {
			if(!fragment.getChildren().isEmpty()) {
				boolean shouldIgnoreFragment = false;
				for(Fragment child: fragment.getChildren()) {
					// If there is a useful DOM_FRAGMENT child, then ignore the fragment.
					if(child.getFragmentParentNode() != null && FragmentManager.usefulFragment(child)) {
						shouldIgnoreFragment = true;
						break;
					}
				}
				if(shouldIgnoreFragment)
					continue;
			}

			if(fragment.getFragmentParentNode() == null && FragmentManager.usefulFragment(fragment) ) {
				List<Fragment> addedChildren = divideFragmentByDom(fragment, driver);
				if(addedChildren!=null)
					added.addAll(addedChildren);
			}

		}

		return added;
	}


	private List<Fragment> divideFragmentByDom(Fragment fragment, WebDriver driver) {
		if(fragment.getFragmentParentNode()!=null) {
			LOG.warn("Cannot divide a fragment with parent node");
			return null;
		}

		Node parentBox = VipsUtils.getParentBox(fragment.getNestedBlocks());

		if(fragment.getParent()!=null) {
			List<Fragment> siblings = fragment.getParent().getChildren();
			List<Node> siblingLcas = getSiblingLca(siblings, fragment);
			List<Fragment> toBeAdded = getDifferentiatingNodes(fragment.getNestedBlocks(), siblingLcas, parentBox, driver);
			if(toBeAdded == null)
				return null;

			for(Fragment toAdd: toBeAdded) {
				int id = getNextFragmentId();
				toAdd.setId(id);
				toAdd.setParent(fragment);
				fragment.addChild(toAdd);
				toAdd.setUseful(true);
				fragments.add(toAdd);
				LOG.info("Added Fragment {} to {} using DOM division", toAdd.getId(), fragment.getId());
			}

			fragment.adjustRectangle();
			return toBeAdded;
		}
		return null;
	}

	private List<Fragment> getDifferentiatingNodes(List<Node> blocks, List<Node> siblingLcas, Node parentBox, WebDriver driver) {
		List<Fragment> fragmentsToAdd = new ArrayList<>();
		for(Node child: VipsUtils.getChildren(parentBox)) {

			if(child.getNodeName().equalsIgnoreCase("#text"))
				continue;

			Fragment newFragment = new Fragment(-1, null, VipsUtils.getRectangle(child, driver), this);
			newFragment.setFragmentParentNode(child);
			if(!FragmentManager.usefulFragment(newFragment))
				continue;

			List<Node> contains = new ArrayList<>();
			for(Node block: blocks) {
				if(DomUtils.contains(child, block)) {
					contains.add(block);
				}
			}
			if(contains.isEmpty())
				continue;

			if(isADifferentiator(siblingLcas, child)) {
				newFragment.setNestedBlocks(contains);
				newFragment.setFragmentParentNode(child);
				fragmentsToAdd.add(newFragment);
			}
			else {
				List<Fragment> childDiffNodes = getDifferentiatingNodes(blocks, siblingLcas, child, driver);
				fragmentsToAdd.addAll(childDiffNodes);
			}

		}

		return fragmentsToAdd;
	}


	private List<Node> getSiblingLca(List<Fragment> fragments, Fragment exclude){
		List<Node> lcas = new ArrayList<>();

		for(Fragment fragment: fragments) {
			if(fragment.equals(exclude))
				continue;
			Node lca = VipsUtils.getParentBox(fragment.getNestedBlocks());
			lcas.add(lca);
		}
		return lcas;
	}

	private Node leastCommonAncestor(List<Fragment> fragments, Fragment exclude) {
		Node returnNode = null;
		List<Node> lcas = getSiblingLca(fragments, exclude);

		returnNode = VipsUtils.getParentBox(lcas);
		return returnNode;
	}

	private boolean isADifferentiator(List<Node> siblingLcas, Node lca) {
		for(Node siblingLca : siblingLcas) {
			if(DomUtils.contains(lca,  siblingLca))
				return false;
		}
		return true;
	}

	public Node highestDifferentiator(Fragment fragment, List<Fragment> siblings) {
		Node lca = VipsUtils.getParentBox(fragment.getNestedBlocks());

		Node siblingLca = leastCommonAncestor(siblings, fragment);

		if(lca==null || siblingLca==null)
			return null;

		List<Node> siblingLcas = getSiblingLca(siblings, fragment);

		// Using LCA of nested Blocks instead of highest differentiator
		if(isADifferentiator(siblingLcas, lca)) {
			return lca;
		}
		return null;
	}

	private void setParentNode(Fragment fragment) {

		if(this.rootFragment!=null && this.rootFragment.equals(fragment)) {
			Node fragmentParentNode = VipsUtils.getParentBox(fragment.getNestedBlocks());
			fragment.setFragmentParentNode(fragmentParentNode);
		}
		if(fragment.getChildren()==null || fragment.getChildren().isEmpty()) {
			return;
		}

		List<Fragment> children = fragment.getChildren();
		if(children.size() > 1) {
			for(Fragment child : children) {
				Node parentNode = highestDifferentiator(child, children);
				if(parentNode == null) {
					if(child.getNestedBlocks().size() == 1) {
						LOG.warn("No differentiator for single node{} of {} in {}", XPathHelper.getSkeletonXpath(child.getNestedBlocks().get(0)), child.getId(), child.getReferenceState().getName());
						parentNode = child.getNestedBlocks().get(0);
					}
				}
				child.setFragmentParentNode(parentNode);
				//				System.out.println("Fragment " + child.getId() + ":" + XPathHelper.getSkeletonXpath(child.getFragmentParentNode()));
				setParentNode(child);
			}
		}
		else {
			setParentNode(children.get(0));
		}
	}

	private void addFragment(Fragment fragment) {
		if(this.fragments == null) {
			this.fragments = new ArrayList<>();
		}
		if(!this.fragments.contains(fragment))
			this.fragments.add(fragment);
	}

	@Override
	public Fragment getRootFragment() {
		return rootFragment;
	}

	private void printFragment(Fragment frag, StringBuilder buffer, String prefix, String childrenPrefix, boolean domOnly) {
		buffer.append(prefix);
		buffer.append(frag.getId());
		buffer.append('\n');

		for (Iterator<Fragment> it = domOnly ?  frag.getDomChildren().iterator():  frag.getChildren().iterator(); it.hasNext();) {
			Fragment next = it.next();
			if (it.hasNext()) {
				printFragment(next, buffer, childrenPrefix + "├── ", childrenPrefix + "│   ", domOnly);
			} else {
				printFragment(next, buffer, childrenPrefix + "└── ", childrenPrefix + "    ", domOnly);
			}
		}

	}
	public void printFragments(boolean domOnly) {
		StringBuilder buffer = new StringBuilder(1000);
		printFragment(rootFragment, buffer, "", "", domOnly);
		System.out.println( buffer.toString());
	}

	public Fragment getClosestDomFragment(Node node) {
		Node parent = node;
		while(parent != null ) {
			Fragment fragment = getFragment(VipsUtils.getFragParent(parent));
			if(fragment!= null) {
				LOG.debug("Closest dom fragment {} in {} for {}", fragment.getId(), this.getName(), XPathHelper.getSkeletonXpath(node));
				return fragment;
			}
			parent = parent.getParentNode();
		}
		return null;
	}


	private Fragment getFragment(int fragParent) {
		if(fragParent>=0) {
			Fragment frag = fragmentMap.get(fragParent);
			if(frag!=null && FragmentManager.usefulFragment(frag)) {
				return frag;
			}
		}
		return null;
	}

	@Override
	public Fragment getClosestFragment(Node node) {
		Fragment root = this.getRootFragment();
		if (!root.containsNode(node)) {
			return null;
		}

		return getClosestFragment(node, root);
	}

	private Fragment getClosestFragment(Node node, Fragment root) {
		//		System.out.println("Node to check :" + XPathHelper.getSkeletonXpath(node));
		if(root.getChildren().isEmpty()) {
			return root;
		}
		boolean foundChild = false;
		for (Fragment child : root.getChildren()) {
			//			System.out.println("Fragment " + child.getId() + ":" + XPathHelper.getSkeletonXpath(child.getFragmentParentNode()));
			if (child.containsNode(node)) {
				if (FragmentManager.usefulFragment(child)) {
					root = child;
					foundChild = true;
					break;
				}
				else {
					return root;
				}
			}
			//			else {
			//				LOG.info("Child {} does not contain the node ", child.getId());
			//			}
		}
		if(foundChild)
			LOG.debug("Child {} is closer and useful" , root.getId());

		else {
			LOG.debug("This is the closest Fragmemt {}", root.getId());
		}

		return foundChild ? getClosestFragment(node, root): root;

	}

	public Fragment getClosestFragment(CandidateElement element, Fragment root) {

		if(root.getChildren().isEmpty()) {
			return root;
		}
		boolean foundChild = false;
		for (Fragment child : root.getChildren()) {
			if (child.containsCandidate(element)) {
				if (FragmentManager.usefulFragment(child)) {
					root = child;
					foundChild = true;
					break;
				}
				else {
					return root;
				}
			}
		}


		return foundChild ? getClosestFragment(element, root): root;

	}


	@Override
	public Fragment getClosestDomFragment(CandidateElement element){
		if(element.getClosestDomFragment()!=null) {
			return element.getClosestDomFragment();
		}
		Node node =element.getElement();
		Fragment closestFragment = getClosestDomFragment(node);

		if(closestFragment!=null) {
			if(closestFragment.containsCandidate(element)) {
				element.setClosestDomFragment(closestFragment);
				return closestFragment;
			}
		}
		return null;
	}


	@Override
	public Fragment getClosestFragment(CandidateElement element) throws Exception{
		if(element.getClosestFragment()!=null) {
			return element.getClosestFragment();
		}
		Node node =element.getElement();
		Fragment closestFragment = getClosestFragment(node);

		if(closestFragment!=null) {
			if(closestFragment.containsCandidate(element)) {
				element.setClosestFragment(closestFragment);
				return closestFragment;
			}
		}
		//		return getClosestFragment(element, root);
		return null;
	}


	protected void addCandidatesToFragments() {

		for(CandidateElement element: super.getCandidateElements()) {

			Fragment closest = getClosestFragment(element.getElement());
			Fragment closestDom = null;
			if(closest!=null) {
				if(closest.getFragmentParentNode()==null) {
					// Separate dom fragment hierarchy
					closestDom = getClosestDomFragment(element.getElement());
				}
				else {
					closestDom = closest;
				}

				// Add to normal hierarchy
				element.setClosestFragment(closest);
				Fragment fragment = closest;
				while(fragment!=null) {
					fragment.addCandidateElement(element);
					fragment = fragment.getParent();
				}

				// Add to DOM hierarchy
				element.setClosestDomFragment(closestDom);
				fragment = closestDom;
				while(fragment!=null) {
					fragment.addCandidateElement(element);
					fragment = fragment.getDomParent();
				}
			}
			else {
				LOG.error("Could not find closest fragment for {}", XPathHelper.getSkeletonXpath(element.getElement()));
			}

		}
	}
}

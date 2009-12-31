package com.crawljax.core.configuration;

import java.util.ArrayList;
import java.util.List;

import com.crawljax.condition.Condition;
import com.crawljax.condition.browserwaiter.ExpectedCondition;
import com.crawljax.condition.browserwaiter.WaitCondition;
import com.crawljax.condition.crawlcondition.CrawlCondition;
import com.crawljax.condition.invariant.Invariant;
import com.crawljax.oracle.ComparatorWithPreconditions;
import com.crawljax.oracle.Oracle;

/**
 * Specifies the crawl options for a single crawl session. The user must specify which HTML elements
 * should be clicked during the crawl session.
 * <p/>
 * The scope can be restricted using {@link #setDepth(int)}and {@link #setMaximumStates(int)}. The
 * duration can be restricted using {@link #setMaximumRuntime(int)}.
 * <p/>
 * By default Crawljax fills in random values for input fields
 * {@link #setRandomInputInForms(boolean)}. Specific input for form elements can be defined with
 * {@link #setInputSpecification(InputSpecification)}. Default values: Maximum runtime: 3600 seconds
 * Time to wait after initial pageload: 500 milliseconds Time to wait after clicking HTML elements:
 * 500 milliseconds Enter random form input data: true EXAMPLE: CrawlSpecification crawler = new
 * CrawlSpecification("http://www.google.com"); //click these elements crawler.click("a");
 * crawler.click("input").withAttribute("type", "submit"); onLoginPageCondition = new
 * UrlCondition("#login"); crawler.when(onLoginPageCondition).click("a").withText("Login"); //but
 * don't click these crawler.dontClick("a").underXpath("//DIV[@id='guser']");
 * crawler.dontClick("a").withText("Language Tools"); //restrict the scope of the crawling
 * crawler.setCrawlMaximumStates(15); crawler.setCrawlDepth(2);
 * 
 * @author DannyRoest@gmail.com (Danny Roest)
 */
public class CrawlSpecification {

	private static final int DEFAULT_MAXIMUMRUNTIME = 3600;
	private static final int DEFAULT_WAITTIMEAFTERRELOADURL = 500;
	private static final int DEFAULT_WAITTIMEAFTEREVENT = 500;

	private final String url;

	private final List<String> crawlEvents = new ArrayList<String>();
	private int depth = 0;
	private int maximumStates = 0;
	private int maximumRuntime = DEFAULT_MAXIMUMRUNTIME; // in seconds
	private int waitTimeAfterReloadUrl = DEFAULT_WAITTIMEAFTERRELOADURL; // in milliseconds
	private int waitTimeAfterEvent = DEFAULT_WAITTIMEAFTEREVENT; // in milliseconds
	private final CrawlActions crawlActions = new CrawlActions();

	private boolean randomInputInForms = true;
	private InputSpecification inputSpecification = new InputSpecification();

	private boolean testInvariantsWhileCrawling = true;
	private final List<Invariant> invariants = new ArrayList<Invariant>();

	private final List<ComparatorWithPreconditions> oracleComparators =
	        new ArrayList<ComparatorWithPreconditions>();
	private final List<WaitCondition> waitConditions = new ArrayList<WaitCondition>();
	private final List<CrawlCondition> crawlConditions = new ArrayList<CrawlCondition>();

	/**
	 * @param url
	 *            the site to crawl
	 */
	public CrawlSpecification(String url) {
		this.crawlEvents.add("onclick");
		this.url = url;
	}

	/**
	 * Specifies that Crawljax should click all the default clickable elements. These include: All
	 * anchor tags All buttons Any div
	 */
	public void clickDefaultElements() {
		crawlActions.click("a");
		crawlActions.click("button");
		crawlActions.click("input").withAttribute("type", "submit");
		crawlActions.click("input").withAttribute("type", "button");
	}

	/**
	 * Set of HTML elements Crawljax will click during crawling For exmple 1) <a.../> 2) <div/>
	 * click("a") will only include 1 This set can be restricted by {@link #dontClick(String)}.
	 * 
	 * @param tagName
	 *            the tag name of the elements to be included
	 * @return this CrawlElement
	 */
	public CrawlElement click(String tagName) {
		return crawlActions.click(tagName);
	}

	/**
	 * Set of HTML elements Crawljax will NOT click during crawling When an HTML is present in the
	 * click and dontClick sets, then the element will not be clicked. For example: 1) <a
	 * href="#">Some text</a> 2) <a class="foo" .../> 3) <div class="foo" .../> click("a")
	 * dontClick("a").withAttribute("class", "foo"); Will include only include HTML element 2
	 * 
	 * @param tagName
	 *            the tag name of the elements to be excluded
	 * @return this CrawlElement
	 */
	public CrawlElement dontClick(String tagName) {
		return crawlActions.dontClick(tagName);
	}

	/**
	 * Crawljax will the HTML elements while crawling if and only if all the specified conditions
	 * are satisfied. IMPORTANT: only works with click()!!! For example:
	 * when(onContactPageCondition) will only click the HTML element if it is on the contact page
	 * 
	 * @param conditions
	 *            the condition to be met.
	 * @return this CrawlActions
	 */
	public CrawlActions when(Condition... conditions) {
		return crawlActions.when(conditions);
	}

	/**
	 * @return the initial url of the site to crawl
	 */
	protected String getUrl() {
		return url;
	}

	/**
	 * @return the maximum crawl depth
	 */
	protected int getDepth() {
		return depth;
	}

	/**
	 * Sets the maximum crawl depth. 1 is one click, 2 is two clicks deep, ...
	 * 
	 * @param crawlDepth
	 *            the maximum crawl depth. 0 to ignore
	 */
	public void setDepth(int crawlDepth) {
		this.depth = crawlDepth;
	}

	/**
	 * @return the crawlMaximumStates
	 */
	protected int getMaximumStates() {
		return maximumStates;
	}

	/**
	 * Sets the maximum number of states. Crawljax will stop crawling when this maximum number of
	 * states are found
	 * 
	 * @param crawlMaximumStates
	 *            the maximum number of states. 0 specifies no bound for the number of crawl states.
	 */
	public void setMaximumStates(int crawlMaximumStates) {
		this.maximumStates = crawlMaximumStates;
	}

	/**
	 * @return the crawlMaximumRuntime
	 */
	protected int getMaximumRuntime() {
		return maximumRuntime;
	}

	/**
	 * Sets the maximum time for Crawljax to run. Crawljax will stop crawling when this timelimit is
	 * reached.
	 * 
	 * @param seconds
	 *            the crawlMaximumRuntime to set
	 */
	public void setMaximumRuntime(int seconds) {
		this.maximumRuntime = seconds;
	}

	/**
	 * @return whether to Crawljax should enter random values in form input fields
	 */
	protected boolean getRandomInputInForms() {
		return randomInputInForms;
	}

	/**
	 * @param value
	 *            whether to Crawljax should enter random values in form input fields
	 */
	public void setRandomInputInForms(boolean value) {
		this.randomInputInForms = value;
	}

	/**
	 * @return the number of milliseconds to wait after reloading the url
	 */
	protected int getWaitTimeAfterReloadUrl() {
		return waitTimeAfterReloadUrl;
	}

	/**
	 * @param milliseconds
	 *            the number of milliseconds to wait after reloading the url
	 */
	public void setWaitTimeAfterReloadUrl(int milliseconds) {
		this.waitTimeAfterReloadUrl = milliseconds;
	}

	/**
	 * @return the number the number of milliseconds to wait after an event is fired
	 */
	protected int getWaitTimeAfterEvent() {
		return waitTimeAfterEvent;
	}

	/**
	 * @param milliseconds
	 *            the number of milliseconds to wait after an event is fired
	 */
	public void setWaitTimeAfterEvent(int milliseconds) {
		this.waitTimeAfterEvent = milliseconds;
	}

	/**
	 * @return the events that should be fired (e.g. onclick)
	 */
	protected List<String> getCrawlEvents() {
		return crawlEvents;
	}

	/**
	 * @return the inputSpecification in which data for input field is specified
	 */
	protected InputSpecification getInputSpecification() {
		return inputSpecification;
	}

	/**
	 * @param inputSpecification
	 *            in which data for input fields is specified
	 */
	public void setInputSpecification(InputSpecification inputSpecification) {
		this.inputSpecification = inputSpecification;
	}

	/**
	 * @return the different crawl actions.
	 */
	protected CrawlActions crawlActions() {
		return crawlActions;
	}

	/**
	 * @return the oracleComparators
	 */
	protected List<ComparatorWithPreconditions> getOracleComparators() {
		return oracleComparators;
	}

	/**
	 * Adds the Oracle Comparator to the list of comparators.
	 * 
	 * @param id
	 *            a name for the Oracle Comparator.
	 * @param oracleComparator
	 *            the oracle to be added.
	 */
	public void addOracleComparator(String id, Oracle oracleComparator) {
		this.oracleComparators.add(new ComparatorWithPreconditions(id, oracleComparator));
	}

	/**
	 * Adds an Oracle Comparator with preconditions to the list of comparators.
	 * 
	 * @param id
	 *            a name for the Oracle Comparator
	 * @param oracleComparator
	 *            the oracle to be added.
	 * @param preConditions
	 *            the preconditions to be met.
	 */
	public void addOracleComparator(String id, Oracle oracleComparator,
	        Condition... preConditions) {
		this.oracleComparators.add(new ComparatorWithPreconditions(id, oracleComparator,
		        preConditions));
	}

	/**
	 * @return the invariants
	 */
	protected List<Invariant> getInvariants() {
		return invariants;
	}

	/**
	 * @param description
	 *            the description of the invariant.
	 * @param condition
	 *            the condition to be met.
	 */
	public void addInvariant(String description, Condition condition) {
		this.invariants.add(new Invariant(description, condition));
	}

	/**
	 * @param description
	 *            the description of the invariant.
	 * @param condition
	 *            the invariant condition.
	 * @param preConditions
	 *            the precondition.
	 */
	public void addInvariant(String description, Condition condition, Condition... preConditions) {
		this.invariants.add(new Invariant(description, condition, preConditions));
	}

	/**
	 * @return whether invariants should be tested while crawling.
	 */
	protected boolean getTestInvariantsWhileCrawling() {
		return testInvariantsWhileCrawling;
	}

	/**
	 * @param testInvariantsWhileCrawling
	 *            whether invariants should be tested while crawling
	 */
	public void setTestInvariantsWhileCrawling(boolean testInvariantsWhileCrawling) {
		this.testInvariantsWhileCrawling = testInvariantsWhileCrawling;
	}

	/**
	 * @return the waitConditions
	 */
	protected List<WaitCondition> getWaitConditions() {
		return waitConditions;
	}

	/**
	 * @param url
	 *            the full url or a part of the url where should be waited for the
	 *            expectedConditions
	 * @param expectedConditions
	 *            the conditions to wait for.
	 */
	public void waitFor(String url, ExpectedCondition... expectedConditions) {
		this.waitConditions.add(new WaitCondition(url, expectedConditions));
	}

	/**
	 * @param url
	 *            the full url or a part of the url where should be waited for the
	 *            expectedConditions
	 * @param expectedConditions
	 *            the conditions to wait for
	 * @param timeout
	 *            the timeout
	 */
	public void waitFor(String url, int timeout, ExpectedCondition... expectedConditions) {
		this.waitConditions.add(new WaitCondition(url, timeout, expectedConditions));
	}

	/**
	 * @return the crawlConditions
	 */
	protected List<CrawlCondition> getCrawlConditions() {
		return crawlConditions;
	}

	/**
	 * @param description
	 *            the description
	 * @param crawlCondition
	 *            the condition
	 */
	public void addCrawlCondition(String description, Condition crawlCondition) {
		this.crawlConditions.add(new CrawlCondition(description, crawlCondition));
	}

	/**
	 * @param description
	 *            the description
	 * @param crawlCondition
	 *            the condition
	 * @param preConditions
	 *            the preConditions
	 */
	public void addCrawlCondition(String description, Condition crawlCondition,
	        Condition... preConditions) {
		this.crawlConditions.add(new CrawlCondition(description, crawlCondition, preConditions));
	}

}
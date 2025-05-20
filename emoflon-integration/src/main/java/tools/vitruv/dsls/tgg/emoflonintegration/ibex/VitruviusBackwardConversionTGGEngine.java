package tools.vitruv.dsls.tgg.emoflonintegration.ibex;

import language.DomainType;
import language.TGGRule;
import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.emoflon.ibex.common.operational.IContextPatternInterpreter;
import org.emoflon.ibex.common.operational.IMatch;
import org.emoflon.ibex.common.operational.IMatchObserver;
import org.emoflon.ibex.common.operational.IPatternInterpreterProperties;
import org.emoflon.ibex.patternmodel.IBeXPatternModel.IBeXContext;
import org.emoflon.ibex.patternmodel.IBeXPatternModel.IBeXModel;
import org.emoflon.ibex.patternmodel.IBeXPatternModel.IBeXPatternSet;
import org.emoflon.ibex.tgg.compiler.patterns.PatternSuffixes;
import org.emoflon.ibex.tgg.compiler.patterns.PatternUtil;
import org.emoflon.ibex.tgg.operational.IBlackInterpreter;
import org.emoflon.ibex.tgg.operational.benchmark.TimeMeasurable;
import org.emoflon.ibex.tgg.operational.benchmark.Times;
import org.emoflon.ibex.tgg.operational.strategies.modules.TGGResourceHandler;
import tools.vitruv.dsls.tgg.emoflonintegration.Timer;
import org.emoflon.ibex.tgg.operational.defaults.IbexOptions;
import org.emoflon.ibex.tgg.operational.matches.ITGGMatch;
import org.emoflon.ibex.tgg.operational.monitoring.IbexObserver;
import org.emoflon.ibex.tgg.operational.strategies.OperationalStrategy;
import org.emoflon.ibex.tgg.operational.strategies.PropagationDirectionHolder.PropagationDirection;
import org.emoflon.ibex.tgg.operational.strategies.modules.IbexExecutable;
import runtime.CorrespondenceNode;
import runtime.TGGRuleApplication;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.composite.description.VitruviusChange;
import tools.vitruv.change.composite.description.VitruviusChangeFactory;
import tools.vitruv.dsls.tgg.emoflonintegration.Util;
import tools.vitruv.dsls.tgg.emoflonintegration.ibex.smartEmfFix.SmartEMFResourceFactoryImplFixed;
import tools.vitruv.dsls.tgg.emoflonintegration.patternconversion.IbexPatternToChangeSequenceTemplateConverter;
import tools.vitruv.dsls.tgg.emoflonintegration.patternconversion.ChangeSequenceTemplateSet;
import tools.vitruv.dsls.tgg.emoflonintegration.patternmatching.*;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * The Pattern Matcher acting as the counterpart for {@link VitruviusTGGChangePropagationIbexEntrypoint},
 * which inherits {@link org.emoflon.ibex.tgg.operational.strategies.sync.SYNC}.
 * <br/>
 * Here, pattern conversion {@link IbexPatternToChangeSequenceTemplateConverter} is triggered,
 * as well as all process steps relevant for generating, handling and choosing matches.
 * The resulting additive or subtractive matches are fed to the {@link VitruviusTGGChangePropagationIbexEntrypoint} via {@link IMatchObserver}, which applies some and calls back this class'
 * {@link #updateMatches()}.
 * <br/>
 * Further, this class implements {@link IbexObserver} to be able to retrieve information about applied forward {@link VitruviusBackwardConversionMatch}es.
 * That is relevant for calculating the next set of matches.
 * Since the {@link IbexObserver} doesn't inform about revoked matches, this class holds an {@link VitruviusTGGIbexRedInterpreter} to enable that knowledge.
 *
 */
public class VitruviusBackwardConversionTGGEngine implements IBlackInterpreter, TimeMeasurable, IContextPatternInterpreter, IbexObserver {
    protected static final Logger logger = Logger.getLogger(VitruviusBackwardConversionTGGEngine.class);
    private boolean needs_paranoid_modifications = false;

    private IMatchObserver iMatchObserver;
    private final URI baseURI;
    private IbexOptions ibexOptions;
    private ResourceSet resourceSet;
    private Resource ibexPatternsResource;
    private IBeXModel ibexModel;
    private IbexExecutable ibexExecutable;
    private Set<VitruviusBackwardConversionMatch> matchesFound;
    private final Set<IMatch> matchesThatHaveBeenApplied;
    private OperationalStrategy observedOperationalStrategy;

    private VitruviusChangePatternMatcher vitruviusChangePatternMatcher;
    private VitruviusChangeBrokenMatchMatcher vitruviusChangeBrokenMatchMatcher;
    private ChangeSequenceTemplateSet changeSequenceTemplateSet;
    private final VitruviusChange<EObject> vitruviusChange;
    private final Map<String, Timer> timeMeasurements;
    private Timer contextMatchingTotalTimer;
    private Timer coverageFlatteningTotalTimer;

    private final PropagationDirection propagationDirection;

    private boolean preexistingConsistencyMatchesInitialized = false;
    private VitruviusTGGIbexRedInterpreter vitruviusTGGIbexRedInterpreter;

    private final Set<VitruviusConsistencyMatch> matchesThatHaveBeenTriedToRepair;

    /**
     *
     * @param vitruviusChange the initial change sequence where the forward matching and parts of the broken match detection are based upon.
     * @param propagationDirection {@link PropagationDirection#FORWARD} or {@link PropagationDirection#BACKWARD},
     *                                                                 depending on what model the given vitruviusChange concerns.
     */
    public VitruviusBackwardConversionTGGEngine(VitruviusChange<EObject> vitruviusChange, PropagationDirection propagationDirection) {
        this.vitruviusChange = new VitruviusChangeTransformer(vitruviusChange).transform();
        this.timeMeasurements = new HashMap<>();
        this.baseURI = URI.createPlatformResourceURI("/", true);
        this.matchesThatHaveBeenApplied = new HashSet<>();
        this.matchesThatHaveBeenTriedToRepair = new HashSet<>();
        this.propagationDirection = propagationDirection;

        // sum up certain sub-aspects' times
        this.contextMatchingTotalTimer = new Timer();
        this.coverageFlatteningTotalTimer = new Timer();
    }

    @Override
    public void initialise(IbexExecutable ibexExecutable, IbexOptions ibexOptions, EPackage.Registry registry, IMatchObserver iMatchObserver) {
        this.ibexExecutable = ibexExecutable;
        this.ibexOptions = ibexOptions;
        this.iMatchObserver = iMatchObserver;
        this.setIbexPatternsResource(new File(ibexOptions.project.workspacePath() + "/"
                + ibexOptions.project.path() + "/bin/" + ibexOptions.project.path() + "/"
                + "sync/hipe/engine/ibex-patterns.xmi"));
        this.ibexModel = ((IBeXModel) ibexPatternsResource.getContents().getFirst());
        logger.debug("Now initializing patterns: ");

        this.initPatterns(ibexModel.getPatternSet());
    }

    @Override
    public void initPatterns(IBeXPatternSet iBeXPatternSet) {
        Timer timer = new Timer();
        timeMeasurements.put("pattern conversion", timer);
        timer.start();

        this.changeSequenceTemplateSet = new IbexPatternToChangeSequenceTemplateConverter(
                this.ibexModel,
                this.ibexOptions.tgg.flattenedTGG(),
                propagationDirection.equals(PropagationDirection.FORWARD) ? DomainType.SRC : DomainType.TRG)
                .convert();
        this.vitruviusChangePatternMatcher = new VitruviusChangePatternMatcher(vitruviusChange, changeSequenceTemplateSet);

        this.vitruviusChangeBrokenMatchMatcher = new VitruviusChangeBrokenMatchMatcher(vitruviusChange, this.ibexOptions.tgg.flattenedTGG().getRules().stream().filter(tggRule -> !tggRule.isAbstract()).collect(Collectors.toSet()));

        timer.stop();

        logger.info("Pattern Conversion took " + timer.getTime(TimeUnit.MILLISECONDS) + " ms");
        for (IBeXContext contextPattern : iBeXPatternSet.getContextPatterns()) {
            PatternUtil.registerPattern(contextPattern.getName(), PatternSuffixes.extractType(contextPattern.getName()));
        }
    }

    @Override
    public void initialise(EPackage.Registry registry, IMatchObserver iMatchObserver) {
        this.iMatchObserver = iMatchObserver;
    }

    @Override
    public ResourceSet createAndPrepareResourceSet(String workspacePath) {
        this.resourceSet = new ResourceSetImpl();
        // use custom class for bugfix.
        resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap()
                .put(Resource.Factory.Registry.DEFAULT_EXTENSION, new SmartEMFResourceFactoryImplFixed(workspacePath));
        try {
            resourceSet.getURIConverter().getURIMap().put(
                    URI.createPlatformResourceURI("/", true),
                    URI.createFileURI(new File(workspacePath).getCanonicalPath() + File.separator));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return resourceSet;
    }

    @Override
    public void monitor(Collection<Resource> collection) {
        logger.debug("::monitor called. We do not use this.");
    }

    /**
     * IBeX doesn't seem to do this on its own (at least I cannot find out how and where), so we do it here:
     * Initialize the matches that are known from the protocol by creating them and handling them to the {@link IMatchObserver}.
     */
    private void initializePreexistingConsistencyMatchesIfNotAlreadyPresent() {
        if (!preexistingConsistencyMatchesInitialized) {
            Timer timer = new Timer();
            timeMeasurements.put("Initializing preexisting consistency matches", timer);
            timer.start();
            Map<TGGRuleApplication, TGGRule> tggRuleApplicationTGGRuleMap = Util.getTGGRuleApplicationsWithRules(this.ibexExecutable.getResourceHandler(),
                    this.ibexOptions.tgg.flattenedTGG().getRules().stream().filter(tggRule -> !tggRule.isAbstract()).collect(Collectors.toSet()));
            Set<IMatch> consistencyMatches = tggRuleApplicationTGGRuleMap.keySet().stream()
                    .map(tggRuleApplication -> new VitruviusConsistencyMatch(tggRuleApplication, tggRuleApplicationTGGRuleMap.get(tggRuleApplication)))
                    .collect(Collectors.toSet());
            timer.stop();
            logger.trace("  LOADED MARKERS : \n    - " + consistencyMatches.stream().map(IMatch::toString).collect(Collectors.joining("\n    - ")));
            this.iMatchObserver.addMatches(consistencyMatches);
        }
        this.ibexExecutable.getOptions().tgg.flattenedTGG().getRules();
        this.preexistingConsistencyMatchesInitialized = true;
    }

    @Override
    public void updateMatches() {
        // otherwise matches from the protocol are ignored and context matching from a previous run ain't possible...
        initializePreexistingConsistencyMatchesIfNotAlreadyPresent();

        // new forward matches. currently only creating forward matches ONCE since we match against the whole change sequence...
        createForwardMatchesIfNotAlreadyPresent();

        // as the pattern matching is incremental, we ensure that only matches that have not already been applied before, are considered.
        Set<VitruviusBackwardConversionMatch> remainingMatches = getMatchesThatHaventBeenAppliedAndAreStillIntact();

        /*
            We do not give ALL matches to SYNC but only those that CURRENTLY match context.
            As matches are applied by SYNC, new matches from this engine may become possible again!
            Flattening must happen after that (and thus, often...) because, if done before context matching,
            it might throw out matches because it prefers matches whose context doesn't match.
         */
        matchContext_flatten_andHandToSYNC(remainingMatches);

        Set<VitruviusConsistencyMatch> brokenMatches = getBrokenMatches();
        brokenMatches.forEach(brokenMatch -> {
            logger.trace("Trying to revoke broken match: " + ((VitruviusConsistencyMatch) brokenMatch).toVerboseString());
            this.iMatchObserver.removeMatch(brokenMatch);
        });

        // Repair matches that are broken and have not been repaired by shortcut rules.
        // Created new forward matches are handed to sync and also stored to be treated as normal forward matches by this method.
        repairUnrepairedBrokenMatches();
    }

    private void repairUnrepairedBrokenMatches() {
        // Only try to repair broken matches ONCE
        Set<VitruviusConsistencyMatch> unrepairedAndUntriedBrokenMatches = this.vitruviusTGGIbexRedInterpreter.getRevokedRuleMatches().stream()
                .map(match -> (VitruviusConsistencyMatch) match)
                .filter(match -> !matchesThatHaveBeenTriedToRepair.contains(match))
                .collect(Collectors.toSet());
        if (unrepairedAndUntriedBrokenMatches.isEmpty()) {
            return; // no point
        }

        // Try to calculate new forward matches:
        List<EChange<EObject>> newChangeSequence = new UnrepairedBrokenMatchOldChangesRetriever(
                this.observedOperationalStrategy.getResourceHandler(), this.ibexOptions.tgg.flattenedTGG().getRules().stream().filter(tggRule -> !tggRule.isAbstract()).collect(Collectors.toSet()),
                unrepairedAndUntriedBrokenMatches, propagationDirection)
                .createNewChangeSequence();

        // Utilize EChanges that have been left over from the main pattern matching, too!
        newChangeSequence.addAll(vitruviusChangePatternMatcher.getUnmatchedEChanges());
        VitruviusChangePatternMatcher newVitruviusChangePatternMatcher = new VitruviusChangePatternMatcher(
                VitruviusChangeFactory.getInstance().createTransactionalChange(newChangeSequence),
                changeSequenceTemplateSet
        );
        Set<VitruviusBackwardConversionMatch> newMatches = newVitruviusChangePatternMatcher.getAdditiveMatches(propagationDirection);

        // Only try to repair broken matches ONCE
        matchesThatHaveBeenTriedToRepair.addAll(unrepairedAndUntriedBrokenMatches);

        // Add matches for further calls of SYNC to THIS->updateMatches
        this.matchesFound.addAll(newMatches);

        // do one run in case this was the last call from SYNC...
        matchContext_flatten_andHandToSYNC(newMatches);
    }

    /**
     * Tries to match context (see {@link VitruviusBackwardConversionMatch#contextMatches(TGGResourceHandler, PropagationDirection)}) on each given match.
     * Context-matched matches are then <I>flattened</I>/ selected by the {@link PatternCoverageFlattener},
     * meaning that it is ensured that there are no overlaps between matches in the change sequence, such that a change is covered by more than one {@link VitruviusBackwardConversionMatch}.
     * Finally, the matches are handed to SYNC by calling the {@link IMatchObserver}.
     */
    private void matchContext_flatten_andHandToSYNC(Set<VitruviusBackwardConversionMatch> matches) {
        Timer contextMatchingTimer = new Timer();
        contextMatchingTimer.start();
        Set<VitruviusBackwardConversionMatch> matchesToBeFlattened = matches.stream()
                .filter(match -> match.contextMatches(this.observedOperationalStrategy.getResourceHandler(), this.propagationDirection))
                .collect(Collectors.toSet());
        contextMatchingTimer.stop();
        this.contextMatchingTotalTimer = this.contextMatchingTotalTimer.add(contextMatchingTimer);


        Timer coverageFlatteningTimer = new Timer();
        coverageFlatteningTimer.start();
        Set<VitruviusBackwardConversionMatch> flattenedPatternApplications = new PatternCoverageFlattener(matchesToBeFlattened, vitruviusChange).getFlattenedPatternApplications();
        coverageFlatteningTimer.stop();
        this.coverageFlatteningTotalTimer = this.coverageFlatteningTotalTimer.add(coverageFlatteningTimer);
        this.iMatchObserver.addMatches(
                // "Flattening": Choose patterns to form a Coverage where each change is covered by at most one pattern and convert them to Matches
                new HashSet<>(flattenedPatternApplications)
        );
    }

    @Override
    public void terminate() {
        logger.info("VitruviusBackwardConversionTGGEngine::terminate called. We do not use this.");
    }

    @Override
    public void setDebugPath(String s) {
        // implement if needed...
    }

    @Override
    public IPatternInterpreterProperties getProperties() {
        return new IPatternInterpreterProperties() {

            /**
             * This is another ibex bug workaround. We need both paranoid and un-paranoid modifications to each prevent different kinds of bugs:
             * For deleting changes, we need it set to true, because otherwise some {@link UnsupportedOperationException} is thrown in some models (e.g. UML).
             * For additive changes, we need it set to false, because otherwise the serialization looks like ... and some references are lost, (e.g. UML-> Generalization::general)
             */
            @Override
            public boolean needs_paranoid_modificiations() { return needs_paranoid_modifications; }
        };
    }

    /**
     * Set the value that {@link VitruviusBackwardConversionTGGEngine#getProperties()#needs_paranoid_modifications} returns.
     * <br/>
     * This is another ibex bug workaround. We need both paranoid and un-paranoid modifications to each prevent different kinds of bugs:
     * For deleting changes, we need it set to true, because otherwise some {@link UnsupportedOperationException} is thrown in some models (e.g. UML).
     * For additive changes, we need it set to false, because otherwise the serialization looks like ... and some references are lost, (e.g. UML-> Generalization::general)
     */
    public void setNeeds_paranoid_modifications(boolean needs_paranoid_modifications) {
        this.needs_paranoid_modifications = needs_paranoid_modifications;
    }

    @Override
    public Times getTimes() {
        // we dont use this...
        return new Times();
    }

    /**
     *
     * @return matches broken (and not already revoked by the {@link VitruviusTGGIbexRedInterpreter})
     * as indicated by the initial change sequence or broken protocol entries.
     */
    private Set<VitruviusConsistencyMatch> getBrokenMatches() {
        return vitruviusChangeBrokenMatchMatcher
                .getBrokenMatches(this.observedOperationalStrategy.getResourceHandler())
                .stream().filter(brokenMatch -> !vitruviusTGGIbexRedInterpreter.getRevokedRuleMatches().contains(brokenMatch))
                .collect(Collectors.toSet());
    }

    /**
     * Create forward matches based on the pattern templates gotten from {@link IbexPatternToChangeSequenceTemplateConverter}, using the {@link VitruviusChangePatternMatcher}.
     */
    private void createForwardMatchesIfNotAlreadyPresent() {
        if (this.matchesFound == null) {

            Timer timer = new Timer();
            timeMeasurements.put("Main green forward matching", timer);
            timer.start();

            this.matchesFound = vitruviusChangePatternMatcher.getAdditiveMatches(propagationDirection);

            timer.stop();
            logger.info("Pattern Matching took " + timer.getTime(TimeUnit.MILLISECONDS) + " ms");
        }
    }

    /**
     * Matches could have been applied already or have become broken by another match having added EObjects that this match would create.
     * @return matches that can be applied, ignoring whether the context matches or not.
     */
    private Set<VitruviusBackwardConversionMatch> getMatchesThatHaventBeenAppliedAndAreStillIntact() {
        // we know what matches have been applied by monitoring what our ping-pong opponent does (in ::update()).
        Set<EObject> createdEObjectsAlreadyCovered = this.matchesThatHaveBeenApplied.stream()
                .map(match -> (VitruviusBackwardConversionMatch) match)
                .flatMap(match -> match.getEObjectsCreatedByThisMatch().stream())
                .collect(Collectors.toSet());

        // need to filter out those matches that contain CREATE nodes that have been applied by other rules!
        return this.matchesFound.stream()
                .filter(match -> !this.matchesThatHaveBeenApplied.contains(match))
                .filter(match -> match.getEObjectsThisMatchWouldCreate().stream().noneMatch(createdEObjectsAlreadyCovered::contains))
                .collect(Collectors.toSet());
    }


    protected Resource loadResource(String path) throws Exception {
        Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("xmi", new XMIResourceFactoryImpl());
        this.resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("xmi", new XMIResourceFactoryImpl());
        this.resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("*", new XMIResourceFactoryImpl());
        Resource modelResource = this.resourceSet.getResource(URI.createURI(path).resolve(this.baseURI), true);
        EcoreUtil.resolveAll(this.resourceSet);
        if (modelResource == null) {
            throw new IOException("File did not contain a valid model.");
        } else {
            return modelResource;
        }
    }

    private void setIbexPatternsResource(File ibexPatternsFile) {
        try {
            this.ibexPatternsResource = this.loadResource("file://" + ibexPatternsFile.getCanonicalPath());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, Timer> getTimeMeasurements() {
        timeMeasurements.put("context matching total", contextMatchingTotalTimer);
        timeMeasurements.put("coverage flattening total", coverageFlatteningTotalTimer);
        return timeMeasurements;
    }

    @Override
    public void update(ObservableEvent eventType, Object... objects) {
        if (Objects.requireNonNull(eventType) == ObservableEvent.MATCHAPPLIED) {
            if (!(objects[0] instanceof ITGGMatch match)) {
                throw new IllegalStateException("MATCHAPPLIED events must be of type IMatch");
            }
            logger.trace("SYNC has applied a match: " + match);

            this.matchesThatHaveBeenApplied.add(match);

            logger.trace("  - updated CORR-CACHING?: ");
            this.observedOperationalStrategy.getResourceHandler().getCorrCaching().forEach((eObject, corrs) ->
                logger.trace("    - " + Util.eObjectToString(eObject) + " -> " + corrs.stream().map(Util::eObjectToString).collect(Collectors.joining(", ")))
            );
        }
    }

    /**
     *
     * @return all new correspondences added in the process of pattern matching.
     */
    public Set<CorrespondenceNode> getNewlyAddedCorrespondences() {
        return this.matchesThatHaveBeenApplied.stream()
                .filter(match -> match instanceof VitruviusBackwardConversionMatch)
                .map(match -> (VitruviusBackwardConversionMatch) match)
                .flatMap(match -> match.getEObjectsCreatedByThisMatch().stream())
                .filter(eObject -> this.observedOperationalStrategy.getResourceHandler().getCorrCaching().containsKey(eObject))
                .flatMap(eObject -> this.observedOperationalStrategy.getResourceHandler().getCorrCaching().get(eObject).stream())
                .map(correspondenceNodeEObject -> (CorrespondenceNode) correspondenceNodeEObject)
                .collect(Collectors.toSet());
    }

    /**
     * Give this class knowledge about the correspondence graph and the protocol.
     */
    public void addObservedOperationalStrategy(OperationalStrategy observedOperationalStrategy) {
        this.observedOperationalStrategy = observedOperationalStrategy;
    }

    /**
     * Add the {@link VitruviusTGGIbexRedInterpreter} to enable this class to have knowledge about matches that have been revoked.
     */
    public void addVitruviusTGGIbexRedInterpreter(VitruviusTGGIbexRedInterpreter vitruviusTGGIbexRedInterpreter) {
        this.vitruviusTGGIbexRedInterpreter = vitruviusTGGIbexRedInterpreter;
    }

    /**
     *
     * @return all new forward matches that have been applied in the process of pattern matching (includes forward matches for repair).
     */
    public Set<VitruviusBackwardConversionMatch> getAppliedMatches() {
        return this.matchesThatHaveBeenApplied.stream()
                .filter(match -> match instanceof VitruviusBackwardConversionMatch)
                .map(match -> (VitruviusBackwardConversionMatch) match)
                .collect(Collectors.toSet());
    }
}

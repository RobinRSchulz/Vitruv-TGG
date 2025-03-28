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
import org.emoflon.ibex.tgg.operational.benchmark.Timer;
import org.emoflon.ibex.tgg.operational.benchmark.Times;
import org.emoflon.ibex.tgg.operational.defaults.IbexOptions;
import org.emoflon.ibex.tgg.operational.matches.ITGGMatch;
import org.emoflon.ibex.tgg.operational.monitoring.IbexObserver;
import org.emoflon.ibex.tgg.operational.strategies.OperationalStrategy;
import org.emoflon.ibex.tgg.operational.strategies.PropagationDirectionHolder.PropagationDirection;
import org.emoflon.ibex.tgg.operational.strategies.modules.IbexExecutable;
import org.emoflon.smartemf.persistence.SmartEMFResourceFactoryImpl;
import runtime.CorrespondenceNode;
import runtime.TGGRuleApplication;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.composite.description.VitruviusChange;
import tools.vitruv.change.composite.description.VitruviusChangeFactory;
import tools.vitruv.dsls.tgg.emoflonintegration.Util;
import tools.vitruv.dsls.tgg.emoflonintegration.patternconversion.IbexPatternToChangeSequenceTemplateConverter;
import tools.vitruv.dsls.tgg.emoflonintegration.patternconversion.ChangeSequenceTemplateSet;
import tools.vitruv.dsls.tgg.emoflonintegration.patternmatching.*;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Pattern Matcher implementing the generation of new matches based on
 * * the existing TGG derivation (protocol)
 * * Pattern templates:
 *      * TGG patterns converted to the EChanges that they consist of.
 *      * todo precompiled (for performance opt.)
 *      * todo short-cut-rules?
 * * matching those templates onto a given VitruviusChange
 * <br/>
 * TODO:
 * 1. Generating pattern templates
 *      1. Design a class structure for pattern templates
 *      2. Design an algorithm to
 *          1. map those templates onto a given change sequence
 *          2. choose between found matches via strategy (? here or in SYNC?)
 *          3. Propose a selection of matches to the SYNC algorithm.
 *          4. Keep in 'mind', which changes from the sequence have already been mapped.
 *          5. DELETE: Might need to revoke found mappings.
 *          6. If all changes are covered, output the Sequence of pattern applications.
 *      3. Map pattern applications to their respective target application sequence
 *      4. Generate an EChange-Sequence or VitruviusChange out of that.
 *      5. Apply that change to the target model. (maybe same step as 6.)
 *      6. Hand that change to Vitruvius (maybe same step as 5.)
 * 2.
 *
 */
public class VitruviusBackwardConversionTGGEngine implements IBlackInterpreter, TimeMeasurable, IContextPatternInterpreter, IbexObserver {
    protected static final Logger logger = Logger.getLogger(VitruviusBackwardConversionTGGEngine.class);

    private EPackage.Registry registry;
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
    private final Times times;
    private final PropagationDirection propagationDirection;

    private boolean preexistingConsistencyMatchesInitialized = false;
    private VitruviusTGGIbexRedInterpreter vitruviusTGGIbexRedInterpreter;

    private final Set<VitruviusConsistencyMatch> matchesThatHaveBeenTriedToRepair;

    /**
     * TODO input here or in init function?
     * VitruviusChange cannot be given in initialize, so here.
     */
    public VitruviusBackwardConversionTGGEngine(VitruviusChange<EObject> vitruviusChange, PropagationDirection propagationDirection) {
        this.vitruviusChange = new VitruviusChangeTransformer(vitruviusChange).transform();
        this.times = new Times();
        this.baseURI = URI.createPlatformResourceURI("/", true);
        this.matchesThatHaveBeenApplied = new HashSet<>();
        this.matchesThatHaveBeenTriedToRepair = new HashSet<>();
        this.propagationDirection = propagationDirection;
    }

    @Override
    public void initialise(IbexExecutable ibexExecutable, IbexOptions ibexOptions, EPackage.Registry registry, IMatchObserver iMatchObserver) {
        //TODO maybe need todo the same as below!
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
        Timer.setEnabled(true);
        Timer.start();

        this.changeSequenceTemplateSet = new IbexPatternToChangeSequenceTemplateConverter(
                this.ibexModel,
                this.ibexOptions.tgg.flattenedTGG(),
                propagationDirection.equals(PropagationDirection.FORWARD) ? DomainType.SRC : DomainType.TRG)
                .convert();
        this.vitruviusChangePatternMatcher = new VitruviusChangePatternMatcher(vitruviusChange, changeSequenceTemplateSet);
        this.vitruviusChangeBrokenMatchMatcher = new VitruviusChangeBrokenMatchMatcher(vitruviusChange, this.ibexOptions.tgg.tgg().getRules());

        long stop = Timer.stop();

        logger.info("Pattern Conversion took " + (stop / 1000000d) + " ms");
        for (IBeXContext contextPattern : iBeXPatternSet.getContextPatterns()) {
            PatternUtil.registerPattern(contextPattern.getName(), PatternSuffixes.extractType(contextPattern.getName()));
        }
    }

    /**
     * todo remove this as it isn't called...
     */
    @Override
    public void initialise(EPackage.Registry registry, IMatchObserver iMatchObserver) {
        this.registry = registry;
        this.iMatchObserver = iMatchObserver;
    }

    @Override
    public ResourceSet createAndPrepareResourceSet(String workspacePath) {
        this.resourceSet = new ResourceSetImpl();
        resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap()
                .put(Resource.Factory.Registry.DEFAULT_EXTENSION, new SmartEMFResourceFactoryImpl(workspacePath));
//				.put(Resource.Factory.Registry.DEFAULT_EXTENSION, new XMIResourceFactoryImpl());
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
     * eMoflon doesn't seem to do this on its own (at least I cannot find out how and where), so we do it here..
     */
    private void initializePreexistingConsistencyMatchesIfNotAlreadyPresent() {
        if (!preexistingConsistencyMatchesInitialized) {
            Map<TGGRuleApplication, TGGRule> tggRuleApplicationTGGRuleMap = Util.getTGGRuleApplicationsWithRules(this.ibexExecutable.getResourceHandler(), this.ibexOptions.tgg.tgg().getRules());
            Set<IMatch> consistencyMatches = tggRuleApplicationTGGRuleMap.keySet().stream()
                    .map(tggRuleApplication -> new VitruviusConsistencyMatch(tggRuleApplication, tggRuleApplicationTGGRuleMap.get(tggRuleApplication)))
                    .collect(Collectors.toSet());
            logger.warn("  LOADED MARKERS : \n    - " + consistencyMatches.stream().map(IMatch::toString).collect(Collectors.joining("\n    - ")));
            this.iMatchObserver.addMatches(consistencyMatches);
        }
        this.preexistingConsistencyMatchesInitialized = true;
    }

    @Override
    public void updateMatches() {
        // otherwise matches from the protocol are ignored and context matching from a previous run ain't possible...
        initializePreexistingConsistencyMatchesIfNotAlreadyPresent();

        // new forward matches. currently only creating forward matches ONCE since we match against the whole change sequence...
        createForwardMatchesIfNotAlreadyPresent();
        Set<VitruviusBackwardConversionMatch> remainingMatches = getMatchesThatHaventBeenApplied();

        // we do not give ALL matches to SYNC but only those that CURRENTLY match context. As matches are applied by SYNC, new matches from this engine may become possible again!
        matchContext_flatten_andHandToSYNC(remainingMatches);

        Set<VitruviusConsistencyMatch> brokenMatches = getBrokenMatches();
        brokenMatches.forEach(brokenMatch -> {
            logger.trace("Trying to revoke broken match: " + ((VitruviusConsistencyMatch) brokenMatch).toVerboseString());
            this.iMatchObserver.removeMatch(brokenMatch);
        });

        // Repair matches that are broken and have not been repaired by shortcut rules
        if (ibexOptions.repair.useShortcutRules()) {
            throw new IllegalStateException("Using shortcut rules not supported yet: Need to filter out repaired matches from the broken matches...");
        }
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
                this.observedOperationalStrategy.getResourceHandler(), this.ibexOptions.tgg.tgg().getRules(),
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

    private void matchContext_flatten_andHandToSYNC(Set<VitruviusBackwardConversionMatch> matches) {
        Set<VitruviusBackwardConversionMatch> matchesToBeFlattened = matches.stream()
                .filter(match -> match.contextMatches(this.observedOperationalStrategy.getResourceHandler(), this.propagationDirection))
                .collect(Collectors.toSet());

        this.iMatchObserver.addMatches(
                // "Flattening": Choose patterns to form a Coverage where each change is covered by at most one pattern and convert them to Matches
                new HashSet<>(new PatternCoverageFlattener(matchesToBeFlattened, vitruviusChange).getFlattenedPatternApplications())
        );
    }

    @Override
    public void terminate() {
        logger.info("VitruviusBackwardConversionTGGEngine::terminate called. We do not use this.");
    }

    @Override
    public void setDebugPath(String s) {
        //TODO implement if needed
    }

    @Override
    public IPatternInterpreterProperties getProperties() {
        return new IPatternInterpreterProperties() {
            //TODO implement methods if needed (e.g. smartEMF support??) this by default returns false for every method
        };
    }

    @Override
    public Times getTimes() {
        return this.times;
    }

    private Set<VitruviusConsistencyMatch> getBrokenMatches() {
        //TODO might be sufficient if calculated once. for now, calculate every time
        return vitruviusChangeBrokenMatchMatcher
                .getBrokenMatches(this.observedOperationalStrategy.getResourceHandler())
                .stream().filter(brokenMatch -> !vitruviusTGGIbexRedInterpreter.getRevokedRuleMatches().contains(brokenMatch))
                .collect(Collectors.toSet());
    }

    private void createForwardMatchesIfNotAlreadyPresent() {
        if (this.matchesFound == null) {

            Timer.setEnabled(true);
            Timer.start();

            this.matchesFound = vitruviusChangePatternMatcher.getAdditiveMatches(propagationDirection);

            long stop = Timer.stop();
            logger.info("Pattern Matching took " + (stop / 1000000d) + " ms");
        }
    }

    /**
     *
     */
    private Set<VitruviusBackwardConversionMatch> getMatchesThatHaventBeenApplied() {
        // we know what matches have been applied by monitoring what our ping-pong opponent does (in ::update()).
        return this.matchesFound.stream().filter(match -> !this.matchesThatHaveBeenApplied.contains(match)).collect(Collectors.toSet());
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

    @Override
    public void update(ObservableEvent eventType, Object... objects) {
        if (Objects.requireNonNull(eventType) == ObservableEvent.MATCHAPPLIED) {
            //                this.getObservers().forEach((o) -> o.update(ObservableEvent.MATCHAPPLIED, new Object[]{match})); // this the code of the caller.
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

    public void addObservedOperationalStrategy(OperationalStrategy observedOperationalStrategy) {
        this.observedOperationalStrategy = observedOperationalStrategy;
    }

    public void addVitruviusTGGIbexRedInterpreter(VitruviusTGGIbexRedInterpreter vitruviusTGGIbexRedInterpreter) {
        this.vitruviusTGGIbexRedInterpreter = vitruviusTGGIbexRedInterpreter;
    }
}

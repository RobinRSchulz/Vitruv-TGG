package tools.vitruv.dsls.tgg.emoflonintegration.ibex;

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
import org.emoflon.ibex.tgg.operational.patterns.IGreenPattern;
import org.emoflon.ibex.tgg.operational.patterns.IGreenPatternFactory;
import org.emoflon.ibex.tgg.operational.strategies.OperationalStrategy;
import org.emoflon.ibex.tgg.operational.strategies.modules.IbexExecutable;
import org.emoflon.smartemf.persistence.SmartEMFResourceFactoryImpl;
import tools.vitruv.change.composite.description.VitruviusChange;
import tools.vitruv.dsls.tgg.emoflonintegration.Util;
import tools.vitruv.dsls.tgg.emoflonintegration.patternconversion.IbexPatternToChangeSequenceTemplateConverter;
import tools.vitruv.dsls.tgg.emoflonintegration.patternconversion.ChangeSequenceTemplateSet;
import tools.vitruv.dsls.tgg.emoflonintegration.patternmatching.VitruviusBackwardConversionMatch;
import tools.vitruv.dsls.tgg.emoflonintegration.patternmatching.VitruviusChangePatternMatcher;

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

    protected static final Logger logger = Logger.getRootLogger();
    private EPackage.Registry registry;
    private IMatchObserver iMatchObserver;
    private final URI baseURI;
    private IbexOptions ibexOptions;
    private ResourceSet resourceSet;
    private Resource ibexPatternsResource;
    private IBeXModel ibexModel;
    private IbexExecutable ibexExecutable;
    private Set<IMatch> matchesFound;
    private final Set<IMatch> matchesThatHaveBeenApplied;
    private OperationalStrategy observedOperationalStrategy;

    private ChangeSequenceTemplateSet changeSequenceTemplateSet;
    private VitruviusChange<EObject> vitruviusChange;
    private final Times times;

    /**
     * TODO input here or in init function?
     * VitruviusChange cannot be given in initialize, so here.
     */
    public VitruviusBackwardConversionTGGEngine(VitruviusChange<EObject> vitruviusChange) {
        this.vitruviusChange = vitruviusChange;
        this.times = new Times();
        this.baseURI = URI.createPlatformResourceURI("/", true);
        this.matchesThatHaveBeenApplied = new HashSet<>();
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
        logger.info("Now initializing patterns: ");

        this.initPatterns(ibexModel.getPatternSet());
    }

//    public void initialise(IbexExecutable executable, IbexOptions options, EPackage.Registry registry, IMatchObserver matchObserver) {
//        //TODO work this stuff into the above (?)
//        super.initialise(registry, matchObserver);
//        this.options = options;
//        this.executable = executable;
//        String cp = "";
//        String path = executable.getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
//        if (!path.contains("bin/")) {
//            path = path + "bin/";
//        }
//
//        path = path + this.generateHiPEClassName().replace(".", "/").replace("HiPEEngine", "ibex-patterns.xmi");
//        File file = new File(path);
//
//        try {
//            cp = file.getCanonicalPath();
//            cp = cp.replace("%20", " ");
//        } catch (IOException e1) {
//            e1.printStackTrace();
//        }
//
//        Resource r = null;
//
//        try {
//            r = this.loadResource("file://" + cp);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        IBeXModel ibexModel = (IBeXModel)r.getContents().get(0);
//        this.ibexPatterns = ibexModel.getPatternSet();
//
//        for(IBeXContext context : this.ibexPatterns.getContextPatterns()) {
//            PatternUtil.registerPattern(context.getName(), PatternSuffixes.extractType(context.getName()));
//        }
//
//        this.initPatterns(this.ibexPatterns);
//    }

    @Override
    public void initPatterns(IBeXPatternSet iBeXPatternSet) {
        Timer.setEnabled(true);
        Timer.start();

        this.changeSequenceTemplateSet = new IbexPatternToChangeSequenceTemplateConverter(this.ibexModel, this.ibexOptions.tgg.flattenedTGG()).convert();
        long stop = Timer.stop();

        logger.info("Pattern Conversion took " + (stop / 1000000d) + " ms");
        iBeXPatternSet.getContextPatterns().forEach( contextPattern ->
                PatternUtil.registerPattern(contextPattern.getName(), PatternSuffixes.extractType(contextPattern.getName())));
    }

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
        logger.warn("[VitruviusBackwardConversionTGGEngine::monitor(Collection<Resource> collection) NOT IMPLEMENTED YET!");
        logger.info(collection.stream().map(Resource::getURI).map(URI::toString).collect(Collectors.joining(", ")));
        //TODO here we get corr, protocol, model1 and model2
//        throw new RuntimeException("TODO implement!");
    }

    @Override
    public void updateMatches() {
        Timer.setEnabled(true);
        Timer.start();

        // new forward matches. TODO currently only creating forward matches ONCE since we match against the whole change sequence...
        createMatchesIfNotAlreadyPresent();
        Set<IMatch> remainingMatches = getMatchesThatHaventBeenApplied();

        long stop = Timer.stop();
        logger.info("Pattern Matching took " + (stop / 1000000d) + " ms");
        this.iMatchObserver.addMatches(remainingMatches);
        // broken matches
        this.iMatchObserver.removeMatches(getBrokenMatches());
//        throw new RuntimeException("TODO implement!");
    }

    @Override
    public void terminate() {
        throw new RuntimeException("TODO implement!");
    }

    @Override
    public void setDebugPath(String s) {
        //TODO implement if needed
    }

    @Override
    public IPatternInterpreterProperties getProperties() {
        return new IPatternInterpreterProperties() {
//            @Override
//            public boolean needs_paranoid_modifications() {
//                return true;
//            }
            //TODO implement methods if needed (e.g. smartEMF support??) this by default returns false for every method
        };
    }

    @Override
    public Times getTimes() {
        return this.times;
    }

    private Collection<IMatch> getBrokenMatches() {
        /*
        todo was überlegen, das so vorgeht:
             * Für alle eChanges : VitruviusChange.getEChanges().filter(::isDeletingEChange)
             *      getRelevantMatches(eChange)
             *      check if broken (entweder anhand des eChanges oder anhand des graphen)
             *      daraus matches basteln
         */
//        throw new RuntimeException("TODO implement");
        return new LinkedList<>();
    }

    private void createMatchesIfNotAlreadyPresent() {
        if (this.matchesFound == null) {
            this.matchesFound = new VitruviusChangePatternMatcher(vitruviusChange).matchPatterns(changeSequenceTemplateSet);

            // TODO remove debug
            logger.debug("ALL MATCHES FOUND");
            for (IMatch iMatch : matchesFound) {
                ITGGMatch itggMatch = (ITGGMatch) iMatch;
                VitruviusBackwardConversionMatch vitruviusBackwardConversionMatch = (VitruviusBackwardConversionMatch) itggMatch;
                logger.debug("- Match: " + vitruviusBackwardConversionMatch.getMatchedChangeSequenceTemplate().getTggRule().getName());
                logger.debug(iMatch.toString());
                IGreenPatternFactory factory = this.observedOperationalStrategy.getGreenFactories().get(itggMatch.getRuleName());
                IGreenPattern greenPattern = factory.create(itggMatch.getType());
                ITGGMatch comatch = itggMatch.copy();

                logger.debug("  - greenPattern.getSrcEdges");
                greenPattern.getSrcEdges().forEach(edge -> {
                    EObject src = (EObject) comatch.get(edge.getSrcNode().getName());
                    String comatchGetSrc = src != null ? Util.eObjectToString(src): "null";
                    logger.debug("    - " + edge.getName() + ", srcnodeName=" + edge.getSrcNode().getName() + " -> comatch-get: " + comatchGetSrc);
                });
//                vitruviusBackwardConversionMatch.getMatchedChangeSequenceTemplate().getTggRule().getEdges()

                logger.debug("  - greenPattern.getTrgEdges");
                greenPattern.getTrgEdges().forEach(edge -> {
                    EObject src = (EObject) comatch.get(edge.getSrcNode().getName());
                    String comatchGetSrc = src != null ? Util.eObjectToString(src): "null";
                    logger.debug("    - " + edge.getName() + ", srcnodeName=" + edge.getSrcNode().getName() + " -> comatch-get: " + comatchGetSrc);
                });


                logger.debug("  - greenPattern.getCorrEdges");
                greenPattern.getCorrEdges().forEach(edge -> {
                    EObject src = (EObject) comatch.get(edge.getSrcNode().getName());
                    String comatchGetSrc = src != null ? Util.eObjectToString(src): "null";
                    logger.debug("    - " + edge.getName() + ", srcnodeName=" + edge.getSrcNode().getName() + " -> comatch-get: " + comatchGetSrc);
                });

                logger.debug("  - greenPattern.getTrgNodes");
                greenPattern.getTrgNodes().forEach(node -> logger.debug("    - " + node.getName()));
                logger.debug("  - greenPattern.getSrcNodes");
                greenPattern.getSrcNodes().forEach(node -> logger.debug("    - " + node.getName()));
                logger.debug("  - greenPattern.getCorrNodes");
                greenPattern.getCorrNodes().forEach(node -> logger.debug("    - " + node.getName()));

            }
        }
    }

    /**
     *
     */
    private Set<IMatch> getMatchesThatHaventBeenApplied() {
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
            logger.info("SYNC has applied a match: " + match);

            this.matchesThatHaveBeenApplied.add(match);
//                IbexObservable var4;
//                if ((var4 = this.getObservable()) instanceof OperationalStrategy) {
//                    OperationalStrategy op = (OperationalStrategy)var4;
//                    this.operationalMatchContainer = op.getMatchContainer();
//                    this.matchesSize = this.operationalMatchContainer.getMatches().size();
//                    String patternName = this.operationalMatchContainer.getNext().getRuleName();
//                    logger.info("Pattern: " + patternName + " hasMatches: " + this.matchesSize);
//                }

            logger.debug("  - updated CORR-CACHING?: ");
            this.observedOperationalStrategy.getResourceHandler().getCorrCaching().forEach((eObject, corrs) -> {
                logger.debug("    - " + Util.eObjectToString(eObject) + " -> " + corrs.stream().map(Util::eObjectToString).collect(Collectors.joining(", ")));

            });
        }
    }

    public void addObservedOperationalStrategy(OperationalStrategy observedOperationalStrategy) {
        this.observedOperationalStrategy = observedOperationalStrategy;
    }
}

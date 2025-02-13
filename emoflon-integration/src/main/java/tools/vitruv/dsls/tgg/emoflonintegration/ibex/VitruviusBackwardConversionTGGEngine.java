package tools.vitruv.dsls.tgg.emoflonintegration.ibex;

import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.emoflon.ibex.common.operational.IContextPatternInterpreter;
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
import org.emoflon.ibex.tgg.operational.strategies.modules.IbexExecutable;
import org.emoflon.smartemf.persistence.SmartEMFResourceFactoryImpl;
import tools.vitruv.dsls.tgg.emoflonintegration.ibex.patternconversion.IbexPatternConverter;
import tools.vitruv.dsls.tgg.emoflonintegration.ibex.patternconversion.VitruviusChangeTemplateSet;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

/**
 * Pattern Matcher implementing the generation of new matches based on
 * * the existing TGG derivation (protocol)
 * * Pattern templates:
 *      * TGG patterns converted to the EChanges that they consist of.
 *      * todo precompiled (for performance opt.)
 *      * todo short-cut-rules?
 * * matching those templates onto a given VitruviusChange
 *
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
public class VitruviusBackwardConversionTGGEngine implements IBlackInterpreter, TimeMeasurable, IContextPatternInterpreter {

    protected static final Logger logger = Logger.getRootLogger();
    private EPackage.Registry registry;
    private IMatchObserver iMatchObserver;
    private URI baseURI;
    private IbexOptions ibexOptions;
    private ResourceSet resourceSet;
    private Resource ibexPatternsResource;
    private IBeXModel ibexModel;
    private IbexExecutable ibexExecutable;

    private VitruviusChangeTemplateSet vitruviusChangeTemplateSet;
    private final Times times;

    /**
     * TODO input here or in init function?
     * VitruviusChange cannot be given in initialize, so here.
     */
    public VitruviusBackwardConversionTGGEngine() {
        this.times = new Times();
        this.baseURI = URI.createPlatformResourceURI("/", true);
    }
    @Override
    public void initialise(IbexExecutable ibexExecutable, IbexOptions ibexOptions, EPackage.Registry registry, IMatchObserver iMatchObserver) {
        this.ibexExecutable = ibexExecutable;
        this.ibexOptions = ibexOptions;
        this.setIbexPatternsResource(new File(ibexOptions.project.workspacePath() + "/"
                + ibexOptions.project.path() + "/bin/" + ibexOptions.project.path() + "/"
                + "sync/hipe/engine/ibex-patterns.xmi"));
        this.ibexModel = ((IBeXModel) ibexPatternsResource.getContents().get(0));
        logger.info("Now initializing patterns: ");

        this.initPatterns(ibexModel.getPatternSet());
    }

    @Override
    public void initPatterns(IBeXPatternSet iBeXPatternSet) {
        Timer.start();
        this.vitruviusChangeTemplateSet = new IbexPatternConverter(this.ibexModel, this.ibexOptions.tgg.flattenedTGG()).convert();
        this.times.addTo("patternConversion", Timer.stop());
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
        throw new RuntimeException("TODO implement!");
    }

    @Override
    public void updateMatches() {
        throw new RuntimeException("TODO implement!");
    }

    @Override
    public void terminate() {
        throw new RuntimeException("TODO implement!");
    }

    @Override
    public void setDebugPath(String s) {
        throw new RuntimeException("TODO implement!");
    }

    @Override
    public IPatternInterpreterProperties getProperties() {
        return null;
    }

    @Override
    public Times getTimes() {
        return this.times;
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
}

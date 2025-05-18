package tools.vitruv.dsls.tgg.emoflonintegration.ibex.hipe;

import hipe.engine.HiPEContentAdapter;
import hipe.engine.IHiPEEngine;
import hipe.engine.config.HiPEOptions;
import hipe.engine.match.ProductionMatch;
import hipe.engine.message.production.ProductionResult;
import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.emoflon.ibex.common.operational.IMatch;
import org.emoflon.ibex.common.operational.IMatchObserver;
import org.emoflon.ibex.patternmodel.IBeXPatternModel.IBeXContext;
import org.emoflon.ibex.patternmodel.IBeXPatternModel.IBeXModel;
import org.emoflon.ibex.patternmodel.IBeXPatternModel.IBeXPatternSet;
import org.emoflon.ibex.tgg.compiler.patterns.PatternSuffixes;
import org.emoflon.ibex.tgg.compiler.patterns.PatternUtil;
import org.emoflon.ibex.tgg.operational.benchmark.Timer;
import org.emoflon.ibex.tgg.operational.defaults.IbexOptions;
import org.emoflon.ibex.tgg.operational.strategies.modules.IbexExecutable;
import org.emoflon.ibex.tgg.runtime.hipe.HiPETGGEngine;
import tools.vitruv.dsls.tgg.emoflonintegration.Util;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Ibex initializes HiPE in a way that doesn't allow the flexibility for it to be called "as a library".
 * So we override and override and override...
 * This is not part of the thesis and it is unsure if this is even used in the evaluation...
 */
public class VitruviusHiPETGGEngine extends HiPETGGEngine {

    protected static final Logger logger = Logger.getRootLogger();
    private IbexOptions options;

    public VitruviusHiPETGGEngine() {
        super();
    }

    @Override
    public void initialise(IbexExecutable executable, IbexOptions options, EPackage.Registry registry, IMatchObserver matchObserver) {
        // the following is copied from the superclass but with changes where necessary (path stuff)
        /*
            Changes to this method compared to the superclass:
            1. Set fields via reflection that are private in the superclass. Object orientation at its best.
            2. Not use the hardcoded stuff to find ibex-patterns but make it relative to the workspacePath in the given IbexOptions
            3. load project-specific classes that ibex generates in the respective eclipse project which HiPE requires to function
         */
        super.initialise(registry, matchObserver);
        this.options = options;
        setPrivateSuperclassField("options", options);
        setPrivateSuperclassField("executable", executable);
        String cp = "";

        String path = getProjectBinDirectory() + generateHiPEClassName().replace(".", "/").replace("HiPEEngine", "ibex-patterns.xmi");

        File file = new File(path);
//        logger.info("ibex-patterns.xmi path: " + file);
        try {
            cp = file.getCanonicalPath();
            cp = cp.replace("%20", " ");
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        Resource r = null;
        try {
            r = loadResource("file://" + cp);
        } catch (Exception e) {
            e.printStackTrace();
        }

        IBeXModel ibexModel = (IBeXModel)r.getContents().getFirst();
        //
        IBeXPatternSet ibexPatterns = ibexModel.getPatternSet();
        setPrivateSuperclassField("ibexPatterns", ibexPatterns);

        for(IBeXContext context : ibexPatterns.getContextPatterns()) {
            PatternUtil.registerPattern(context.getName(), PatternSuffixes.extractType(context.getName()));
        }

        initPatterns(ibexPatterns);
    }

    private String getProjectBinDirectory() {
        return options.project.workspacePath() + "/" + options.project.path() + "/bin/";
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void initEngine(final Collection<Resource> resources) {
        if(engine == null) {
            Class<? extends IHiPEEngine> engineClass;
            try {
                this.getClass().getClassLoader().loadClass("hipe.network.HiPENetwork");
                engineClass = loadIbexProjectSpecificClass(new File(getProjectBinDirectory()), engineClassName);
                engineClass.getClassLoader().loadClass("hipe.network.HiPENetwork");
                engineClass.getClassLoader().loadClass("HiPENetwork");
            } catch (ClassNotFoundException e1) {
                throw new RuntimeException(e1);
            }

            try {
                Constructor<? extends IHiPEEngine> constructor = engineClass.getConstructor();
                constructor.setAccessible(true);
                engine = constructor.newInstance();
            } catch (InstantiationException | IllegalAccessException | NoSuchMethodException |
                     SecurityException | IllegalArgumentException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }
        try {
            HiPEOptions options = new HiPEOptions();
            options.cascadingNotifications = cascadingNotifications(resources);
            options.lazyInitialization = initializeLazy();
            engine.initialize(options);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        adapter = new HiPEContentAdapter(resources.stream().filter(res -> !res.getURI().toString().contains("-trash")).collect(Collectors.toSet()), engine);
    }

    @Override
    public void updateMatches() {
        Timer.start();

        try {
            Map<String, ProductionResult> extractData = this.engine.extractData();
            this.getTimes().addTo("findMatches", Timer.stop());

            for(String patternName : extractData.keySet()) {
                if (this.patterns.get(patternName) != null) {
                    String pName = this.patterns.get(patternName);
                    logger.debug("In VitruviusHiPETGGEngine: pattern debug: patternName=" + patternName + ", pName=" + pName + ", productionMatchesNames: " +
                            extractData.get(patternName).getNewMatches().stream().map(productionMatch -> productionMatch.patternName).collect(Collectors.joining(", ")));

                    Collection<ProductionMatch> matches = extractData.get(patternName).getNewMatches();
                    for (ProductionMatch m : matches) {
                        IMatch match = this.createMatch(m, pName);
                        logger.debug("  - HIPE MATCH: [" + match.getPatternName() + "] params= ");
                        for (String paramName : match.getParameterNames()) {
                            logger.debug("    - " + paramName + "=" + Util.eObjectToString(match.get(paramName)));
                        }
                    }
                }
            }

            this.addNewMatches(extractData);
            this.deleteInvalidMatches(extractData);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    private Class loadIbexProjectSpecificClass(final File directory, String className) throws ClassNotFoundException {
        try {
            //class loader should have access to this CL's classes as well as the ibex project

//            return new SimpleNameSupportingURLClassLoader(
//                    new URL[]{directory.toURI().toURL()},
//                    this.getClass().getClassLoader())
            return SimpleNameSupportingURLClassLoader
                    .getInstance(new URL[]{directory.toURI().toURL()})
                    .loadClass(className);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    private void setPrivateSuperclassField(String fieldName, Object newValue) {
        try {
            Field field = this.getClass().getSuperclass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(this, newValue);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Irgendein Reflection-Exception in HiPE-Stuff: " + e);
        }
    }
}

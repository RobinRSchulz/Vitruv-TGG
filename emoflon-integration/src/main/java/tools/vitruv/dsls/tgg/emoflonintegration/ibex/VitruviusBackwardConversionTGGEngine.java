package tools.vitruv.dsls.tgg.emoflonintegration.ibex;

import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.emoflon.ibex.common.operational.IMatchObserver;
import org.emoflon.ibex.common.operational.IPatternInterpreterProperties;
import org.emoflon.ibex.patternmodel.IBeXPatternModel.IBeXPatternSet;
import org.emoflon.ibex.tgg.operational.IBlackInterpreter;
import org.emoflon.ibex.tgg.operational.benchmark.TimeMeasurable;
import org.emoflon.ibex.tgg.operational.benchmark.Times;
import org.emoflon.ibex.tgg.operational.defaults.IbexOptions;
import org.emoflon.ibex.tgg.operational.strategies.modules.IbexExecutable;
import tools.vitruv.change.composite.description.VitruviusChange;

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
public class VitruviusBackwardConversionTGGEngine implements IBlackInterpreter, TimeMeasurable {

    /**
     * TODO input here or in init function?
     * VitruviusChange cannot be given in initialize, so here.
     */
    public VitruviusBackwardConversionTGGEngine() {

    }
    @Override
    public void initialise(IbexExecutable ibexExecutable, IbexOptions ibexOptions, EPackage.Registry registry, IMatchObserver iMatchObserver) {

    }

    @Override
    public void initPatterns(IBeXPatternSet iBeXPatternSet) {

    }

    @Override
    public void initialise(EPackage.Registry registry, IMatchObserver iMatchObserver) {

    }

    @Override
    public ResourceSet createAndPrepareResourceSet(String s) {
        return null;
    }

    @Override
    public void monitor(Collection<Resource> collection) {

    }

    @Override
    public void updateMatches() {

    }

    @Override
    public void terminate() {

    }

    @Override
    public void setDebugPath(String s) {

    }

    @Override
    public IPatternInterpreterProperties getProperties() {
        return null;
    }

    @Override
    public Times getTimes() {
        return null;
    }
}

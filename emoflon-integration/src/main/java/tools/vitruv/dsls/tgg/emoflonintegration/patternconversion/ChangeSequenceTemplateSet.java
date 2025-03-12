package tools.vitruv.dsls.tgg.emoflonintegration.patternconversion;

import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.dsls.tgg.emoflonintegration.Util;
import tools.vitruv.dsls.tgg.emoflonintegration.patternconversion.echange.ChangeSequenceTemplate;
import tools.vitruv.dsls.tgg.emoflonintegration.patternconversion.echange.EChangeWrapper;

import java.util.*;

/**
 * A representation of Ibex TGG patterns to ease mapping them to VitruviusChanges (sequences of atomic changes).
 * This Set holds parent ${@link ChangeSequenceTemplate}s that can be invoked to be matched to concrete ${@link EChange}s.
 * Further, this set provides a method to partly invoke a ${@link ChangeSequenceTemplate} based on one ${@link EChange}.
 */
public class ChangeSequenceTemplateSet {
    static Logger logger = Logger.getLogger(ChangeSequenceTemplateSet.class);

    private final Collection<ChangeSequenceTemplate> patternTemplateParents;
    /**
     * maps an Echange Type to all EChange-Wrappers this pattern contains
     */
    private Map<EClass, Set<ChangeSequenceTemplate>> ibexPatternTemplatesByEChangeType;

    public ChangeSequenceTemplateSet(Collection<ChangeSequenceTemplate> patternTemplateParents) {
        this.patternTemplateParents = patternTemplateParents;

        initialize();
    }

    private void initialize() {
        // make the mapping EChangeType -> relevant patterns easily accessible
        ibexPatternTemplatesByEChangeType = new HashMap<>();
        for (ChangeSequenceTemplate ibexPatternTemplate : patternTemplateParents) {
            for (EChangeWrapper eChangeWrapper : ibexPatternTemplate.getEChangeWrappers()) {
                ibexPatternTemplatesByEChangeType.computeIfAbsent(eChangeWrapper.getEChangeType(), k -> new HashSet<>()).add(ibexPatternTemplate);
            }
        }
    }

    /**
     * Matches this set's templates' ${@link tools.vitruv.dsls.tgg.emoflonintegration.patternconversion.echange.EChangeWrapper}s against the given ${@link EChange}
     * and return all matches in form of invocations (copies) of the ${@link ChangeSequenceTemplate}s this set holds.
     * One ${@link ChangeSequenceTemplate} this set holds might be invoked more than once,
     * if different ${@link tools.vitruv.dsls.tgg.emoflonintegration.patternconversion.echange.EChangeWrapper}s of that template match the ${@link EChange}!
     *
     * @return invoked ChangeSequenceTemplates that contain the given eChangeType in one of their change wrappers and thus are a possible candidate.
     *      Those are already partly initialized with the given EChange.
     */
    public Set<ChangeSequenceTemplate> getAndInitRelevantIbexPatternTemplatesByEChange(EChange<EObject> eChange) {
        Set<ChangeSequenceTemplate> partlyInitializedTemplates = new HashSet<>();
        if (!ibexPatternTemplatesByEChangeType.containsKey(eChange.getClass())) {
            logger.warn("No rule defined to cover the following change's type: " + Util.eChangeToString(eChange));
            return Collections.emptySet();
        }
        for (ChangeSequenceTemplate ibexPatternTemplate : ibexPatternTemplatesByEChangeType.get(eChange.eClass())) {
            ibexPatternTemplate.getEChangeWrappers().stream()
                    .filter(eChangeWrapper -> eChangeWrapper.matches(eChange))
                    .forEach(eChangeWrapper -> {
                        // we got a pattern with >= 1 eChangewrappers matching the eChange. We now want to create one invoked IbexPatternTemplate with the respective eChangeWrapper already initialized.
                        // thus, we initialize the one eChangeWrapper here
                        ChangeSequenceTemplate changeSequenceTemplateCopy = ibexPatternTemplate.deepCopy();
                        changeSequenceTemplateCopy.getThisInstancesEChangeWrapperFromParent(eChangeWrapper).initialize(eChange);
                        partlyInitializedTemplates.add(changeSequenceTemplateCopy);
                    });
        }
        return partlyInitializedTemplates;
    }

    /**
     *
     * @return the uninitialized, uncopied pattern templates. Don't use these Templates for matching, they need to be invoked (copied), first!
     */
    public Collection<ChangeSequenceTemplate> getPatternTemplateParents() {
        return patternTemplateParents;
    }
}

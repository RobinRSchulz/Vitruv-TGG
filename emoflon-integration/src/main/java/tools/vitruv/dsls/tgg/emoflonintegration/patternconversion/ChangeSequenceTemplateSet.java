package tools.vitruv.dsls.tgg.emoflonintegration.patternconversion;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.dsls.tgg.emoflonintegration.Util;
import tools.vitruv.dsls.tgg.emoflonintegration.patternconversion.echange.ChangeSequenceTemplate;

import java.util.*;

/**
 * A representation of Ibex TGG patterns to ease mapping them to VitruviusChanges (sequences of atomic changes)
 */
public class ChangeSequenceTemplateSet {

    private final Collection<ChangeSequenceTemplate> patternTemplates;
    /**
     * maps an Echange Type to all EChange-Wrappers this pattern contains
     */
    private Map<EClass, Set<ChangeSequenceTemplate>> ibexPatternTemplatesByEChangeType;

    public ChangeSequenceTemplateSet(Collection<ChangeSequenceTemplate> patternTemplates) {
        this.patternTemplates = patternTemplates;

        initialize();
    }

    private void initialize() {
        // make the mapping EChangeType -> relevant patterns easily accessible
        ibexPatternTemplatesByEChangeType = new HashMap<>();
        patternTemplates.forEach(ibexPatternTemplate -> {
            ibexPatternTemplate.getEChangeWrappers().forEach(eChangeWrapper -> {
                ibexPatternTemplatesByEChangeType.computeIfAbsent(eChangeWrapper.getEChangeType(), k -> new HashSet<>()).add(ibexPatternTemplate);
            });
        });
    }

    /**
     *
     * @param eChange
     * @return all IbexPatternTemplates that contain the given eChangeType in one of their change wrappers and thus are a possible candidate.
     *      Those are already partly initialized with the given EChange
     */
    public Set<ChangeSequenceTemplate> getAndInitRelevantIbexPatternTemplatesByEChange(EChange<EObject> eChange) {
        Set<ChangeSequenceTemplate> partlyInitializedTemplates = new HashSet<>();
        ibexPatternTemplatesByEChangeType.get(eChange.eClass())
                .forEach(ibexPatternTemplate -> {
                    ibexPatternTemplate.getEChangeWrappers().stream()
                            .filter(eChangeWrapper ->
                                    // das reicht nicht! TODO ich muss eine matches(EChange, VitruviusChange)-Methode in EChangeWrapper implementieren, die dann Klassenabhängig checkt!
                                    eChangeWrapper.matches(eChange))
//                                    eChangeWrapper.getEChangeType().equals(eChange.eClass()) &&
//                                    eChangeWrapper.getAffectedElementEClass().equals(Util.getAffectedEObjectFromEChange(eChange).eClass()))
                            .forEach(eChangeWrapper -> {
                                // we got a pattern with >= 1 eChangewrappers matching the eChange. We now want to create one invoked IbexPatternTemplate with the respective eChangeWrapper already initialized.
                                // thus, we initialize the one eChangeWrapper here
                                ChangeSequenceTemplate changeSequenceTemplateCopy = ibexPatternTemplate.deepCopy();
                                changeSequenceTemplateCopy.getThisInstancesEChangeWrapperFromParent(eChangeWrapper).initialize(eChange);
                                partlyInitializedTemplates.add(changeSequenceTemplateCopy); //TODO this doesnt work, we need a mapping between parent eChangeWrapper and child eChangeWrapper...
                            });
                });
        return partlyInitializedTemplates;
    }

    /**
     *
     * @return the uninitialized, uncopied pattern templates. Don't use these Templates for matching, they need to be copied, first!
     */
    public Collection<ChangeSequenceTemplate> getPatternTemplates() {
        return patternTemplates;
    }
}

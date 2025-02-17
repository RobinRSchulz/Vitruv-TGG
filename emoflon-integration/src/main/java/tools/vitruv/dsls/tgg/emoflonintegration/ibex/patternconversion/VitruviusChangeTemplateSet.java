package tools.vitruv.dsls.tgg.emoflonintegration.ibex.patternconversion;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.composite.description.VitruviusChange;
import tools.vitruv.dsls.tgg.emoflonintegration.ibex.patternmatching.Util;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A representation of Ibex TGG patterns to ease mapping them to VitruviusChanges (sequences of atomic changes)
 */
public class VitruviusChangeTemplateSet {

    private Collection<IbexPatternTemplate> patternTemplates;
    /**
     * maps an Echange Type to all EChange-Wrappers this pattern contains
     */
    private Map<EClass, Set<IbexPatternTemplate>> ibexPatternTemplatesByEChangeType;

    public VitruviusChangeTemplateSet(Collection<IbexPatternTemplate> patternTemplates) {
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
     * @param eChangeType
     * @return all IbexPatternTemplates that contain the given eChangeType in one of their change wrappers and thus are a possible candidate
     */
    public Set<IbexPatternTemplate> getRelevantIbexPatternTemplatesByEChangeType(EClass eChangeType) {
        //TODO maybe need to do more
        return ibexPatternTemplatesByEChangeType.get(eChangeType).stream().map(IbexPatternTemplate::deepCopy).collect(Collectors.toSet());
    }
    /**
     *
     * @param eChange
     * @return all IbexPatternTemplates that contain the given eChangeType in one of their change wrappers and thus are a possible candidate.
     *      Those are already partly initialized with the given EChange
     */
    public Set<IbexPatternTemplate> getAndInitRelevantIbexPatternTemplatesByEChange(EChange<EObject> eChange, VitruviusChange<EObject> vitruviusChange) {
        Set<IbexPatternTemplate> partlyInitializedTemplates = new HashSet<>();
        ibexPatternTemplatesByEChangeType.get(eChange.eClass()).stream()
                .forEach(ibexPatternTemplate -> {
                    ibexPatternTemplate.getEChangeWrappers().stream().filter(eChangeWrapper ->
                            eChangeWrapper.getEChangeType().equals(eChange.eClass()) &&
                            eChangeWrapper.getAffectedElementEClass().equals(Util.getAffectedEObjectFromEChange(eChange, vitruviusChange).eClass()))
                            .forEach(eChangeWrapper -> {
                                // we got a pattern with >= 1 eChangewrappers matching the eChange. We now want to create one invoked IbexPatternTemplate with the respective eChangeWrapper already initialized.
                                // thus, we initialize the one eChangeWrapper here
                                IbexPatternTemplate ibexPatternTemplateCopy = ibexPatternTemplate.deepCopy();
                                initializeEChangeWrapper(ibexPatternTemplateCopy.getThisInstancesEChangeWrapperFromParent(eChangeWrapper), eChange);

                                partlyInitializedTemplates.add(ibexPatternTemplateCopy); //TODO this doesnt work, we need a mapping between parent eChangeWrapper and child eChangeWrapper...
                            });
                });

        // filter patterns that have the
        return ibexPatternTemplatesByEChangeType.get(eChange.eClass()).stream()
                .filter( ibexPatternTemplate ->
                        ibexPatternTemplate.getEChangeWrappers().stream()
                                .anyMatch(eChangeWrapper -> eChangeWrapper.getAffectedElementEClass().equals(Util.getAffectedEObjectFromEChange(eChange, vitruviusChange).eClass())))
                .map(IbexPatternTemplate::deepCopy)
                .collect(Collectors.toSet());

    }
}

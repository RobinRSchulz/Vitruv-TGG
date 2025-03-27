package tools.vitruv.dsls.tgg.emoflonintegration.patternmatching;

import edu.kit.ipd.sdq.commons.util.java.Pair;
import org.eclipse.emf.ecore.EObject;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.composite.description.VitruviusChange;
import tools.vitruv.dsls.tgg.emoflonintegration.patternconversion.echange.ChangeSequenceTemplate;
import tools.vitruv.dsls.tgg.emoflonintegration.patternconversion.echange.EChangeWrapper;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Enables flattening the coverage of a {@link VitruviusChange} with {@link ChangeSequenceTemplate}s (in the following: pattern applications) to 1 per change each.<br/>
 * We need to make a choice between different pattern applications, and this class does it in the following way:
 * <ol>
 *     <li/> Leaning on Khelladi's containment level heuristic, we detect containment relations between pattern applications and remove all but the "top container".
 *     <li/> If there are still conflicts, we use a mix of our own maxCoverage heuristic and Khelladi's distance heuristic:
 *     <ol>
 *         <li/> First, for each conflict, we find the coverage-maximal (most EChanges covered) pattern applications.
 *         <li/> Then, from these pattern applications, we find the distance-minimal candidates.
 *         <li/> From the remaining pattern applications, we arbitrarily choose one of the candidates.
 *     </ol>
 *
 * </ol>
 */
public class PatternCoverageFlattener {

    /**
     * This remains unmodified, only provides the sorting!
     */
    private final LinkedList<ChangeSequenceTemplate> sortedPatternApplications;
    /**
     * This is the set we modify and return
     */
    private final Set<ChangeSequenceTemplate> patternApplications;
    private final List<EChange<EObject>> changeSequence;

    public PatternCoverageFlattener(Set<ChangeSequenceTemplate> patternApplications, VitruviusChange<EObject> vitruviusChange) {
        this.patternApplications = new HashSet<>(patternApplications);
        this.changeSequence = vitruviusChange.getEChanges();
        this.sortedPatternApplications = patternApplications.stream()
                .sorted(Comparator.comparingInt(a -> a.getEChangeWrappers().size()))
                .collect(Collectors.toCollection(LinkedList::new));
    }

    public Set<ChangeSequenceTemplate> getFlattenedPatternApplications() {
        applyContainmentLevelHeuristic();
        applyMaxCoverageAndDistanceHeuristic();
        assertFlatness();
        return patternApplications;
    }

    private void applyContainmentLevelHeuristic() {
        ListIterator<ChangeSequenceTemplate> listIterator = sortedPatternApplications.listIterator();
        while (listIterator.hasNext()) {
            ChangeSequenceTemplate patternApplication = listIterator.next();
            if (listIterator.hasNext()) {
                for (ChangeSequenceTemplate containerCandidate : sortedPatternApplications.subList(listIterator.nextIndex(), sortedPatternApplications.size())) {
                    if (isContainedIn(patternApplication, containerCandidate)) {
                        patternApplications.remove(patternApplication);
                        // we don't need to run through ALL containers, since the next will be covered anyway...
                        break;
                    }
                }
                if (!patternApplications.contains(patternApplication)) {
                    //TODO we dont really need modification of this list. --> delete?
                    listIterator.remove();
                }
            }
        }
    }

    private boolean isContainedIn(ChangeSequenceTemplate potentialContainee, ChangeSequenceTemplate potentialContainer) {
        Set<EChange<EObject>> containerEChanges = potentialContainer.getEChanges();
        Set<EChange<EObject>> containeeEChanges = potentialContainee.getEChanges();
        return containerEChanges.containsAll(containeeEChanges);
    }

    private void applyMaxCoverageAndDistanceHeuristic() {
        for (EChange<EObject> eChange : changeSequence) {
            Set<ChangeSequenceTemplate> relevantPatternApplications = getRelevantPatternApplications(eChange);
            if (relevantPatternApplications.isEmpty()) continue;
            int maxCoverage = getMaxCoverage(relevantPatternApplications);
            // first ensure only maximum-coverage pattern applications are chosen. Then choose the one with the highest density
            ChangeSequenceTemplate survivingPatternApplication = relevantPatternApplications.stream()
                    .filter(patternApplication -> patternApplication.getEChanges().size() == maxCoverage)
                    .map(patternApplication -> new Pair<>(patternApplication, densityHeuristic(patternApplication)))
                    .max(Comparator.comparing(Pair::getSecond))
                    .orElseThrow(() -> new IllegalStateException("No ChangeSequenceTempplates"))
                    .getFirst();
            // remove all but the survivor
            patternApplications.removeAll(relevantPatternApplications.stream()
                    .filter(patternApplication -> !patternApplication.equals(survivingPatternApplication))
                    .collect(Collectors.toSet()));
        }
    }

    /**
     *
     * @param changeSequenceTemplate
     * @return Khelladi's distance/ density heuristic: is between 0 and 1. the higher the denser the changes are grouped together
     */
    private double densityHeuristic(ChangeSequenceTemplate changeSequenceTemplate) {
        List<Integer> indexes = changeSequenceTemplate.getEChanges().stream().map(changeSequence::indexOf).toList();
        int firstIndex = indexes.stream().mapToInt(i -> i).min().orElseThrow(() -> new IllegalStateException("Empty ChangeSequenceTemplate"));
        int lastIndex = indexes.stream().mapToInt(i -> i).max().orElseThrow(() -> new IllegalStateException("Empty ChangeSequenceTemplate"));
        int size = changeSequenceTemplate.getEChangeWrappers().size();

        return ((double)(size-1)) / ((double)(lastIndex - firstIndex));
    }

    private Set<ChangeSequenceTemplate> getRelevantPatternApplications(EChange<EObject> eChange) {
        return patternApplications.stream().filter(patternApplication -> patternApplication.getEChanges().contains(eChange)).collect(Collectors.toSet());
    }

    private int getMaxCoverage(Set<ChangeSequenceTemplate> relevantPatternApplications) {
        return relevantPatternApplications.stream()
                .map(patternApplication -> patternApplication.getEChanges().size())
                .mapToInt(size -> size)
                .max()
                .orElseThrow(() -> new IllegalStateException("Empty ChangeSequenceTemplate"));
    }

    /**
     * If conflicts exist after flattening, the algorithm is faulty.
     *
     */
    private void assertFlatness() {
        changeSequence.forEach(eChange -> {
            assert getRelevantPatternApplications(eChange).size() <= 1;
        });
    }
}

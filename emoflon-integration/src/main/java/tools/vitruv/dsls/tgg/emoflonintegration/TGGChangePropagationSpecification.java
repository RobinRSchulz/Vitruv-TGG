package tools.vitruv.dsls.tgg.emoflonintegration;

import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.atomic.eobject.EObjectExistenceEChange;
import tools.vitruv.change.composite.MetamodelDescriptor;
import tools.vitruv.change.composite.description.VitruviusChange;
import tools.vitruv.change.correspondence.Correspondence;
import tools.vitruv.change.correspondence.view.EditableCorrespondenceModelView;
import tools.vitruv.change.propagation.impl.AbstractChangePropagationSpecification;
import tools.vitruv.change.utils.ResourceAccess;
import tools.vitruv.dsls.tgg.emoflonintegration.ibex.VitruviusTGGChangePropagationRegistrationHelper;
import tools.vitruv.dsls.tgg.emoflonintegration.ibex.VitruviusTGGChangePropagationIbexEntrypoint;
import tools.vitruv.dsls.tgg.emoflonintegration.ibex.VitruviusBackwardConversionTGGEngine;
import tools.vitruv.dsls.tgg.emoflonintegration.ibex.hipe.VitruviusHiPETGGEngine;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Extend this class for each TGG-defined set of consistency preservation rules between two metamodels.
 * Subclasses handle ${@link VitruviusChange}s.
 */
public abstract class TGGChangePropagationSpecification extends AbstractChangePropagationSpecification {
    static Logger logger = Logger.getLogger(TGGChangePropagationSpecification.class);

    private final String sourceMetamodelPlatformUri;
    private final String targetMetamodelPlatformUri;

    private File ibexProjectPath;
    private EClass targetRootEclass;
    private URI targetRootURI;

    /**
     *
     * @param sourceMetamodelDescriptor
     * @param targetMetamodelDescriptor
     * @param ibexProjectPath file system path to the eMoflon TGG project
     * @param targetRootEclass the root class for the target model to be able to create a corresponding model if none already exists. todo check if this strategy is required.
     * @param targetRootURI URI under which to persist the model created on calling {@code propagateChanges } if no corresponding model already exist.
     */
    public TGGChangePropagationSpecification(MetamodelDescriptor sourceMetamodelDescriptor, MetamodelDescriptor targetMetamodelDescriptor,
                                             String sourceMetamodelPlatformUri, String targetMetamodelPlatformUri,
                                             File ibexProjectPath,
                                             EClass targetRootEclass, URI targetRootURI) {
        super(sourceMetamodelDescriptor, targetMetamodelDescriptor);
        this.sourceMetamodelPlatformUri = sourceMetamodelPlatformUri;
        this.targetMetamodelPlatformUri = targetMetamodelPlatformUri;
        this.ibexProjectPath = ibexProjectPath;
        this.targetRootEclass = targetRootEclass;
        this.targetRootURI = targetRootURI;
    }

    @Override
    public boolean doesHandleChange(EChange<EObject> eChange,
                                    EditableCorrespondenceModelView<Correspondence> editableCorrespondenceModelView) {
        return false; //todo maybe we need true here, if it's called elsewhere to filter CPSs before the ChangePropagator.
    }

    @Override
    public void propagateChange(EChange<EObject> eChange,
                                EditableCorrespondenceModelView<Correspondence> editableCorrespondenceModelView,
                                ResourceAccess resourceAccess) {
        final String message = "propagateChange not implemented, use propagateChanges (with an s).";
        logger.warn("propagateChange not implemented, use propagateChanges (with an s).");
    }

    @Override
    public boolean doesHandleNonAtomicChanges() {
        return true;
    }

    @Override
    public void propagateNonAtomicChange(VitruviusChange<EObject> change,
                                         EditableCorrespondenceModelView<Correspondence> correspondenceModel,
                                         ResourceAccess resourceAccess) {
        logger.debug("In propagateNonAtomicChange: Gotten the following EChanges: ");
        change.getEChanges().forEach(eChange -> logger.debug("  - " + Util.eChangeToString(eChange)));
        logger.debug(String.format("Propagate changes from %s to %s", this.getSourceMetamodelDescriptor(), this.getTargetMetamodelDescriptor()));

        // get metamodel resources
        if (this.getSourceMetamodelDescriptor().getNsUris().size() != 1) {
            throw new IllegalStateException("none or more than one source metamodel. Can only handle exactly one! " + this.getSourceMetamodelDescriptor());
        }
        if (this.getTargetMetamodelDescriptor().getNsUris().size() != 1) {
            throw new IllegalStateException("none or more than one target metamodel. Can only handle exactly one! " + this.getTargetMetamodelDescriptor());
        }
        EPackage sourceMetamodel = org.eclipse.emf.ecore.EPackage.Registry.INSTANCE.getEPackage(this.getSourceMetamodelDescriptor().getNsUris().stream()
                .findAny().orElseThrow(() -> new IllegalStateException("No source metamodel registered! " + this.getSourceMetamodelDescriptor()))
        );
        EPackage targetMetamodel = org.eclipse.emf.ecore.EPackage.Registry.INSTANCE.getEPackage(this.getTargetMetamodelDescriptor().getNsUris().stream()
                .findAny().orElseThrow(() -> new IllegalStateException("No target metamodel registered! " + this.getTargetMetamodelDescriptor()))
        );

        // get source and target models
        Resource sourceModel = findModel(change).orElseThrow(() -> new IllegalArgumentException("Change not related to a source model: " + change));
        logger.debug("In propagateNonAtomicChange: Found source model " + sourceModel);
        Resource targetModel = getTargetModel(sourceModel, correspondenceModel);

        logger.info("------- Calling ibex -------");
        try {
            new VitruviusTGGChangePropagationIbexEntrypoint(new VitruviusTGGChangePropagationRegistrationHelper(sourceMetamodel, targetMetamodel, sourceMetamodelPlatformUri, targetMetamodelPlatformUri,
                    sourceModel, targetModel, ibexProjectPath,
                    //TODO remove this switching stuff, it is only for debug!
                    new VitruviusBackwardConversionTGGEngine(change) // alternative: new VitruviusHiPETGGEngine()
//                    new VitruviusHiPETGGEngine()
            )).propagateChanges();
        } catch (IOException e) {
            throw new RuntimeException("Could not set up eMoflon! " + e);
        }
    }

    /**
     *
     * Tries to find a model ${@link Resource} related to a ${@link VitruviusChange}.
     * Problem: a change either inserts a root object to a model, changes an existing model element or creates/ deletes an EObject.
     * The latter type of EChange (subclasses of ${@link  EObjectExistenceEChange}) don't directly relate to a model ${@link Resource}.
     * Thus, if a ${@link VitruviusChange} only consists of ${@link  EObjectExistenceEChange}s, it cannot be related to a model ${@link Resource}.
     *
     * @return the model ${@link Resource} a ${@link VitruviusChange} relates to, if it relates to one.
     */
    private Optional<Resource> findModel(VitruviusChange<EObject> vitruviusChange) {
        /*
            1. Find whether there is any non-existence-changing change, because those are not related to a model (resource)!
               We only need to look at one.
            2. Get the affected EObject of that change (every concrete EChange has that property) and retrieve the related Resource.
         */
        AtomicReference<Optional<Resource>> resourceOptional = new AtomicReference<>();
        vitruviusChange.getEChanges().stream()
                .filter( change -> !(change instanceof EObjectExistenceEChange))
                .findAny().ifPresentOrElse((eChange) -> {
                    resourceOptional.set(Optional.ofNullable(Util.getAffectedEObjectFromEChange(eChange).eResource()));
                    }, () -> {
                    resourceOptional.set(Optional.empty());
                });
        return resourceOptional.get();
    }

    /**
     *
     * @return the target model resource, based on
     * <ul>
     * <li>existing correspondences between the (known) source model and the target model</li>
     * <li>information provided by the methodologist implementing this class. The following approaches are to be tried. Currently, the third is in place:<br/>
     * First approach:<br/>
     * 1. try simply creating an empty resource<br/>
     *       2. handing that to emolfon<br/>
     *       3. gettin' the filled resource back and registering it in Vitruv<br/>
     *       ==> doesnt work, no access to resourceSet via resourceAccess<br/><br/>
     *
     *       Second approach:<br/>
     *       1. Create an artificial root (methodologist gives the EClass needed for that).<br/>
     *       2. Persist that as root in resourceAccess.<br/>
     *       3. Add a correspondence between a source object and that artificial root<br/>
     *       ==> A little hacky, but kept in commented-out code, in case third doesnt work as expected...<br/><br/>
     *
     *       Third approach:<br/>
     *       1. Create a resource for the target model via the resourceSet of the source model.<br/>
     *       2. perform change propagation.<br/>
     *       3. persist the now-existing root node as root in the resourceAccess. todo implement that step!<br/>
     *       ==> Better (not having to create artificial root), but (presumably?) Vitruvius doesn't automatically monitor the changes made to the target model.<br/>
     *       Since we want to create the Changes manually, this comes in handy, if true.
     * </li>
     * </ul>
     */
    private Resource getTargetModel(Resource sourceModel,
                                    EditableCorrespondenceModelView<Correspondence> correspondenceModel) {
        Resource targetModel = null;
        /*  If no target model exists yet, we need to create one. There are different possible approaches. Currently, the third is in place.
         */
        if (!correspondenceModel.hasCorrespondences(sourceModel.getContents())) {
            logger.info("Source Model has no respective target model yet. Creating one.");
            // [Third approach]
            targetModel = sourceModel.getResourceSet().createResource(this.targetRootURI);
        } else {
            logger.info("Found target model via correspondence model");
            targetModel = correspondenceModel.getCorrespondingEObjects(sourceModel.getContents().getFirst()).stream().findFirst()
                    .orElseThrow(() -> new RuntimeException("Target model found (via correspondence model) but source model has no contents...")).eResource();
        }
        return targetModel;
    }
}

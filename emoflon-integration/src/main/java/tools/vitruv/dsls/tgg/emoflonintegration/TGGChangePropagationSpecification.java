package tools.vitruv.dsls.tgg.emoflonintegration;

import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.atomic.eobject.EObjectExistenceEChange;
import tools.vitruv.change.atomic.feature.list.UpdateSingleListEntryEChange;
import tools.vitruv.change.atomic.feature.reference.UpdateReferenceEChange;
import tools.vitruv.change.atomic.root.InsertRootEObject;
import tools.vitruv.change.composite.MetamodelDescriptor;
import tools.vitruv.change.composite.description.VitruviusChange;
import tools.vitruv.change.correspondence.Correspondence;
import tools.vitruv.change.correspondence.view.EditableCorrespondenceModelView;
import tools.vitruv.change.propagation.impl.AbstractChangePropagationSpecification;
import tools.vitruv.change.utils.ResourceAccess;
import tools.vitruv.dsls.tgg.emoflonintegration.ibex.DefaultRegistrationHelper;
import tools.vitruv.dsls.tgg.emoflonintegration.ibex.SYNCDefault;

import java.io.File;
import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.Optional;

public class TGGChangePropagationSpecification extends AbstractChangePropagationSpecification {
    static Logger logger = Logger.getLogger(TGGChangePropagationSpecification.class);

    private File ibexProjectPath;
    private EClass targetRootEclass;
    private URI targetRootURI;

    /**
     *
     * @param sourceMetamodelDescriptor
     * @param targetMetamodelDescriptor
     * @param ibexProjectPath file system path to the eMoflon TGG project
     * @param targetRootEclass the root class for the target model to be able to create a corresponding model if none already exists.
     * @param targetRootURI URI under which to persist the model created on calling {@code propagateChanges } if no corresponding model already exist.
     */
    public TGGChangePropagationSpecification(MetamodelDescriptor sourceMetamodelDescriptor, MetamodelDescriptor targetMetamodelDescriptor,
                                             File ibexProjectPath,
                                             EClass targetRootEclass, URI targetRootURI) {
        super(sourceMetamodelDescriptor, targetMetamodelDescriptor);
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
        logger.warn("propagateNonAtomicChange not implemented yet, TODO!");
        Resource sourceModel = findModel(change, correspondenceModel, resourceAccess).orElseThrow(() -> {
            throw new IllegalArgumentException("Change not related to a source model: " + change);
        });
        logger.info("############################################# changed uris:");
        change.getChangedURIs().forEach(it -> logger.info("  " + it));
        logger.info("In propagateNonAtomicChange: Found source model " + sourceModel);
        // This is called only if the occurring change affects a model of this.getSourceMetamodelDescriptor().
        // this.getTargetMetamodelDescriptor() is not checked, since we know we want

        //TODO
        // get model1 (from change param or resource access or whatever
        // get model2 (by THIS targetMetamodelDescriptor and correspondence model)
        // get protocol etc (TGG info)
        // debug
        logger.info("In propagateNonAtomicChange: Gotten the following EChanges: ");
        change.getEChanges().stream().forEach(eChange -> {
            logger.info("  - " + eChange.toString());
        });


        debug("The following EObjects are changed with this VitruviusChange:");
        //TODO use THAT for getting the resource?
        resourceAccess.getModelResource(change.getChangedURIs().stream()
                .findAny().orElseThrow(() -> new NoSuchElementException("change empty (no affected EObjects)!")))
                .getAllContents().forEachRemaining((eObject -> logger.info("  - " + eObject)));
        logger.info(String.format("Propagate changes from %s to %s",
                this.getSourceMetamodelDescriptor(),
                this.getTargetMetamodelDescriptor()));



        //TODO get metamodel resources
        logger.info(" YYYYYYYYYYYYYYYYYYYYYYYYYYYYYY----[ Source metamodel Search ]----YYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYY " + sourceModel.getAllContents().next().eClass());
//        logger.info("ESuperTypes:");
//        sourceModel.getAllContents().next().eClass().getESuperTypes().forEach(superType -> logger.info("  - " + superType));
        logger.info("- Access via EPackage.Registry.getEPackage(targetMetamodelDescriptor):");
        this.getTargetMetamodelDescriptor().getNsUris().forEach(targetMetamodelDescriptor -> {
            logger.info("   - Descriptor" + targetMetamodelDescriptor);
            logger.info("   - Metamodel" + org.eclipse.emf.ecore.EPackage.Registry.INSTANCE.getEPackage(targetMetamodelDescriptor));
        });
        logger.info("- Access via EPackage.Registry.getEPackage(sourceMetamodelDescriptor):");
        this.getSourceMetamodelDescriptor().getNsUris().forEach(sourceMetamodelDescriptor -> {
            logger.info("   - Descriptor" + sourceMetamodelDescriptor);
            logger.info("   - Metamodel" + org.eclipse.emf.ecore.EPackage.Registry.INSTANCE.getEPackage(sourceMetamodelDescriptor));
        });
        if (this.getSourceMetamodelDescriptor().getNsUris().size() != 1) {
            throw new RuntimeException("none or more than one source metamodel. Can only handle exactly one! " + this.getSourceMetamodelDescriptor());
        }
        if (this.getTargetMetamodelDescriptor().getNsUris().size() != 1) {
            throw new RuntimeException("none or more than one target metamodel. Can only handle exactly one! " + this.getTargetMetamodelDescriptor());
        }
        EPackage sourceMetamodel = org.eclipse.emf.ecore.EPackage.Registry.INSTANCE.getEPackage(this.getSourceMetamodelDescriptor().getNsUris().stream()
                .findAny().orElseThrow(() -> new RuntimeException("No source metamodel registered! " + this.getSourceMetamodelDescriptor()))
        );
        EPackage targetMetamodel = org.eclipse.emf.ecore.EPackage.Registry.INSTANCE.getEPackage(this.getTargetMetamodelDescriptor().getNsUris().stream()
                .findAny().orElseThrow(() -> new RuntimeException("No target metamodel registered! " + this.getTargetMetamodelDescriptor()))
        );
        logger.info(" sourceMetamodel: " + sourceMetamodel + "\n targetMetamodel: " + targetMetamodel);

        //TODO target model handling:
        /*  If no target model exists (todo check via correspondence model?),
                create an instance of the target metamodel? Or should we expect one to be present?
         */
        Resource targetModel = null;
        if (!correspondenceModel.hasCorrespondences(sourceModel.getContents())) {
            //TODO nu approach:
            /*
                1. try simply creating an empty resource
                2. handing that to emolfon
                3. gettin' the filled resource back and registering it in Vitruv
                ==> doesnt work, no access to resourceSet via resourceAccess

                Nu idea: use the EObject to register a resource, but delete it afterwards!
             */


            logger.info("Source Model has no respective target model yet. Creating one.");
            // we need a root EObject for registering a new model to Vitruvius, so we create one and also create a correspondence...
//            EObject targetRoot = targetMetamodel.getEFactoryInstance().create(this.targetRootEclass);
//            resourceAccess.persistAsRoot(targetRoot, this.targetRootURI);
            // TODO maybe derive the source model root and the tag from a class field (to be given by the methodologist).
//            correspondenceModel.addCorrespondenceBetween(sourceModel.getContents().getFirst(), targetRoot, "Root2Root");
//            if (!correspondenceModel.hasCorrespondences(sourceModel.getContents())) {
//                throw new RuntimeException("Target model creation failed!");
//            }
//            targetModel = resourceAccess.getModelResource(this.targetRootURI);

            //TODO try the following: Benefit: not having to create artificial root.
            targetModel = sourceModel.getResourceSet().createResource(this.targetRootURI);
            //TODO AFTER SYNC, WE  PROBABLY WILL NEED TO perform resourceAccess.persistAsRoot!
        } else {
            logger.info("Found target model. Starting SYNC");
            targetModel = correspondenceModel.getCorrespondingEObjects(sourceModel.getContents().getFirst()).stream().findFirst()
                    .orElseThrow(() -> new RuntimeException("Target model found (via correspondence model) but source model has no contents...")).eResource();
        }

        //TODO create eMoflon's corr from Vitruv correspondence model or let eMoflon do its own thing while Vitruvius does the same?
        // 123. Call Emoflon
        logger.info("------- Calling ibex -------");
        try {
            new SYNCDefault(new DefaultRegistrationHelper(sourceMetamodel, targetMetamodel,
                    sourceModel, targetModel, null, change, ibexProjectPath))
                    .propagateChanges(sourceModel, targetModel, change);
        } catch (IOException e) {
            throw new RuntimeException("Could not set up eMoflon! " + e);
        }
        //TODO hand the following:
        /*
            TODO implement change prop.
            Idea: maybe Use a strategy-subclass pattern (or however its called) to be able to propagate changes
            based on
            1. Using the pattern matcher HiPe and SYNC
            2. Using the pattern matcher Democles and SYNC
            3. using my own pattern matcher and SYNC
            4. Using my own pattern matcher and custom_sync (?)
         */
        /* TODO
            1. Call SYNC{$STRATEGY}::propagateChanges, getting the changes to the target model and corr
            2. Update correspondences

         */
//        new SYNCDefault()

    }

    private Optional<Resource> findModel(VitruviusChange<EObject> vitruviusChange,
                                         EditableCorrespondenceModelView<Correspondence> correspondenceModel,
                                         ResourceAccess resourceAccess) {
        // TODO wo bekomm ich das Modell her? Ich brauch den Kontext...
        // 1. EChange --> Leeres Interface. ==> Einzige Option: Über Kindklassen instanceOf-
        // 2. CorrespondenceModel hilft auch nicht
        // 3. resourceAccess hilft, wenn ich die Modell-URI hab.
        //      Problem: die hat entweder nur ein Modell-Root oder existierende Modell-Elemente
        //
        // aktueller Versuch: Kindklassen iterieren..., Klasse finden und dann
        // Idee: Annahme: Ein VItruviuChange betrifft entweder ein konkretes Modell oder nicht.
        // Falls kein konkretes Modell betroffen, gibt es nichts konsistent zu halten
        // Falls konkretes Modell betroffen, bekomm ich das irgendwie raus, da ein Change entweder
        //      * ein root-Objekt einfügt
        //      * ein bestehendes Modell-Element ändert (Hinzufügen von irgend
        //      * ein neues Element erzeugt oder eines löscht, das noch nicht ins Modell hinzugefügt wurde.
        //        Dann muss es aber einen anderen Change im VitruivusChange geben, der nicht dieser Art ist,
        //        sonst Widerspruch zur Annahme, dass konkretes Modell betroffen ist. TODO testen: Einfach nur ein create.
        //        TODO: falls VitruviusChange nur solche changes enthält --> werfen oder zurückkehren.
        // In den ersten zwei Fällen kann entweder direkt auf die Resource zugegriffen, oder zum Parent iteriert und von da zugegriffen werden.


        // 1. Find whether there is any non-existence-changing-change
        //    We only need to look at one
        EChange<EObject> modelResourceRelatedEchange = vitruviusChange.getEChanges().stream()
                .filter( change -> !(change instanceof EObjectExistenceEChange))
                .findAny().orElseThrow(() -> {
                    throw new IllegalArgumentException("change cannot be mapped to a model: " + vitruviusChange);
                });

//        debug("Found EChange for model-finding: " + modelResourceRelatedEchange);

        // 2. Get affected Element
        // 3. Traverse the model to its root.
        EObject rootEObject = getAffectedElement(modelResourceRelatedEchange);
//        debug("That EChange has the following affected EObject: " + rootEObject);
        while (rootEObject.eContainer() != null) {
            rootEObject = rootEObject.eContainer();
        }
//        debug("That EObject has the following affected model root: " + rootEObject);
        return Optional.ofNullable(rootEObject.eResource());
    }

    private EObject getAffectedElement(EChange<EObject> change) {
//        if (change instanceof AdditiveEChange<?,?>) {
//            AdditiveEChange additiveEChange = ((AdditiveEChange<?, ?>) change);
//            additiveEChange.
//        }
        if (change instanceof InsertRootEObject<?>) {
            InsertRootEObject insertRootEObject = ((InsertRootEObject<?>) change);
            return (EObject)insertRootEObject.getNewValue();
            //wenn das geht, kann ich eigentlich auch zwischen additiveEChange und Subtractive unterscheiden!
            //TODO das machen! besser als der dreck da unten
        }
        if (change instanceof UpdateReferenceEChange<?>) {
            UpdateReferenceEChange updateReferenceEChange = ((UpdateReferenceEChange<?>) change);
            return (EObject)updateReferenceEChange.getAffectedElement();

        }
        if (change instanceof UpdateSingleListEntryEChange<?,?>) {
            UpdateSingleListEntryEChange updateSingleListEntryEChange = (UpdateSingleListEntryEChange<?,?>) change;
            return (EObject)updateSingleListEntryEChange.getAffectedElement();
        }
        throw new RuntimeException("change not mappable to an EObject" + change);
    }

    private void debug(Object s) {
        logger.debug("[TGGChangePropagationSpecification] " + s);
    }
}

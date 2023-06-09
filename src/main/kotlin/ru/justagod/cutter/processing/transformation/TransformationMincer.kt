package ru.justagod.cutter.processing.transformation

import ru.justagod.cutter.mincer.processor.SubMincer
import ru.justagod.cutter.processing.base.AugmentedMincer
import ru.justagod.cutter.processing.config.CutterConfig
import ru.justagod.cutter.processing.model.ProjectModel
import ru.justagod.cutter.processing.transformation.validation.ValidationAugment
import ru.justagod.cutter.processing.transformation.validation.ValidationError
import ru.justagod.cutter.processing.transformation.validation.ValidationResult

class TransformationMincer(model: ProjectModel, config: CutterConfig) : AugmentedMincer<Unit, ValidationResult>() {

    init {
        register(MembersDeletionAugment(config, model))
        if (config.deleteAnnotations) register(AnnotationRemoverAugment(config.annotation))
    }

    private val validator = ValidationAugment(config, model).register()

    override fun endProcessing(input: Unit, output: ValidationResult): ValidationResult {
        return ValidationResult(validator.result)
    }

}
package com.blueprint.cubing.core.pipeline

import com.blueprint.cubing.core.model.CubeEvent
import com.blueprint.cubing.core.pipeline.base.PipelineNode

interface UiMapperNode: PipelineNode<CubeEvent, CubeEvent>
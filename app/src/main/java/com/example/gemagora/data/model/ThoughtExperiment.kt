package com.example.gemagora.data.model

data class ThoughtExperiment(
    val id: String,
    val title: String,
    val subtitle: String,
    val description: String,
    val classicAuthor: String,
    val defaultPremise: String,
    val variables: List<ExperimentVariable>
)

data class ExperimentVariable(
    val id: String,
    val name: String,
    val description: String,
    val minValue: Float,
    val maxValue: Float,
    val defaultValue: Float,
    val step: Int = 1,
    val isToggle: Boolean = false,
    val options: List<String>? = null
)

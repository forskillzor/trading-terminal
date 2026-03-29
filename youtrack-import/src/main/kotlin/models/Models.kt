package models

data class ProjectConfig(
    val project: ProjectInfo,
    val customFields: Map<String, CustomFieldConfig>,
    val components: List<Component>,
    val epics: List<Epic>,
    val sprints: List<Sprint>,
    val tasks: List<Task>,
    val tags: List<Tag>,
    val workflow: Workflow?,
    val agileBoard: AgileBoard?
)

data class ProjectInfo(
    val id: String,
    val name: String,
    val description: String,
    val lead: String,
    val template: String
)

data class CustomFieldConfig(
    val name: String,
    val type: String,
    val values: List<String>? = null,
    val default: String? = null
)

data class Component(
    val id: String,
    val name: String,
    val description: String
)

data class Epic(
    val id: String,
    val name: String,
    val description: String,
    val priority: String,
    val assignee: String = "",
    val sprints: List<String> = emptyList()
)

data class Sprint(
    val id: String,
    val name: String,
    val goal: String,
    val startDate: String,
    val endDate: String,
    val taskIds: List<String>,
    val epic: String,
    val capacity: Int
)

data class Task(
    val id: String,
    val name: String,
    val description: String,
    val type: String,
    val priority: String,
    val assignee: String = "",
    val epic: String = "",
    val components: List<String> = emptyList(),
    val acceptanceCriteria: List<String> = emptyList(),
    val notes: List<String> = emptyList(),
    val relatedFiles: List<String> = emptyList(),
    val dependencies: List<String> = emptyList()
)

data class Tag(
    val name: String,
    val color: String
)

data class Workflow(
    val name: String,
    val states: List<String>,
    val transitions: List<Transition>
)

data class Transition(
    val from: String,
    val to: String,
    val name: String
)

data class AgileBoard(
    val name: String,
    val type: String,
    val columns: List<BoardColumn>,
    val swimlanes: List<Swimlane>,
    val cardLayout: CardLayout
)

data class BoardColumn(
    val name: String,
    val states: List<String>
)

data class Swimlane(
    val name: String,
    val query: String
)

data class CardLayout(
    val fields: List<String>
)

data class FieldInfo(
    val id: String,
    val name: String,
    val fieldType: String,
    val localizedName: String
)
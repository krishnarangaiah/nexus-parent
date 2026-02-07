package app.dao.model.monitoring

import jakarta.persistence.*

/**
 * Agent Group Entity
 *
 * Represents a logical grouping of agents (e.g., "Production", "Staging", "Dev").
 */
@Entity
@Table(name = "agent_group")
class AgentGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id

    /** Unique group name */
    @Column(unique = true, nullable = false)
    String name

    /** Display name shown in UI */
    @Column(nullable = false)
    String displayName

    /** Description of this group */
    @Column(columnDefinition = "TEXT")
    String description

    /** Display order in UI */
    @Column
    Integer displayOrder = 0

    /** Color for UI display (hex code) */
    @Column
    String color = "#6c757d"

    /** Icon class (Bootstrap icons) */
    @Column
    String icon = "bi-collection"

    /** Is this group active */
    @Column
    Boolean active = true

    /** When the group was created */
    @Column
    Long createdAt = System.currentTimeMillis()

    @Override
    String toString() {
        return displayName ?: name
    }
}


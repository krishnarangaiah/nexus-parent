package app.entity.command

import jakarta.persistence.*

/**
 * Command Script Entity
 *
 * Represents a Groovy script that can be executed on agents.
 * Users can create, edit, and manage scripts through the UI.
 */
@Entity
@Table(name = "command_script")
class CommandScript {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id

    /** Unique script name/identifier */
    @Column(unique = true, nullable = false)
    String name

    /** Display name shown in UI */
    @Column(nullable = false)
    String displayName

    /** Description of what this script does */
    @Column(columnDefinition = "TEXT")
    String description

    /** The Groovy script content */
    @Column(columnDefinition = "TEXT", nullable = false)
    String scriptContent

    /** Script version (increment on each update) */
    @Column
    Integer version = 1

    /** Is this a built-in command (not deletable) */
    @Column
    Boolean builtIn = false

    /** Is this script active/enabled */
    @Column
    Boolean active = true

    /** Username who created this script */
    @Column
    String createdBy

    /** When the script was created */
    @Column
    Long createdAt = System.currentTimeMillis()

    /** Username who last updated this script */
    @Column
    String updatedBy

    /** When the script was last updated */
    @Column
    Long updatedAt

    /** Default timeout in milliseconds (0 = no timeout) */
    @Column
    Long defaultTimeoutMs = 30000L

    /** Category for organizing scripts */
    @Column
    String category = "General"

    @Override
    String toString() {
        return displayName ?: name
    }
}


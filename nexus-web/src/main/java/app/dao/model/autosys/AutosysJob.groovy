package app.dao.model.autosys

import app.dao.model.StaticUtils

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id

@Entity
class AutosysJob {

    private static final long serialVersionUID = 3327643027629528402L

    @Id
    @GeneratedValue
    Long id;

    @Column(nullable = false)
    String jobName

    @Override
    String toString() {
        StaticUtils.GSON.toJson(this)
    }
}

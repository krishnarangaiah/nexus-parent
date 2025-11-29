package app.dao.model.autosys

import app.dao.model.StaticUtils
import app.dao.model.user.Role
import com.google.gson.Gson

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id

@Entity
class AutosysDefinitionFile implements Serializable{

    private static final long serialVersionUID = 3327643027629528402L

    @Id
    @GeneratedValue
    Long id;
    @Column(unique = true, nullable = false)
    String absoluteFile

    @Override
    String toString() {
        StaticUtils.GSON.toJson(this)
    }

}

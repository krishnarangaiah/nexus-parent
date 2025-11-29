package app.dao.model.component

import app.dao.model.enums.Environment
import com.google.gson.Gson
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id

@Entity
class RatesComponent {

    private static final long serialVersionUID = 3327643027629528402L
    private static Gson gson = new Gson()

    @Id
    @GeneratedValue
    Long id
    @Column(unique = true, nullable = false)
    String componentName
    @Column(nullable = false)
    String description
    @Column(nullable = false)
    Environment environment
    @Column(nullable = false)
    Boolean hasSubprocess
    @Column(nullable = false)
    Boolean isActive
    @Column(nullable = false)
    String host
    @Column(nullable = false)
    String logFileLocation

}

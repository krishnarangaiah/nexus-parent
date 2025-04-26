package app.conf

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class AppProperty implements Serializable {

    private static final Logger LOGGER = LoggerFactory.getLogger(AppProperty.class)


    @Value('${app.name:Not Available}')
    String appName

    @Value('${app.admin.user:system}')
    String appAdminUser

    @Value('${app.admin.password:system}')
    transient String appAdminPassword

    @Value('${app.user.login.module:true}')
    boolean appUserLoginModule

    @Override
    String toString() {
        return (new ObjectMapper()).writeValueAsString(this)
    }
}
